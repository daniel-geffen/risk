import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.websocket.Session;
import java.io.*;
import java.util.*;

public class GameManager {
    private static final String mapFilename = "/Users/tuvia/IdeaProjects/Risk/src/countries.json";
    private static final int numOfPlayers = 3;
    private static int gameIdCounter = 0;
    private static final Map<String, Integer> continentBonuses = new HashMap<String, Integer>() {{
        put("Africa", 3);
        put("Asia", 7);
        put("Europe", 5);
        put("North America", 5);
        put("Oceania", 2);
        put("South America", 2);
    }};

    private int gameId;
    private int turnNumber;
    private int currentPlayerId;
    private List<Player> players;
    private List<String> playerColors = Arrays.asList("rgb(58,118,207)", "rgb(100,61,166)", "rgb(42,175,157)", "rgb(108,126,83)", "rgb(55,101,206)", "rgb(34,135,174)");
    private Country[] countries;
    private Map<String, Continent> continents;

    private static JSONArray readMapFile() {
        try {
            InputStream inputStream = new FileInputStream(new File(mapFilename));
            return new JSONArray(new JSONTokener(inputStream));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Country[] getCountryArray() {
        JSONArray map = readMapFile();
        Country[] countries = new Country[map.length()];
        for (int i = 0; i < countries.length; i++) {
            JSONObject obj = map.getJSONObject(i);
            List<Integer> neighbors = new ArrayList<>();
            for (Object n: obj.getJSONArray("neighbors"))
                neighbors.add((Integer) n);

            countries[i] = new Country(obj.getString("name"), obj.getString("continent"), neighbors);
        }

        return countries;
    }

    private Map<String, Continent> getContinents() {
        Map<String, Continent> continents = new HashMap<>();
        for (String continentName : continentBonuses.keySet())
            continents.put(continentName, new Continent(continentName, continentBonuses.get(continentName)));

        for (Country country : countries)
            continents.get(country.getContinent()).addCountry(country);

        return continents;
    }

    public GameManager() {
        this.gameId = gameIdCounter++;
        this.turnNumber = -1;
        this.currentPlayerId = -1;
        this.players = new ArrayList<>();
        Collections.shuffle(this.playerColors);
        this.countries = getCountryArray();
        this.continents = getContinents();
    }

    public void addPlayer(String name, Session socket) {
        this.players.add(new Player(name, socket, this.playerColors.get(this.players.size())));
    }

    public void updateGame(JSONObject turnObj) {
        for (Player player : this.players) {
            player.clearCountries();
            JSONObject playerCountriesObj = turnObj.getJSONObject("players").getJSONObject(player.getName()).getJSONObject("countries");
            for (String countryId : playerCountriesObj.keySet())
                player.addCountry(this.countries[Integer.parseInt(countryId)], playerCountriesObj.getInt(countryId));
        }
    }
    
    public boolean readyToStart() {
        return this.players.size() == numOfPlayers;
    }

    public void dealCountries() {
        int initialTroops = 35 - (numOfPlayers - 3) * 5;
        List<Country> countriesToDeal = new ArrayList<>(Arrays.asList(countries));
        Collections.shuffle(countriesToDeal);
        for (int i = 0; i < this.players.size(); i++) {
            this.players.get(i).setInitialCountries(countriesToDeal.subList((i * countriesToDeal.size()) / this.players.size(), ((i + 1) * countriesToDeal.size()) / this.players.size()), initialTroops);
        }
    }

    private String createTurnJSON() {
        this.turnNumber++;
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

    public void sendStateToAllPlayers() throws IOException {
        String turnJSON = this.createTurnJSON();
        for (Player player : this.players)
            player.send(turnJSON);
    }
}
