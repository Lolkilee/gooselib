package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

public class LibInfoReq implements Serializable {
    public final String username;
    public final String password;

    public LibInfoReq(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
