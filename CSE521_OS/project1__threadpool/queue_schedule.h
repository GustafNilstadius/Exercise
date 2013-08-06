#ifndef _QUEUE_SCHEDULE_H_
#define _QUEUE_SCHEDULE_H_

enum SCHED_POLICY
  {
    FCFS = 0,
    SJF  = 1
  };


int init_queuescheduler(int queue_size, int sched_policy);
void destroy_queuescheduler();
int workthreadpool_create(int thread_count, int queue_size);
int workthreadpool_destroy();

void queueThread();
void schedulerThread();

#endif /*_QUEUE_SCHEDULE_H_*/
