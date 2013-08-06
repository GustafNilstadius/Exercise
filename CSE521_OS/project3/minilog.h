#ifndef _LOG_H_
#define _LOG_H_

#include <stdarg.h>

void minilog ( const char * format, ... );

void open_log(char *logFile);

void close_log();
void enable_debug();

#endif /*_LOG_H_*/
