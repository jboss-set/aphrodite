package org.jboss.pull.shared.connectors.bugzilla;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConversionUtils {

    private ConversionUtils() {}

    public static Date convertToDate(String dateAsString) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
        try {
            return formatter.parse(dateAsString);
        } catch ( ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

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
