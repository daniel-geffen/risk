import org.json.JSONObject;

import javax.websocket.Session;
import java.io.IOException;
import java.util.*;

/**
 * A class representing a player in the game.
 */
public class Player {
    private String name; // The name of the player.
    private Session socket; // The socket used to communicate with the player.
    private String color; // The color of the player.
    private List<Country> countries; // A list of the countries the player occupies.

    /**
     * A constructor that sets object variables.
     */
    public Player(String name, Session socket, String color) {
        this.name = name;
        this.socket = socket;
        this.color = color;
    }

    /**
     * Sends the message to the player using his socket.
     * @param message The message to send.
     */
    public void send(String message) {
        try {
            this.socket.getBasicRemote().sendText(message);
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    public String getName() {
        return this.name;
    }

    /**
     * Clears the countries list.
     */
    public void clearCountries() {
        this.countries = new ArrayList<>();
    }

    /**
     * Occupies a country and adds it to the list.
     * @param country The country to occupy.
     * @param numOfTroops The number of troops in the new country.
     */
    public void addCountry(Country country, int numOfTroops) {
        country.occupy(this, numOfTroops);
        this.countries.add(country);
    }

    /**
     * Handles the process of distributing troops between the countries before the game starts.
     * It gives every country a random number of troops, while making sure there is at least one troop in every country.
     * @param initialCountries A list of the countries the player controls at the beginning of the game.
     * @param initialTroops The amount of troops the player receives at the beginning of the game.
     */
    public void setInitialCountries(List<Country> initialCountries, int initialTroops) {
        this.countries = new ArrayList<>();
        Random random = new Random();
        int maxTroopsInCountry = (int) ((initialTroops / (double) initialCountries.size()) * 2);

        for (int i = 0; i < initialCountries.size(); i++) {
            Country country = initialCountries.get(i);

            int troopsForCountry;
            if (i != initialCountries.size() - 1) {
                troopsForCountry = random.nextInt(Math.min(maxTroopsInCountry, initialTroops - (initialCountries.size() - i))) + 1;
            }
            else troopsForCountry = initialTroops;

            this.addCountry(country, troopsForCountry);
            initialTroops -= troopsForCountry;
        }
    }

    /**
     * @return A json object with all of the relevant data about the player, so that it can be sent to all players.
     */
    public JSONObject getJSONObject() {
        JSONObject playerObject = new JSONObject();
        playerObject.put("color", this.color);

        JSONObject countriesObject = new JSONObject();
        for (Country country : this.countries)
            countriesObject.put(country.getName(), country.getNumOfTroops());
        playerObject.put("countries", countriesObject);

        return playerObject;
    }

    /**
     * Calculates the number of troops the player should receive in this turn.
     * It is based on the number of countries he occupies and any continents he fully controls.
     * @param continents The list of continents.
     * @return The number of new troops the player should receive in this turn.
     */
    public int getNumberOfNewTroops(Collection<Continent> continents) {
        int continentBonuses = 0;
        for (Continent continent : continents) {
            if (this.countries.containsAll(continent.getCountries()))
                continentBonuses += continent.getTroopsBonus();

        }
        return Math.max(this.countries.size() / 3 + continentBonuses, 3);
    }

    /**
     * @return Whether the player has lost and shouldn't play anymore (doesn't control any countries).
     */
    public boolean hasLost() {
        return this.countries.isEmpty();
    }
}
