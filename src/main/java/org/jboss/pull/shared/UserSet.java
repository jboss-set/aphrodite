/**
 * Internal Use Only
 *
 * Copyright 2011 Red Hat, Inc. All rights reserved.
 */
package org.jboss.pull.shared;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ryan Emerson
 */
public class UserSet {
    private final Set<String> list;
    private final File file;

    private UserSet(Set<String> list, File file) {
        this.list = list;
        this.file = file;
    }

    public static UserSet loadUserList(String fileName) throws IOException {
        File file = new File(fileName);
        file.createNewFile();
        Set<String> set = new HashSet<>(Files.readAllLines(file.toPath()));
        return new UserSet(set, file);
    }

    public boolean has(String name) {
        return list.contains(name);
    }

    public void add(String user) throws IOException {
        try (PrintWriter stream = new PrintWriter(new FileOutputStream(file, true))) {
            stream.println(user);
        }
    }
}
