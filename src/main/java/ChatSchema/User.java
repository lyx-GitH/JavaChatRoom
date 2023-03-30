package ChatSchema;

public class User extends BasicSchema {
    public static final int MAX_USERNAME_LEN = 16;
    public static final int MAX_PWD_LEN = 16;
    public int uid;
    public String username;
    public String password;

    public User(int index) {
        this.uid = index;
    }

    public User() {
        uid = 0;
        username = "";
        password = "";
    }

    public static String initTableStatement() {
        String template = """
                        CREATE TABLE %s (
                        uid int NOT NULL,
                        username varchar(%d) NOT NULL UNIQUE,
                        password varchar(%d) NOT NULL,
                        primary key(uid)
                        );
                """;
        return template.formatted(User.class.getSimpleName(), User.MAX_USERNAME_LEN, MAX_PWD_LEN);
    }


}
