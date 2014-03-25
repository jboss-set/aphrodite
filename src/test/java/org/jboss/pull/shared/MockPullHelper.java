package org.jboss.pull.shared;

import org.jboss.shared.connectors.bugzilla.MockBZHelper;
import org.jboss.shared.connectors.github.MockGithubHelper;

public class MockPullHelper extends PullHelper{

    public MockPullHelper(String configurationFileProperty, String configurationFileDefault) throws Exception {
        super(configurationFileProperty, configurationFileDefault);
        ghHelper = new MockGithubHelper();
        bzHelper = new MockBZHelper();
    }

}
