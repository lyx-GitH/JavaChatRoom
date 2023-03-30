package ChatSchema;

public class Message extends BasicSchema {
    public final static int MAX_CONTENTS = 128;
    public  String contents;
    public  long unixTimeStamp;

    public Message(String contents, long timeStamp) {
        this.contents = contents;
        this.unixTimeStamp = timeStamp;
    }

    public Message() {
        contents = "";
        unixTimeStamp = 0;
    }
}
