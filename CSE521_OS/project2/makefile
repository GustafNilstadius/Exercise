#
# This is an example Makefile for a countwords program.  This
# program uses both the scanner module and a counter module.
# Typing 'make' or 'make count' will create the executable file.
#

# define some Makefile variables for the compiler and compiler flags
# to use Makefile variables later in the Makefile: $()
#
#  -g    adds debugging information to the executable file
#  -Wall turns on most, but not all, compiler warnings
#
CC = g++
CFLAGS  = -g -Wall

# typing 'make' will invoke the first target entry in the file 
# (in this case the default target entry)
#
default: virtualmem


virtualmem:        fifo.o lru_stack.o lru_clock.o lru_ref8.o lfu.o optimal.o main.o virtualmem.o
		$(CC) $(LDFLAGS) -o virtualmem fifo.o lru_stack.o lru_clock.o lru_ref8.o lfu.o optimal.o virtualmem.o main.o

fifo.o:		fifo.cpp fifo.h virtualmem.h
		$(CC) $(CFLAGS) -c fifo.cpp

lru_stack.o:	lru_stack.cpp lru_stack.h virtualmem.h
		$(CC) $(CFLAGS) -c lru_stack.cpp

lru_clock.o:	lru_clock.cpp lru_clock.h lru_clock_entry.h virtualmem.h
		$(CC) $(CFLAGS) -c lru_clock.cpp

lru_ref8.o:	lru_ref8.cpp lru_ref8.h lru_clock_entry.h virtualmem.h
		$(CC) $(CFLAGS) -c lru_ref8.cpp

lfu.o:		lfu.cpp lfu.h virtualmem.h
		$(CC) $(CFLAGS) -c lfu.cpp

optimal.o:	optimal.cpp optimal.h virtualmem.h
		$(CC) $(CFLAGS) -c optimal.cpp

virtualmem.o:	virtualmem.cpp virtualmem.h
		$(CC) $(CFLAGS) -c virtualmem.cpp

main.o:		main.cpp fifo.h
		$(CC) $(CFLAGS) -c main.cpp

# To start over from scratch, type 'make clean'.  This
# removes the executable file, as well as old .o object
# files and *~ backup files:
#
clean:
		rm -f virtualmem *.o *~

