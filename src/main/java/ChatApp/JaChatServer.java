package ChatApp;

import ChatWeb.SocketDefs;
import ChatWeb.SocketServer;

public class JaChatServer {
    public static void main(String[] args) {
        var JaChatSocketServer = new SocketServer(SocketDefs.SOCKET_PORT);
        JaChatSocketServer.start();
    }
}
