#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <netdb.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <arpa/inet.h>

#include "parameter.h"

static void display_usage()
{
  printf("dec_client [-h] [-s server-host] [-p port-number]\n");
  printf("-h output online help\n");
  printf("-s host name or ip address of server\n");
  printf("-p listening port of server\n");
  return;
}

int main(int argc, char *argv[])
{
    short int port=ECHO_PORT;
    int sockfd = 0;
    char buf[MAX_LINE];
    int bytes;
    struct sockaddr_in serv_addr;
    socklen_t    serveraddrlen = sizeof(serv_addr);
    fd_set ready;
    int done=0;
    
    int c;
    char     *endptr;  
    char *server_host ="localhost";
    struct hostent *hp;

while ((c = getopt (argc, argv, "hs:p:")) != -1)
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
	case 's':
	  server_host = optarg;	  
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

   if ((hp = gethostbyname(server_host)) == NULL) {
     printf("%s unknown host\n",  server_host);
     exit(1);
   }

   /*if(argc != 2)
    {
        printf("\n Usage: %s <ip of server> \n",argv[0]);
        return 1;
	}*/

    memset(buf, '0',sizeof(buf));
    if((sockfd = socket(AF_INET, SOCK_DGRAM, 0)) < 0)
    {
        printf("\n Error : Could not create socket \n");
        return 1;
    }

    memset(&serv_addr, 0, sizeof(serv_addr));

    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(port);

    memcpy(&serv_addr.sin_addr, hp->h_addr, hp->h_length);

    while(!done)
      {
	FD_ZERO(&ready);
	FD_SET(sockfd,&ready);
	FD_SET(fileno(stdin), &ready);

	if (select((sockfd + 1), &ready, 0, 0, 0) < 0) {
	  perror("select");
	  exit(1);
	}

	if (FD_ISSET(fileno(stdin), &ready)) {
	  if ((bytes = read(fileno(stdin), buf, MAX_LINE)) <= 0)
	    done++;
	  else
	    {
	      sendto(sockfd, buf, bytes, 0,(struct sockaddr *)&serv_addr,
		     sizeof(serv_addr));
	    }
	}

	if (FD_ISSET(sockfd, &ready)) {
	  if ((bytes = recvfrom(sockfd, buf, MAX_LINE,0,
                    (struct sockaddr *)&serv_addr,
                    & serveraddrlen)) <= 0)
	    {
	      done++;
	    }
	  else
	    { 
	      fprintf(stderr,"response from server:");
	      write(fileno(stdout), buf, bytes);
	    }
	}
      }

      
    close(sockfd);
    return 0;
}
