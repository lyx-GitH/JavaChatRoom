package ChatWeb;

import ChatActions.ChatActions;
import ChatGui.GuiManager;
import ChatGui.MainChatFrame;
import ChatSchema.ChatMessage;
import ChatSchema.GroupMessage;
import ChatSchema.User;
import ChatUtils.TinyJson;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketClient {

    private final Object mutex = new Object();
    private final GuiManager guiManager;

    private final String hostname;
    private final int port;

    private Socket socket;

    private Thread receiveThread;

    private User user = null;

    public class Receiver implements Runnable {
        private volatile boolean runningFlag = true;

        private void receiveFromServer() throws IOException {
            var inputStream = socket.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            try {
                TinyJson json = (TinyJson) objectInputStream.readObject();
                System.out.println("receive json: " + json);
                int actionId = (int) json.getPrimitive("actionId");
                ChatActions.values()[actionId].doClientActions(this, json);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void addLoggedUser(String username, int uid) {
            if (guiManager.isMainFrame()) {
                ((MainChatFrame) guiManager.getCurrentFrame()).addOnlineUser(username, uid);
            }
        }

        public void removeLoggedUser(String username) {
            if (guiManager.isMainFrame()) {
                if (guiManager.getSocketClient().getCurrentUser().username.equals(username)) {
                    guiManager.raiseLethalError("服务器已将你踢出聊天室");
                } else ((MainChatFrame) guiManager.getCurrentFrame()).removeOnlineUser(username);
            }
        }

        public void emitJson(TinyJson json) {
            System.out.println("emit json: " + json);
            guiManager.getSyncQueue().add(json);
        }

        public void emitMessage(String message) {
            guiManager.raiseMessage(message);
        }

        public void onReceiveGroupMessage(GroupMessage groupMessage) {
            doWhenReceiveGroupMessage(groupMessage);
        }

        public void onReceiveChatMessage(ChatMessage chatMessage) {
            doWhenReceiveChatMessage(chatMessage);
        }


        public void unpackOnlineUsers(TinyJson json) {
            assert guiManager.isMainFrame();
            var mainChatFrame = (MainChatFrame) guiManager.getCurrentFrame();
            mainChatFrame.initUserList(json);
        }

        public void noteSyncDone(int uid, TinyJson json) {
            if (guiManager.isMainFrame()) {
                guiManager.getMainChatFrame().letSyncFinish(uid, json);
            }
        }


        public void shutdown() {
            runningFlag = false;
            try {
                socket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
                System.exit(-1);
            }
        }

        @Override
        public void run() {
            System.out.println("Start Listen!");
            while (runningFlag) {
                try {
                    receiveFromServer();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }

    }

    public SocketClient(String hostname, int port, GuiManager guiManager) {
        this.guiManager = guiManager;
        this.hostname = hostname;
        this.port = port;
    }

    public void setCurrentUser(int uid, String username) {
        synchronized (mutex) {
            if (user == null) {
                user = new User();
            }
            user.uid = uid;
            user.username = username;
        }

        System.out.println("User logged in. uid = " + uid + " username = " + username);
    }

    public User getCurrentUser() {
        return user;
    }

    public boolean start() {
        try {
            socket = new Socket(hostname, port);
        } catch (IOException ioException) {
            return false;
        }
        receiveThread = new Thread(new Receiver());
        receiveThread.start();
        return true;

    }


    public synchronized void sendJson(TinyJson json) throws IOException {
        System.out.println("send json: " + json);
        OutputStream outputStream = socket.getOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(json);
    }

    public void doWhenReceiveGroupMessage(GroupMessage message) {
        String output = message.format(message.sendUserId == getCurrentUser().uid);
        MainChatFrame mainChatFrame = guiManager.getMainChatFrame();
        mainChatFrame.getUserContainers().get(0).addMessage(output);
        if (guiManager.getMainChatFrame().getCurrentTalkingUser() != 0) {
            mainChatFrame.updateUnreadGroupMessage();
        }

    }

    public void doWhenReceiveChatMessage(ChatMessage message) {
        String output = message.format(message.fromUserId == getCurrentUser().uid);
        MainChatFrame mainChatFrame = (MainChatFrame) guiManager.getCurrentFrame();
        int targetId = message.fromUserId == getCurrentUser().uid ? message.toUserId : message.fromUserId;
        var container = mainChatFrame.getUserContainers().get(targetId);
        container.addMessage(output);
        mainChatFrame.relocateOnlineUser(message.fromUsername);
    }


    // send a message to the server to shut down this session
    public void shutdown() {
        var shutDownJson = ChatActions.SHUTDOWN.getJsonTemplate();
        try {
            sendJson(shutDownJson);
            receiveThread.join();
        } catch (IOException e) {
            System.err.println("Unable to send!");
            e.printStackTrace();
            receiveThread.interrupt();
            System.exit(-1);
        } catch (InterruptedException e) {
            System.err.println("join failed!");
            System.exit(-1);
        }

    }

}
