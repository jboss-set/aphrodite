/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.set.aphrodite.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.set.aphrodite.spi.NotFoundException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ryan Emerson
 */
public class Utils {

    private static final Log LOG = LogFactory.getLog(Utils.class);

    public static String decodeURLParam(String parameter) {
        try {
            return URLDecoder.decode(parameter, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String getParamaterFromUrl(Pattern pattern, URL url) throws NotFoundException {
        Matcher matcher = pattern.matcher(url.getQuery());
        if (!matcher.find())
            throw new NotFoundException("No parameter matching the specified pattern exists in the provided url.");

        return decodeURLParam(matcher.group(1));
    }

    public static List<String> getParametersFromUrls(Pattern pattern, List<URL> urls) {
        List<String> ids = new ArrayList<>();
        for (URL url : urls) {
            try {
                ids.add(Utils.getParamaterFromUrl(pattern, url));
            } catch (NotFoundException e) {
                Utils.logException(LOG, e);
            }
        }
        return ids;
    }

    public static String getTrailingValueFromUrlPath(URL url) {
        String path = url.getPath();
        String[] components = path.split("/");
        return components[components.length - 1];
    }

    public static URL createURL(String path) {
        try {
            return new URL(path);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void logWarnMessage(Log log, String message) {
        if (log.isWarnEnabled())
            log.warn(message);
    }

    public static void logException(Log log, Exception e) {
        logException(log, null, e);
    }

    public static void logException(Log log, String message, Exception e) {
        if (log.isErrorEnabled()) {
            if (message == null)
                log.error(e);
            else
                log.error(message, e);
        }
    }

    public static Exception logExceptionAndGet(Log log, Exception e) {
        logException(log, null, e);
        return e;
    }

    public static Exception logExceptionAndGet(Log log, String message, Exception e) {
        logException(log, message, e);
        return e;
    }
}
