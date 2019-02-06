/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
package org.xwiki.refactoring.internal.job;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.xwiki.job.Job;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.event.DocumentCopiedEvent;
import org.xwiki.refactoring.event.DocumentCopyingEvent;
import org.xwiki.refactoring.event.EntitiesCopiedEvent;
import org.xwiki.refactoring.event.EntitiesCopyingEvent;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CopyJob}.
 *
 * @version $Id$
 */
public class CopyJobTest extends AbstractEntityJobTest
{
    @Rule
    public MockitoComponentMockingRule<Job> mocker = new MockitoComponentMockingRule<Job>(CopyJob.class);

    @Override
    protected MockitoComponentMockingRule<Job> getMocker()
    {
        return this.mocker;
    }

    private CopyRequest createRequest(EntityReference source, EntityReference destination)
    {
        CopyRequest request = new CopyRequest();
        request.setEntityReferences(Arrays.asList(source));
        request.setDestination(destination);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        request.setInteractive(false);
        return request;
    }

    @Test
    public void copyDocumentThrowErrorInCaseOfDocuments() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);

        DocumentReference copyReference = new DocumentReference("wiki", "Copy", "Page");
        when(this.modelBridge.copy(sourceReference, copyReference)).thenReturn(true);

        CopyRequest request = this.createRequest(sourceReference, copyReference);
        run(request);
        verify(this.mocker.getMockedLogger()).error("Unsupported destination entity type [{}] for a document.",
            EntityType.DOCUMENT);
    }

    @Test
    public void copyDocument() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Foo");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);

        SpaceReference copyReference = new SpaceReference("wiki", "Copy", "Page");
        DocumentReference copyDestination = new DocumentReference("Foo", copyReference);
        when(this.modelBridge.copy(sourceReference, copyDestination)).thenReturn(true);

        CopyRequest request = this.createRequest(sourceReference, copyReference);
        Map<String, String> parameters = Collections.singletonMap("foo", "bar");
        request.setEntityParameters(sourceReference, parameters);
        Job job = run(request);

        verify(this.observationManager).notify(new EntitiesCopyingEvent(), job, request);
        verify(this.observationManager).notify(new DocumentCopyingEvent(sourceReference, copyDestination), job,
            request);

        verify(this.modelBridge).copy(sourceReference, copyDestination);
        verify(this.modelBridge, never()).delete(any(DocumentReference.class));
        verify(this.mocker.getMockedLogger(), never()).error(any());

        verify(this.observationManager).notify(new DocumentCopiedEvent(sourceReference, copyDestination), job, request);
        verify(this.observationManager).notify(new EntitiesCopiedEvent(), job, request);
    }

    @Test
    public void cancelEntitiesCopyingEvent() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "One", "Page");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);

        DocumentReference targetReference = new DocumentReference("wiki", "Two", "Page");
        CopyRequest request = createRequest(sourceReference, targetReference);

        doAnswer((Answer<Void>) invocation -> {
            ((EntitiesCopyingEvent) invocation.getArgument(0)).cancel();
            return null;
        }).when(this.observationManager).notify(eq(new EntitiesCopyingEvent()), any(CopyJob.class), eq(request));

        Job job = run(request);

        verify(this.observationManager).notify(new EntitiesCopyingEvent(), job, request);
        verify(this.observationManager, never()).notify(any(DocumentCopyingEvent.class), any(), any());

        verify(this.modelBridge, never()).delete(any());
        verify(this.modelBridge, never()).copy(any(), any());

        verify(this.observationManager, never()).notify(any(DocumentCopiedEvent.class), any(), any());
        verify(this.observationManager, never()).notify(any(EntitiesCopiedEvent.class), any(), any());
    }

    @Test
    public void cancelDocumentCopyingEvent() throws Throwable
    {
        SpaceReference sourceReference = new SpaceReference("wiki", "Source");
        DocumentReference aliceReference = new DocumentReference("Alice", sourceReference);
        when(this.modelBridge.exists(aliceReference)).thenReturn(true);
        DocumentReference bobReference = new DocumentReference("Bob", sourceReference);
        when(this.modelBridge.exists(bobReference)).thenReturn(true);
        when(this.modelBridge.getDocumentReferences(sourceReference))
            .thenReturn(Arrays.asList(aliceReference, bobReference));

        SpaceReference destinationReference = new SpaceReference("wiki", "Destination");
        DocumentReference copyAliceReference =
            new DocumentReference("Alice", new SpaceReference("Source", destinationReference));
        DocumentReference copyBobReference =
            new DocumentReference("Bob", new SpaceReference("Source", destinationReference));
        when(this.modelBridge.copy(bobReference, copyBobReference)).thenReturn(true);

        CopyRequest request = createRequest(sourceReference, destinationReference);

        // Cancel the copy of the first document.
        doAnswer((Answer<Void>) invocation -> {
            ((DocumentCopyingEvent) invocation.getArgument(0)).cancel();
            return null;
        }).when(this.observationManager).notify(eq(new DocumentCopyingEvent(aliceReference, copyAliceReference)),
            any(CopyJob.class), eq(request));

        Job job = run(request);

        verify(this.observationManager).notify(new EntitiesCopyingEvent(), job, request);

        // The copy of the first document is canceled.
        verify(this.observationManager).notify(new DocumentCopyingEvent(aliceReference, copyAliceReference), job,
            request);
        verify(this.modelBridge, never()).copy(aliceReference, copyAliceReference);
        verify(this.observationManager, never()).notify(new DocumentCopiedEvent(aliceReference, copyAliceReference),
            job, request);

        // The second document is still copied.
        verify(this.observationManager).notify(new DocumentCopyingEvent(bobReference, copyBobReference), job, request);
        verify(this.modelBridge).copy(bobReference, copyBobReference);
        verify(this.observationManager).notify(new DocumentCopiedEvent(bobReference, copyBobReference), job, request);

        verify(this.observationManager).notify(new EntitiesCopiedEvent(), job, request);
    }
}
