#ifndef __X_H_VIRTUAL__
#define __X_H_VIRTUAL__
#include <iostream>
#include <list>
using namespace std;

class virtualmem
{
 public:
  unsigned int max_frame_nbr;
  unsigned int replace_nbr;

 public:
  virtualmem (int framenumber);

  virtual int access_newpage(int page_number) = 0;

  virtual void access_pagelist(list<unsigned int> pagelist)=0;

  virtual void show()=0;

};


#endif
