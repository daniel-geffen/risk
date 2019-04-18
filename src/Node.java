/**
 * A class representing a node in a graph used for dijkstra algorithm.
 */
public class Node implements Comparable<Node> {
    private Country country; // The country the node represents.
    private Node prevNode; // The node that leads to this node in the path.
    private int distance; // The distance from the first node in the graph.

    /**
     * A constructor that sets the variables.
     * The distance is set to infinity, and the previous node to null.
     */
    public Node(Country country) {
        this.country = country;
        this.prevNode = null;
        this.distance = Integer.MAX_VALUE;
    }

    public Country getCountry() {
        return this.country;
    }

    public Node getPrevNode() {
        return this.prevNode;
    }

    public int getDistance() {
        return this.distance;
    }

    /**
     * Called when a better path is found to the node. Sets the new previous node and distance.
     */
    public void setNewPath(Node prevNode, int distance) {
        this.distance = distance;
        this.prevNode = prevNode;
    }

    /**
     * Implementing the compareTo function, in order to be able to compare nodes by distance.
     * @param node A node to compare to.
     * @return The difference between the distances.
     */
    @Override
    public int compareTo(Node node) {
        return this.distance - node.distance;
    }
}
