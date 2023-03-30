package ChatGui;


import ChatActions.ChatActions;
import ChatUtils.TinyJson;
import ChatWeb.SocketClient;
import ChatWeb.SocketDefs;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GuiManager {

    public TinyJson isLogInSucceed(String username, String password) {
        if (username.isEmpty() || password.isEmpty())
            return null;
        TinyJson loginJson = ChatActions.SIGN_IN.getJsonTemplate().put("username", username).put("password", password);
        try {
            socketClient.sendJson(loginJson);
        } catch (IOException e) {
            e.printStackTrace();
            raiseLethalError("无法连接服务器");
        }


        TinyJson feedback;
        try {
            feedback = syncQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (feedback == null)
            return null;
        boolean isLogged = (boolean) feedback.getPrimitive("loginResult");
        if (isLogged) {
            getSocketClient().setCurrentUser(
                    (int) feedback.getPrimitive("uid"),
                    (String) feedback.getPrimitive("username")
            );
        }

        return feedback;
    }

    public TinyJson isSignUpSucceed(String username, String password) {
        var signUpJson = ChatActions.SIGN_UP.getJsonTemplate();
        signUpJson.put("username", username);
        signUpJson.put("password", password);
        try {
            socketClient.sendJson(signUpJson);
        } catch (IOException e) {
            e.printStackTrace();
            raiseLethalError("无法连接服务器");
        }

        try {
            return syncQueue.take(); // block this thread until receive the response form server
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


    private BlockingQueue<TinyJson> syncQueue;

    private SocketClient socketClient;

    private ChatFrame currentFrame = null;


    public GuiManager() {
        syncQueue = new LinkedBlockingQueue<>();
        socketClient = new SocketClient("localhost", SocketDefs.SOCKET_PORT, this);
        if (socketClient.start()) {
            start();
        } else {
            raiseLethalError("无法连接服务器");
        }
    }

    public void start() {
        ChatFrame initFrame = new WelcomeFrame();
        initFrame.setGuiManager(this);
        changeFrame(initFrame);
    }


    void changeFrame(ChatFrame chatFrame) {
        if (currentFrame != null)
            currentFrame.close();
        currentFrame = chatFrame;
        chatFrame.show();
    }

    public BlockingQueue<TinyJson> getSyncQueue() {
        return syncQueue;
    }

    public SocketClient getSocketClient() {
        return socketClient;
    }

    public ChatFrame getCurrentFrame() {
        return currentFrame;
    }

    public MainChatFrame getMainChatFrame() {
        return (MainChatFrame) currentFrame;
    }



    public static void main(String[] args) {
        GuiManager guiManager = new GuiManager();
    }

    public boolean isMainFrame() {
        return currentFrame instanceof MainChatFrame;
    }

    public void raiseMessage(String message) {
        JOptionPane.showMessageDialog(currentFrame.getFrame(), message, "消息", JOptionPane.PLAIN_MESSAGE);
    }

    public void raiseLethalError(String message) {
        JOptionPane.showMessageDialog(null, message, "错误", JOptionPane.PLAIN_MESSAGE);
        System.exit(-1);
    }
}
