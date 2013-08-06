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
default: dec_server dec_client


dec_server:     server.o analyze.o graph.o minilog.o
		$(CC) $(LDFLAGS) -o dec_server server.o analyze.o graph.o minilog.o

dec_client:	client.o
		$(CC) $(LDFLAGS) -o dec_client client.o	


server.o:	server.c  analyze.h parameter.h minilog.h
		$(CC) $(CFLAGS) -c server.c

client.o:	client.c parameter.h
		$(CC) $(CFLAGS) -c client.c

analyze.o:	analyze.cpp graph.h analyze.h minilog.h
		$(CC) $(CFLAGS) -c analyze.cpp

graph.o:	graph.cpp graph.h
		$(CC) $(CFLAGS) -c graph.cpp

minilog.o:	minilog.c minilog.h
		$(CC) $(CFLAGS) -c minilog.c

# To start over from scratch, type 'make clean'.  This
# removes the executable file, as well as old .o object
# files and *~ backup files:
#
clean:
		rm -f dec_server dec_client *.o *~

