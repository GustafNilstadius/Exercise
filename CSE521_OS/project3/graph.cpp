// Program to check if there is exist a path between two vertices of a graph.
//#include<iostream>

#include "graph.h"
  Graph::Graph(int V){    
    this->V = V; 
    Vertex_Valid = new int[V];
    adj = new list<int>[V];} 

  void Graph::addEdge(int v, int w)
  { 
    Vertex_Valid[v]=1;
    Vertex_Valid[w]=1;
    adj[v].push_back(w); // Add w to v’s list.
  } 

bool Graph::VertexExist(int v)
{
  return (1==Vertex_Valid[v]);
}

  // A BFS based function to check whether d is reachable from s.
  bool Graph::isReachable(int s, int d)
  {    
    // Base case    
    if (s == d)      
      return true;     

    // Mark all the vertices as not visited    
    bool *visited = new bool[V];    
    for (int i = 1; i <V; i++)        
      visited[i] = false;     

    // Create a queue for BFS    
    list<int> queue;     
    // Mark the current node as visited and enqueue it    
    visited[s] = true;    
    queue.push_back(s);     // it will be used to get all adjacent vertices of a vertex    
    list<int>::iterator i;
     
    while (!queue.empty())    {        
      // Dequeue a vertex from queue and print it        
      s = queue.front();        
      queue.pop_front();         

      // Get all adjacent vertices of the dequeued vertex s        
      // If a adjacent has not been visited, then mark it visited        
      //and enqueue it        
      for (i = adj[s].begin(); i != adj[s].end(); ++i)        
	{            
	  // If this adjacent node is the destination node, then return true            
	  if (*i == d)                
	    return true;             
	  // Else, continue to do BFS            
	  if (!visited[*i])            
	    {                
	      visited[*i] = true;                
	      queue.push_back(*i);            
	    }        
	}    
    }     
    return false;
  }

void Graph::reset()
{ 
  if( adj )    
    {        
      for(int i = 0; i < V; i++)
	{
	adj[i].clear();
	}
    } 
}

Graph::~Graph()
{ 
  if(Vertex_Valid)
    delete[] Vertex_Valid;

  if( adj )    
    {        
      for(int i = 0; i < V; i++)            
	adj[i].clear();         
      delete[] adj;    
    } 
}
 

/*
int main()
{    
  // Create a graph given in the above diagram    
Graph g(6);
 g.addEdge(0, 1);
 g.addEdge(0, 2);
 g.addEdge(0, 4);
 g.addEdge(1, 3);
 g.addEdge(1, 5);
 
 g.addEdge(2, 1);
 g.addEdge(2, 3);
 g.addEdge(2, 4);
 g.addEdge(3, 5);
 g.addEdge(4, 3);
 g.addEdge(4, 5);  
  

 for (int i=0;i<6; i++)
   {
   cout <<"vertex " <<i;
   if (g.VertexExist(i))
     cout <<"exist\n";
   else
     cout <<"not exist\n";
   }
 
  int u = 0, v = 5;    
  if(g.isReachable(u, v))        
    cout<< "\n There is a path from " << u << " to " << v;    
  else        
    cout<< "\n There is no path from " << u << " to " << v;     

  u = 5, v = 4;    
  if(g.isReachable(u, v))        
    cout<< "\n There is a path from " << u << " to " << v;    
  else        
    cout<< "\n There is no path from " << u << " to " << v;     

  return 0;
}
*/
