package org.jboss.set.aphrodite.domain;

import java.util.HashMap;
import java.util.Map;

public class Stage {

    private Map<Flag, FlagStatus> state;

    public Stage () {
        this.state = new HashMap<Flag, FlagStatus>();
    }

    public FlagStatus getStatus(Flag flag) {
        return state.containsKey(flag) ? state.get(flag) : FlagStatus.NO_SET;
    }

    public FlagStatus setStatus(Flag flag, FlagStatus status) {
        return state.put(flag, status);
    }
}
