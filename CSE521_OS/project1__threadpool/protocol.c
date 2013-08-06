#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h> 
#include <unistd.h>
#include <time.h>
#include <dirent.h>
#include "protocol.h"
#include "minilog.h"
#include "Server.h"
//#include "queue_scheduler.h"



#define LINETOKEN "\r\n"
#define SPACETOKEN " "


extern struct Config sConfig;


/*parse the http request, get the request length
which is the the criteria of scheduling

input:
conn_fd
pSession---pointer to a session struct

return:
0----parse success
>0---invalid request,or the request is not supported
<0---socket error


*/
int httpParseRequest(int conn_fd, httpsession* pSession)
{
  int ret = 0;
  int bytesRecv;
  char *requestLine;
  char *saveptr1;
 
  char *method;
  char *request_URI;
  char *http_version;

  struct stat filest;
  char   indexfile[256]={0};

  pSession->fd = conn_fd;

  //read request buffer from client
  if(( bytesRecv =read(conn_fd,pSession->requestBuf, sizeof(pSession->requestBuf)-1)) > 0)
    {
      //minilog("receive data from client\n");
      //parse request buffer, get request line which is ending with CRLF
      requestLine = strtok_r(pSession->requestBuf, LINETOKEN, &saveptr1);

      if((NULL == requestLine)|| (requestLine != pSession->requestBuf))
	{
	  minilog ("bad request\n");
	  pSession->response_status = STATUS_BAD_REQUEST;
	  return 1;
	}
      else
	{
	  strcpy(pSession->requestLine, requestLine);
      
	  //strtok the request line, get sub strings
	  method = strtok_r(requestLine,SPACETOKEN,&saveptr1);
	  request_URI = strtok_r(NULL,SPACETOKEN,&saveptr1);
	  http_version = strtok_r(NULL,SPACETOKEN,&saveptr1);
	  
	  //compare the few characters with "GET" and "HEAD"
	  if(NULL != method)
	    {
	      if (0 == strcmp(method, "GET"))
		{
		  pSession->request_type = GET;
		}
	      else if (0 == strcmp(method, "HEAD"))
		{
		  pSession->request_type = HEAD;
		}
	      else
		{
		  minilog("requst type not support\n");
		  pSession->response_status = STATUS_BAD_REQUEST;
		  return 1;
		}
	    }
	  else
	    {
	      minilog("can not found request type\n");
	      pSession->response_status = STATUS_BAD_REQUEST;
	      return 1;
	    }

	  if(NULL != http_version)
	    {
	      if( 0 == strcmp(http_version, "HTTP/1.0"))
		{
		  pSession->version = 10;
		}
	      else if( 0 == strcmp(http_version, "HTTP/1.1"))
		{
		  pSession->version = 11;
		}
	      else
		{
		  minilog("wrong version number\n");
		  pSession->response_status = STATUS_BAD_REQUEST;
		  return 1;
		}
		   
	    }
	  else
	    {
	      minilog("can not found version number\n");
	      pSession->response_status = STATUS_BAD_REQUEST;
	      return 1;
	    }

	  if(NULL != request_URI)
	    {
	      if((NULL != strstr(request_URI, "/."))|| (NULL != strstr(request_URI, "/..")))
		{
		  minilog ("client try to access upper directory\n");
		  pSession->response_status = STATUS_BAD_REQUEST;
		  return 1;
		}

	      if(('/' == request_URI[0])&&('~' == request_URI[1]))
		{
		  strcpy(pSession->filepath,sConfig.cwd);
		  strcat(pSession->filepath,"/");
		  strcat(pSession->filepath,&(request_URI[2]));
		
		}
	      else
		{
		  strncpy(pSession->filepath, sConfig.rootDir,sizeof(sConfig.rootDir));
		  strcat(pSession->filepath,request_URI);
		}

	       
	      if(stat(pSession->filepath, &filest)<0)
		{
		  minilog("get file status of %s failure\n",pSession->filepath);
		  pSession->response_status = STATUS_BAD_REQUEST;
		  return 1;
		}

	      if(S_ISDIR(filest.st_mode))
		{
		  strcpy(indexfile,pSession->filepath);
		  strcat(indexfile,"/index.html");
		  if(stat(indexfile,&filest)<0)
		    {
		      //minilog("retrive directory");
		      pSession->reqdir = 1;
		      {
			
		      }
		      
		    }
		  else
		    {
		      strcpy(pSession->filepath, indexfile);
		    }
		}

	      
	      pSession->file_mtime = filest.st_mtime;
	      pSession->file_size  = filest.st_size;
	      
	      if(pSession->request_type == HEAD)
		{
		  pSession->file_size  = 0;
		}	       
	    }
	    
	  else
	    {
	      minilog("can not found request URI\n");
	      pSession->response_status = STATUS_BAD_REQUEST;
	      return 1;
	    }
	}
    }  
  else if(bytesRecv < 0)
    {
      minilog ("Read error \n");
      return -1;
    }
  else
    {
      minilog("client socket closed\n");
      return -1;
    }

  pSession->response_status = STATUS_OK;
  return ret;
}

int httpCapsulateResponse(httpsession* pSession)
{
  int ret=0;
  time_t current;
  char timeBuff[256];
  char modtimeBuf[256];
  char contentLengthBuff[32];

  current = time(NULL);
  ctime_r(&current, timeBuff);

  // status line
  if( STATUS_BAD_REQUEST == pSession->response_status)
    {
      strcat(pSession->responseBuf,"HTTP/1.0 400 BADREQUEST");  
    }
  else
    {
      strcat(pSession->responseBuf,"HTTP/1.0 200 OK");    
    }
  strcat(pSession->responseBuf,LINETOKEN);

  //Date
  strcat(pSession->responseBuf,"Date: ");
  strcat(pSession->responseBuf,timeBuff);
  strcat(pSession->responseBuf," GMT");
  strcat(pSession->responseBuf,LINETOKEN);

  //Server
  strcat(pSession->responseBuf,"Server: ");
  //need to fill up more information???
  strcat(pSession->responseBuf,LINETOKEN);

  if( STATUS_OK == pSession->response_status)
    {
      //Last_Modified
      strcat(pSession->responseBuf,"Last-Modified: ");
      ctime_r(&pSession->file_mtime, modtimeBuf);
      strcat(pSession->responseBuf,modtimeBuf);
      strcat(pSession->responseBuf," GMT"); 
      strcat(pSession->responseBuf,LINETOKEN);

      //Content-Type
        //get extension
      // if(NULL == strrchr (pSession->filepath, '.'))
      //{
      //}
      strcat(pSession->responseBuf,"Content-Type: ");
      strcat(pSession->responseBuf,"text/html");
      strcat(pSession->responseBuf,LINETOKEN);

      //Content-Length
      pSession->pContentlen = pSession->responseBuf;
      strcat(pSession->responseBuf,"Content-Length: ");
      snprintf(contentLengthBuff,sizeof(contentLengthBuff),"%d", pSession->file_size);
      strcat(pSession->responseBuf,contentLengthBuff);
      strcat(pSession->responseBuf,LINETOKEN);    
    }

  return ret;
}

/*send the reponse to the socket fd
attention:
this socket is still valid? it is already closed by client

ret = -1,fail;
ret =0, success;
*/
int httpSendResponse(httpsession* pSession)
{
  int ret = 0;
  FILE *pFile;;
  int result;

  char *buff = NULL;
  int buff_len;

  if(0 == pSession->file_size)
    {
      //HEAD
      write(pSession->fd,pSession->responseBuf,strlen(pSession->responseBuf));
    }
  else if(0 == pSession-> reqdir)
    {
      buff_len = strlen(pSession->responseBuf) + pSession->file_size;
      buff = malloc(buff_len);

      if(NULL == buff)
	{
	  minilog ("malloc fail\n");
	  ret = -1;
	  return ret;
	}
      else 
	{	 
	  memset(buff,0,buff_len);

	  memcpy(buff,pSession->responseBuf,strlen(pSession->responseBuf));
	  pFile = fopen (pSession->filepath ,"rb" );
	  if (NULL == pFile)
	  {
	    minilog("fopen %s failure\n", pSession->filepath);
	    free (buff);
	    ret = -1;
	    return ret;
	  }
	  else
	  {	  
	    result = fread(buff +strlen(pSession->responseBuf), 1, pSession->file_size, pFile);
	    if(result != pSession->file_size)
	    {
	      minilog("enounter error when read file %s\n", pSession->filepath);
	      ret = -1;
	      free (buff);
	      return ret;
	    }
	    else
	    {
	      //????
	      write(pSession->fd,buff,buff_len);
	      free(buff);
	    }
				          
	    fclose(pFile);
	    
	  }	   		 
  
	}
     }
  else
    {
      //request dir
      struct dirent **filelist = {0};
      int fcount = -1;
      int i = 0;
      int actual_len = 0;
      char dirbuff[4096]={0};
      char *buff = NULL;
      char contentLengthBuff[32];
      
      fcount = scandir(pSession->filepath, &filelist, 0, alphasort);

      if(fcount < 0) {
	minilog("scandir %s fail\n",pSession->filepath);
	ret = -1;
      }

      for(i = 0; i < fcount; i++)  {
	if(!(strcmp(filelist[i]->d_name,".")==0 || strcmp(filelist[i]->d_name,"..")==0))
	  {
	    if((actual_len + strlen(filelist[i]->d_name)+2)>= 4096)      
	      {
	      break;
	      }
	    actual_len += sprintf(dirbuff + actual_len,"%s\n", filelist[i]->d_name);
	  }
	free(filelist[i]);
      }

      free(filelist);

      //dirty hack code for diretory request
      strcpy(pSession->pContentlen,"Content-Length: ");
      snprintf(contentLengthBuff,sizeof(contentLengthBuff),"%d", actual_len);
      strcat(pSession->pContentlen,contentLengthBuff);
      strcat(pSession->pContentlen,LINETOKEN); 

      buff_len = strlen(pSession->responseBuf) + actual_len;
      buff = malloc(buff_len);

      if(NULL == buff)
	{
	  minilog ("malloc fail\n");
	  ret = -1;
	  return ret;
	}
      else 
	{	 
	  memset(buff,0,buff_len);
	  memcpy(buff,pSession->responseBuf,strlen(pSession->responseBuf));
	  memcpy(buff +strlen(pSession->responseBuf),dirbuff,actual_len);
	  
	  write(pSession->fd,buff,buff_len);
	  free(buff);
	  
	}
      
      return 0;
    }

  

  return ret;
}

/*temp code, for test*/
void httpProtocol(conn_fd)
{
  httpsession session;
  int result = 0;

  memset(&session,0,sizeof(session));

  if((result = httpParseRequest(conn_fd, &session))>=0)
    {    
      httpCapsulateResponse(&session);
      httpSendResponse(&session);
    }
     
  close(conn_fd);

  return;
}
