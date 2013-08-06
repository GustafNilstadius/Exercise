#include "lru_clock.h"
using namespace std;

lru_clock::lru_clock(unsigned int framenumber)
    :virtualmem(framenumber)
  {
    
  }

 void lru_clock::access_pagelist(list<unsigned int> pagelist)
  {
    list<unsigned int>::iterator it;
    for(it=pagelist.begin(); it!= pagelist.end();++it)
      {
	access_newpage(*it);
      }
    return;
  }

//http://www.cs.columbia.edu/~junfeng/os/lectures/l19-vm.pdf
  int lru_clock::access_newpage(int page_number)
  {
    list<lru_clock_enty>::iterator it;
    lru_clock_enty node;
    bool replaced = false;

    if(frames.size()> max_frame_nbr)
      {
	return -1;
      }
    else if(frames.size()== max_frame_nbr)
      {
	bool found = false;
	for (it=frames.begin(); it!=frames.end(); ++it)
	  {
	    if(page_number == (*it).frame)
	      {
		//found
		found = true;
		(*it).reference = 1;
		break;
	      }
	  }
	//need to find a viticm, replace it
	if (false == found)
	  {
	  
	    for (it=current_ptr; it!=frames.end(); ++it)
	      {
		if(0 == (*it).reference)
		  {	
		    //replace
		    (*it).frame = page_number;
		    (*it).reference =0 ;
		    replace_nbr++;
		    current_ptr = ++it;
		    if(frames.end()==current_ptr)
		      {
			current_ptr = frames.begin();
		      }

		    replaced = true;
		    break;
		  }
		else
		  {
		    (*it).reference = 0;
		  }
	      }

	if(false == replaced)
	  {
	    // continue to scan from begin
	    for (it=frames.begin(); it!=frames.end(); ++it)
	      {
		if(0 == (*current_ptr).reference)
		  {
		    //replace
		    (*it).frame = page_number;
		    (*it).reference =0;
		    replace_nbr++;
		    current_ptr = ++it;
		    if(frames.end()==current_ptr)
		      {
			current_ptr = frames.begin();
		      }

		    replaced = true;
		    break;
		  }
		else
		  {
		    (*it).reference = 0;
		  }
	      }

	  }
      }		
  }
    else
      {
	lru_clock_enty node;
	bool duplicate = false;

	for (it=frames.begin(); it!=frames.end(); ++it)
	  {
	    if(page_number == (*it).frame)
	      {
		duplicate = true;
		(*it).reference = 1;
		break;
	      }
	  }
	if(false==duplicate)
	  {
	    node.frame = page_number;
	    node.reference = 0;
	    frames.push_front(node);
	  }

	current_ptr = frames.begin();
      }
    //show();
    return 0;
  }
  
  void lru_clock::show()
  {
    list<lru_clock_enty>::iterator it;
    cout << "lru clock current_ptr:" <<(*current_ptr).frame<<endl;
    
    cout << "physical frames:"<<endl;
    for (it=frames.begin(); it!=frames.end(); ++it)
      {
	cout << *it << " ";
      }
    
    cout <<endl;
  }  

  lru_clock::~lru_clock()
  {  
    frames.clear();
  }
