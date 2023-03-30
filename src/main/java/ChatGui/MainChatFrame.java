package ChatGui;

import ChatActions.ChatActions;
import ChatSchema.Message;
import ChatUtils.MessageContainer;
import ChatUtils.TinyJson;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;


public class MainChatFrame extends ChatFrame {
    private JTextArea chatArea;

    private JTextArea inputArea;

    private JTable onlineUserTable;

    public JTextArea getChatArea() {
        return chatArea;
    }

    //    private HashMap<String, Integer> onlineUsers = new HashMap<>();
    private HashMap<Integer, Document> userDocuments = new HashMap<>();
    private HashMap<Integer, MessageContainer> userContainers = new HashMap<>();


    public HashMap<Integer, MessageContainer> getUserContainers() {
        return userContainers;
    }

    private int currentTalkingUser = 0; // 0 for global chatroom, other for valid UIDs

    private LinkedList<String> onlineUsernames = new LinkedList<>();
    private LinkedList<Integer> onlineUserIds = new LinkedList<>();

    public int getCurrentTalkingUser() {
        return currentTalkingUser;
    }

    class TableCellRender extends DefaultTableCellRenderer {

        public TableCellRender() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (Objects.equals(value, String.valueOf(currentTalkingUser)) || currentTalkingUser == 0 && row == 0 && column == 0) {
                setForeground(Color.black);
                setBackground(Color.yellow);
            } else if(column == 2 && Objects.equals(value, "*")) {
                setForeground(Color.black);
                setBackground(Color.blue);
            }
            else {
                setBackground(Color.white);
                setForeground(Color.black);
            }
            setText(value != null ? value.toString() : "");
            return this;
        }
    }

    // init frame layout
    void init() {
        frame = new JFrame("JaChat聊天室");
        setDefaultProperties();

        chatArea = new JTextArea();
        // set auto-line and no-edit attrs
        chatArea.setLineWrap(true);
        chatArea.setEditable(false);
        // set a scroll bar of the chatArea
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setBounds(20, 20, 600, 360);
        frame.add(chatScrollPane);

        // the input area
        inputArea = new JTextArea();
        inputArea.setBounds(20, 400, 600, 60);
        frame.add(inputArea);

        JButton button = new JButton("发送");
        button.setBounds(440, 500, 160, 30);
        frame.add(button);

        button.addActionListener((ActionEvent e) -> {
            String message = inputArea.getText();
            if (!message.isEmpty()) {
                if (message.length() > Message.MAX_CONTENTS) {
                    guiManager.raiseMessage("消息不可超过%d个字符".formatted(Message.MAX_CONTENTS));
                } else {
                    boolean isSent = false;
                    if (currentTalkingUser == 0) {
                        isSent = sendBroadcastMessage(message);
                    } else {
                        isSent = sendPointMessage(message, currentTalkingUser);
                    }

                    if (!isSent) {
                        guiManager.raiseMessage("无法送达，对方可能已下线！");
                    } else {
                        inputArea.setText("");
                    }

                }
            }
        });

        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    button.doClick();
                    inputArea.setText("");
                }
            }
        });

        String[] columns = new String[]{"用户号", "用户名", "是否未读"};
        String[][] rows = null;

        onlineUserTable = new JTable(new DefaultTableModel(rows, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });

        onlineUserTable.setShowHorizontalLines(true);
        onlineUserTable.setShowVerticalLines(true);

        onlineUserTable.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
               try{
                DefaultTableModel tbm = (DefaultTableModel) onlineUserTable.getModel();
                int selectedIndex = onlineUserTable.getSelectedRow();
                if (selectedIndex == 0) {
                    leaveToChatTo(0);
                } else {
                    String uidS = (String) tbm.getValueAt(selectedIndex, 0);
                    tbm.setValueAt("", selectedIndex, 2);
                    int uid = Integer.parseInt(uidS);
                    leaveToChatTo(uid);
                }}finally {
                   renderOnlineTable(false);
               }

            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        // Scroll bar for online tables
        JScrollPane onlineTableScroll = new JScrollPane(onlineUserTable);
        onlineTableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        onlineTableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        onlineTableScroll.setBounds(640, 20, 250, 400);
        frame.add(onlineTableScroll);

    }

    public int getThisUserId() {
        return guiManager.getSocketClient().getCurrentUser().uid;
    }

    boolean sendBroadcastMessage(String message) {
        var json = ChatActions.BROADCAST.getJsonTemplate();
        json.put("groupId", 0);
        json.put("sendUserId", guiManager.getSocketClient().getCurrentUser().uid);
        json.put("unixTimeStamp", System.currentTimeMillis());
        json.put("contents", message);
        json.put("sendUsername", guiManager.getSocketClient().getCurrentUser().username);

        try {
            guiManager.getSocketClient().sendJson(json);
        } catch (IOException e) {
            e.printStackTrace();
            guiManager.raiseLethalError("服务器错误");
        }
        return true;
    }

    boolean sendPointMessage(String message, int toUid) {
        var send = ChatActions.SEND_MESSAGE.getJsonTemplate();
        send.put("fromUserId", getThisUserId());
        send.put("fromUsername", guiManager.getSocketClient().getCurrentUser().username);
        send.put("toUserId", toUid);
        send.put("unixTimeStamp", System.currentTimeMillis());
        send.put("contents", message);
        try {
            guiManager.getSocketClient().sendJson(send);
        } catch (IOException e) {
            e.printStackTrace();
            guiManager.raiseLethalError("服务器错误");
        }

        return true;
    }


    private synchronized void leaveToChatTo(int uid) {
        if (uid == currentTalkingUser)
            return;
        if (onlineUserIds.contains(uid) || uid == 0) {
            userDocuments.put(currentTalkingUser, chatArea.getDocument());
            chatArea.setDocument(userDocuments.get(uid));
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
            currentTalkingUser = uid;
            userContainers.get(uid).dump();
        }
    }

    public synchronized void addOnlineUser(String username, int uid) {
        System.out.println("New user: " + username + " " + uid);
        onlineUserIds.add(uid);
        onlineUsernames.add(username);
        userDocuments.put(uid, new PlainDocument());
        userContainers.put(uid, new MessageContainer(getThisUserId(), uid, this));
        renderOnlineTable(false);
    }

    public synchronized void removeOnlineUser(String username) {
        System.out.println("New user: " + username + " quit");
        int index = onlineUsernames.indexOf(username);
        if (index != -1) {
            int uid = onlineUserIds.get(index);
            if (currentTalkingUser == uid) {
                guiManager.raiseMessage("当前用户已下线！");
                leaveToChatTo(0); // switch to global chat room
            }
            onlineUsernames.remove(index);
            onlineUserIds.remove(index);
            userDocuments.remove(uid);
            userContainers.remove(uid);
            renderOnlineTable(false);
        }
    }

    public synchronized void relocateOnlineUser(String username) {
        int index = onlineUsernames.indexOf(username);
        if (index != -1) {
            int uid = onlineUserIds.get(index);
            onlineUsernames.remove(index);
            onlineUserIds.remove(index);
            onlineUserIds.add(0, uid);
            onlineUsernames.add(0, username);
            renderOnlineTable(true);
        }
    }

    public synchronized void updateUnreadGroupMessage() {
        onlineUserTable.setValueAt("*", 0, 2);
    }


    // place a new message to the chatArea
    public void updateOnReceive(String receivedMessage) {
        chatArea.append(receivedMessage);
        chatArea.append("\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void requestOnlineUsers() {
        var req = ChatActions.GET_ONLINE_USERS.getJsonTemplate();
        try {
            this.guiManager.getSocketClient().sendJson(req);
        } catch (IOException e) {
            e.printStackTrace();
            guiManager.raiseLethalError("服务器错误");
        }
    }


    public synchronized void initUserList(TinyJson tinyJson) {
        // init global room
        userDocuments.put(0, new PlainDocument());
        userContainers.put(0, new MessageContainer(getThisUserId(), 0, this));
        String[] usernames = (String[]) tinyJson.getPrimitive("usernames");
        int[] uids = (int[]) tinyJson.getPrimitive("uids");
        for (int i = 0; i < usernames.length; i++) {
            onlineUserIds.add(uids[i]);
            onlineUsernames.add(usernames[i]);
            System.out.println("add user: " + uids[i] + ", " + usernames[i]);
            userDocuments.put(uids[i], new PlainDocument());
            userContainers.put(uids[i], new MessageContainer(getThisUserId(), uids[i], this));
        }

        renderOnlineTable(false);
    }

    public void letSyncFinish(int uid, TinyJson json) {
        if (userContainers.containsKey(uid)) {
            userContainers.get(uid).finishSync(json);
        }
    }

    private void renderOnlineTable(boolean getMessage) {
        var tableModel = (DefaultTableModel) onlineUserTable.getModel();
        int targetRow = 0;
        tableModel.setRowCount(0);
        tableModel.addRow(new String[]{"---", "公共聊天室", "---"});
        for (int i = 0; i < onlineUsernames.size(); i++) {
            String[] row = new String[3];
            row[0] = String.valueOf(onlineUserIds.get(i));
            if (onlineUserIds.get(i) == currentTalkingUser) {
                targetRow = onlineUserIds.get(i);
            }
            row[1] = onlineUsernames.get(i);
            row[2] = "";
            if (getMessage && i == 0) {
                row[2] = "*";
            }
            if (Objects.equals(row[1], guiManager.getSocketClient().getCurrentUser().username))
                continue; // filter self
            tableModel.addRow(row);
        }
        var renderer = new TableCellRender();
        renderer.setHorizontalAlignment(JLabel.CENTER);
        onlineUserTable.setDefaultRenderer(Object.class, renderer);

    }


    void show() {
        requestOnlineUsers();
        super.show();
    }


    MainChatFrame() {
        init();
    }

    public static void main(String[] args) {
        var frame = new MainChatFrame();
        frame.show();
    }

}
