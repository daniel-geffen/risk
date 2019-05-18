import java.util.*;

/**
 * A class representing an AI player.
 */
public class AIPlayer extends Player {
    private static final double BREAK_INTO_ENEMY_CONTINENT_THRESHOLD = 0.7; // The minimum probability for a journey to break into an enemy continent.
    private static final double OTHER_ATTACKS_THRESHOLD = 0.75; // The minimum probability for good attacks.
    private static final int MILLISECONDS_TO_WAIT = 2000; // The number of milliseconds to wait at the end of the turn.

    private GameManager game; // The game object, for the AI to have the map objects.
    private int troopsToDraft; // The number of troops remaining to draft.

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
        this.troopsToDraft = this.getNumberOfNewTroops(this.game.getContinents());

        List<Continent> myContinents = this.getMyContinents();
        myContinents.sort(new Comparator<Continent>() {
            @Override
            public int compare(Continent c1, Continent c2) {
                return c1.getBorders().size() - c2.getBorders().size();
            }
        }); // So continents with fewer borders will be protected first.
        for (Continent continent : myContinents)
            addTroopsToProtectBorders(continent);

        List<Continent> enemyContinents = this.getEnemyContinents();
        enemyContinents.sort(new Comparator<Continent>() {
            @Override
            public int compare(Continent c1, Continent c2) {
                return c1.getTroopsBonus() - c2.getTroopsBonus();
            }
        }); // So continents with higher bonus will be attacked first.
        for (Continent continent : enemyContinents)
            this.breakIntoEnemyContinent(continent);

        Continent continentToConquer = this.chooseBestContinentToConquer();
        if (continentToConquer != null) {
            Country countryToConquerWith = this.getClosestCountry(continentToConquer);
            if (countryToConquerWith != null) {
                countryToConquerWith.addTroops(this.troopsToDraft);
                this.troopsToDraft = 0;
                this.conquerContinent(continentToConquer, countryToConquerWith);
            }
        }

        this.doOtherGoodAttacks();

        this.handleFortify();

        try {
            Thread.sleep(AIPlayer.MILLISECONDS_TO_WAIT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.game.finishTurn();
    }

    /**
     * @param continent A continent.
     * @return A map with the continent borders as keys and the number of opponent troops that are neighbors as values.
     */
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

    /**
     * @return A list with all of the continents that are fully controlled by me.
     */
    private List<Continent> getMyContinents() {
        List<Continent> myContinents = new ArrayList<>();
        for (Continent continent : this.game.getContinents()) {
            if (continent.getOwner() == this)
                myContinents.add(continent);
        }
        return myContinents;
    }

    /**
     * @return A list with all the continents that are fully controlled by an opponent.
     */
    private List<Continent> getEnemyContinents() {
        List<Continent> myContinents = new ArrayList<>();
        for (Continent continent : this.game.getContinents()) {
            Player continentOwner = continent.getOwner();
            if (continentOwner != null && continentOwner != this)
                myContinents.add(continent);
        }
        return myContinents;
    }

    /**
     * Adds troops to the borders of the continent until they are safe.
     * @param continent The continent to protect.
     */
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

    /**
     * @param source The country to start the journey from.
     * @param destination The target of the journey.
     * @return The probability all attacks from source to destination will be successful.
     */
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

    /**
     * A comparator got countries that compares based on the number of enemy neighbors the country has.
     */
    private class EnemyNeighborsComparator implements Comparator<Country> {

        @Override
        public int compare(Country c1, Country c2) {
            int numOfC1EnemyNeighbors = 0;
            for (Integer neighborId : c1.getNeighbors())
                if (AIPlayer.this.game.getCountries()[neighborId].getOwner() != AIPlayer.this)
                    numOfC1EnemyNeighbors++;

            int numOfC2EnemyNeighbors = 0;
            for (Integer neighborId : c2.getNeighbors())
                if (AIPlayer.this.game.getCountries()[neighborId].getOwner() != AIPlayer.this)
                    numOfC2EnemyNeighbors++;

            return numOfC1EnemyNeighbors - numOfC2EnemyNeighbors;
        }
    }

    /**
     * @param continent A continent.
     * @return The closest country controlled by me by distance to the continent.
     */
    private Country getClosestCountry(Continent continent) {
        List<Country> myCountriesInContinent = new ArrayList<>();
        for (Country myCountry : continent.getCountries())
            if (myCountry.getOwner() == this)
                myCountriesInContinent.add(myCountry);

        if (!myCountriesInContinent.isEmpty()) {
            return Collections.max(myCountriesInContinent, new EnemyNeighborsComparator());
        } else {
            Country bestCountry = null;
            float bestProbability = 0;

            for (Country country : this.countries)
                if (!this.getMyContinents().contains(country.getContinent())) {
                    Country closestContinentBorder = country.getClosestContinentBorder(continent, this.game.getCountries());
                    if (closestContinentBorder != null) {
                        float probabilityOfJourneySuccess = this.probabilityOfJourneySuccess(country, closestContinentBorder);
                        if (probabilityOfJourneySuccess > bestProbability) {
                            bestCountry = country;
                            bestProbability = probabilityOfJourneySuccess;
                        }
                    }
                }

            return bestCountry;
        }
    }

    /**
     * Chooses the best country and tries to attack in order to break into the continent.
     * @param continent An enemy continent.
     */
    private void breakIntoEnemyContinent(Continent continent) {
        Country bestCountry = this.getClosestCountry(continent);
        if (bestCountry != null) {
            Country bestBorder = bestCountry.getClosestContinentBorder(continent, this.game.getCountries());

            if (bestBorder != null) {
                int troopsAdded = 0;
                while (this.troopsToDraft > 0 && this.probabilityOfJourneySuccess(bestCountry, bestBorder) < AIPlayer.BREAK_INTO_ENEMY_CONTINENT_THRESHOLD) {
                    bestCountry.addTroops(1);
                    this.troopsToDraft--;
                    troopsAdded++;
                }

                if (this.troopsToDraft == 0) {
                    bestCountry.addTroops(-troopsAdded);
                    this.troopsToDraft = troopsAdded;
                } else
                    bestCountry.goOnAttackJourney(bestBorder, this.game.getCountries());
            }
        }
    }

    /**
     * @return The continent (not controlled by me) I should conquer next.
     */
    private Continent chooseBestContinentToConquer() {
        List<Continent> continents = this.game.getContinents();
        continents.removeAll(this.getMyContinents());

        return Collections.max(continents, new Comparator<Continent>() {
            @Override
            public int compare(Continent c1, Continent c2) {
                return Float.compare(c1.getContinentRating(AIPlayer.this), c2.getContinentRating(AIPlayer.this));
            }
        });
    }

    /**
     * Goes on a journey from countryToConquerWith to conquer the continent.
     * @param continent The continent to conquer.
     * @param countryToConquerWith The country to use to conquer the continent.
     */
    private void conquerContinent(Continent continent, Country countryToConquerWith) {
        if (countryToConquerWith.getContinent() != continent) {
            Country continentClosestBorder = countryToConquerWith.getClosestContinentBorder(continent, this.game.getCountries());
            if (continentClosestBorder != null && !countryToConquerWith.goOnAttackJourney(continentClosestBorder, this.game.getCountries()))
                return;
        }

        while (countryToConquerWith.getNumOfTroops() > 1) {
            List<Country> enemyNeighbors = new ArrayList<>();
            for (Integer neighborId : countryToConquerWith.getNeighbors()) {
                Country neighbor = this.game.getCountries()[neighborId];
                if (neighbor.getOwner() != this && neighbor.getContinent() == continent)
                    enemyNeighbors.add(neighbor);
            }

            if (enemyNeighbors.isEmpty()) return;

            Country enemyToAttack = Collections.max(enemyNeighbors, new EnemyNeighborsComparator());
            if (countryToConquerWith.attack(enemyToAttack, true))
                countryToConquerWith = enemyToAttack;
        }
    }

    /**
     * Do attacks that are with high probability of winning at the end of the turn.
     */
    private void doOtherGoodAttacks() {
        List<Continent> myContinents = this.getMyContinents();
        for (Country country : new ArrayList<>(this.countries))
            if (!myContinents.contains(country.getContinent())) {
                boolean shouldKeepAttacking = true;
                while (shouldKeepAttacking) {
                    List<Country> neighbors = new ArrayList<>();
                    for (Integer neighborId : country.getNeighbors())
                        if (this.game.getCountries()[neighborId].getOwner() != this)
                            neighbors.add(this.game.getCountries()[neighborId]);

                    if (!neighbors.isEmpty()) {
                        Country weakestNeighbor = Collections.min(neighbors);
                        shouldKeepAttacking = BattleUtils.percentageOfWinning(country.getNumOfTroops(), weakestNeighbor.getNumOfTroops()) > AIPlayer.OTHER_ATTACKS_THRESHOLD;
                        if (shouldKeepAttacking)
                            shouldKeepAttacking = country.attack(weakestNeighbor, true);
                        country = weakestNeighbor;
                    } else shouldKeepAttacking = false;

                }
            }
    }

    /**
     * @return The country with the most troops that is inside a continent controlled by me (not on its borders).
     */
    private Country getInnerCountryWithMostTroops() {
        List<Country> innerCountries = new ArrayList<>();
        for (Continent continent : this.getMyContinents()) {
            innerCountries.addAll(continent.getCountries());
            innerCountries.removeAll(continent.getBorders());
        }

        return innerCountries.isEmpty() ? null : Collections.max(innerCountries);
    }

    /**
     * Moves troops from within continents controlled by me to the most needing border.
     * @return Whether troops were moved.
     */
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

    /**
     * Handles the fortify stage.
     */
    private void handleFortify() {
        if (!this.moveTroopsFromWithinContinentsToBorders()) {

        }
    }
}
