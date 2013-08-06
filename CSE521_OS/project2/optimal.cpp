#include "optimal.h"
using namespace std;


optimal::optimal(int framenumber)
    :virtualmem(framenumber)
  {
    
  }


void optimal::access_pagelist(list<unsigned int> pagelist)
  {
    list<unsigned int>::iterator page_it;
    list<int>::iterator it;

    for(page_it = pagelist.begin(); page_it != pagelist.end();++page_it)
      {
	//*page_it is new page
	bool found = false;
	for (it=frames.begin(); it!=frames.end(); ++it)
	  {
	    if(*it == *page_it)
	      {
		//found
		found = true;
		break;
	      }
	  }

	if(false == found)
	  {
	     if(frames.size() < max_frame_nbr)
	       {
		 frames.push_back(*page_it);
	       }
	     else
	       {
		 list<unsigned int>  locationlist;
		 list<unsigned int>::iterator location_it;
		 unsigned int loc=0,farest_loc=0;
		 
		 //find a victim
		 //scan_page_it point to the future pages
		 list<unsigned int>::iterator scan_page_it;

		 list<int>::iterator replaced_page;
		 for (it=frames.begin(); it!=frames.end(); ++it)
		   {	
		     for(scan_page_it=page_it,loc=0;scan_page_it!=pagelist.end(); ++scan_page_it,++loc)
		       {
			 if((*scan_page_it) ==(*it))
			   {
			     locationlist.push_back(loc);
			     break;
			   }
		       }
		     
		     if(pagelist.end()==scan_page_it)
		       {
			 locationlist.push_back(0xFFFFFFFF);
			 break;
		       }
		   }

		 replaced_page= frames.begin();
		 farest_loc= *locationlist.begin();
		 for(location_it = locationlist.begin(),it=frames.begin();location_it!=locationlist.end();++location_it,++it)
		   {
		     if(*location_it> farest_loc)
		       {
			 farest_loc = *location_it;
			 replaced_page = it;
		       }
		   }


		 (*replaced_page)=(*page_it);
		 replace_nbr++;
	       }
	  }
	
	//show();
      }

    
  }

int optimal::access_newpage(int page_number)
{
  return 0;
}


void optimal::show()
{
    list<int>::iterator it;
    cout << "physical frames:";
    for (it=frames.begin(); it!=frames.end(); ++it)
      {
	cout << *it << " ";
      }
    cout << endl;
}


  optimal::~optimal()
  {  
    frames.clear();
  }
