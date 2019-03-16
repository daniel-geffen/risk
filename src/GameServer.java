import org.json.JSONObject;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/ws")
public class GameServer {

    private static GameManager gameManager = new GameManager();

    @OnOpen
    public void onOpen(Session session) {

    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        JSONObject messageObj = new JSONObject(message);
        gameManager.addPlayer(messageObj.getString("username"), session);

        if (gameManager.readyToStart()) {
            gameManager.dealCountries();

            gameManager.sendToAllPlayers(gameManager.createTurnJSON());
        }

    }

    @OnClose
    public void onClose(Session session) {
        gameManager.removePlayer(session);
    }
}
