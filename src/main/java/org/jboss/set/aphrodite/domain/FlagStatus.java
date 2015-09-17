package org.jboss.set.aphrodite.domain;

public enum FlagStatus {

    SET("?"), ACCEPTED("+"), REJECTED("-"), NO_SET("");

    private final String symbol;

    FlagStatus (String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
