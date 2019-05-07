import java.util.ArrayList;
import java.util.List;

/**
 * A class representing a continent on the map of the game.
 */
public class Continent {
    private String name; // The name of the continent.
    private List<Country> countries; // A list of the countries in the continent.
    private int troopsBonus; // The number of troops a player gets if he controls all the countries in this continent at the beginning of his turn.

    /**
     * A constructor that sets object variables.
     */
    public Continent(String name, int troopsBonus) {
        this.name = name;
        this.troopsBonus = troopsBonus;
        this.countries = new ArrayList<>();
    }

    /**
     * Add a country to the continent.
     * @param country The country to add.
     */
    public void addCountry(Country country) {
        this.countries.add(country);
    }

    public List<Country> getCountries() {
        return this.countries;
    }

    public int getTroopsBonus() {
        return this.troopsBonus;
    }

    public Player getOwner() {
        Player owner = this.countries.get(0).getOwner();
        for (Country country : this.countries) {
            if (owner != country.getOwner())
                return null;
        }

        return owner;
    }

    public List<Country> getBorders() {
        List<Integer> continentIds = new ArrayList<>();
        for (Country country : this.countries)
            continentIds.add(country.getId());

        List<Country> borders = new ArrayList<>();
        for (Country country : this.countries) {
            if (!continentIds.containsAll(country.getNeighbors()))
                borders.add(country);
        }

        return borders;
    }
}
