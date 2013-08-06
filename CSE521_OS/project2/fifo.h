#ifndef __X_H_FIFO__
#define __X_H_FIFO__
#include <iostream>
#include <list>
#include "virtualmem.h"
//using namespace std;

class fifo: public virtualmem 
{
private:
  list<int> frames;
  

public:
  fifo(int framenumber);
  void access_pagelist(list<unsigned int> pagelist);
  int access_newpage(int page_number);
  void show();
  ~fifo();
};
#endif 
  
