package ChatDatabase;

import ChatSchema.ChatMessage;
import ChatSchema.GroupMessage;
import ChatSchema.User;
import ChatUtils.TinyJson;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;

public class DBManager {
    // Default db user info in my computer
    // fix this in your computer
    private static final String url = "jdbc:mysql://localhost:3306/" + ServerDBConfig.DB_NAME;
    private static final String username = ServerDBConfig.USERNAME;
    private static final String password = ServerDBConfig.PASSWORD;


    private Connection connection = null;

    static boolean isTablesSetup = false;


    public void initTables() throws SQLException {

        final String getTableSQL = "show tables;";
        try (var tables = executeSQL(getTableSQL)) {
            HashSet<String> tableNames = new HashSet<>();
            while (tables.next()) {
                tableNames.add(tables.getString(1));
            }
            if (!tableNames.contains(User.class.getSimpleName())) {
                System.out.println("Init Table: " + User.class.getSimpleName());
                executeSQL(User.initTableStatement());
            }

            if (!tableNames.contains(ChatMessage.class.getSimpleName())) {
                System.out.println("Init Table: " + ChatMessage.class.getSimpleName());
                executeSQL(ChatMessage.initTableStatement());
            }

            /*
            Not Used by now
            if (!tableNames.contains(ChatGroup.class.getSimpleName())) {
                System.out.println("Init Table: " + ChatGroup.class.getSimpleName());
                executeSQL(ChatGroup.initTableStatement());
            }
            */

            if (!tableNames.contains(GroupMessage.class.getSimpleName())) {
                System.out.println("Init Table: " + GroupMessage.class.getSimpleName());
                executeSQL(GroupMessage.initTableStatement());
            }

            /*
            Not Used by now
            if (!tableNames.contains(GroupMember.class.getSimpleName())) {
                System.out.println("Init Table: " + GroupMember.class.getSimpleName());
                executeSQL(GroupMember.initTableStatement());
            }
            */
        }

    }

    private PreparedStatement storeGroupMessageStmt;
    private PreparedStatement storeChatMessageStmt;

    ResultSet executeSQL(final String SQLCommand) throws SQLException {
        Statement statement = connection.createStatement();
        var rs = statement.execute(SQLCommand);
        if (rs) {
            return statement.getResultSet();
        } else return null;
    }

    public DBManager() {
        try {
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Connection Established!");
            var tables = executeSQL("show tables;");
            while (tables.next()) {
                var foundType = tables.getString(1);
                System.out.println(foundType);
            }
            storeGroupMessageStmt = connection.prepareStatement("INSERT INTO GroupMessage VALUES (?,?, ?, ?, ?)");
            storeChatMessageStmt = connection.prepareStatement("INSERT INTO ChatMessage VALUES (?, ?, ?, ?, ?)");


        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
            System.out.println("Unable to Connect! " + sqlException);
        }


    }

    public void close() throws SQLException {
        if (connection != null) {
            storeChatMessageStmt.close();
            storeGroupMessageStmt.close();
            connection.close();
        }
    }


    public static void main(String[] args) throws SQLException {
        DBManager dbManager = new DBManager();
        dbManager.initTables();
        dbManager.close();
    }

    // to determine whether this user is in the User database table
    public boolean isQualifiedUser(User user) throws SQLException {

        var findPwdState = connection.prepareStatement("SELECT uid, password FROM User WHERE username = ? ");

        findPwdState.setString(1, user.username);
        findPwdState.execute();
        var rs = findPwdState.getResultSet();
        boolean flag = false;
        if (rs.next()) {
            if (user.password.equals(rs.getString(2))) {
                flag = true;
                user.uid = rs.getInt(1);
            }

        }
        rs.close();
        findPwdState.close();
        return flag;
    }

    // Add a new user to the database, ensures that this user must be new
    private void addNewUser(User user) throws SQLException {

        var insertState = connection.prepareStatement("INSERT INTO User VALUES (?, ?, ?)");
        insertState.setInt(1, user.uid);
        insertState.setString(2, user.username);
        insertState.setString(3, user.password);
        insertState.execute();
        insertState.close();
    }

    // to determine whether this user is a new user
    public boolean isValidNewUser(User user) throws SQLException {

        var findNewUserStatement = connection.prepareStatement("SELECT username from User WHERE username = ?");
        findNewUserStatement.setString(1, user.username);
        findNewUserStatement.execute();

        var rs = findNewUserStatement.getResultSet();
        boolean isValid = !rs.next();
        rs.close();
        findNewUserStatement.close();
        if (isValid) {
            addNewUser(user);
        }
        return isValid;
    }

    public int getLatestUserId() throws SQLException {
        var getIdStatement = connection.prepareStatement("SELECT uid from User ORDER BY uid DESC ");
        getIdStatement.execute();
        var rs = getIdStatement.getResultSet();
        int id = -1;
        if (rs.next()) {
            id = rs.getInt(1);
        }
        rs.close();
        getIdStatement.close();
        return id;
    }

    public void saveGroupMessage(GroupMessage groupMessage) throws SQLException {
        storeGroupMessageStmt.setInt(1, groupMessage.sendUserId);
        storeGroupMessageStmt.setString(2, groupMessage.sendUsername);
        storeGroupMessageStmt.setInt(3, groupMessage.groupId);
        storeGroupMessageStmt.setLong(4, groupMessage.unixTimeStamp);
        storeGroupMessageStmt.setString(5, groupMessage.contents);
        storeGroupMessageStmt.execute();
    }

    public void saveChatMessage(ChatMessage chatMessage) throws SQLException {
        storeChatMessageStmt.setInt(1, chatMessage.fromUserId);
        storeChatMessageStmt.setString(2, chatMessage.fromUsername);
        storeChatMessageStmt.setInt(3, chatMessage.toUserId);
        storeChatMessageStmt.setLong(4, chatMessage.unixTimeStamp);
        storeChatMessageStmt.setString(5, chatMessage.contents);
        storeChatMessageStmt.execute();
    }

    // fetch more recent 100
    public ArrayList<TinyJson> syncGroupMessages(int numItems) throws SQLException {
        var stmt = connection.prepareStatement("SELECT * FROM GroupMessage  ORDER BY unixTimeStamp DESC LIMIT " + numItems);
        stmt.execute();
        var rs = stmt.getResultSet();
        ArrayList<TinyJson> messages = new ArrayList<>();
        while (rs.next()) {
            GroupMessage groupMessage = new GroupMessage();
            groupMessage.sendUserId = rs.getInt(1);
            groupMessage.sendUsername = rs.getString(2);
            groupMessage.groupId = rs.getInt(3);
            groupMessage.unixTimeStamp = rs.getLong(4);
            groupMessage.contents = rs.getString(5);
            TinyJson json = new TinyJson(groupMessage);
            messages.add(json);
        }
        rs.close();
        stmt.close();
        return messages;
    }


    public ArrayList<TinyJson> syncChatMessages(int fromUserId, int toUserId, int numItems) throws SQLException {
        var stmt = connection.prepareStatement("SELECT * FROM  ChatMessage WHERE " +
                "fromUserId = ? AND  toUserId = ? " +
                "OR fromUserId = ? AND toUserId = ? ORDER BY unixTimeStamp DESC LIMIT " + numItems);
        stmt.setInt(1, fromUserId);
        stmt.setInt(2, toUserId);
        stmt.setInt(3, toUserId);
        stmt.setInt(4, fromUserId);
        stmt.execute();
        var rs = stmt.getResultSet();
        ArrayList<TinyJson> messages = new ArrayList<>();
        while (rs.next()) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.fromUserId = rs.getInt(1);
            chatMessage.fromUsername = rs.getString(2);
            chatMessage.toUserId = rs.getInt(3);
            chatMessage.unixTimeStamp = rs.getLong(4);
            chatMessage.contents = rs.getString(5);
            TinyJson tinyJson = new TinyJson(chatMessage);
            messages.add(tinyJson);
        }

        stmt.close();
        rs.close();
        return messages;
    }


}
