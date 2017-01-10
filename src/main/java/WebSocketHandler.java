import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.*;

@WebSocket
public  class  WebSocketHandler {

    @OnWebSocketConnect
    public void connected(Session session) {
        //Do nothing until client sends message with authentication
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Closed");
        Main.disconnectUser(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message)  throws IOException {
        Main.receiveMessage(session , message);
    }
}