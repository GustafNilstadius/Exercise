#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <unistd.h>
#include "minilog.h"

FILE *stream=NULL;
int debug = 0;
void minilog ( const char * format, ... )
{
  va_list args;

  if(0!= debug)
    {
      // debug mode, output to stdout
      va_start (args, format);
      vprintf (format, args);
      va_end (args);
    }
  else if(NULL != stream)
    {
      // output log file
      va_start (args, format);
      vfprintf(stream,format,args);
      fflush(stream);
      fsync(fileno(stream));
      va_end (args);
    }

  return;
}

void open_log(char *logFile)
{
  stream = fopen(logFile,"w");
  if (NULL == stream)
    {
      printf("open log file %s fail\n",logFile);
      exit(0);
    }

}

void close_log()
{
  if(NULL != stream)
    {
      fclose(stream);
    }
}

void enable_debug()
{
  debug = 1;
}
