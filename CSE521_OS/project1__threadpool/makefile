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
CC = gcc
CFLAGS  = -g -Wall
LDFLAGS = -lpthread

# typing 'make' will invoke the first target entry in the file 
# (in this case the default target entry)
#
default: myhttpd


myhttpd:          Server.o protocol.o minilog.o queue_schedule.o poollib/threadpool.o
		$(CC) $(LDFLAGS) -o myhttpd Server.o protocol.o minilog.o queue_schedule.o poollib/threadpool.o


Server.o:	Server.c protocol.h minilog.h Server.h queue_schedule.h
		$(CC) $(CFLAGS) -c Server.c

protocol.o:	protocol.c protocol.h minilog.h Server.h
		$(CC) $(CFLAGS) -c protocol.c

minilog.o:	minilog.c minilog.h
		$(CC) $(CFLAGS) -c minilog.c

queue_schedule.o:	queue_schedule.c queue_schedule.h minilog.h poollib/threadpool.h
		$(CC) $(CFLAGS) -c queue_schedule.c


# To start over from scratch, type 'make clean'.  This
# removes the executable file, as well as old .o object
# files and *~ backup files:
#
clean:
		rm -f myhttpd *.o *~

