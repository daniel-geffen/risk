/**
 * A class representing an AI player.
 */
public class AIPlayer extends Player {
    private GameManager game; // The game object, for the AI to have the map objects.

    public AIPlayer(int AICount, String color, GameManager game) {
        super("AI #" + AICount, color);
        this.game = game;
    }

    public void doTurn() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.game.finishTurn();
    }
}
