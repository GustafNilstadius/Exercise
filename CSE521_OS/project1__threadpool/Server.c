#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <pthread.h>
#include <string.h>
#include <signal.h>
#include "Server.h"
#include "protocol.h"
#include "queue_schedule.h"
#include "minilog.h"

#define SUCCESS 0


struct Config sConfig;
//
//   listenerThread()
//     ListenerThread() accept the connection and add the request to the ready queue.
//
void listenerThread()
{
    int listen_fd, conn_fd;
    struct sockaddr_in serv_addr;

    char sendBuff[1025];
    //time_t ticks;

    listen_fd = 0;
    conn_fd   = 0;

    listen_fd = socket(AF_INET, SOCK_STREAM, 0);
    memset(&serv_addr, '0', sizeof(serv_addr));
    memset(sendBuff, '0', sizeof(sendBuff));

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    serv_addr.sin_port = htons(sConfig.port);

    bind(listen_fd, (struct sockaddr*)&serv_addr, sizeof(serv_addr));

    listen(listen_fd, 10);

    while(1)
    {
        conn_fd = accept(listen_fd, (struct sockaddr*)NULL, NULL);

        //Add the request to the ready queue here.
        // Select the appropriate data structure for the ready queue
	httpProtocol(conn_fd);

        //the following code will just return the local time
        //in server to the client	

	//ticks = time(NULL);
        //snprintf(sendBuff, sizeof(sendBuff), "%.24s\r\n", ctime(&ticks));
        //write(conn_fd, sendBuff, strlen(sendBuff));

        //close(conn_fd);

        //sleep(1);
     }

   close(listen_fd);
}


//
//  schedulerThread()
//     select the request from the ready queue and schedule it
//     for execution
//

//void schedulerThread()
//{
//}

static int my_daemon()
{
  pid_t pid;
  int fd;

  if ( (pid = fork()) < 0)
    return (-1);
  else if (pid)
    _exit(0); /* parent terminates */
  /* child 1 continues... */
  if (setsid() < 0) /* become session leader */
    return (-1);
  signal(SIGHUP, SIG_IGN);
  if ( (pid = fork()) < 0)
    return (-1);
  else if (pid)
    _exit(0); /* child 1 terminates */
  /* child 2 continues... */
  chdir("/");
  
  if ((fd = open("/dev/null", O_RDWR, 0)) != -1)
    { 
      dup2(fd, STDIN_FILENO); 
      dup2(fd, STDOUT_FILENO); 
      dup2(fd, STDERR_FILENO); 
    }
  
  return 0;
}

int init()
{
  pthread_t listener_t, sched_t;

  if(0 == sConfig.debug_mode)
    {
      //
      if(0!=strlen(sConfig.logFile))
	{
	  open_log(sConfig.logFile);
	}
      
      //daemonize
      //call my own version daemaon: only redirect 0,1,2;not close log file
      my_daemon();
    }
  else
    {
      enable_debug();
    }
	  

  if (workthreadpool_create(sConfig.threadNum, 6)!=0)
    {
      minilog ("create thread pool fail\n");
      close_log();
      exit(0);
    }

  if (init_queuescheduler(3, sConfig.schedPolicy)!=0)
    {
      minilog ("init_queuescheduler fail\n");
      workthreadpool_destroy();
      close_log();
      exit(0);
    }
  //create the listener thread
  if(pthread_create(&listener_t, NULL, (void*)queueThread, (void *)0) != 0)
    {
      minilog ("create queue thread fail\n");
      destroy_queuescheduler();
      workthreadpool_destroy();
      close_log();
      exit(0);
    }

  //create the scheduler thread
  if(pthread_create(&sched_t, NULL, (void*)schedulerThread, (void *)0) != 0)
    {
      minilog ("create scheduler thread fail\n");
      destroy_queuescheduler();
      workthreadpool_destroy();
      close_log();
      exit(0);
    }


  pthread_join(listener_t, NULL);
  pthread_join(sched_t, NULL);

  return SUCCESS;
}

static void display_usage()
{
  printf("myhttpd [-d] [-h] [-l file] [-p port] [-r dir] [-t time] [-n threadnum] [-s sched]\n");
  printf("-d enable debug mode\n");
  printf("-h output online help\n");
  printf("-l logfile\n");
  printf("-p server listening port\n");
  printf("-r root director\n");
  printf("-t scheduler sleep time\n");
  printf("-n thread number in pool\n");
  printf("-s FCFS or SJF\n");
  return;
}

int main(int argc, char *argv[])
{
  int c;

  memset(&sConfig,0,sizeof(sConfig));
  sConfig.debug_mode = 0;
  sConfig.port = 8080; //default port
  sConfig.threadNum = 4; //default thread count
  sConfig.queuingTime = 60; //second
  sConfig.schedPolicy = FCFS;
  getcwd(sConfig.rootDir,sizeof(sConfig.rootDir));

  //Parse the input arguments here and set the configuration data structure
  //  cout<<"\n Usage: %s [-d] [-h] [-l file] [-p port] [-r dir] [-t time] [-n threadnum] [-s sched] \n";
  while ((c = getopt (argc, argv, "dhl:p:r:t:n:s:")) != -1)
    {
         switch (c)
           {
           case 'd':
	     sConfig.debug_mode = 1;
             break;
           case 'l':
	     strncpy(sConfig.logFile,optarg,strlen(optarg));
             break;
	   case 'p':
	     sConfig.port = atoi(optarg);
	     break;
	   case 'r':
	     strncpy(sConfig.rootDir,optarg,strlen(optarg));
	     break;
	   case 't':
	     sConfig.queuingTime = atoi(optarg);
	     break;
	   case 'n':
	     sConfig.threadNum = atoi(optarg);
	     break;
	   case 's':
	     if(strcmp(optarg,"SJF")==0)
	       {
		 sConfig.schedPolicy=SJF;
	       }
	     else if(strcmp(optarg,"FCFS")==0)
	       {
		 sConfig.schedPolicy= FCFS;
	       }
	     else
	       {
		 printf("invalid schedule\n");
		 exit(0);
	       }
	     break;
	   case 'h':
	     display_usage();
	     exit(0);
	     break;
	   default:
	     display_usage();
	     exit(0);
	   }	     
    }

  getcwd(sConfig.cwd,sizeof(sConfig.cwd));
  init();

  return SUCCESS;
}
