
#include "lru_stack.h"
using namespace std;

lru_stack::lru_stack(int framenumber)
    :virtualmem(framenumber)
  {
    
  }

  void lru_stack::access_pagelist(list<unsigned int> pagelist)
  {
    list<unsigned int>::iterator it;
    for(it=pagelist.begin(); it!= pagelist.end();++it)
      {
	access_newpage(*it);
      }
    return;
  }

  int lru_stack::access_newpage(int page_number)
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
		frames.erase(it);
		frames.push_front(page_number);
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
  
  void lru_stack::show()
  {
    list<int>::iterator it;
    cout << "physical frames:";
    for (it=frames.begin(); it!=frames.end(); ++it)
      {
	cout << *it << " ";
      }
    cout << endl;

  }  

  lru_stack::~lru_stack()
  {  
    frames.clear();
  }



