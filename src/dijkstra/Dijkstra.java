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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Dijkstra {

    // N
    private final List<Node> nodes;
    private final List<Edge> edges;

    private Set<Node> TreatedNodes = new HashSet<Node>();
    // N'
    private Set<Node> NotTreatedNodes;
    // p(v)
    private Map<Node, Node> predecessors;
    // D(v)
    private Map<Node, Integer> distances;

    public Dijkstra(Graph graph) {
        // create a copy of the array so that we can operate on this array
        this.nodes = new ArrayList<Node>(graph.getVertexes());
        this.edges = new ArrayList<Edge>(graph.getEdges());
    }

    public void calculate(Node source) {
        NotTreatedNodes = new HashSet<Node>();
        distances = new HashMap<Node, Integer>();
        predecessors = new HashMap<Node, Node>();
        // set infinity to all other node than myself
        for (Node entry : distances.keySet()) {
            distances.put(entry, Integer.MAX_VALUE);
        }
        distances.put(source, 0);
        NotTreatedNodes.add(source);
        // tant que N' != V
        while (NotTreatedNodes.size() > 0) {
            // find u not in N' such that D(u) is minimum
            Node u = getClosestNode(NotTreatedNodes);
            TreatedNodes.add(u);
            NotTreatedNodes.remove(u);
            // for each v adjacent to u
            findMinimalDistances(u);
        }
    }

    /**
     * Return the distance between two node
     *
     * @param node
     * @param target
     * @return
     */
    private int getDistance(Node node, Node target) {
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
    private List<Node> getNeighbors(Node node) {
        ArrayList<Node> neighbors = new ArrayList<Node>();
        for (Edge edge : this.edges) {
            if (node.equals(edge.getSource()) && !isTreated(edge.getDestination())) {
                neighbors.add(edge.getDestination());
            }
        }
        return neighbors;
    }

    /**
     * Return the closest node from source
     *
     * @param vertexes
     * @return
     */
    private Node getClosestNode(Set<Node> vertexes) {
        Node minimum = null;
        for (Node vertex : vertexes) {
            if (minimum == null) {
                minimum = vertex;
            } else {
                if (getShortestDistance(vertex) < getShortestDistance(minimum)) {
                    minimum = vertex;
                }
            }
        }
        return minimum;
    }

    /**
     * Updates the distances between the source and the not treated node
     * (neighbors)
     *
     * @param node
     */
    void findMinimalDistances(Node node) {
        List<Node> adjacentNodes = getNeighbors(node);
        for (Node target : adjacentNodes) {
            if (getShortestDistance(target) > getShortestDistance(node) + getDistance(node, target)) {
                distances.put(target, getShortestDistance(node) + getDistance(node, target));
                predecessors.put(target, node);
                NotTreatedNodes.add(target);
            }
        }
    }

    /**
     * Checks if a node is already processed
     *
     * @param node
     * @return
     */
    boolean isTreated(Node node) {
        return TreatedNodes.contains(node);
    }

    /**
     * Return the shortest distance to a specific node
     *
     * @param destination
     * @return
     */
    private int getShortestDistance(Node destination) {
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
    public LinkedList<Node> getPath(Node target) {
        LinkedList<Node> path = new LinkedList<Node>();
        Node step = target;
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
    public int getDistanceOfPath(List<Node> path) {
        return getShortestDistance(path.get(path.size() - 1));
    }

}
