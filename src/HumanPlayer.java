import javax.websocket.Session;
import java.io.IOException;

public class HumanPlayer extends Player {
    private Session socket; // The socket used to communicate with the player.

    public HumanPlayer(String name, String color, Session socket) {
        super(name, color);
        this.socket = socket;
    }

    /**
     * Sends the message to the player using his socket.
     * @param message The message to send.
     */
    public void send(String message) {
        try {
            this.socket.getBasicRemote().sendText(message);
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }
}
