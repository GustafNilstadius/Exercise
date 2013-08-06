#ifndef _SERVER_H_
#define _SERVER_H_
struct Config
{
    char logFile[1025];
    int  port;
    char rootDir[1025];
    int  queuingTime;
    int  threadNum;
    int schedPolicy;
    int debug_mode;

    char cwd[1025];
};

#endif /*_SERVER_H_*/
