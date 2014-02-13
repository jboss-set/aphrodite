/**
 * Internal Use Only
 *
 * Copyright 2011 Red Hat, Inc. All rights reserved.
 */
package org.jboss.pull.shared;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jason T. Greene
 */
public class UserList {
    private final Set<String> list;
    private final File file;

    private UserList(Set<String> list, File file) {
        this.list = list;
        this.file = file;
    }

    public static UserList loadUserList(String fileName) {
        BufferedReader reader = null;
        try {
            File file = new File(fileName);
            file.createNewFile();
            reader = new BufferedReader(new FileReader(file));
            HashSet<String> list = new HashSet<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
            return new UserList(list, file);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Util.safeClose(reader);
        }
    }

    public boolean has(String name) {
        return list.contains(name);
    }

    public void add(String user) {
        list.add(user);
        PrintWriter stream = null;
        try {
            stream = new PrintWriter(new FileOutputStream(file, true));
            stream.println(user);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Util.safeClose(stream);
        }
    }
}
