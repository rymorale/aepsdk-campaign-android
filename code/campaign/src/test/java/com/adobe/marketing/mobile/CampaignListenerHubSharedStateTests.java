/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class CampaignListenerHubSharedStateTests extends BaseTest {

	private CampaignListenerHubSharedState listener;
	private MockCampaignExtension campaignExtension;

	@Before
	public void beforeEach() {
		super.beforeEach();
		campaignExtension = new MockCampaignExtension(eventHub, platformServices);
		listener = new CampaignListenerHubSharedState(campaignExtension, EventType.HUB, EventSource.SHARED_STATE);
	}

	@After
	public void afterEach() {
		super.afterEach();
	}

	@Test
	public void testHear_when_happy_then_shouldCallProcessSharedStateUpdate() throws Exception {
		// setup
		final EventData eventData = new EventData();
		eventData.putString(CampaignTestConstants.EventDataKeys.STATE_OWNER, "TestExtension");
		Event testEvent = new Event.Builder("test", EventType.HUB,
											EventSource.SHARED_STATE).setData(eventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertTrue("process shared state update was called", campaignExtension.processSharedStateUpdateWasCalled);
		assertEquals("the proper state owner was passed", "TestExtension",
					 campaignExtension.processSharedStateUpdateParameterStateName);
	}

	@Test
	public void testHear_when_nullEventData_then_shouldNotCallProcessSharedStateUpdate() throws Exception {
		// setup
		Event testEvent = new Event.Builder("test", EventType.HUB,
											EventSource.SHARED_STATE).setData(null).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse("process shared state update was called", campaignExtension.processSharedStateUpdateWasCalled);
	}

	@Test
	public void testHear_when_emptyEventData_then_shouldNotCallProcessSharedStateUpdate() throws Exception {
		// setup
		final EventData testEventData = new EventData();
		Event testEvent = new Event.Builder("test", EventType.HUB,
											EventSource.SHARED_STATE).setData(testEventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse("process shared state update was called", campaignExtension.processSharedStateUpdateWasCalled);
	}

	@Test
	public void testHear_when_noStateOwnerInEventData_then_shouldNotCallProcessSharedStateUpdate() throws Exception {
		// setup
		final EventData eventData = new EventData();
		Event testEvent = new Event.Builder("test", EventType.HUB,
											EventSource.SHARED_STATE).setData(eventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse("process shared state update was called", campaignExtension.processSharedStateUpdateWasCalled);
	}

	@Test
	public void testHear_when_emptyStateOwnerInEventData_then_shouldNotCallProcessSharedStateUpdate() throws Exception {
		// setup
		final EventData eventData = new EventData();
		eventData.putString(CampaignTestConstants.EventDataKeys.STATE_OWNER, "");
		Event testEvent = new Event.Builder("test", EventType.HUB,
											EventSource.SHARED_STATE).setData(eventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse("process shared state update was called", campaignExtension.processSharedStateUpdateWasCalled);
	}

	@Test
	public void testHear_when_nullStateOwnerInEventData_then_shouldNotCallProcessSharedStateUpdate() throws Exception {
		// setup
		final EventData eventData = new EventData();
		eventData.putString(CampaignTestConstants.EventDataKeys.STATE_OWNER, null);
		Event testEvent = new Event.Builder("test", EventType.HUB,
											EventSource.SHARED_STATE).setData(eventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse("process shared state update was called", campaignExtension.processSharedStateUpdateWasCalled);
	}
}
