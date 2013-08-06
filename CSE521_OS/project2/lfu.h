#ifndef __X_H_LFU__
#define __X_H_LFU__
#include <iostream>
#include <list>
#include <set>
#include "virtualmem.h"
class lfu: public virtualmem 
{
private:
  list<int> frames;
  multiset<int> page;
  

public:
  lfu(int framenumber);
  void access_pagelist(list<unsigned int> pagelist);
  int access_newpage(int page_number);
  void show();
  ~lfu();
};

#endif

