/**
 * A class representing an AI player.
 */
public class AIPlayer extends Player {
    private GameManager game; // The game object, for the AI to have the map objects.

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
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.game.finishTurn();
    }
}
