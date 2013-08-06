#ifndef __X_H_LRU_REF8__
#define __X_H_LRU_REF8__
#include <iostream>
#include <list>
#include "virtualmem.h"
#include "lru_clock_entry.h"
//http://www.yolinux.com/TUTORIALS/LinuxTutorialC++STL.html

class lru_ref8:public virtualmem 
{
private:
  list<lru_clock_enty> frames;

public:
  lru_ref8(unsigned int framenumber);
  void access_pagelist(list<unsigned int> pagelist);
  int access_newpage(int page_number);
  void show();
  ~lru_ref8();
};
#endif
