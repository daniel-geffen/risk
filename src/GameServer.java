import org.json.JSONObject;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that handles the incoming communication from the server. Functions as the server endpoint.
 */
@ServerEndpoint("/ws")
public class GameServer {

    private static List<GameManager> games = new ArrayList<GameManager>() {{
        add(new GameManager());
    }}; // A list of the games that are happening. The index of the list is the game id.

    /**
     * Finds the relevant game using the game id, updates it with the new state and sends the new state to all the players.
     * @param messageObj The json object with the new state of the game.
     */
    private static void updateGameOfExistingPlayer(JSONObject messageObj) {
        GameManager game = games.get(messageObj.getInt("gameId"));
        game.updateGame(messageObj);
        game.sendStateToAllPlayers();
    }

    /**
     * Adds a new player to the last game (always a game that hasn't started).
     * If there are enough players, it starts the game and creates a new one for future players.
     * @param session The socket of the new player.
     * @param name The name of the new player.
     */
    private static void addNewPlayerToGame(Session session, String name) {
        GameManager game = games.get(games.size() - 1);
        game.addHumanPlayer(name, session);

        if (game.readyToStart()) {
            game.dealCountries();
            game.sendStateToAllPlayers();

            games.add(new GameManager());
        }
    }

    /**
     * Implements the OnMessage function - listens to new messages to the server.
     * It updates the game of an existing player or adds a new player to a game.
     * @param message The message received from the player.
     * @param session The socket used to communicate with the player.
     */
    @OnMessage
    public static void onMessage(String message, Session session) {
        JSONObject messageObj = new JSONObject(message);
        if (messageObj.has("gameId")) updateGameOfExistingPlayer(messageObj);
        else addNewPlayerToGame(session, messageObj.getString("username"));
    }
}
