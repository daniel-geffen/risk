import org.json.JSONObject;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ServerEndpoint("/ws")
public class GameServer {

    private static List<GameManager> games = new ArrayList<>();

    public GameServer() {
        games.add(new GameManager());
    }

    @OnOpen
    public void onOpen(Session session) {

    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        JSONObject messageObj = new JSONObject(message);
        GameManager gameManager = games.get(0);
        gameManager.addPlayer(messageObj.getString("username"), session);

        if (gameManager.readyToStart()) {
            gameManager.dealCountries();
            gameManager.sendToAllPlayers(gameManager.createTurnJSON());

            games.add(0, new GameManager());
        }

    }

    @OnClose
    public void onClose(Session session) {
//        gameManager.removePlayer(session);
    }
}
