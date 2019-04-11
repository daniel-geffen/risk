public class Node implements Comparable<Node> {
    private Country country;
    private Node prevNode;
    private int distance;

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
        return distance;
    }

    public void setNewPath(Node prevNode, int distance) {
        this.distance = distance;
        this.prevNode = prevNode;
    }

    @Override
    public int compareTo(Node node) {
        return this.distance - node.distance;
    }
}
