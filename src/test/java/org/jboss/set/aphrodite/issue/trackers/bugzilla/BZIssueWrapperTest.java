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


package org.jboss.set.aphrodite.issue.trackers.bugzilla;

import org.jboss.set.aphrodite.domain.Flag;
import org.jboss.set.aphrodite.domain.FlagStatus;
import org.jboss.set.aphrodite.domain.Issue;
import org.jboss.set.aphrodite.domain.IssueEstimation;
import org.jboss.set.aphrodite.domain.IssueStatus;
import org.jboss.set.aphrodite.domain.IssueType;
import org.jboss.set.aphrodite.domain.Release;
import org.jboss.set.aphrodite.domain.Stage;
import org.jboss.set.aphrodite.issue.trackers.util.TestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Martin Stefanko (mstefank@redhat.com)
 */
public class BZIssueWrapperTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public static String BUGZILLA_URL = "https://bugzilla.redhat.com/";

    private URL bugzillaURL;
    private URL bz01URL;
    private Map<String, Object> bz01;
    private Map<String, Object> loginMap;
    private Issue issue01;

    private IssueWrapper wrapper = new IssueWrapper();

    @Before
    public void setUp() throws MalformedURLException {
        bugzillaURL = new URL(BUGZILLA_URL);
        bz01URL = new URL("https://bugzilla.redhat.com/show_bug.cgi?id=1111111");

        bz01 = createTestBZ01();
        issue01 = createTestIssue01(bz01URL);

        loginMap = new HashMap<>();
        loginMap.put(BugzillaFields.LOGIN, "user");
        loginMap.put(BugzillaFields.PASSWORD, "pass");
    }

    @Test
    public void validBZToIssueTest() throws MalformedURLException {
        Issue result = wrapper.bugzillaBugToIssue(bz01, bugzillaURL);

        assertNotNull(result);
        TestUtils.assertDeepEqualsIssue(issue01, result);
        assertEquals(bz01URL, issue01.getURL());
    }

    @Test
    public void nullBZToIssueTest() throws MalformedURLException {
        expectedException.expect(NullPointerException.class);

        Issue result = wrapper.bugzillaBugToIssue(null, bugzillaURL);
        assertNull(result);
    }

    @Test
    public void nullURLBZROIssueTest() throws MalformedURLException {
        expectedException.expect(MalformedURLException.class);

        Issue result = wrapper.bugzillaBugToIssue(bz01, null);
        assertNull(result);
    }

    @Test
    public void validIssueToBZTest() {
        Map<String, Object> result = wrapper.issueToBugzillaBug(issue01, loginMap);

        assertNotNull(result);
        assertDeepEqualsBZ(bz01, result);
    }

    @Test
    public void nullIssuetoBZTest() {
        expectedException.expect(NullPointerException.class);
        Map<String, Object> result = wrapper.issueToBugzillaBug(null, loginMap);

        assertNull(result);
    }

    @Test
    public void nullLoginIssuetoBZTest() {
        expectedException.expect(NullPointerException.class);
        Map<String, Object> result = wrapper.issueToBugzillaBug(issue01, null);

        assertNull(result);
    }

    private Map<String,Object> createTestBZ01() {
        Map<String, Object> result = new HashMap<>();

        result.put(BugzillaFields.ID, 1111111);
        result.put(BugzillaFields.ASSIGNEE, "jboss-set@redhat.com");
        result.put(BugzillaFields.DESCRIPTION, "Test bugzilla");
        result.put(BugzillaFields.STATUS, "NEW");
        result.put(BugzillaFields.COMPONENT, new String[]{"CLI"});
        result.put(BugzillaFields.PRODUCT, "EAP");
        result.put(BugzillaFields.ISSUE_TYPE, "BUG");
        result.put(BugzillaFields.VERSION, new String[]{"6.4.4"});
        result.put(BugzillaFields.TARGET_MILESTONE, "---");
        result.put(BugzillaFields.DEPENDS_ON, new String[]{
                "1111112",
                "1111113"
        });
        result.put(BugzillaFields.BLOCKS, new String[]{
                "1111114",
                "1111115"
        });
        result.put(BugzillaFields.ESTIMATED_TIME, 8.0);
        result.put(BugzillaFields.HOURS_WORKED, 8.0);

        Map<String, Object> develFlag = new HashMap<>();
        develFlag.put(BugzillaFields.FLAG_NAME, "devel_ack");
        develFlag.put(BugzillaFields.FLAG_STATUS, FlagStatus.ACCEPTED);

        Map<String, Object> flags = new HashMap<>();
        flags.put(BugzillaFields.FLAG_NAME , BugzillaFields.FLAG_ACK_DEVEL);
        flags.put(BugzillaFields.FLAG_STATUS, FlagStatus.ACCEPTED.getSymbol());
        result.put(BugzillaFields.FLAGS, new Object[]{
                flags
        });

        return result;
    }

    private Issue createTestIssue01(URL url) throws MalformedURLException {
        Issue result = new Issue(url);

        result.setTrackerId("1111111");
        result.setAssignee("jboss-set@redhat.com");
        result.setDescription("Test bugzilla");
        result.setStatus(IssueStatus.NEW);
        result.setComponent("CLI");
        result.setProduct("EAP");
        result.setType(IssueType.BUG);
        result.setRelease(new Release("6.4.4", "---"));
        result.setDependsOn(Arrays.asList(
                new URL("https://bugzilla.redhat.com/show_bug.cgi?id=1111112"),
                new URL("https://bugzilla.redhat.com/show_bug.cgi?id=1111113")
        ));
        result.setBlocks(Arrays.asList(
                new URL("https://bugzilla.redhat.com/show_bug.cgi?id=1111114"),
                new URL("https://bugzilla.redhat.com/show_bug.cgi?id=1111115")
        ));
        result.setEstimation(new IssueEstimation(8.0, 8.0));

        Stage issueStage = new Stage();
        issueStage.setStatus(Flag.DEV, FlagStatus.ACCEPTED);

        result.setStage(issueStage);

        return result;
    }

    private void assertDeepEqualsBZ(Map<String, Object> expected, Map<String, Object> other) {
        assertEquals("bug tracker id mismatch", expected.get(BugzillaFields.ID), Integer.parseInt((String) other.get(BugzillaFields.ISSUE_IDS)));
        assertEquals("bug assignee mismatch", expected.get(BugzillaFields.ASSIGNEE), other.get(BugzillaFields.ASSIGNEE));
        assertEquals("bug status mismatch", expected.get(BugzillaFields.STATUS), other.get(BugzillaFields.STATUS));

        Object expectedComponents[] = (Object []) expected.get(BugzillaFields.COMPONENT);
        assertEquals("bug component mismatch", expectedComponents[0], other.get(BugzillaFields.COMPONENT));

        assertEquals("bug product mismatch", expected.get(BugzillaFields.PRODUCT), other.get(BugzillaFields.PRODUCT));
        assertEquals("bug type mismatch", expected.get(BugzillaFields.ISSUE_TYPE), other.get(BugzillaFields.ISSUE_TYPE));

        Object expectedVersions[] = (Object []) expected.get(BugzillaFields.VERSION);
        assertEquals("bug version mismatch", expectedVersions[0], other.get(BugzillaFields.VERSION));

        assertEquals("bug milestone mismatch", expected.get(BugzillaFields.TARGET_MILESTONE), other.get(BugzillaFields.TARGET_MILESTONE));


        String expectedDependsOn[] = (String []) expected.get(BugzillaFields.DEPENDS_ON);

        List<String> otherDependsOn = getBZList(other, BugzillaFields.DEPENDS_ON);
        assertEquals("bug depends on list mismatch", expectedDependsOn.length, otherDependsOn.size());
        for (String dependency : expectedDependsOn) {
            assertTrue("bug depends on list mismatch", otherDependsOn.contains(dependency));
        }

        String expectedBlocks[] = (String []) expected.get(BugzillaFields.BLOCKS);

        List<String> otherBlocks = getBZList(other, BugzillaFields.BLOCKS);
        assertEquals("bug blocks list mismatch", expectedBlocks.length, otherBlocks.size());
        for (String dependency : expectedBlocks) {
            assertTrue("bug blocks list mismatch", otherBlocks.contains(dependency));
        }

        assertEquals("bug estimation mismatch", expected.get(BugzillaFields.ESTIMATED_TIME), other.get(BugzillaFields.ESTIMATED_TIME));
        assertEquals("bug estimation mismatch", expected.get(BugzillaFields.HOURS_WORKED), other.get(BugzillaFields.HOURS_WORKED));

        Stage expectedStage = new Stage();
        Map<String,FlagStatus> expectedStreams = new HashMap<>();
        getStageAndStreams(expected, expectedStage, expectedStreams);

        Stage otherStage = new Stage();
        Map<String,FlagStatus> otherStreams = new HashMap<>();
        getStageAndStreams(expected, otherStage, otherStreams);

        assertEquals("bug stage mismatch", expectedStage.getStateMap(), otherStage.getStateMap());
        assertEquals("bug streams status mismatch", expectedStreams, otherStreams);
    }

    @SuppressWarnings("unchecked")
    private List<String> getBZList(Map<String, Object> bz, String set) {
        Map<String, Object> dependsOnMap = (Map<String, Object>) bz.get(set);
        return (List<String>) dependsOnMap.get("set");
    }

    private void getStageAndStreams(Map<String, Object> expected, Stage stage, Map<String, FlagStatus> streams) {
        for (Object object : (Object[]) expected.get(BugzillaFields.FLAGS)) {
            @SuppressWarnings("unchecked")
                    Map<String, Object> flagMap = (Map<String, Object>) object;
            String name = (String) flagMap.get(BugzillaFields.FLAG_NAME);

            if (name.contains("_ack")) {
                Optional<Flag> flag = BugzillaFields.getAphroditeFlag(name);
                if (!flag.isPresent())
                    continue;

                FlagStatus status = FlagStatus.getMatchingFlag(flagMap.get(BugzillaFields.FLAG_STATUS));
                stage.setStatus(flag.get(), status);
            } else {
                FlagStatus status = FlagStatus.getMatchingFlag(flagMap.get(BugzillaFields.FLAG_STATUS));
                streams.put(name, status);
            }
        }
    }
}
