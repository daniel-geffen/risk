import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.websocket.Session;
import java.io.*;
import java.util.*;

public class GameManager {
    private static final String mapFilename = "/Users/tuvia/IdeaProjects/Risk/src/countries.json"; // The path to the map json file.
    private static final int numOfPlayers = 3; // The number of players in a game.
    private static final Map<String, Integer> continentBonuses = new HashMap<String, Integer>() {{
        put("Africa", 3);
        put("Asia", 7);
        put("Europe", 5);
        put("North America", 5);
        put("Oceania", 2);
        put("South America", 2);
    }}; // A map with the troop bonuses for every continent, by its name.

    private static int gameIdCounter = 0; // The counter of the game ids.

    private int gameId; // The id of the game.
    private int currentPlayerId; // The id of the player that is currently playing his turn.
    private List<Player> players; // A list of the players in the game.
    private List<String> playerColors = Arrays.asList("rgb(58,118,207)", "rgb(100,61,166)", "rgb(134,30,22)", "rgb(222,65,118)", "rgb(28,138,101)", "rgb(90,90,90)"); // A list with the optional colors for players.
    private Country[] countries; // An array of the countries on the game map. The index is the country id.
    private Map<String, Continent> continents; // A map of the continents. Continent names are keys, continent objects are values.

    /**
     * A function that reads the map json file.
     * @return A json array that was in the map json file.
     */
    private static JSONArray readMapFile() {
        try {
            InputStream inputStream = new FileInputStream(new File(mapFilename));
            return new JSONArray(new JSONTokener(inputStream));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates the countries array from the map json file. Creates new Country objects.
     * @return The countries array.
     */
    private Country[] createCountryArray() {
        JSONArray map = readMapFile();
        Country[] countries = new Country[map.length()];
        for (int i = 0; i < countries.length; i++) {
            JSONObject obj = map.getJSONObject(i);
            List<Integer> neighbors = new ArrayList<>();
            for (Object n: obj.getJSONArray("neighbors"))
                neighbors.add((Integer) n);

            Continent continent = this.continents.get(obj.getString("continent"));
            countries[i] = new Country(i, obj.getString("name"), continent, neighbors);
            continent.addCountry(countries[i]);
        }

        return countries;
    }

    /**
     * Creates the continents map. Creates new Continent objects.
     * @return The continents map, with names as continent keys and continent objects as values.
     */
    private Map<String, Continent> createContinentsMap() {
        Map<String, Continent> continents = new HashMap<>();
        for (String continentName : continentBonuses.keySet())
            continents.put(continentName, new Continent(continentName, continentBonuses.get(continentName)));

        return continents;
    }

    /**
     * A constructor that initializes a new game.
     */
    public GameManager() {
        this.gameId = gameIdCounter++;
        this.currentPlayerId = -1;
        this.players = new ArrayList<>();
        Collections.shuffle(this.playerColors);
        this.continents = createContinentsMap();
        this.countries = createCountryArray();
    }

    /**
     * Adds a player to the game.
     * Gives the player a random color, creates a new player object and adds it to the list.
     * @param name The name of the player.
     * @param socket The socket to communicate with the player.
     */
    public void addPlayer(String name, Session socket) {
        this.players.add(new Player(name, socket, this.playerColors.get(this.players.size())));
    }

    /**
     * Update the players and countries with the new state of the game.
     * Clears the countries from every player and gives him the countries he controls in the new state.
     * @param turnObj The json object with the new state.
     */
    public void updateGame(JSONObject turnObj) {
        for (Player player : this.players) {
            player.clearCountries();
            JSONObject playerCountriesObj = turnObj.getJSONObject("players").getJSONObject(player.getName()).getJSONObject("countries");
            for (String countryId : playerCountriesObj.keySet())
                player.addCountry(this.countries[Integer.parseInt(countryId)], playerCountriesObj.getInt(countryId));
        }
    }

    /**
     * @return Whether there are enough players to start the game.
     */
    public boolean readyToStart() {
        return this.players.size() == numOfPlayers;
    }

    /**
     * Deals the countries randomly between the players and gives them the number of initial troops they have at the beginning of the game (depends on number of players).
     */
    public void dealCountries() {
        int initialTroops = 50 - numOfPlayers * 5;
        List<Country> countriesToDeal = new ArrayList<>(Arrays.asList(this.countries));
        Collections.shuffle(countriesToDeal);
        for (int i = 0; i < this.players.size(); i++) {
            List<Country> playerInitialCountries = countriesToDeal.subList((i * countriesToDeal.size()) / this.players.size(), ((i + 1) * countriesToDeal.size()) / this.players.size());
            this.players.get(i).setInitialCountries(playerInitialCountries, initialTroops);
        }
    }

    /**
     * Creates a json object with the state of the game, in order to send to the players.
     * @return A string with the json object.
     */
    private String createTurnJSON() {
        do
            this.currentPlayerId = (this.currentPlayerId + 1) % numOfPlayers;
        while (this.players.get(this.currentPlayerId).hasLost());

        JSONObject turnObj = new JSONObject();
        turnObj.put("gameId", this.gameId);

        JSONObject playersObj = new JSONObject();
        for (Player player : this.players) {
            playersObj.put(player.getName(), player.getJSONObject());
        }

        turnObj.put("players", playersObj);
        Player currentPlayer = this.players.get(this.currentPlayerId);
        turnObj.put("currentPlayer", currentPlayer.getName());
        turnObj.put("newTroops", currentPlayer.getNumberOfNewTroops(this.continents.values()));

        return turnObj.toString();
    }

    /**
     * Sends the current state of the game to all the players.
     */
    public void sendStateToAllPlayers() {
        String turnJSON = this.createTurnJSON();
        for (Player player : this.players)
            player.send(turnJSON);
    }
}
