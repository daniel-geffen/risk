import java.util.*;

public class Country {
    private int id;
    private String name;
    private String continent;
    private List<Integer> neighbors;
    private Player owner;
    private int numOfTroops;

    public Country(int id, String name, String continent, List<Integer> neighbors) {
        this.id = id;
        this.name = name;
        this.continent = continent;
        this.neighbors = neighbors;
        this.owner = null;
    }

    public String getName() {
        return name;
    }

    public String getContinent() {
        return continent;
    }

    public void occupy(Player owner, int numOfTroops) {
        this.owner = owner;
        this.numOfTroops = numOfTroops;
    }

    public int getNumOfTroops() {
        return numOfTroops;
    }

    private void populateGraphWithConnectedRivalCountries(Player attacker, Map<Integer, Node> graph, Country[] countries) {
        for (int countryId : this.neighbors) {
            Country neighbor = countries[countryId];
            if (!graph.containsKey(countryId) && neighbor.owner != attacker) {
                graph.put(countryId, new Node(neighbor));
                neighbor.populateGraphWithConnectedRivalCountries(attacker, graph, countries);
            }
        }
    }

    private Map<Integer, Node> createInitialGraph(Country[] countries) {
        Map<Integer, Node> graph = new HashMap<>();
        Node initialNode = new Node(this);
        initialNode.setNewPath(null, 0);
        graph.put(this.id, initialNode);
        this.populateGraphWithConnectedRivalCountries(this.owner, graph, countries);
        return graph;
    }

    public Stack<Country> getDistanceFromRival(Country countryToFind, Country[] countries) {
        Map<Integer, Node> graph = createInitialGraph(countries);

        Node pathNode = graph.get(countryToFind.id);
        while (!graph.isEmpty()) {
            Node node = Collections.min(graph.values());
            graph.remove(node.getCountry().id);
            if (node.getCountry() != countryToFind && node.getDistance() < pathNode.getDistance()) {
                for (int neighbor : node.getCountry().neighbors) {
                    if (graph.containsKey(neighbor)) {
                        Node neighborNode = graph.get(neighbor);
                        if (node.getDistance() + neighborNode.getCountry().numOfTroops < neighborNode.getDistance())
                            neighborNode.setNewPath(node, node.getDistance() + neighborNode.getCountry().numOfTroops);
                    }
                }
            }
        }

        // pathNode.getDistance() to get the num of troops to kill on the way.

        Stack<Country> path = new Stack<>();
        while (pathNode.getPrevNode() != null) {
            path.push(pathNode.getCountry());
            pathNode = pathNode.getPrevNode();
        }

        return path;
    }
}
