import java.util.*;

/**
 * A class representing a country on the map of the game.
 */
public class Country {
    private int id; // The id of the country, also its index in the countries array in GameManager class.
    private String name; // The name of the country.
    private Continent continent; // The continent the country is in.
    private List<Integer> neighbors; // A list of the ids of the countries that are neighbors of the country.
    private Player owner; // The player that controls the country.
    private int numOfTroops; // The number of troops that are in the country.

    /**
     * A constructor that sets object variables.
     */
    public Country(int id, String name, Continent continent, List<Integer> neighbors) {
        this.id = id;
        this.name = name;
        this.continent = continent;
        this.neighbors = neighbors;
        this.owner = null;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Occupy the country - set both a new owner and a new number of troops.
     * @param owner The new player that controls the country.
     * @param numOfTroops The number of troops that are now in the country.
     */
    public void occupy(Player owner, int numOfTroops) {
        this.owner = owner;
        this.numOfTroops = numOfTroops;
    }

    public int getNumOfTroops() {
        return this.numOfTroops;
    }

    /**
     * A recursive function that populates the graph with nodes for all the connected countries around this country, that aren't controlled by the attacker.
     * This is done by every country adding its neighbors, and calling the function with them.
     * @param attacker The attacking player.
     * @param graph The graph to populate.
     * @param countries The array of countries.
     */
    private void populateGraphWithConnectedRivalCountries(Player attacker, Map<Integer, Node> graph, Country[] countries) {
        for (int countryId : this.neighbors) {
            Country neighbor = countries[countryId];
            if (!graph.containsKey(countryId) && neighbor.owner != attacker) {
                graph.put(countryId, new Node(neighbor));
                neighbor.populateGraphWithConnectedRivalCountries(attacker, graph, countries);
            }
        }
    }

    /**
     * Creates a graph for dijkstra algorithm. It contains the attacking country, and all of the countries that are
     * controlled by rival players, and are connected to the attacking country.
     * The node of this country is set to 0 as the beginning of the graph, and all others are set to infinity.
     * @param countries The array of countries.
     * @return A graph, as a map of country id as keys and node objects as values.
     */
    private Map<Integer, Node> createInitialGraph(Country[] countries) {
        Map<Integer, Node> graph = new HashMap<>();
        Node initialNode = new Node(this);
        initialNode.setNewPath(null, 0);
        graph.put(this.id, initialNode);
        this.populateGraphWithConnectedRivalCountries(this.owner, graph, countries);
        return graph;
    }

    /**
     * An implementation of the dijkstra algorithm, in order to determine the shortest path from this country to another.
     * The path can only go through countries controlled by other players.
     * @param countryToFind The country to find a path to.
     * @param countries The countries array.
     * @return A stack with the countries creating the shortest path. The first country is the first country to conquer.
     */
    public Stack<Country> getPathToRival(Country countryToFind, Country[] countries) {
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
