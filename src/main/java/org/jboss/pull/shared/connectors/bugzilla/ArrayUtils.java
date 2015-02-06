package org.jboss.pull.shared.connectors.bugzilla;

import java.util.Map;
import java.util.Set;

public final class ArrayUtils {

    public static Object[] turnMapIntoObjectArray(Map<Object, Object> params) {
        Object[] objs = { params };
        return objs;
    }

    public static Integer[] turnIdIntoAnArray(Integer id) {
        Integer[] ids = new Integer[1];
        ids[0] = id;
        return ids;
    }

    public static String[] turnToStringArray(Set<String> set) {
        String[] strings = new String[set.size()];
        int i = 0;
        for (String string : set)
            strings[i++] = string;
        return strings;
    }
}
