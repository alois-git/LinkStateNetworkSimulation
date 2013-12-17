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
  private final List<Node> nodes;
  private final List<Edge> edges;

  public Graph(Collection<Node> nodes, Collection<Edge> edges) {
    this.nodes =  new ArrayList<>(nodes);
    this.edges = new ArrayList<>(edges);
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public List<Edge> getEdges() {
    return edges;
  }
  
  
  
} 
