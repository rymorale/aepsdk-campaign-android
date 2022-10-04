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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CampaignListenerLifecycleResponseContentTests extends BaseTest {

	private CampaignListenerLifecycleResponseContent listener;
	private MockCampaignExtension campaignExtension;

	@Before
	public void beforeEach() {
		super.beforeEach();
		campaignExtension = new MockCampaignExtension(eventHub, platformServices);
		listener = new CampaignListenerLifecycleResponseContent(campaignExtension, EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT);
	}

	@After
	public void afterEach() {
		super.afterEach();
	}

	@Test
	public void testHear_when_nullEventData_then_shouldNotQueueEventInParent() throws Exception {
		// setup
		final Event testEvent = new Event.Builder("test", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(null).build();

		// test
		listener.hear(testEvent);

		// verify
		assertEquals("event did not get queued in parent module", 0, campaignExtension.waitingEvents.size());
	}

	@Test
	public void testHear_when_emptyEventData_then_shouldNotQueueEventInParent() throws Exception {
		// setup
		final EventData testEventData = new EventData();
		final Event testEvent = new Event.Builder("test", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(testEventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertEquals("event did not get queued in parent module", 0, campaignExtension.waitingEvents.size());
	}

	@Test
	public void testHear_when_noLaunchEventInEventData_then_shouldNotQueueEventInParent() throws Exception {
		// setup
		final EventData testEventData = new EventData();
		final Map<String, String> testMap = new HashMap<String, String>();
		testMap.put("someKey", "someValue");
		testEventData.putStringMap(CampaignTestConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA, testMap);
		final Event testEvent = new Event.Builder("test", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(testEventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertEquals("event did not get queued in parent module", 0, campaignExtension.waitingEvents.size());
	}

	@Test
	public void testHear_when_LaunchEventInEventData_then_shouldQueueEventInParent()
	throws Exception {
		// setup
		final EventData testEventData = new EventData();
		final Map<String, String> testMap = new HashMap<String, String>();
		testMap.put(CampaignTestConstants.EventDataKeys.Lifecycle.LAUNCH_EVENT, "LaunchEvent");
		testEventData.putStringMap(CampaignTestConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA, testMap);
		final Event testEvent = new Event.Builder("test", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(testEventData).build();

		// test
		listener.hear(testEvent);

		waitForExecutor(campaignExtension.getExecutor(), 1);

		// verify
		assertEquals("event got queued in parent module", 1, campaignExtension.waitingEvents.size());
		assertEquals("correct event is in waitingEvents", testEvent, campaignExtension.waitingEvents.peek());
		final EventData newEventData = campaignExtension.waitingEvents.peek().getData();
		assertTrue("queued event has lifecycle context data",
				   newEventData.containsKey(CampaignTestConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA));
		final Map<String, String> lifecycleMap = newEventData.optStringMap(
					CampaignTestConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA, null);
		assertNotNull("lifecycle context data should not be null", lifecycleMap);
		assertEquals("lifecycle context data should have correct values", "LaunchEvent",
					 lifecycleMap.get(CampaignTestConstants.EventDataKeys.Lifecycle.LAUNCH_EVENT));
	}
}