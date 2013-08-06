#include "fifo.h"
#include "lru_stack.h"
#include "lru_clock.h"
#include "lru_ref8.h"
#include "lfu.h"
#include "optimal.h"
#include<stdio.h>
#include<stdlib.h>
#include<sys/types.h>
#include<sys/time.h>
#include <unistd.h>
#include <iostream>
#include <fstream>
#include <sstream>


void test()
{
  fifo fifo(3);

  fifo.access_newpage(1);
  fifo.access_newpage(2);
  fifo.access_newpage(3);
  fifo.access_newpage(1);
  cout <<"fifo:";
  fifo.show();
  fifo.access_newpage(4);
  fifo.show();

  cout <<"lru_stack:";
  lru_stack lru_stack(3);

  lru_stack.access_newpage(1);
  lru_stack.access_newpage(2);
  lru_stack.access_newpage(3);
  lru_stack.access_newpage(1);
  lru_stack.show();
  lru_stack.access_newpage(4);
  lru_stack.show();

  cout <<"lru_clock:";
  lru_clock lru_clock(4);
  lru_clock.access_newpage(1);
  lru_clock.access_newpage(2);
  lru_clock.access_newpage(3);
  lru_clock.access_newpage(4);
  lru_clock.access_newpage(1);
  lru_clock.access_newpage(2);
  lru_clock.access_newpage(5);
  
  lru_clock.access_newpage(1);
  lru_clock.access_newpage(2);
  lru_clock.access_newpage(3);
  lru_clock.access_newpage(4);
  lru_clock.access_newpage(5);

  cout <<"lru_ref8:";
  lru_ref8 lru_ref8(4);
  lru_ref8.access_newpage(1);
  lru_ref8.access_newpage(2);
  lru_ref8.access_newpage(3);
  lru_ref8.access_newpage(4);
  lru_ref8.access_newpage(1);
  lru_ref8.access_newpage(2);
  lru_ref8.access_newpage(5);
  
  lru_ref8.access_newpage(1);
  lru_ref8.access_newpage(2);
  lru_ref8.access_newpage(3);
  lru_ref8.access_newpage(4);
  lru_ref8.access_newpage(5);

  cout <<"lfu:";
  lfu lfu(4);
  lfu.access_newpage(1);
  lfu.access_newpage(2);
  lfu.access_newpage(3);
  lfu.access_newpage(4);
  lfu.access_newpage(1);
  lfu.access_newpage(2);
  lfu.access_newpage(5);
  
  lfu.access_newpage(1);
  lfu.access_newpage(2);
  lfu.access_newpage(3);
  lfu.access_newpage(4);
  lfu.access_newpage(5);
}

static void display_usage()
{
  cout << "virtualmem [−h] [-f available-frames] [−r replacement-policy] [−i input_file]"<<endl;
  return;
}

enum REPL_POLICY
  {
    FIFO = 0,
    LFU  = 1,
    LRU_STACK = 2,
    LRU_CLOCK = 3,
    LRU_REF8  = 4
  };



int main(int argc, char *argv[])
{
  //test();
  int c;
  unsigned int available_frames=5;
  int policy = FIFO;
  string filename,policy_str("FIFO");

  ifstream file;
  string line;
  unsigned int num;
  list<unsigned int> pagelist;
  int line_number =0, word_number;

  struct timeval org_tv,later_tv;
  int actual_ms,optimal_ms,actual_replace=0,optimal_replace;

  if(argc==1)
    {
      display_usage();
      exit(0);
    }
  while ((c = getopt (argc, argv, "hf:r:i:")) != -1)
    {
      switch(c)
	{

        case 'f':
	  available_frames = atoi(optarg);
	  if(available_frames<=0)
	    {
	      display_usage();
	      exit(1);
	    }
	  break;
	case 'r':
	  policy_str = optarg;
	  if(strcmp(optarg,"FIFO")==0)
	    {
	      policy = FIFO;
	    }
	  else if(strcmp(optarg,"LFU")==0)
	    {
	      policy = LFU;
	    }
	  else if(strcmp(optarg,"LRU_STACK")==0)
	    {
	      policy = LRU_STACK;
	    }
	  else if(strcmp(optarg,"LRU_CLOCK")==0)
	    {
	      policy = LRU_CLOCK;
	    }
	  else if(strcmp(optarg,"LRU_REF8")==0)
	    {
	      policy = LRU_REF8;
	    }
	  else 
	    {
	      display_usage();
	      exit(1);
	    }
	 
	  break;
	case 'i':
	  filename = optarg;
	  break;
	case 'h':
	default:
	  display_usage();
	  exit(0);
	}
    }

  if(!filename.empty())
    {
      file.exceptions ( ifstream::failbit | ifstream::badbit );
      try {
	file.open (filename.c_str());
      }
      catch (ifstream::failure e) {
	cout << "Exception opening/reading file";
	exit(0);
      }
      file.exceptions ( ifstream::goodbit);
  

      //while (!file.eof()) file.get();
      while (getline(file, line))
	{
	  istringstream iss(line,istringstream::in);
	  line_number++;
	  word_number=1; 
      
	  while ((iss >> num))
	    {
	      word_number++;
	      //cout<<num <<" ";
	      pagelist.push_back(num);
	    }
     
	  if(iss.eof())
	    {
	      //cout <<"only end of line";
	    }
	  else if(iss.fail())
	    {
	      cout <<"line"<< line_number<<" word" <<word_number <<" :invalid page number";
	      pagelist.clear();
	      exit(1);
	    }

	}

      file.close();
    }
  else
    {  
      getline(cin, line);  
      istringstream iss(line,istringstream::in);
      word_number =1;
      while ((iss >> num))
	{
	  word_number++;
	  //cout<<num <<" ";
	  pagelist.push_back(num);
	}
     
      if(iss.eof())
	{
	  //cout <<"only end of line";
	}
      else if(iss.fail())
	{
	  cout <<"word" <<word_number <<" :invalid page number";
	  pagelist.clear();
	  exit(0);
	}
    }

  {
    fifo fifo(available_frames);
    lru_stack lru_stack(available_frames);
    lru_clock lru_clock(available_frames);
    lru_ref8 lru_ref8(available_frames);
    lfu lfu(available_frames);
    optimal optimal(available_frames);
    
     gettimeofday(&org_tv, NULL);
    switch(policy)
      {
      case FIFO:
	fifo.access_pagelist(pagelist);
	actual_replace= fifo.replace_nbr;
	break;
      case LFU:
	lfu.access_pagelist(pagelist);
	actual_replace= lfu.replace_nbr;
	break;
      case LRU_STACK:
	lru_stack.access_pagelist(pagelist);
	actual_replace= lru_stack.replace_nbr;
	break;
      case LRU_CLOCK:
	lru_clock.access_pagelist(pagelist);
	actual_replace= lru_clock.replace_nbr;
	break;
      case LRU_REF8:
	lru_ref8.access_pagelist(pagelist);
	actual_replace= lru_ref8.replace_nbr;
	break;
      default:
	break;
      }
    gettimeofday(&later_tv, NULL);
    actual_ms = (later_tv.tv_sec - org_tv.tv_sec)*1000000+(later_tv.tv_usec-org_tv.tv_usec);
    
    
    gettimeofday(&org_tv, NULL);
    optimal.access_pagelist(pagelist);
    optimal_replace = optimal.replace_nbr;
    gettimeofday(&later_tv, NULL);
    optimal_ms = (later_tv.tv_sec - org_tv.tv_sec)*1000000+(later_tv.tv_usec-org_tv.tv_usec);

    cout<<endl;
    cout<<"# of page replacements with "<< policy_str<<" :" << actual_replace<<endl;
    cout<<"# of page replacements with "<<"Optimal"<<" :" << optimal_replace<<endl;
    if(0!= optimal_replace)
      {
	cout.precision(3);
	cout << "% page replacement penalty using " << policy_str <<" "<<100.0*(actual_replace-optimal_replace)/optimal_replace <<"%"<<endl;
      }
    cout<<endl;

    cout<<"Total time to run " <<policy_str<<" algorithm: "<< actual_ms<<"ms"<<endl;
    cout<<"Total time to run " <<"Optimal"<<" algorithm: "<< optimal_ms<<"ms"<<endl;

    if(0!= optimal_ms)
      {
	cout.precision(3);
	if(actual_ms < optimal_ms)
	  {	    
	    cout << policy_str <<" is "<<100.0*(optimal_ms-actual_ms)/optimal_ms<<"%"<<" faster than Optimal algorithm."<<endl;
	  }
	else
	  {
	    cout << policy_str <<" is "<<100.0*(actual_ms-optimal_ms)/actual_ms<<"%"<<" slower than Optimal algorithm." <<endl;
	  }
      }

  }

  pagelist.clear();
  return 0;
}
