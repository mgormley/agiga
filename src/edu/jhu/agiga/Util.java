package edu.jhu.agiga;

import java.util.Arrays;

public class Util {

    private Util() {
        // Private constructor.
    }
    
    public static boolean safeEquals(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null) {
            return false;
        } else if (b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }
    
    public static int safeHashCode(Object... objects) {
        return Arrays.hashCode(objects);
    }

}
