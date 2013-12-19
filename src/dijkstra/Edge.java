/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dijkstra;

/**
 *
 * @author alo
 */
public class Edge  {
  private String id; 
  private FibonacciHeapNode source;
  private FibonacciHeapNode destination;
  private int weight; 
  
  public Edge(String id, FibonacciHeapNode source, FibonacciHeapNode destination, int weight) {
    this.id = id;
    this.source = source;
    this.destination = destination;
    this.weight = weight;
  }
  
  public String getId() {
    return id;
  }
  public FibonacciHeapNode getDestination() {
    return destination;
  }

  public FibonacciHeapNode getSource() {
    return source;
  }
  public int getWeight() {
    return weight;
  }
  
  public void setWeight(int w){
      this.weight = w;
  }
  
  @Override
  public String toString() {
    return source + " " + destination;
  }
  
  
} 
