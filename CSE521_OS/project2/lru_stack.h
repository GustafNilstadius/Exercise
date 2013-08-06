#ifndef __X_H_LRU_STACK__
#define __X_H_LRU_STACK__
#include <iostream>
#include <list>
#include "virtualmem.h"
using namespace std;

class lru_stack:public virtualmem 
{
private:
  list<int> frames;

public:
  lru_stack(int framenumber);
  void access_pagelist(list<unsigned int> pagelist);
  int access_newpage(int page_number);
  void show();
  ~lru_stack();
};
#endif
