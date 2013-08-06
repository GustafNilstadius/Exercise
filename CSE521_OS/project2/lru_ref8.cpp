#include "lru_ref8.h"
using namespace std;

lru_ref8::lru_ref8(unsigned int framenumber)
    :virtualmem(framenumber)
  {
    
  }

void lru_ref8::access_pagelist(list<unsigned int> pagelist)
  {
    list<unsigned int>::iterator it;
    for(it=pagelist.begin(); it!= pagelist.end();++it)
      {
	access_newpage(*it);
      }
    return;
  }

  int lru_ref8::access_newpage(int page_number)
  {
    list<lru_clock_enty>::iterator it;

    if(frames.size()> max_frame_nbr)
      {
	return -1;
      }
    else
      {
	lru_clock_enty node;
	bool duplicate = false;

	// first right shift 1 bit
	 for (it=frames.begin(); it!=frames.end(); ++it)
	   {
	     (*it).reference /= 2;
	   }
	 
	 //lookup, if duplicate
	 for (it=frames.begin(); it!=frames.end(); ++it)
	  {
	    if(page_number == (*it).frame)
	      {
		duplicate = true;
		break;
	      }
	  }
	 
	 if(duplicate)
	   {
	     (*it).reference = ((*it).reference) | 0x80;
	   }
	 else
	   {	 
	     if(frames.size()<max_frame_nbr)
	       {
		 //then insert to the end
		 node.frame = page_number;
		 node.reference = 0x80;
		 frames.push_back(node);
	       }
	     else
	       {
		 //find a victim, and repalce it
		 list<lru_clock_enty>::iterator least=frames.begin();
		 for (it=frames.begin(); it!=frames.end(); ++it)
		   {
		     if((*least).reference>(*it).reference)
		       {
			 least = it;
		       }
		   }

		 (*least).reference = 0x80;
		 (*least).frame = page_number;
		 replace_nbr++;
	       }
	   }
      }
    //show();
    return 0;
  }
  
  void lru_ref8::show()
  {
    list<lru_clock_enty>::iterator it;
    cout << "lru ref8:"<<endl;
    
    cout << "physical frames:"<<endl;
    for (it=frames.begin(); it!=frames.end(); ++it)
      {
	cout << *it << " ";
      }
    
    cout <<endl;
  }  

  lru_ref8::~lru_ref8()
  {  
    frames.clear();
  }
