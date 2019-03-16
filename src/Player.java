import org.json.JSONObject;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Player {
    private String name;
    private Session socket;
    private String color;
    private HashMap<Country, Integer> countries;

    public Player(String name, Session socket, String color) {
        this.name = name;
        this.socket = socket;
        this.color = color;
    }

    public void send(String message) throws IOException {
        this.socket.getBasicRemote().sendText(message);
    }

    public String getName() {
        return this.name;
    }

    public boolean isSocket(Session session) {
        return this.socket.getId().equals(session.getId());
    }

    public void setCountries(List<Country> initialCountries, int initialTroops) {
        this.countries = new HashMap<>();
        Collections.shuffle(initialCountries);
        Random random = new Random();
        int maxTroopsInCountry = (int) ((initialTroops / (double) initialCountries.size()) * 2);

        for (int i = 0; i < initialCountries.size(); i++) {
            int troopsForCountry;
            if (i != initialCountries.size() - 1)
                troopsForCountry = random.nextInt(Math.min(maxTroopsInCountry, initialTroops - (initialCountries.size() - i))) + 1;
            else troopsForCountry = initialTroops;

            this.countries.put(initialCountries.get(i), troopsForCountry);
            initialTroops -= troopsForCountry;
        }
    }

    public JSONObject getJSONObject() {
        JSONObject playerObject = new JSONObject();
        playerObject.put("color", this.color);
        JSONObject countriesObject = new JSONObject();
        for (Country country : this.countries.keySet())
            countriesObject.put(country.getName(), this.countries.get(country));
        playerObject.put("countries", countriesObject);

        return playerObject;
    }
}
