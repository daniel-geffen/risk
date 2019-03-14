import org.json.JSONObject;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;

@ServerEndpoint("/ws")
public class GameServer {

    private static ArrayList<Player> players = new ArrayList<>();

    @OnOpen
    public void onOpen(Session session) {

    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        JSONObject messageObj = new JSONObject(message);
        GameServer.players.add(new Player(messageObj.getString("username"), session));

        if (GameServer.players.size() == 3) {
            String response = "{\"players\":{\"Player1\":{\"color\":\"rgb(58,118,207)\",\"countries\":{\"North Africa\":2,\"Western Europe\":6}},\"Player2\":{\"color\":\"rgb(108,126,83)\",\"countries\":{\"Brazil\":3,\"Central America\":7,\"Venezuela\":5}},\"Player3\":{\"color\":\"rgb(42,175,157)\",\"countries\":{\"Middle East\":4,\"Ukraine\":2}}},\"currentPlayer\":\"Player2\"}";
            for (int i = 0; i < GameServer.players.size(); i++)
                response = response.replace("Player" + (i + 1), GameServer.players.get(i).getName());

            for (Player player : GameServer.players)
                player.send(response);
        }

    }

    @OnClose
    public void onClose(Session session) {
        Player playerToRemove = null;
        for (Player player : GameServer.players) {
            if (player.getSocket().getId().equals(session.getId()))
                playerToRemove = player;
        }

        if (playerToRemove != null)
            GameServer.players.remove(playerToRemove);
    }
}
