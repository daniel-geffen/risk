import org.json.JSONObject;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ServerEndpoint("/ws")
public class GameServer {

    private static List<GameManager> games = new ArrayList<GameManager>() {{
        add(new GameManager());
    }};

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        JSONObject messageObj = new JSONObject(message);
        if (messageObj.has("gameId")) {
            GameManager game = games.get(messageObj.getInt("gameId"));
            game.updateGame(messageObj);
            game.sendStateToAllPlayers();
        } else {
            GameManager game = games.get(games.size() - 1);
            game.addPlayer(messageObj.getString("username"), session);

            if (game.readyToStart()) {
                game.dealCountries();
                game.sendStateToAllPlayers();

                games.add(new GameManager());
            }
        }
    }
}
