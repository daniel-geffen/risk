import javafx.util.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

public class BattleUtils {

    private static final int numOfSimulations = 10000;
    private static Map<Pair<Integer, Integer>, Float> cache = new HashMap<>();

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
