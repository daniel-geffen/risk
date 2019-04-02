import java.util.List;

public class Country {
    private String name;
    private String continent;
    private List<Integer> neighbors;
    private Player owner;
    private int numOfTroops;

    public Country(String name, String continent, List<Integer> neighbors) {
        this.name = name;
        this.continent = continent;
        this.neighbors = neighbors;
        this.owner = null;
    }

    public String getName() {
        return name;
    }

    public String getContinent() {
        return continent;
    }

    public void occupy(Player owner, int numOfTroops) {
        this.owner = owner;
        this.numOfTroops = numOfTroops;
    }

    public int getNumOfTroops() {
        return numOfTroops;
    }
}
