/* The authors of this work have released all rights to it and placed it
 in the public domain under the Creative Commons CC0 1.0 waiver
 (http://creativecommons.org/publicdomain/zero/1.0/).

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Retrieved from: http://en.literateprograms.org/Dijkstra's_algorithm_(Java)?oldid=15444
 */
package dijkstra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author alo
 */
public class Dijkstra {

    // N
    private final List<FibonacciHeapNode> nodes;
    private final List<Edge> edges;

    // N'
    private FibonacciHeap NotTreatedNodes;
    // p(v)
    private Map<FibonacciHeapNode, FibonacciHeapNode> predecessors;
    // D(v)
    private Map<FibonacciHeapNode, Integer> distances;

    public Dijkstra(Graph graph) {
        // create a copy of the array so that we can operate on this array
        this.nodes = new ArrayList<FibonacciHeapNode>(graph.getNodes());
        this.edges = new ArrayList<Edge>(graph.getEdges());
    }

    public void calculate(FibonacciHeapNode source) {
        NotTreatedNodes = new FibonacciHeap();
        distances = new HashMap<FibonacciHeapNode, Integer>();
        predecessors = new HashMap<FibonacciHeapNode, FibonacciHeapNode>();
        // set infinity to all other node than myself
        for (FibonacciHeapNode entry : distances.keySet()) {
            distances.put(entry, Integer.MAX_VALUE);
        }
        for (FibonacciHeapNode node : this.nodes) {
            if (node == source) {
                NotTreatedNodes.insert(source, 0);
            } else {
                NotTreatedNodes.insert(node, Integer.MAX_VALUE);
            }
        }
        distances.put(source, 0);
        // tant que N' != V
        while (!NotTreatedNodes.isEmpty()) {
            // find u not in N' such that D(u) is minimum
            FibonacciHeapNode closestNode = NotTreatedNodes.removeMin();
            // for each v adjacent to u
            List<FibonacciHeapNode> adjacentNodes = getNeighbors(closestNode);
            for (FibonacciHeapNode target : adjacentNodes) {
                // if you have Integer.Max_Value and add a distance, it makes a negative distance (took me a while debugging that)
                if (getDistance(closestNode, target) != Integer.MAX_VALUE && getShortestDistance(closestNode) + getDistance(closestNode, target) < getShortestDistance(target)) {
                    distances.put(target, getShortestDistance(closestNode) + getDistance(closestNode, target));
                    predecessors.put(target, closestNode);
                    NotTreatedNodes.decreaseKey(target, getShortestDistance(closestNode) + getDistance(closestNode, target));
                }
            }
        }
    }

    /**
     * Return the distance between two node
     *
     * @param node
     * @param target
     * @return
     */
    private int getDistance(FibonacciHeapNode node, FibonacciHeapNode target) {
        for (Edge edge : edges) {
            if (edge.getSource().equals(node)
                    && edge.getDestination().equals(target)) {
                return edge.getWeight();
            }
        }
        throw new RuntimeException("Should not happen");
    }

    /**
     * Return a list of node neighbors to a source node
     *
     * @param node
     * @return
     */
    private List<FibonacciHeapNode> getNeighbors(FibonacciHeapNode node) {
        ArrayList<FibonacciHeapNode> neighbors = new ArrayList<FibonacciHeapNode>();
        for (Edge edge : this.edges) {
            if (node.equals(edge.getSource())) {
                neighbors.add(edge.getDestination());
            }
        }
        return neighbors;
    }

    /**
     * Return the shortest distance to a specific node
     *
     * @param destination
     * @return
     */
    private int getShortestDistance(FibonacciHeapNode destination) {
        Integer d = distances.get(destination);
        if (d == null) {
            return Integer.MAX_VALUE;
        } else {
            return d;
        }
    }

    /**
     * Return the path to a specific node
     *
     * @param target
     * @return
     */
    public LinkedList<FibonacciHeapNode> getPath(FibonacciHeapNode target) {
        LinkedList<FibonacciHeapNode> path = new LinkedList<FibonacciHeapNode>();
        FibonacciHeapNode step = target;
        // check if a path exists
        if (predecessors.get(step) == null) {
            return null;
        }
        path.add(step);
        while (predecessors.get(step) != null) {
            step = predecessors.get(step);
            path.add(step);
        }
        // Put it into the correct order
        Collections.reverse(path);
        return path;
    }

    /**
     * Return the total cost of a path
     *
     * @param path
     * @return
     */
    public int getDistanceOfPath(List<FibonacciHeapNode> path) {
        return getShortestDistance(path.get(path.size() - 1));
    }

}
