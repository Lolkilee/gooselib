package nl.thomasgoossen.gooselib.util;

import java.io.Serializable;

public class ShutdownReq implements Serializable {
    private final String adminPass;

    public ShutdownReq(String password) {
        this.adminPass = password;
    }

    public String getAdminPass() {
        return adminPass;
    }
}
