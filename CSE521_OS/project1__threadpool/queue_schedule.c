#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <netdb.h>
#include <pthread.h>
#include "protocol.h"
#include "Server.h"
#include "minilog.h"
#include "queue_schedule.h"
#include "poollib/threadpool.h"

typedef struct {
  httpsession *pSession;
  int priority;
} schedulenode_t;



struct queuescheduler_t {
  pthread_mutex_t lock;
  pthread_cond_t notify;
  schedulenode_t *pSchedulerEntity;
  int queue_size;
  int head;
  int tail;
  int count;
  int sched_policy;
};

static struct queuescheduler_t queuescheduler;
static threadpool_t *pThreadpool = NULL;

static int enqueue(httpsession *pSession,int priority); 
static void workerFunction(void * argument);

extern struct Config sConfig;

void static get_time_string(time_t raw_time, char *buffer)
{
  struct tm timeinfo;
  localtime_r(&raw_time,&timeinfo);
  strftime (buffer,80,"%d/%b/%Y:%H:%M:%S %z",&timeinfo);
  return;
}


/*
queue_size should <=10
sched_policy =0, FCFS
sched_policy =1, SJF
 */
int init_queuescheduler(int queue_size, int sched_policy)
{
  int ret = 0;

  if(queue_size <0 || queue_size > 10)
    {
      minilog("invalid queue size\n");
      ret = -1;
      return ret;
    }

  if((sched_policy != FCFS) && (sched_policy != SJF))
    {
      minilog("invalid sched policy\n");
      ret = -1;
      return ret;
    }
  
  memset(&queuescheduler,0,sizeof(queuescheduler));
  queuescheduler.queue_size = queue_size;
  queuescheduler.sched_policy = sched_policy;

  queuescheduler.pSchedulerEntity = malloc(queue_size *sizeof(schedulenode_t));

  if((pthread_mutex_init(&(queuescheduler.lock), NULL) != 0) ||
     (pthread_cond_init(&(queuescheduler.notify), NULL) != 0) ||
     (NULL == queuescheduler.pSchedulerEntity)) {
    if(NULL != queuescheduler.pSchedulerEntity)
      {
	free(queuescheduler.pSchedulerEntity);
	minilog("malloc queuescheduler  or init mutex/cond fail\n");
	return -1;
      }
  }

  memset(queuescheduler.pSchedulerEntity,0, queue_size *sizeof(schedulenode_t));
  
  return 0;  
}



void destroy_queuescheduler()
{
  if(NULL != queuescheduler.pSchedulerEntity)
    {
      free(queuescheduler.pSchedulerEntity);
    }
  return;
}


//allocate httpSession memory
//queue http session to the tail of queue
void queueThread()
{
    int listen_fd, conn_fd;
    struct sockaddr_in serv_addr;
    httpsession *pSession;
    int priority = 0;
    time_t raw_time;
    char time_buffer[128];
    //struct sockaddr client_addr;

    listen_fd = 0;
    conn_fd   = 0;

    listen_fd = socket(AF_INET, SOCK_STREAM, 0);
    memset(&serv_addr, '0', sizeof(serv_addr));

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    serv_addr.sin_port = htons(sConfig.port);

    bind(listen_fd, (struct sockaddr*)&serv_addr, sizeof(serv_addr));

    listen(listen_fd, 10);

    while(1)
    {
      //memset(&client_addr,0,sizeof(client_addr));
      conn_fd = accept(listen_fd, NULL, NULL);

      pSession = malloc(sizeof(httpsession));
      memset(pSession,0,sizeof(httpsession));

      /* if (client_addr.sa_family == AF_INET) {
	struct sockaddr_in *s = (struct sockaddr_in *)&client_addr;
	inet_ntop(AF_INET, &s->sin_addr, pSession->ipstr, INET_ADDRSTRLEN);
      } 
      else { // AF_INET6
	struct sockaddr_in6 *s = (struct sockaddr_in6 *)&client_addr;
	inet_ntop(AF_INET6, &s->sin6_addr, pSession->ipstr, INET6_ADDRSTRLEN );
      }
      */

      time(&raw_time);
      pSession->receive_time = raw_time;
      if((httpParseRequest(conn_fd, pSession))>=0)
	{
	  // set priority
	  // enqueue
	  if(SJF == queuescheduler.sched_policy)
	    {
	      priority = pSession->file_size;
	    }

	  get_time_string(raw_time, time_buffer);
	  //minilog("%s:",time_buffer);
	  //minilog("to enqueue %s,which length is %d\n",pSession->filepath,pSession->file_size);

	  if(0 != enqueue(pSession,priority))
	    {
	      free(pSession);
	      close(conn_fd);
	    }
	    
	}
      else
	{
	  free(pSession);
	  close(conn_fd);
	}
    }

   close(listen_fd);

}

/*

return 0:success
return other value: fail
*/
static int enqueue(httpsession *pSession,int priority)
{
  int ret = 0;
  int next;

  if(pthread_mutex_lock(&(queuescheduler.lock)) != 0) {
    minilog("lock fail\n"); 
    ret = -1;
    return ret;
    }

  next = queuescheduler.tail + 1;
  next = (next == queuescheduler.queue_size) ? 0: next;

  do{
    //full?
    if(queuescheduler.count == queuescheduler.queue_size)
      {
	ret = -1;
	break;
      }

    /*add to queue*/
    queuescheduler.pSchedulerEntity[queuescheduler.tail].pSession = pSession;
    queuescheduler.pSchedulerEntity[queuescheduler.tail].priority = priority;
    queuescheduler.tail = next;
    queuescheduler.count += 1;
    

    if(pthread_cond_signal(&(queuescheduler.notify))!=0) {
      minilog("notify error\n");
      ret =-1;
      break;
    }
  }while(0);

  
  if(pthread_mutex_unlock(&(queuescheduler.lock)) != 0){
    minilog("unlock error");
    ret = -1;
  }
  return ret;
}


void schedulerThread()
{
  httpsession *pSession;

  int highest,location;
  int tempPri;
  httpsession *tempSession = NULL;
  time_t raw_time;
  char time_buffer[128];

  //sleep
  sleep(sConfig.queuingTime);

  //minilog("scheulder Thread wake up\n");

  //dequeue http session from the head of queue
  //then dispatch to worker threads
  for (;;) {
    pthread_mutex_lock(&(queuescheduler.lock));

    while(queuescheduler.count == 0)
      {
	pthread_cond_wait (&(queuescheduler.notify),&(queuescheduler.lock));
      }

     if (SJF == queuescheduler.sched_policy)
      {
	//SJF: short job processed earlier
	//scan the queue, find the highest priority, move it to the head, O(n) search
	highest = queuescheduler.head;
	
	if(queuescheduler.tail> queuescheduler.head)
	  {
	    for (location=queuescheduler.head;location<queuescheduler.tail;location++)
	      {
		if( queuescheduler.pSchedulerEntity[highest].priority > queuescheduler.pSchedulerEntity[location].priority)
		  {
		    highest = location;
		  }
	      }
	  }
	else if(queuescheduler.tail <= queuescheduler.head)
	  {
	    for (location=queuescheduler.head;location<queuescheduler.queue_size;location++)
	      {
		if( queuescheduler.pSchedulerEntity[highest].priority> queuescheduler.pSchedulerEntity[location].priority)
		  {
		    highest = location;
		  }
	      } 

	    for (location=0;location<queuescheduler.tail;location++)
	      {
		if( queuescheduler.pSchedulerEntity[highest].priority > queuescheduler.pSchedulerEntity[location].priority)
		  {
		    highest = location;
		  }
	      }
	  
	  }

	
	if(highest != queuescheduler.head)
	  {
	    //swap highestpriority and head
	    minilog("To move highest priority %d at location %d to head\n",queuescheduler.pSchedulerEntity[highest].priority,highest);
	    
	    tempPri = queuescheduler.pSchedulerEntity[queuescheduler.head].priority;
	    tempSession = queuescheduler.pSchedulerEntity[queuescheduler.head].pSession;


	    queuescheduler.pSchedulerEntity[queuescheduler.head].priority = queuescheduler.pSchedulerEntity[highest].priority;
	    queuescheduler.pSchedulerEntity[queuescheduler.head].pSession = queuescheduler.pSchedulerEntity[highest].pSession;


	    queuescheduler.pSchedulerEntity[highest].priority = tempPri;	
	    queuescheduler.pSchedulerEntity[highest].pSession = tempSession;
	  }
      }
    
    pSession = queuescheduler.pSchedulerEntity[queuescheduler.head].pSession;
    queuescheduler.head += 1;
    queuescheduler.head = (queuescheduler.head == queuescheduler.queue_size)? 0 : queuescheduler.head;
    queuescheduler.count -= 1;

    pthread_mutex_unlock(&(queuescheduler.lock));

    //minilog("%s - ",pSession->ipstr);
    get_time_string(pSession->receive_time,time_buffer);
    minilog(" [%s] ",time_buffer);
    time(&raw_time);
    get_time_string(raw_time, time_buffer);
    minilog(" [%s] ",time_buffer);
    minilog(" \"%s\" ",pSession->requestLine);
    minilog("%d ", pSession->response_status);
    minilog("%d\n",pSession->file_size);
    /*minilog("scheduler get %s,which length is %d, send to work pool\n",pSession->filepath,pSession->file_size);*/

    // pass to worker threads
    threadpool_add(pThreadpool,workerFunction,pSession,0);
  }  
}


// there are "n" worker threads
static void workerFunction(void * argument)
{
  httpsession *pSession;
  
  pSession = (httpsession*)argument;

  httpCapsulateResponse(pSession);
  httpSendResponse(pSession);

  /*minilog("wokderFunction:send back%s,which length is %d\n",pSession->filepath,pSession->file_size);*/

  // freeup httpsession memory 
  close(pSession->fd);
  free(pSession);
}


int workthreadpool_create(int thread_count, int queue_size)
{
  pThreadpool = threadpool_create(thread_count, queue_size, 0);
  if(NULL == pThreadpool)
    {
      minilog("create thread pool fail\n");
      return -1;
    }
  return 0;
}

int workthreadpool_destroy()
{
  if(threadpool_destroy(pThreadpool,0)<0)
    {
      minilog("destroy thread pool fail\n");
      return -1;
    }
  return 0;
}
