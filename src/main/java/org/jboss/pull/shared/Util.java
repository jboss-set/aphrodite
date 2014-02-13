/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.pull.shared;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jason T. Greene
 */
public class Util {

    public static void safeClose(Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable e) {
        }
    }

    public static Map<String, String> map(String... args) {
        if (args == null || args.length == 0) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < args.length;) {
            map.put(args[i++], args[i++]);
        }

        return map;
    }

    public static Properties loadProperties(String configurationFileProperty, String configurationFileDefault) throws IOException {
        String propsFileUserPath = System.getProperty(configurationFileProperty, configurationFileDefault);
        Properties props = new Properties();
        props.load(new FileReader(new File(propsFileUserPath)));
        return props;
    }

    public static String require(Properties props, String name) {
        String ret = (String) props.get(name);
        if (ret == null)
            throw new RuntimeException(name + " must be specified in processor.properties");

        return ret.trim();
    }

    public static String get(Properties props, String name) {
        return (String) props.get(name);
    }

    public static String get(Properties props, String name, String defaultValue) {
        String value = (String) props.get(name);
        return (value == null) ? defaultValue : value;
    }

    public static String getTime() {
        Date date = new Date();
        return getTime(date);
    }

    public static String getTime(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(date);
    }

}
