#ifndef __X_H_OPTIMAL__
#define __X_H_OPTIMAL__
#include <iostream>
#include <list>
#include "virtualmem.h"
//using namespace std;

class optimal: public virtualmem 
{
private:
  list<int> frames;
  

public:
  optimal(int framenumber);
  void access_pagelist(list<unsigned int> pagelist);
  int access_newpage(int page_number);
  void show();
  ~optimal();
};
#endif
