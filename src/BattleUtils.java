import javafx.util.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * A class with static functions used for handling battles in the game.
 */
public class BattleUtils {
    private static final int numOfSimulations = 10000; // The number of simulations that should be run in the monte carlo algorithm.
    private static Map<Pair<Integer, Integer>, Float> cache = new HashMap<>(); // A map for the cache of the percentageOfWinning function.

    /**
     * Simulates a battle (with dice). Simulates rounds until the defense is out of troops or the offense has only 1 left.
     * @param numOfAttackingTroops The number of troops on the attacking country.
     * @param numOfDefendingTroops The number of troops on the defending country.
     * @return A pair with the remaining number of troops on each side of the battle.
     */
    public static Pair<Integer, Integer> simulateBattle(int numOfAttackingTroops, int numOfDefendingTroops) {
        Random random = new Random();

        while (numOfAttackingTroops > 1 && numOfDefendingTroops > 0) {
            int numOfAttackDice = Math.min(numOfAttackingTroops - 1, 3), numOfDefendDice = Math.min(numOfDefendingTroops, 2);
            IntStream attackingDice = random.ints(numOfAttackDice, 1, 7).sorted().skip(Math.max(numOfAttackDice - numOfDefendDice, 0));
            IntStream defendingDice = random.ints(numOfDefendDice, 1, 7).sorted().skip(Math.max(numOfDefendDice - numOfAttackDice, 0));

            Iterator<Integer> attackingIterator = attackingDice.iterator(), defendingIterator = defendingDice.iterator();
            while (attackingIterator.hasNext()) {
                if (attackingIterator.next() > defendingIterator.next()) numOfDefendingTroops--;
                else numOfAttackingTroops--;
            }
        }

        return new Pair<>(numOfAttackingTroops, numOfDefendingTroops);
    }

    /**
     * Implementation of the monte carlo algorithm, to determine the probability of the attacker winning a battle in the game.
     * Runs a large number of simulations and counts number of wins and number of losses for the attacker.
     * The return values of the function are cached, so that a large number of simulations is only ran for new parameters.
     * @param numOfAttackingTroops The number of troops on the attacking country.
     * @param numOfDefendingTroops The number of troops on the defending country.
     * @return The probability the attacker wins the battle.
     */
    public static float percentageOfWinning(int numOfAttackingTroops, int numOfDefendingTroops) {
        Pair<Integer, Integer> pair = new Pair<>(numOfAttackingTroops, numOfDefendingTroops);
        if (cache.containsKey(pair)) return cache.get(pair);

        int countAttackWins = 0;
        for (int i = 0; i < numOfSimulations; i++) {
            int defendingTroopsAfterBattle = simulateBattle(numOfAttackingTroops, numOfDefendingTroops).getValue();
            if (defendingTroopsAfterBattle == 0) countAttackWins++;
        }

        float result = (float) countAttackWins / numOfSimulations;
        cache.put(pair, result);
        return result;
    }
}
