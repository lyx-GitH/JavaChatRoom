package ChatSchema;

import ChatDatabase.DBManager;

import java.io.Serializable;

public abstract class BasicSchema implements Serializable {
    static String initTableStatement() {
        return null;
    }

    BasicSchema() {}

}
