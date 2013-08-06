
#include "fifo.h"
using namespace std;

  fifo::fifo(int framenumber)
    :virtualmem(framenumber)
  {
    
  }

  void fifo::access_pagelist(list<unsigned int> pagelist)
  {
    list<unsigned int>::iterator it;
    for(it=pagelist.begin(); it!= pagelist.end();++it)
      {
	access_newpage(*it);
      }
    return;
  }

  int fifo::access_newpage(int page_number)
  {
    list<int>::iterator it;
    if(frames.size()> max_frame_nbr)
      {
	return -1;
      }
    else if(frames.size()== max_frame_nbr)
      {
	bool found = false;
	for (it=frames.begin(); it!=frames.end(); ++it)
	  {
	    if(page_number == *it)
	      {
		//found
		found = true;
		break;
	      }
	  }
	// find a viticm, replace it
	if (false == found)
	  {
	    // fifo
	    frames.pop_back();
	    frames.push_front(page_number);
	    replace_nbr++;
	  }		
      }
    else
      {
	bool duplicate = false;
	for (it=frames.begin(); it!=frames.end(); ++it)
	  {
	    if(page_number == *it)
	      {
		duplicate = true;
		break;
	      }
	  }
	
	if(false==duplicate)
	  {
	    frames.push_front(page_number);
	  }
      }
    return 0;
  }
  
  void fifo::show()
  {
    list<int>::iterator it;
    cout << "physical frames:";
    for (it=frames.begin(); it!=frames.end(); ++it)
      {
	cout << *it << " ";
      }
    cout << endl;

  }  

  fifo::~fifo()
  {  
    frames.clear();
  }



