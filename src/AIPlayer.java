public class AIPlayer extends Player {

    private GameManager game;

    public AIPlayer(String name, String color, GameManager game) {
        super(name, color);
        this.game = game;
    }
}
