import java.util.List;

public class Country {
    private String name;
    private String continent;
    private List<Integer> neighbors;

    public Country(String name, String continent, List<Integer> neighbors) {
        this.name = name;
        this.continent = continent;
        this.neighbors = neighbors;
    }

    public String getName() {
        return name;
    }

    public String getContinent() {
        return continent;
    }
}
