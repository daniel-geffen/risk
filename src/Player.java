import javax.websocket.Session;
import java.io.IOException;

public class Player {
    private String name;
    private Session socket;

    public Player(String name, Session socket) {
        this.name = name;
        this.socket = socket;
    }

    public void send(String message) throws IOException {
        this.socket.getBasicRemote().sendText(message);
    }

    public String getName() {
        return this.name;
    }

    public Session getSocket() {
        return socket;
    }
}
