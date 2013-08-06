#include "lfu.h"
using namespace std;


lfu::lfu(int framenumber)
    :virtualmem(framenumber)
  {
    
  }

  void lfu::access_pagelist(list<unsigned int> pagelist)
  {
    list<unsigned int>::iterator it;
    for(it=pagelist.begin(); it!= pagelist.end();++it)
      {
	access_newpage(*it);
      }
    return;
  }

//http://www.yolinux.com/TUTORIALS/CppStlMultiMap.html
  int lfu::access_newpage(int page_number)
  {
    list<int>::iterator it;
    bool duplicate = false;

    if(frames.size()> max_frame_nbr)
      {
	return -1;
      }
    else
      {	
	for (it=frames.begin(); it!=frames.end(); ++it)
	  {
	    if(page_number == (*it))
	      {
		duplicate = true;
		break;
	      }
	  }
	
      }
    if(!duplicate)
      {
	if(frames.size()<max_frame_nbr)
	  {
	    frames.push_back(page_number);
	  }
	else
	  {
	    //lookup a victim
	    list<int>::iterator least=frames.begin();
	    int counter = page.count(*least);
	    for (it=frames.begin(); it!=frames.end(); ++it)
	      {
		if(counter>page.count(*it))
		  {
		    least = it;
		  }
	      }
	    //replace
	    *least = page_number;
	    replace_nbr++;	    
	  }
      }

    page.insert(page_number);
    //cout<<"debug info ";
    //show();
  }


 void lfu::show()
  {
    list<int>::iterator it;
    
    cout << "physical frames:";
    for (it=frames.begin(); it!=frames.end(); ++it)
      {
	cout << *it << ":counter "<<page.count(*it)<<" ";
      }
    cout << endl;

  }  

  lfu::~lfu()
  {  
    frames.clear();
    page.clear();
  }
