import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.websocket.Session;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GameManager {
    private static final String mapFilename = "/Users/tuvia/IdeaProjects/Risk/src/countries.json";
    private static final int initialTroops = 35;
    private static final int numOfPlayers = 3;

    private List<Player> players;
    private List<String> playerColors = Arrays.asList("rgb(58,118,207)", "rgb(100,61,166)", "rgb(42,175,157)", "rgb(108,126,83)", "rgb(55,101,206)", "rgb(34,135,174)");
    private Country[] countries;

    private JSONArray readMap() {
        try {
            InputStream inputStream = new FileInputStream(new File(mapFilename));
            return new JSONArray(new JSONTokener(inputStream));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void populateCountryArray(JSONArray map) {
        for (int i = 0; i < this.countries.length; i++) {
            JSONObject obj = map.getJSONObject(i);
            List<Integer> neighbors = new ArrayList<>();
            for (Object n: obj.getJSONArray("neighbors"))
                neighbors.add((Integer) n);

            this.countries[i] = new Country(obj.getString("name"), obj.getString("continent"), neighbors);
        }
    }

    public GameManager() {
        this.players = new ArrayList<>();
        Collections.shuffle(this.playerColors);
        JSONArray map = this.readMap();
        this.countries = new Country[map.length()];
        populateCountryArray(map);
    }

    public void addPlayer(String name, Session socket) {
        this.players.add(new Player(name, socket, this.playerColors.get(this.players.size())));
    }

    public void removePlayer(Session session) {
        Player playerToRemove = null;
        for (Player player : this.players) {
            if (player.isSocket(session))
                playerToRemove = player;
        }

        if (playerToRemove != null)
            this.players.remove(playerToRemove);
    }

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
        JSONObject playersObj = new JSONObject();
        for (Player player : this.players) {
            playersObj.put(player.getName(), player.getJSONObject());
        }

        turnObj.put("players", playersObj);
        turnObj.put("currentPlayer", this.players.get(0).getName());

        return turnObj.toString();
    }

    public void sendToAllPlayers(String message) throws IOException {
        for (Player player : this.players)
            player.send(message);
    }
}
