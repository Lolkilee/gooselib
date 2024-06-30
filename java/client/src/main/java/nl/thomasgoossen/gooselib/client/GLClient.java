package nl.thomasgoossen.gooselib.client;

import java.io.IOException;

public class GLClient {
    private static HTTPInterface itf;

    public static void main(String[] args) {
        try {
            itf = new HTTPInterface(8123);
            while (!itf.stopFlag) {
            }
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
    }
}
