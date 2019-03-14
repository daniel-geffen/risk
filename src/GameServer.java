import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/ws")
public class GameServer {

    @OnOpen
    public void onOpen(Session session) throws IOException {
        String response = "{\"players\":{\"Player1\":{\"color\":\"rgb(58,118,207)\",\"countries\":{\"North Africa\":2,\"Western Europe\":6}},\"Player2\":{\"color\":\"rgb(108,126,83)\",\"countries\":{\"Brazil\":3,\"Central America\":7,\"Venezuela\":5}},\"Player3\":{\"color\":\"rgb(42,175,157)\",\"countries\":{\"Middle East\":4,\"Ukraine\":2}}},\"currentPlayer\":\"Player1\"}";
        session.getBasicRemote().sendText(response);
    }

    @OnClose
    public void onClose(Session session) {

    }

    @OnMessage
    public void onMessage(String message, Session session) {

    }
}
