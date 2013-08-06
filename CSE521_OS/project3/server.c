


#include <sys/socket.h>       /*  socket definitions        */
#include <sys/types.h>        /*  socket types              */
#include <arpa/inet.h>        /*  inet (3) funtions         */
#include <unistd.h>           /*  misc. UNIX functions      */
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "analyze.h"
#include "parameter.h"
#include "minilog.h"

static void display_usage()
{
  printf("decserver [-h] [-l logfile] [-p port-number]\n");
  printf("-h output online help\n");
  printf("-l logfile\n");
  printf("-p server listening port\n");
  return;
}

int main(int argc, char *argv[]) {
  int       list_s;                /*  listening socket          */
  short int port=ECHO_PORT;                  /*  port number               */
  struct    sockaddr_in servaddr;  /*  socket address structure  */
  char      buffer[MAX_LINE];      /*  character buffer          */
  char      txbuffer[MAX_LINE];
  char     *endptr;                /*  for strtol()              */
  struct sockaddr_in msgfrom;
  socklen_t msgsize = sizeof(msgfrom);
  union {
    uint32_t addr;
    char bytes[4];
  } fromaddr;
  int n=0;

  int c;

  /*  Get port number from the command line, and
      set to default port if no arguments were supplied  */

  while ((c = getopt (argc, argv, "hl:p:")) != -1)
    {
      switch(c)
	{
	case 'p':
	  port = strtol(optarg,&endptr,0);
	  if(*endptr){
	    printf("Invalid port number.\n");
            exit(0);
	  }
	  break;
	case 'l':
	  open_log(optarg);
	  break;
	case 'h':
	  display_usage();
	  exit(0);
	  break;
	default:
	  display_usage();
	  exit(0);
	}
    }

  /*if ( argc == 2 ) {
    port = strtol(argv[1], &endptr, 0);
    if ( *endptr ) {
      fprintf(stderr, "ECHOSERV: Invalid port number.\n");
      exit(EXIT_FAILURE);
    }
  }
  else if ( argc < 2 ) {
    port = ECHO_PORT;
  }
  else {
    fprintf(stderr, "ECHOSERV: Invalid arguments.\n");
    exit(EXIT_FAILURE);
    }*/

	
  /*  Create the listening socket  */

  if ( (list_s = socket(AF_INET, SOCK_DGRAM, 0)) < 0 ) {
    fprintf(stderr, "ECHOSERV: Error creating listening socket.\n");
    exit(EXIT_FAILURE);
  }


  /*  Set all bytes in socket address structure to
      zero, and fill in the relevant data members   */

  memset(&servaddr, 0, sizeof(servaddr));
  servaddr.sin_family      = AF_INET;
  servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
  servaddr.sin_port        = htons(port);


  /*  Bind our socket addresss to the 
      listening socket, and call listen()  */

  if ( bind(list_s, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0 ) {
    fprintf(stderr, "ECHOSERV: Error calling bind()\n");
    exit(EXIT_FAILURE);
  }



    
  /*  Enter an infinite loop to respond
      to client requests and echo input  */

    /*  Retrieve an input line from the connected socket
	then simply write it back to the same socket.     */
    while(1)
      {
	memset(buffer,0,sizeof(buffer));
	n = recvfrom(list_s, buffer, MAX_LINE-1, 0, (struct sockaddr *)&msgfrom, &msgsize);
      if(n>0)
	{
	  fromaddr.addr = ntohl(msgfrom.sin_addr.s_addr);
	  minilog("request received from %d.%d.%d.%d: ",
		  0xff & (unsigned int)fromaddr.bytes[3],
		  0xff & (unsigned int)fromaddr.bytes[2],
		  0xff & (unsigned int)fromaddr.bytes[1],
		  0xff & (unsigned int)fromaddr.bytes[0]);
	  
	  memset(txbuffer,0,sizeof(txbuffer));
	  analyze_response(buffer,txbuffer);
	  if(strlen(txbuffer))
	    {
	      txbuffer[strlen(txbuffer)]='\n';
	    }
	  sendto(list_s, txbuffer, strlen(txbuffer),0,(struct sockaddr *)&msgfrom, sizeof(msgfrom));
	}
      }

      /*  Close the socket  */

	  if ( close(list_s) < 0 ) {
	    fprintf(stderr, "ECHOSERV: Error calling close()\n");
	    exit(EXIT_FAILURE);
	  }
}

