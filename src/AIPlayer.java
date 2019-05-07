import java.util.*;

/**
 * A class representing an AI player.
 */
public class AIPlayer extends Player {
    private static final double BREAK_INTO_ENEMY_CONTINENT_THRESHOLD = 0.7;
    private static final double OTHER_ATTACKS_THRESHOLD = 0.8;

    private GameManager game; // The game object, for the AI to have the map objects.
    private int troopsToDraft;

    /**
     * A constructor that creates a new for the AI and sets the variables.
     * @param AICount The number of this AI in the game (so the name will be unique).
     * @param color The color of the player.
     * @param game The game object to use to play turns.
     */
    public AIPlayer(int AICount, String color, GameManager game) {
        super("AI #" + AICount, color);
        this.game = game;
    }

    /**
     * Plays the turn for the AI.
     */
    public void doTurn() {
        this.troopsToDraft = this.getNumberOfNewTroops(game.getContinents());

        List<Continent> myContinents = this.getMyContinents();
        myContinents.sort(new Comparator<Continent>() {
            @Override
            public int compare(Continent c1, Continent c2) {
                return c1.getBorders().size() - c2.getBorders().size();
            }
        }); // So continents with fewer borders will be protected first.
        for (Continent continent : myContinents)
            addTroopsToProtectBorders(continent);

        Continent continentToConquer = this.chooseBestContinentToConquer();
        if (continentToConquer != null) {
            Country countryToConquerWith = this.getClosestCountry(continentToConquer);
            countryToConquerWith.addTroops(this.troopsToDraft);
            this.troopsToDraft = 0;
            this.conquerContinent(continentToConquer, countryToConquerWith);
        }

        List<Continent> enemyContinents = this.getEnemyContinents();
        enemyContinents.sort(new Comparator<Continent>() {
            @Override
            public int compare(Continent c1, Continent c2) {
                return c1.getTroopsBonus() - c2.getTroopsBonus();
            }
        }); // So continents with higher bonus will be attacked first.
        for (Continent continent : enemyContinents)
            this.breakIntoEnemyContinent(continent);

        this.doOtherGoodAttacks();

        this.handleFortify();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.game.finishTurn();
    }

    private Map<Country, Integer> getOpponentTroopsOnBorders(Continent continent) {
        Map<Country, Integer> opponentTroopsOnBorder = new HashMap<>();

        List<Country> borders = continent.getBorders();
        for (Country border : borders) {
            int opponentTroops = 0;
            for (int neighborId : border.getNeighbors()) {
                Country neighbor = this.game.getCountries()[neighborId];
                if (neighbor.getOwner() != this)
                    opponentTroops += neighbor.getNumOfTroops();
            }

            opponentTroopsOnBorder.put(border, opponentTroops);
        }

        return opponentTroopsOnBorder;
    }

    private List<Continent> getMyContinents() {
        List<Continent> myContinents = new ArrayList<>();
        for (Continent continent : this.game.getContinents()) {
            if (continent.getOwner() == this)
                myContinents.add(continent);
        }
        return myContinents;
    }

    private List<Continent> getEnemyContinents() {
        List<Continent> myContinents = new ArrayList<>();
        for (Continent continent : this.game.getContinents()) {
            Player continentOwner = continent.getOwner();
            if (continentOwner != null && continentOwner != this)
                myContinents.add(continent);
        }
        return myContinents;
    }

    private void addTroopsToProtectBorders(Continent continent) {
        Map<Country, Integer> continentBorders = this.getOpponentTroopsOnBorders(continent);
        for (Country border : continentBorders.keySet())
            if (border.getNumOfTroops() < continentBorders.get(border)) {
                int troopsToAdd = Math.max(continentBorders.get(border) - border.getNumOfTroops(), this.troopsToDraft);
                border.addTroops(troopsToAdd);
                this.troopsToDraft -= troopsToAdd;
                if (this.troopsToDraft == 0) return;
            }
    }

    private float probabilityOfJourneySuccess(Country source, Country destination) {
        Stack<Country> path = source.getPathToRival(destination, this.game.getCountries());
        if (path.isEmpty()) return 0;
        float journeyProbability = 1;
        int troopsRemaining = source.getNumOfTroops();
        while (!path.isEmpty()) {
            float battleProbability = BattleUtils.percentageOfWinning(troopsRemaining, path.pop().getNumOfTroops());
            journeyProbability *= battleProbability;
            troopsRemaining = Math.round(battleProbability * troopsRemaining);
        }
        return journeyProbability;
    }

    private Country getClosestCountry(Continent continent) {
        Country bestCountry = null;
        float bestProbability = 0;

        for (Country country : this.countries)
            if (!this.getMyContinents().contains(country.getContinent())) {
                float probabilityOfJourneySuccess = this.probabilityOfJourneySuccess(country, country.getClosestContinentBorder(continent, this.game.getCountries()));
                if (probabilityOfJourneySuccess > bestProbability) {
                    bestCountry = country;
                    bestProbability = probabilityOfJourneySuccess;
                }
            }

        return bestCountry;
    }

    private void breakIntoEnemyContinent(Continent continent) {
        Country bestCountry = this.getClosestCountry(continent);
        Country bestBorder = bestCountry.getClosestContinentBorder(continent, game.getCountries());

        if (this.probabilityOfJourneySuccess(bestCountry, bestBorder) > BREAK_INTO_ENEMY_CONTINENT_THRESHOLD)
            bestCountry.goOnAttackJourney(bestBorder, this.game.getCountries());
    }

    private Continent chooseBestContinentToConquer() {
        // TODO
        return null;
    }

    private void conquerContinent(Continent continent, Country countryToConquerWith) {
        Country continentClosestBorder = countryToConquerWith.getClosestContinentBorder(continent, this.game.getCountries());
        if (countryToConquerWith.goOnAttackJourney(continentClosestBorder, this.game.getCountries())) {
            // TODO
        }
    }

    private void doOtherGoodAttacks() {
        List<Continent> myContinents = this.getMyContinents();
        for (Country country : new ArrayList<>(this.countries))
            if (!myContinents.contains(country.getContinent())) {
                boolean shouldKeepAttacking = true;
                while (shouldKeepAttacking) {
                    List<Country> neighbors = new ArrayList<>();
                    for (Integer neighborId : country.getNeighbors())
                        if (game.getCountries()[neighborId].getOwner() != this)
                            neighbors.add(game.getCountries()[neighborId]);

                    if (!neighbors.isEmpty()) {
                        Country weakestNeighbor = Collections.min(neighbors);
                        shouldKeepAttacking = BattleUtils.percentageOfWinning(country.getNumOfTroops(), weakestNeighbor.getNumOfTroops()) > OTHER_ATTACKS_THRESHOLD;
                        if (shouldKeepAttacking)
                            shouldKeepAttacking = country.attack(weakestNeighbor, true);
                        country = weakestNeighbor;
                    } else shouldKeepAttacking = false;

                }
            }
    }

    private Country getInnerCountryWithMostTroops() {
        List<Country> innerCountries = new ArrayList<>();
        for (Continent continent : this.getMyContinents()) {
            innerCountries.addAll(continent.getCountries());
            innerCountries.removeAll(continent.getBorders());
        }

        return innerCountries.isEmpty() ? null : Collections.max(innerCountries);
    }

    private boolean moveTroopsFromWithinContinentsToBorders() {
        Country bestInnerCountry = this.getInnerCountryWithMostTroops();
        if (bestInnerCountry == null) return false;
        Map<Country, Integer> continentBorders = this.getOpponentTroopsOnBorders(bestInnerCountry.getContinent());
        Country weakestBorder = Collections.min(continentBorders.keySet(), new Comparator<Country>() {
            @Override
            public int compare(Country c1, Country c2) {
                int c1Percent = (int) (BattleUtils.percentageOfWinning(c1.getNumOfTroops(), continentBorders.get(c1)) * 100);
                int c2Percent = (int) (BattleUtils.percentageOfWinning(c2.getNumOfTroops(), continentBorders.get(c2)) * 100);
                return c1Percent - c2Percent;
            }
        });

        if (continentBorders.get(weakestBorder) != 0) {
            weakestBorder.addTroops(bestInnerCountry.getNumOfTroops() - 1);
            bestInnerCountry.occupy(this, 1);
            return true;
        }

        return false;
    }

    private void handleFortify() {
        if (!this.moveTroopsFromWithinContinentsToBorders()) {
            // TODO
        }
    }
}
