import java.util.ArrayList;
import java.util.List;

public class Continent {
    private String name;
    private List<Country> countries;
    private int troopsBonus;

    public Continent(String name, int troopsBonus) {
        this.name = name;
        this.troopsBonus = troopsBonus;
        this.countries = new ArrayList<>();
    }

    public void addCountry(Country countryId) {
        this.countries.add(countryId);
    }

    public List<Country> getCountries() {
        return this.countries;
    }

    public int getTroopsBonus() {
        return this.troopsBonus;
    }
}
