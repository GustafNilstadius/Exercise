#ifndef _PROTOCOL_H_
#define _PROTOCOL_H_

enum STATUS
  {
    STATUS_OK             = 200,
    STATUS_CREATED        = 201,
    STATUS_ACCEPTED       = 202,
    STATUS_NO_CONTENT     = 204,
    STATUS_NOT_MODIFIED   = 304,
    STATUS_BAD_REQUEST    = 400,
    STATUS_FORBIDDEN      = 403,
    STATUS_NOTFOUND       = 404,
    STATUS_INTERNAL_ERR   = 500,
    STATUS_NOT_IMPLEMENT  = 501   
  };

enum REQUEST
  {
    GET,
    HEAD
  };

typedef struct httpsession
{ 
  int fd;
  
  // char ipstr[64];//64 > INET6_ADDRSTRLEN  

  //request
  char requestBuf[1024];
  char requestLine[256];
  enum REQUEST request_type;
  int  version;
  
  int reqdir;
  char *pContentlen;

  time_t receive_time;
  time_t schedule_time;
  char filepath[256];
  time_t file_mtime;
  int file_size; 

  //response
  char responseBuf[1024];// not include the attched file
  enum STATUS response_status;
} httpsession;

void httpProtocol(int conn_fd);
int httpParseRequest(int conn_fd, httpsession* pSession);
int httpCapsulateResponse(httpsession* pSession);
int httpSendResponse(httpsession* pSession);

#endif /*_PROTOCOL_H_*/
