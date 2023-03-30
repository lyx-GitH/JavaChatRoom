package ChatSchema;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GroupMessage extends Message {
    public int sendUserId;

    public String sendUsername;
    public int groupId;

    public GroupMessage() {
        super("", 0);
        this.sendUsername = "";
        this.sendUserId = 0;
        this.groupId = 0;
    }

    public GroupMessage(int sendId, int groupId, String contents, long timeStamp) {
        super(contents, timeStamp);
        this.sendUserId = sendId;
        this.groupId = groupId;
    }

    public static String initTableStatement() {
        String template = """
                        CREATE TABLE %s (
                        sendUserId int NOT NULL,
                        sendUsername varchar(%d) NOT NULL,
                        groupId int NOT NULL,
                        unixTimeStamp bigint NOT NULL,
                        contents varchar(%d) NOT NULL
                        );
                """;
        return template.formatted(GroupMessage.class.getSimpleName(), User.MAX_USERNAME_LEN, Message.MAX_CONTENTS);
    }

    @Override
    public String toString() {
        return "GroupMessage{" +
                "sendUserId=" + sendUserId +
                ", sendUsername='" + sendUsername + '\'' +
                ", groupId=" + groupId +
                ", contents='" + contents + '\'' +
                ", unixTimeStamp=" + unixTimeStamp +
                '}';
    }

    public String format(boolean toMe) {
        var date = new Date();
        date.setTime(unixTimeStamp);
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        String prefix = toMe ? "[ME] " : "";
        return prefix + formattedDate + " " + sendUsername + "\n" + contents + "\n";
    }
}
