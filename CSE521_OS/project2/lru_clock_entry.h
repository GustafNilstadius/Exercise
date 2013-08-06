#ifndef __X_H_LRU_CLOCK_ENTRY__
#define __X_H_LRU_CLOCK_ENTRY__
#include <bitset>
#include <iostream>
using namespace std;
//http://www.yolinux.com/TUTORIALS/LinuxTutorialC++STL.html


class lru_clock_enty
{
   friend ostream &operator<<(ostream &, const lru_clock_enty &);

   public:
      unsigned int frame;
      unsigned char reference;

      lru_clock_enty();
      lru_clock_enty(const lru_clock_enty &);
      ~lru_clock_enty(){};
      lru_clock_enty &operator=(const lru_clock_enty &rhs);
      int operator==(const lru_clock_enty &rhs) const;
      int operator<(const lru_clock_enty &rhs) const;
};



inline lru_clock_enty::lru_clock_enty()   // Constructor
{
   frame = 0;
   reference = 0;
}

inline lru_clock_enty::lru_clock_enty(const lru_clock_enty &copyin)   // Copy constructor to handle pass by value.
{                             
   frame = copyin.frame;
   reference = copyin.reference;
}

inline ostream &operator<<(ostream &output, const lru_clock_enty &aaa)
{
  output <<"frame:"<< aaa.frame << ' '<<"reference:" << std::bitset<8>(aaa.reference) << ' ' << endl;
   return output;
}

inline lru_clock_enty& lru_clock_enty::operator=(const lru_clock_enty &rhs)
{
   this->frame = rhs.frame;
   this->reference = rhs.reference;
   return *this;
}

inline int lru_clock_enty::operator==(const lru_clock_enty &rhs) const
{
   return 1;
}

// This function is required for built-in STL list functions like sort
inline int lru_clock_enty::operator<(const lru_clock_enty &rhs) const
{
   return 0;
}

#endif
