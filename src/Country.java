import javafx.util.Pair;

import java.util.*;

/**
 * A class representing a country on the map of the game.
 */
public class Country implements Comparable<Country> {
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

    public Player getOwner() {
        return this.owner;
    }

    /**
     * Occupy the country - set both a new owner and a new number of troops.
     * @param owner The new player that controls the country.
     * @param numOfTroops The number of troops that are now in the country.
     */
    public void occupy(Player owner, int numOfTroops) {
        System.out.println(this.name + " occupied by " + owner.getName() + " with " + numOfTroops);
        this.owner = owner;
        this.numOfTroops = numOfTroops;
    }

    public int getNumOfTroops() {
        return this.numOfTroops;
    }

    public List<Integer> getNeighbors() {
        return this.neighbors;
    }

    public int getId() {
        return this.id;
    }

    public Continent getContinent() {
        return this.continent;
    }

    /**
     * Add troops to the country.
     * @param troopsToAdd The number of troops to add.
     */
    public void addTroops(int troopsToAdd) {
        System.out.println(this.owner.getName() + " adding " + troopsToAdd + " troops to " + this.name);
        this.numOfTroops += troopsToAdd;
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
     * Creates a Dijkstra graph from this country to countryToFind.
     * @param countryToFind The country we are trying to find.
     * @param countries The array of countries.
     * @return The graph node of the countryToFind, connected to the other nodes accordingly and with the appropriate weight.
     */
    private Node createDijkstraGraph(Country countryToFind, Country[] countries) {
        Map<Integer, Node> graph = createInitialGraph(countries);

        Node pathNode = graph.get(countryToFind.id);
        if (pathNode != null)
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
        return pathNode;
    }

    /**
     * An implementation of the dijkstra algorithm, in order to determine the shortest path from this country to another.
     * The path can only go through countries controlled by other players.
     * @param countryToFind The country to find a path to.
     * @param countries The countries array.
     * @return A stack with the countries creating the shortest path. The first country is the first country to conquer.
     */
    public Stack<Country> getPathToRival(Country countryToFind, Country[] countries) {
        Node pathNode = createDijkstraGraph(countryToFind, countries);
        Stack<Country> path = new Stack<>();
        if (pathNode != null)
            while (pathNode.getPrevNode() != null) {
                path.push(pathNode.getCountry());
                pathNode = pathNode.getPrevNode();
            }

        return path;
    }

    /**
     * @param continent A continent.
     * @param countries The countries array.
     * @return Get the border of the continent that is closest to this country.
     */
    public Country getClosestContinentBorder(Continent continent, Country[] countries) {
        Country closestBorder = null;
        int distanceOfClosestBorder = Integer.MAX_VALUE;
        for (Country border : continent.getBorders()) {
            Node graphNode = createDijkstraGraph(border, countries);
            if (graphNode != null) {
                int distanceOfBorder = graphNode.getDistance();
                if (distanceOfBorder < distanceOfClosestBorder) {
                    closestBorder = border;
                    distanceOfClosestBorder = distanceOfBorder;
                }
            }
        }

        return closestBorder;
    }

    /**
     * @param continent A continent.
     * @param countries The countries array.
     * @return The distance (in number of troops on the way) of this country from the continent (from the closest border).
     */
    public int getDistanceFromContinent(Continent continent, Country[] countries) {
        int distanceOfClosestBorder = Integer.MAX_VALUE;
        for (Country border : continent.getBorders()) {
            Node graphNode = createDijkstraGraph(border, countries);
            if (graphNode != null) {
                int distanceOfBorder = graphNode.getDistance();
                if (distanceOfBorder < distanceOfClosestBorder) {
                    distanceOfClosestBorder = distanceOfBorder;
                }
            }
        }

        return distanceOfClosestBorder == Integer.MAX_VALUE ? 0 : distanceOfClosestBorder;
    }

    /**
     * Attack another country.
     * @param countryToAttack The country to attack.
     * @param moveAllTroopsOnWin Whether to move all troops (besides 1 that has to stay) to the new country when winning.
     * @return Whether the attack was a success.
     */
    public boolean attack(Country countryToAttack, boolean moveAllTroopsOnWin) {
        Pair<Integer, Integer> battleResults = BattleUtils.simulateBattle(this.numOfTroops, countryToAttack.numOfTroops);
        System.out.println("Battle results: " + this.name + " - " + battleResults.getKey() + ", " + countryToAttack.name + " - " + battleResults.getValue());
        if (battleResults.getValue() == 0) {
            if (moveAllTroopsOnWin) {
                this.numOfTroops = 1;
                countryToAttack.owner.removeCountry(countryToAttack);
                this.owner.addCountry(countryToAttack, battleResults.getKey() - 1);
            } else {
                this.numOfTroops = battleResults.getKey() - 1;
                countryToAttack.owner.removeCountry(countryToAttack);
                this.owner.addCountry(countryToAttack, 1);
            }
            return true;
        } else {
            this.numOfTroops = battleResults.getKey();
            countryToAttack.numOfTroops = battleResults.getValue();
            return false;
        }
    }

    /**
     * Goes on a series of attacks in the shortest path to destination.
     * @param destination The destination of the attacks.
     * @param countries The countries array.
     * @return Whether the journey was successful.
     */
    public boolean goOnAttackJourney(Country destination, Country[] countries) {
        Stack<Country> path = this.getPathToRival(destination, countries);
        Country attacker = this;
        boolean attackSuccess = true;
        while (!path.isEmpty() && attackSuccess) {
            Country countryToAttack = path.pop();
            attackSuccess = attacker.attack(countryToAttack, true);
            attacker = countryToAttack;
        }

        return attackSuccess;
    }

    @Override
    public int compareTo(Country c) {
        return this.numOfTroops - c.numOfTroops;
    }
}
