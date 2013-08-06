#ifndef __GRAPH_H_
#define __GRAPH_H_
#include <list> 
using namespace std; 
// This class represents a directed graph using adjacency list representation
class Graph
{    
  int V;    // No. of vertices    
  int *Vertex_Valid;
  list<int> *adj;    // Pointer to an array containing adjacency lists

public:    
  Graph(int V);  // Constructor    
  void addEdge(int v, int w); // function to add an edge to graph 
  bool VertexExist(int v);
  bool isReachable(int s, int d);  // returns true if there is a path from s to d

  void reset();

  ~Graph();
}; 
#endif
