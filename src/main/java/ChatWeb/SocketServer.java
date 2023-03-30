package ChatWeb;

import ChatActions.ChatActions;
import ChatDatabase.DBManager;
import ChatSchema.User;
import ChatUtils.TinyJson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class SocketServer {
    public class Server implements Runnable {
        private volatile boolean runningFlag = true;
        private Socket socket;

        private int cntId;

        private DBManager dbManager;

        private User user = null;

        public DBManager getDbManager() {
            return dbManager;
        }

        public Server(int cntId, Socket socket, DBManager dbManager) {
            this.cntId = cntId;
            this.socket = socket;
            this.dbManager = dbManager;
        }

        public AtomicInteger getLastSignedUserId() {
            return lastSignedUserId;
        }


        public void shutdown() {
            runningFlag = false;
            logout();
            synchronized (mutex) {
                connectionUserId.remove(this.cntId);
            }

            try {
                this.socket.close();
                this.dbManager.close();
                System.out.println("End Connection " + socket);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }

        public void submitLogin(User user) {
            synchronized (mutex) {
                this.user = user;
                loggedUsers.put(user.username, cntId);
                usernameUIDs.put(user.uid, user.username);
            }

            var json = ChatActions.SUBMIT_LOGIN.getJsonTemplate().put("username", user.username).put("uid", user.uid);
            ChatActions.SUBMIT_LOGIN.doServerActions(this, json);
        }

        public void logout() {
            String username = null;
            if (user != null) {
                username = user.username;
                var json = ChatActions.SUBMIT_LOGOUT.getJsonTemplate().put("username", username);
                ChatActions.SUBMIT_LOGOUT.doServerActions(this, json);
                synchronized (mutex) {
                    loggedUsers.remove(user.username);
                    usernameUIDs.remove(user.uid);
                }
                user = null;
            }
        }

        public void packCurrentOnlineUsers(TinyJson json) {
            synchronized (mutex) {
                String[] usernames = new String[usernameUIDs.size()];
                int[] uids = new int[usernameUIDs.size()];
                int i = 0;
                for (Integer uid : usernameUIDs.keySet()) {
                    usernames[i] = usernameUIDs.get(uid);
                    uids[i] = uid;
                    i++;
                }
                json.put("uids", uids);
                json.put("usernames", usernames);
            }

        }

        void serve() {
            try {
                InputStream inputStream = socket.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
                TinyJson tinyJson = (TinyJson) objectInputStream.readObject();
                log("get", tinyJson);
                handleMessage(tinyJson);
            } catch (ClassNotFoundException e) {
                System.err.println("Not Found");
            } catch (IOException e) {
                System.out.println("IO failed!");
                shutdown();
            }
        }

        private TinyJson packSyncedData(TinyJson tinyJson, ArrayList<TinyJson> messages) {
            TinyJson[] feedback = new TinyJson[messages.size()];
            for (int i = 0; i < messages.size(); i++) {
                feedback[messages.size() - 1 - i] = messages.get(i);
            }
            tinyJson.put("data", feedback);
            return tinyJson;
        }

        public void syncData(TinyJson tinyJson, int numItems) {
            Thread serverSyncer = new Thread(() -> {
                var dbManager = new DBManager();
                if ((boolean) tinyJson.getPrimitive("isGroup")) {
                    try {
                        var messages = dbManager.syncGroupMessages(numItems);
                        sendJson(packSyncedData(tinyJson, messages));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    int toUid = (int) tinyJson.getPrimitive("toUserId");
                    int fromUserId = (int) tinyJson.getPrimitive("fromUserId");
                    try {
                        var messages = dbManager.syncChatMessages(fromUserId, toUid, numItems);
                        sendJson(packSyncedData(tinyJson, messages));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                }


                try {
                    dbManager.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
            serverSyncer.start();
        }

        void log(String method, TinyJson json) {
            System.out.println(socket + " " + method + ": " + json);
        }

        void handleMessage(TinyJson json) throws IOException {
            int actionId = (int) json.getPrimitive("actionId");
            ChatActions.values()[actionId].doServerActions(this, json);
        }

        public synchronized void sendJson(TinyJson json) {
            try {
                OutputStream outputStream = socket.getOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(json);
                log("sent", json);
            } catch (IOException e) {
                System.err.println("Unable to send JSON!!!!");
            }
        }

        public boolean post(int uid, TinyJson json) {
            try {
                String thatUsername = usernameUIDs.get(uid);
                Integer thatConnection = loggedUsers.get(thatUsername);
                Server thatServer = connectionUserId.get(thatConnection);
                thatServer.sendJson(json);
                return true;
            } catch (NullPointerException e) {
                System.err.println("unable to post data to " + uid);
                return false;
            }
        }

        public void broadcast(TinyJson json, boolean sendSelf) {
            synchronized (mutex) {
                for (int cntId : loggedUsers.values()) {
                    if (cntId != this.cntId || sendSelf) {
                        connectionUserId.get(cntId).sendJson(json);
                    }
                }
            }
        }


        void sendMessage(byte[] buffer) throws IOException {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(buffer);
        }

        public boolean isAlreadyLogged(User user) {
            synchronized (mutex) {
                return loggedUsers.containsKey(user.username);
            }
        }


        @Override
        public void run() {
            try {
                while (runningFlag) {
                    serve();
                }
            } finally {
                shutdown();
            }

        }
    }

    private final Object mutex = new Object(); // lock for the following three maps

    private TreeMap<Integer, Server> connectionUserId; // TreeMap
    private HashMap<String, Integer> loggedUsers; // username | connectId
    private HashMap<Integer, String> usernameUIDs; // uid | username


    private AtomicInteger lastSignedUserId = new AtomicInteger(1000);

    private ServerSocket serverSocket;

    private AtomicBoolean isRunning = new AtomicBoolean(true);


    private void prepareDatabase() {
        DBManager dbManager = new DBManager();
        try {
            dbManager.initTables();
            int id = dbManager.getLatestUserId();
            if (id != -1)
                lastSignedUserId.set(id);
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }

        System.out.println("Database init done, last user id: " + lastSignedUserId);
    }


    public SocketServer(int port) {
        prepareDatabase();
        connectionUserId = new TreeMap<>();
        loggedUsers = new HashMap<>();
        usernameUIDs = new HashMap<>();

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioException) {
            System.err.println("Unable to run server!");
            System.exit(-1);
        }
        // monitor thread to shut down this server
        Thread t = new Thread(() -> {
            System.out.println("monitor is running!");
            Scanner in = new Scanner(System.in);
            while (in.hasNext()) {
                if (in.next().equals("exit"))
                    this.shutdown();
            }
        });

        t.start();


        connectionUserId.put(0, null); // ensure that all connections start with 1.
    }

    public void start() {
        System.out.println("Server is running!");

        while (isRunning.get()) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException ioException) {
                System.err.println("accepts failed!");
                continue;
            }
            assert socket != null;
            System.out.println("a new connection is on! " + socket);

            insertConnection(new User(1), socket);
        }
    }


    private void insertConnection(User user, Socket socket) {
        synchronized (mutex) {
            TreeMap<Integer, Server> userTreeMap = this.connectionUserId;

            assert !userTreeMap.isEmpty();
            int connectId = userTreeMap.lastKey() + 1;
            Server server = new Server(connectId, socket, new DBManager());
            connectionUserId.put(connectId, server);
            Thread t = new Thread(server);
            t.start();
        }
    }

    public static void main(String[] args) throws SQLException {
        var JaChatDatabaseManager = new DBManager();
        JaChatDatabaseManager.initTables();
        var JaChatSocketServer = new SocketServer(SocketDefs.SOCKET_PORT);
        JaChatSocketServer.start();
    }

    private void shutdown() {
        isRunning.set(false);
        for (var entry : connectionUserId.entrySet()) {
            if (entry.getValue() == null)
                continue;
            entry.getValue().runningFlag = false;
            entry.getValue().shutdown();
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Bye!");
        System.exit(0);
    }
}