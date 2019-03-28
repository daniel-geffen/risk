import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.websocket.Session;
import java.io.*;
import java.util.*;

public class GameManager {
    private static final String mapFilename = "/Users/tuvia/IdeaProjects/Risk/src/countries.json";
    private static final int initialTroops = 35;
    private static final int numOfPlayers = 3;
    private static int gameIdCounter = 1;
    private static final Map<String, Integer> continentBonuses = new HashMap<String, Integer>() {{
        put("Africa", 3);
        put("Asia", 7);
        put("Europe", 5);
        put("North America", 5);
        put("Oceania", 2);
        put("South America", 2);
    }};
    private static Country[] countries = getCountryArray();
    private static Map<String, Continent> continents = getContinents();

    private int gameId;
    private int turnNumber;
    private List<Player> players;
    private List<String> playerColors = Arrays.asList("rgb(58,118,207)", "rgb(100,61,166)", "rgb(42,175,157)", "rgb(108,126,83)", "rgb(55,101,206)", "rgb(34,135,174)");

    private static JSONArray readMapFile() {
        try {
            InputStream inputStream = new FileInputStream(new File(mapFilename));
            return new JSONArray(new JSONTokener(inputStream));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Country[] getCountryArray() {
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

    private static Map<String, Continent> getContinents() {
        Map<String, Continent> continents = new HashMap<>();
        for (String continentName : continentBonuses.keySet())
            continents.put(continentName, new Continent(continentName, continentBonuses.get(continentName)));

        for (Country country : countries)
            continents.get(country.getContinent()).addCountry(country);

        return continents;
    }

    public GameManager() {
        this.gameId = gameIdCounter++;
        this.turnNumber = 1;
        this.players = new ArrayList<>();
        Collections.shuffle(this.playerColors);
    }

    public void addPlayer(String name, Session socket) {
        this.players.add(new Player(name, socket, this.playerColors.get(this.players.size())));
    }

//    public void removePlayer(Session session) {
//        Player playerToRemove = null;
//        for (Player player : this.players) {
//            if (player.isSocket(session))
//                playerToRemove = player;
//        }
//
//        if (playerToRemove != null)
//            this.players.remove(playerToRemove);
//    }

    public boolean readyToStart() {
        return this.players.size() == numOfPlayers;
    }

    public void dealCountries() {
        List<Country> countriesToDeal = new ArrayList<>(Arrays.asList(this.countries));
        Collections.shuffle(countriesToDeal);
        for (int i = 0; i < this.players.size(); i++) {
            this.players.get(i).setCountries(countriesToDeal.subList((i * countriesToDeal.size()) / this.players.size(), ((i + 1) * countriesToDeal.size()) / this.players.size()), initialTroops);
        }
    }

    public String createTurnJSON() {
        JSONObject turnObj = new JSONObject();
        turnObj.put("gameId", this.gameId);

        JSONObject playersObj = new JSONObject();
        for (Player player : this.players) {
            playersObj.put(player.getName(), player.getJSONObject());
        }

        turnObj.put("players", playersObj);
        Player currentPlayer = this.players.get(this.turnNumber++ / numOfPlayers);
        turnObj.put("currentPlayer", currentPlayer.getName());
        turnObj.put("newTroops", currentPlayer.getNumberOfNewTroops(continents.values()));

        return turnObj.toString();
    }

    public void sendToAllPlayers(String message) throws IOException {
        for (Player player : this.players)
            player.send(message);
    }
}
