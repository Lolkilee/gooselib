package nl.thomasgoossen.gooselib.client;

import java.io.Serializable;

public class ErrorWrapper implements Serializable {
    public final String error;
    
    public ErrorWrapper(String error) {
        this.error = error;
    }
}
