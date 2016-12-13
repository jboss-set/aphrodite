package org.jboss.set.aphrodite.domain;

import java.util.Optional;

/**
 * @author Ryan Emerson
 */
public class User {
    private final String email;
    private final String name;


    public static User createWithEmail(String email) {
        return new User(email, null);
    }

    public static User createWithUsername(String name) {
        return new User(null, name);
    }

    public User(String email, String name) {
        if (email == null && name == null)
            throw new NullPointerException("A valid User object requires that one of the 'email' or 'name' fields are non-null.");
        this.email = email;
        this.name = name;
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (email != null ? !email.equals(user.email) : user.email != null) return false;
        return name != null ? name.equals(user.name) : user.name == null;

    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
