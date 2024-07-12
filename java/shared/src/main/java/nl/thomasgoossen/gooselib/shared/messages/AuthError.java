package nl.thomasgoossen.gooselib.shared.messages;

import java.io.Serializable;

public class AuthError implements Serializable {
    public String reason;

    public AuthError(String reason) {
        this.reason = reason;
    }
}
