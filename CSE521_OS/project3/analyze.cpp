#include "graph.h"
#include "analyze.h"
#include "minilog.h"

#define IS_CAP(a) ((a>='A')&&(a<='Z'))
#define IS_SPAC(a) (a>=' ')

Graph g(26);

void analyze_response(char* buffer,char* txbuffer)
{
  char *cmd;
  char * cmdsavedptr,*subcmdsavedptr;
  char *insert_pair;
  char begin,end;

  minilog("%s",buffer);
  cmd = strtok_r (buffer,";",&cmdsavedptr);
  while (cmd != NULL)
    {
      //printf ("%s\n",cmd);
      if(0== strncmp(cmd,"insert",6))
	{
	  //printf("request received cmd %s\n",cmd);
	  {
	    insert_pair = strtok_r(cmd," ",&subcmdsavedptr);
	    while(NULL != insert_pair)
	      {
		insert_pair= strtok_r(NULL, " ",&subcmdsavedptr);
		if(NULL != insert_pair)
		  {
		    if((4==strlen(insert_pair))&&('-'==insert_pair[1])&&('>'==insert_pair[2]))
		      {
			begin=insert_pair[0];
			end = insert_pair[3];
			if(IS_CAP(begin)&&IS_CAP(end)&&(begin!=end))
			  {
			    begin = begin-'A';
			    end = end - 'A';
			    if(g.isReachable(end,begin))
			      {
				sprintf(txbuffer+strlen(txbuffer),"CONFLICT DETECTEDINSERT FAILED ");			      
			      }
			    else
			      {
				g.addEdge(begin,end);
				sprintf(txbuffer+strlen(txbuffer),"INSERT DONE."); 
				minilog("INSERT DONE.\n");
			      }
			  }
			else
			  {
			    sprintf(txbuffer+strlen(txbuffer),"UNSUPPORTED CMD ");
			  }

		      }
		    else
		      {
			sprintf(txbuffer+strlen(txbuffer),"UNSUPPORTED CMD ");
		      }
		  }
	      }
	  
	  }
	}
      else if(0== strncmp(cmd,"query",5))
	{
	  //printf("request received cmd %s\n",cmd);
	  {
	    if(9==strlen(cmd))
	      {
		if(IS_SPAC(cmd[5])&&IS_SPAC(cmd[7]))
		  {
		    begin=cmd[6];
		    end= cmd[8];

		    if(IS_CAP(begin)&&IS_CAP(end)&&(begin!=end))
		      {
			begin = begin-'A';
			end = end - 'A';
			if(!g.VertexExist(begin))
			  {
			    sprintf(txbuffer+strlen(txbuffer),"Event not found: %c",begin+'A');  
			    
			  }
			else if(!g.VertexExist(end))
			  {
			    sprintf(txbuffer+strlen(txbuffer),"Event not found: %c",end+'A');  
			  }
			else
			  {
			    if (g.isReachable(begin,end))
			      {
				sprintf(txbuffer+strlen(txbuffer),"%c happened before %c",begin+'A',end+'A');
				minilog("%c happened before %c\n",begin+'A',end+'A');
			      }
			    else if(g.isReachable(end,begin))
			      {
				sprintf(txbuffer+strlen(txbuffer),"%c happened before %c",end+'A',begin+'A');  
				minilog("%c happened before %c\n",end+'A',begin+'A');
			      }
			    else
			      {
				sprintf(txbuffer+strlen(txbuffer),"%c current to %c",begin+'A',end+'A');  
				minilog("%c current to %c\n",begin+'A',end+'A');
			      }
			  }
		      }		  
		    else
		      {
			sprintf(txbuffer+strlen(txbuffer),"UNSUPPORTED CMD ");
		      }
		  }
		else
		  {
		    sprintf(txbuffer+strlen(txbuffer),"UNSUPPORTED CMD ");
		  }
	      }
	    else
	      {
		sprintf(txbuffer+strlen(txbuffer),"UNSUPPORTED CMD ");
	      }
	  }
	
	} 
      else if(0== strncmp(cmd,"reset",5))
	{
	  //printf("cmd reset\n");
	  g.reset();
	  sprintf(txbuffer+strlen(txbuffer),"RESET DONE.");
	  minilog("RESET DONE.\n");
	} 
    
      cmd = strtok_r (NULL, ";",&cmdsavedptr);
    }

}
