package org.jcs.egm.client.input;

public class ClientPossessionTracker {
    private static boolean possessing = false;
    public static boolean isPossessing() { return possessing; }
    public static void setPossessing(boolean val) { possessing = val; }
}
