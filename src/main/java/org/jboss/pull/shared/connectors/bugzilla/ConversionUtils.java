package org.jboss.pull.shared.connectors.bugzilla;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConversionUtils {

    private ConversionUtils() {}

    public static Set<Integer> convertIntoIntegerSet(Object[] objectsArray) {
        Set<Integer> result = new HashSet<Integer>(objectsArray.length);
        for (Object obj : objectsArray) {
            result.add((Integer) obj);
        }
        return result;
    }

    public static Set<String> convertIntoStringSet(Object[] objectsArray) {
        Set<String> result = new HashSet<String>(objectsArray.length);
        for (Object obj : objectsArray) {
            result.add((String) obj);
        }
        return result;
    }

    public static List<String> convertIntoStringList(Object[] objectsArray) {
        List<String> result = new ArrayList<String>(objectsArray.length);
        for (Object obj : objectsArray) {
            result.add((String) obj);
        }
        return result;
    }
}
