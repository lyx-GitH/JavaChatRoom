package ChatSchema;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatMessage extends Message {
    public int fromUserId;

    public String fromUsername;
    public int toUserId;


    public ChatMessage(int from, int to, String contents, long timeStamp) {
        super(contents, timeStamp);
        fromUsername = "";
        this.fromUserId = from;
        this.toUserId = to;
    }


    public ChatMessage() {
        super();
        fromUserId = 0;
        toUserId = 0;
    }

    public static String initTableStatement() {
        String template = """
                        CREATE TABLE %s (
                        fromUserId int NOT NULL,
                        fromUsername varchar(%d) NOT NULL,
                        toUserId int NOT NULL,
                        unixTimeStamp bigint NOT NULL,
                        contents varchar(%d) NOT NULL,
                        primary key(fromUserId, toUserId, unixTimeStamp)
                        );
                """;
        return template.formatted(ChatMessage.class.getSimpleName(), User.MAX_USERNAME_LEN,Message.MAX_CONTENTS);
    }

    public String format(boolean toMe) {
        var date = new Date();
        date.setTime(unixTimeStamp);
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        String prefix = toMe ? "[ME] " : "";
        return prefix + formattedDate + " " + fromUsername + "\n" + contents + "\n";
    }
}
