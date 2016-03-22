/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.set.aphrodite.repository.services;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.when;
import org.jboss.set.aphrodite.Aphrodite;
import org.jboss.set.aphrodite.domain.Label;
import org.jboss.set.aphrodite.domain.Patch;
import org.jboss.set.aphrodite.domain.Repository;
import org.jboss.set.aphrodite.spi.AphroditeException;
import org.jboss.set.aphrodite.spi.NotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.Mockito;

/**
 * Test the label function,get ,set and remove method
 * 
 * @author Maoqian Chen (mchen@redhat.com)
 */
public class GitHubLabelTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private List<Label> labels, labelsTest;
    private List<Label> prlabels, prlabelsTest, addlabel;
    private String labelname = "bug";

    @Mock
    private Aphrodite aphrodite;
    @Mock
    private Patch patch;
    @Mock
    private Repository repository;

    @Before
    public void setUp() throws NotFoundException, IOException {
        MockitoAnnotations.initMocks(this);

        labels = new ArrayList<>();
        prlabels = new ArrayList<>();
        addlabel = new ArrayList<>();
        createTestLabel();
        mockLabel();
    }

    @Test
    public void getLabelsfromRepositoryTest() throws NotFoundException, IOException {
        labelsTest = aphrodite.getLabelsFromRepository(repository);
        Label label1 = labelsTest.get(0);
        assertEquals("bug color mismatch", "fc2929", label1.getColor());
        assertEquals("bug name mismatch", "bug", label1.getName());
        assertEquals("bug url mismatch", "https://api.github.com/repos/abc/xyz/labels/bug",
                label1.getUrl());

        Label label2 = labelsTest.get(1);
        assertEquals(
                "bug color mismatch", "cccccc", label2.getColor());
        assertEquals("bug name mismatch", "duplicate",
                label2.getName());
        assertEquals("bug url mismatch",
                "https://api.github.com/repos/abc/xyz/labels/duplicate", label2.getUrl());

        Label label3 = labelsTest.get(2);
        assertEquals("bug color mismatch", "84b6eb", label3.getColor());
        assertEquals("bug name mismatch", "enhancement", label3.getName());
        assertEquals("bug url mismatch", "https://api.github.com/repos/abc/xyz/labels/enhancement",
                label3.getUrl());

        Label label4 = labelsTest.get(3);
        assertEquals("bug color mismatch", "159818", label4.getColor());
        assertEquals("bug name mismatch", "help wanted", label4.getName());
        assertEquals("bug url mismatch", "https://api.github.com/repos/abc/xyz/labels/help wanted",
                label4.getUrl());

        Label label5 = labelsTest.get(4);
        assertEquals("bug color mismatch", "e6e6e6", label5.getColor());
        assertEquals("bug name mismatch", "invalid", label5.getName());
        assertEquals("bug url mismatch", "https://api.github.com/repos/abc/xyz/labels/invalid",
                label5.getUrl());

        Label label6 = labelsTest.get(5);
        assertEquals("bug color mismatch", "cc317c", label6.getColor());
        assertEquals("bug name mismatch", "question", label6.getName());
        assertEquals("bug url mismatch", "https://api.github.com/repos/abc/xyz/labels/question",
                label6.getUrl());

        Label label7 = labelsTest.get(6);
        assertEquals("bug color mismatch", "ffffff", label7.getColor());
        assertEquals("bug name mismatch", "wontfix", label7.getName());
        assertEquals("bug url mismatch", "https://api.github.com/repos/abc/xyz/labels/wontfix",
                label7.getUrl());

    }

    @Test
    public void getLabelsfromPatchTest() throws NotFoundException {
        prlabelsTest = aphrodite.getLabelsFromPatch(patch);

        Label label1 = prlabelsTest.get(0);
        assertEquals("bug color mismatch", "fc2929", label1.getColor());
        assertEquals("bug name mismatch", "bug", label1.getName());
        assertEquals("bug url mismatch", "https://api.github.com/repos/abc/xyz/labels/bug",
                label1.getUrl());

        Label label2 = prlabelsTest.get(1);
        assertEquals(
                "bug color mismatch", "cccccc", label2.getColor());
        assertEquals("bug name mismatch", "duplicate",
                label2.getName());
        assertEquals("bug url mismatch",
                "https://api.github.com/repos/abc/xyz/labels/duplicate", label2.getUrl());

    }

    @Test
    public void setLabelTest() throws NotFoundException, AphroditeException {

        Mockito.doAnswer(new Answer<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                int count = 0;
                addlabel = (List<Label>) args[1];
                for (Label label : addlabel) {
                    if (!labels.contains(label)) {
                        addlabel.remove(label);
                        count++;
                    }

                }
                if (prlabels.size() - count == addlabel.size())
                    return "yes";
                else
                    throw new RuntimeException();
            }
        }).when(aphrodite).setLabelsToPatch(patch, addlabel);

        aphrodite.setLabelsToPatch(patch, addlabel);
    }

    @Test
    public void removeLabelTest() throws NotFoundException {

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                labelname = (String) args[1];
                for (Label label : prlabels) {
                    if (label.getName().equals(labelname)) {
                        prlabels.remove(label);
                    }
                }
                if (prlabels.size() == 1 && prlabels.get(0).getName().equals("duplicate"))
                    return "yes";
                else
                    throw new RuntimeException();
            }
        }).when(aphrodite).removeLabelFromPatch(patch, labelname);

        aphrodite.removeLabelFromPatch(patch, labelname);
        ;
    }

    private void createTestLabel() {
        labels.add(new Label(null, "fc2929", "bug", "https://api.github.com/repos/abc/xyz/labels/bug"));
        labels.add(new Label(null, "cccccc", "duplicate",
                "https://api.github.com/repos/abc/xyz/labels/duplicate"));
        labels.add(new Label(null, "84b6eb", "enhancement",
                "https://api.github.com/repos/abc/xyz/labels/enhancement"));
        labels.add(new Label(null, "159818", "help wanted",
                "https://api.github.com/repos/abc/xyz/labels/help wanted"));
        labels.add(new Label(null, "e6e6e6", "invalid",
                "https://api.github.com/repos/abc/xyz/labels/invalid"));
        labels.add(new Label(null, "cc317c", "question",
                "https://api.github.com/repos/abc/xyz/labels/question"));
        labels.add(new Label(null, "ffffff", "wontfix",
                "https://api.github.com/repos/abc/xyz/labels/wontfix"));

        prlabels.add(new Label("1", "fc2929", "bug", "https://api.github.com/repos/abc/xyz/labels/bug"));
        prlabels.add(new Label("1", "cccccc", "duplicate",
                "https://api.github.com/repos/abc/xyz/labels/duplicate"));

        addlabel.add(new Label("1", "84b6eb", "enhancement",
                "https://api.github.com/repos/abc/xyz/labels/enhancement"));
        addlabel.add(new Label("1", "159818", "help wanted",
                "https://api.github.com/repos/abc/xyz/labels/help wanted"));
    }

    private void mockLabel() throws NotFoundException {
        when(aphrodite.getLabelsFromRepository(repository)).thenReturn(labels);
        when(aphrodite.getLabelsFromPatch(patch)).thenReturn(prlabels);

    }
}
