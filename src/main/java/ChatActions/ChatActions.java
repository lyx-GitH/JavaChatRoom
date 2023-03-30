package ChatActions;

import ChatSchema.ChatMessage;
import ChatSchema.GroupMessage;
import ChatSchema.User;
import ChatUtils.TinyJson;
import ChatWeb.SocketClient;
import ChatWeb.SocketServer;

import java.sql.SQLException;

/**
 * This is the central class that drives the Chat program.
 * Each Action is coded in a json, once server/client receives a json, it will do the doServerActions functions
 * i.e. A massive finite state-machine, with Jsons as its input string
 */

public enum ChatActions {


    DEFAULT_IO {
        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            System.out.println(json);
            serverInstance.sendJson(json);
        }

        @Override
        public void doClientActions(SocketClient.Receiver receiver, TinyJson json) {
            System.out.println(json);
        }
    },

    SIGN_IN {
        @Override
        public TinyJson getJsonTemplate() {
            return new TinyJson(new String[]{"username", "password"}).put("actionId", this.ordinal());
        }

        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            assertRightJson(json);
            User user = new User(0);
            user = json.fillObject(user);


            boolean isQualified = false;
            boolean isDuplicate = false;
            try {
                isDuplicate = serverInstance.isAlreadyLogged(user);
                isQualified = !isDuplicate && serverInstance.getDbManager().isQualifiedUser(user);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (isQualified) {
                serverInstance.submitLogin(user);
            }
            TinyJson confirmLoginJsonTemplate = CONFIRM_LOGIN.getJsonTemplate();
            confirmLoginJsonTemplate.put("loginResult", isQualified).put("uid", user.uid).put("username", user.username);
            if(isDuplicate) {
                confirmLoginJsonTemplate.put("reason", "当前账号已被登录");
            } else if (!isQualified) {
                confirmLoginJsonTemplate.put("reason","账号或密码错误");
            }
            CONFIRM_LOGIN.doServerActions(serverInstance, confirmLoginJsonTemplate);

        }
    },

    CONFIRM_LOGIN {
        @Override
        public TinyJson getJsonTemplate() {
            return super.getJsonTemplate()
                    .put("uid", null).put("username", null).addEntry("loginResult").addEntry("reason");
        }
    },

    SIGN_UP {
        @Override
        public TinyJson getJsonTemplate() {
            return new TinyJson(new String[]{"username", "password"}).put("actionId", this.ordinal());
        }

        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            assertRightJson(json);
            User user = json.fillObject(new User());
            user.uid = serverInstance.getLastSignedUserId().get() + 1;
            boolean isValidNewUser = false;
            try {
                isValidNewUser = serverInstance.getDbManager().isValidNewUser(user);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (isValidNewUser) {
                serverInstance.getLastSignedUserId().set(user.uid);
            }
            TinyJson LoginResultJson = CONFIRM_LOGIN.getJsonTemplate().put("loginResult", isValidNewUser);
            serverInstance.sendJson(LoginResultJson);
        }


    },


    /**
     * Get all users currently online
     */
    GET_ONLINE_USERS {
        @Override
        public TinyJson getJsonTemplate() {
            return super.getJsonTemplate().addEntry("usernames").addEntry("uids");
        }

        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            assertRightJson(json);
            serverInstance.packCurrentOnlineUsers(json);
            serverInstance.sendJson(json);
        }

        @Override
        public void doClientActions(SocketClient.Receiver receiver, TinyJson json) {
            assertRightJson(json);
            receiver.unpackOnlineUsers(json);
        }
    },

    SEND_MESSAGE {
        @Override
        public TinyJson getJsonTemplate() {
            return new TinyJson(new ChatMessage())
                    .put("actionId", ordinal())
                    .put("isSent", true)
                    .put("isSelf", false);
        }

        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            ChatMessage chatMessage = json.fillObject(new ChatMessage());
            boolean isSent = serverInstance.post(chatMessage.toUserId, json);
            json.put("isSent", isSent);
            json.put("isSelf", true);
            if (isSent) {
                try {
                    serverInstance.getDbManager().saveChatMessage(chatMessage);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            serverInstance.sendJson(json);
        }

        @Override
        public void doClientActions(SocketClient.Receiver receiver, TinyJson json) {
            assertRightJson(json);
            boolean isSelf = (boolean) json.getPrimitive("isSelf");
            if (!(boolean) json.getPrimitive("isSent")) {
                receiver.emitMessage("消息无法送达!");
            } else {
                ChatMessage chatMessage = json.fillObject(new ChatMessage());
                receiver.onReceiveChatMessage(chatMessage);
            }
        }
    },

    RECEIVE_MESSAGE {
        @Override
        public TinyJson getJsonTemplate() {
            return super.getJsonTemplate().addEntry("isSent").put("isSelf", false);
        }

        @Override
        public void doClientActions(SocketClient.Receiver receiver, TinyJson json) {
            assertRightJson(json);
            boolean isSelf = (boolean) json.getPrimitive("isSelf");
            if (isSelf) {
                boolean isSent = (boolean) json.getPrimitive("isSent");


            } else {
                ChatMessage chatMessage = json.fillObject(new ChatMessage());
//                receiver.doWhenReceiveGroupMessage(chatMessage);
            }
        }
    },

    SHUTDOWN {
        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            assertRightJson(json);
            serverInstance.sendJson(json); // send back, tell client to stop
            serverInstance.shutdown(); // stop the connection
        }

        @Override
        public void doClientActions(SocketClient.Receiver receiver, TinyJson json) {
            receiver.shutdown();
        }
    },

    SUBMIT_LOGIN {
        @Override
        public TinyJson getJsonTemplate() {
            return new TinyJson(new String[]{"uid", "username"}).put("actionId", this.ordinal());
        }

        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            serverInstance.broadcast(json, false);
        }

        @Override
        public void doClientActions(SocketClient.Receiver receiver, TinyJson json) {
            assertRightJson(json);
            receiver.addLoggedUser((String) json.getPrimitive("username"), (int) json.getPrimitive("uid"));
        }
    },

    LOG_OUT {
        @Override
        public TinyJson getJsonTemplate() {
            return new TinyJson(new String[]{"username"}).put("actionId", this.ordinal());
        }

        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            serverInstance.logout();
        }
    },

    SUBMIT_LOGOUT {
        @Override
        public TinyJson getJsonTemplate() {
            return super.getJsonTemplate().addEntry("username");
        }

        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            serverInstance.broadcast(json, false);
        }

        @Override
        public void doClientActions(SocketClient.Receiver receiver, TinyJson json) {
            assertRightJson(json);
            receiver.removeLoggedUser((String) json.getPrimitive("username"));
        }
    },

    SYNC_HISTORY {
        @Override
        public TinyJson getJsonTemplate() {
            return super.getJsonTemplate()
                    .addEntry("isGroup")
                    .addEntry("toUserId")
                    .addEntry("fromUserId")
                    .addEntry("timeStamps");
        }

        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            serverInstance.syncData(json, 512);
        }

        @Override
        public void doClientActions(SocketClient.Receiver receiver, TinyJson json) {
            if ((boolean) json.getPrimitive("isGroup")) {
                receiver.noteSyncDone(0, json);
            } else {
                int uid = (int) json.getPrimitive("toUserId");
                receiver.noteSyncDone(uid, json);
            }
        }
    },

    // send message to all users
    BROADCAST {
        @Override
        public TinyJson getJsonTemplate() {
            return new TinyJson(new GroupMessage()).put("actionId", ordinal());
        }


        @Override
        public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
            assertRightJson(json);
            var groupMessage = json.fillObject(new GroupMessage());
            try {
                serverInstance.getDbManager().saveGroupMessage(groupMessage);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            serverInstance.broadcast(json, true);
        }

        @Override
        public void doClientActions(SocketClient.Receiver receiver, TinyJson json) {
            var groupMessage = json.fillObject(new GroupMessage());
            receiver.onReceiveGroupMessage(groupMessage);
        }
    };


    public TinyJson getJsonTemplate() {
        return new TinyJson().put("actionId", this.ordinal());
    }

    public void doServerActions(SocketServer.Server serverInstance, TinyJson json) {
        assertRightJson(json);
        serverInstance.sendJson(json);
    }

    public void doClientActions(SocketClient.Receiver receiver, TinyJson json) {
        assertRightJson(json);
        receiver.emitJson(json); // default: push this into the message queue
    }

    // make sure this is real stuff
    void assertRightJson(TinyJson json) {
        assert (int) json.getPrimitive("actionId") == this.ordinal();
    }

}
