#ifndef __X_H_LRU_CLOCK__
#define __X_H_LRU_CLOCK__
#include <iostream>
#include <list>
#include "virtualmem.h"
#include "lru_clock_entry.h"
//http://www.yolinux.com/TUTORIALS/LinuxTutorialC++STL.html

class lru_clock:public virtualmem 
{
private:
  list<lru_clock_enty> frames;
  list<lru_clock_enty>::iterator current_ptr;

public:
  lru_clock(unsigned int framenumber);
  void access_pagelist(list<unsigned int> pagelist);
  int access_newpage(int page_number);
  void show();
  ~lru_clock();
};
#endif
