package org.jboss.set.aphrodite.domain;


public class Violation {

    private final String message;
    private final String checkName;
    private Severity level = Severity.MINOR;

    public Violation(String checkName, String message) {
        this.message = message;
        this.checkName = checkName;
    }

    public Violation(String checkName, String message, Severity level) {
        this(checkName, message);
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public String getCheckName() {
        return checkName;
    }

    public Severity getLevel() {
        return level;
    }

    public void setLevel(Severity level) {
        this.level = level;
    }

    @Override
    public String toString() {
        return "Violation [message=" + message + ", checkName=" + checkName + ", level=" + level
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((checkName == null) ? 0 : checkName.hashCode());
        result = prime * result + ((level == null) ? 0 : level.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        Violation other = (Violation) obj;

        if (checkName == null) {
            if (other.checkName != null)
                return false;
        } else if (!checkName.equals(other.checkName))
            return false;

        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        return true;
    }

}