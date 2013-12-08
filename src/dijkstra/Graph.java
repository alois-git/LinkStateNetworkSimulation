/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dijkstra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author alo
 */
public class Graph {
  private final List<Vertex> vertexes;
  private final List<Edge> edges;

  public Graph(Collection<Vertex> vertexes, Collection<Edge> edges) {
    this.vertexes =  new ArrayList<>(vertexes);
    this.edges = new ArrayList<>(edges);
  }

  public List<Vertex> getVertexes() {
    return vertexes;
  }

  public List<Edge> getEdges() {
    return edges;
  }
  
  
  
} 
