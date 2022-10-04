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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CampaignListenerGenericDataOSTests extends BaseTest {
	private CampaignListenerGenericDataOS listener;
	private MockCampaignExtension campaignExtension;

	@Before
	public void beforeEach() {
		super.beforeEach();
		campaignExtension = new MockCampaignExtension(eventHub, platformServices);
		listener = new CampaignListenerGenericDataOS(campaignExtension, EventType.GENERIC_DATA,
				EventSource.OS);
	}

	@After
	public void afterEach() {
		super.afterEach();
	}

	@Test
	public void testHear_when_nullEventData_then_shouldNotQueueEventInParent() throws Exception {
		// setup
		final Event testEvent = new Event.Builder("test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(null).build();

		// test
		listener.hear(testEvent);
		waitForExecutor(campaignExtension.getExecutor(), 1);

		// verify
		assertEquals("event should not get queued in parent module", 0, campaignExtension.waitingEvents.size());
	}

	@Test
	public void testHear_when_emptyEventData_then_shouldNotQueueEventInParent() throws Exception {
		// setup
		final EventData testEventData = new EventData();
		final Event testEvent = new Event.Builder("test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(testEventData).build();

		// test
		listener.hear(testEvent);
		waitForExecutor(campaignExtension.getExecutor(), 1);

		// verify
		assertEquals("event should not get queued in parent module", 0, campaignExtension.waitingEvents.size());
	}

	@Test
	public void testHear_when_validEventData_then_shouldQueueEventInParent() throws Exception {
		// setup
		final EventData testEventData = new EventData();
		testEventData.putString("broadlogId", "h1234");
		testEventData.putString("deliveryId", "a1a2");
		testEventData.putString("action", "1");

		final Event testEvent = new Event.Builder("test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(testEventData).build();

		// test
		listener.hear(testEvent);
		waitForExecutor(campaignExtension.getExecutor(), 1);

		// verify
		assertEquals("event should get queued in the parent module", 1, campaignExtension.waitingEvents.size());
		assertEquals("correct event is in waitingEvents", testEvent, campaignExtension.waitingEvents.peek());
		final EventData newEventData = campaignExtension.waitingEvents.peek().getData();
		assertTrue("queued event has broadlogId",
				   newEventData.containsKey(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID));
		final String broadlogId = newEventData.optString(
									  CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID, null);
		assertEquals("broadlogId should have correct value", "h1234", broadlogId);
		assertTrue("queued event has deliveryId",
				   newEventData.containsKey(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID));
		final String deliveryId = newEventData.optString(
									  CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID, null);
		assertEquals("deliveryId should have correct value", "a1a2", deliveryId);
		assertTrue("queued event has action",
				   newEventData.containsKey(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION));
		final String action = newEventData.optString(
								  CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION, null);
		assertEquals("action should have correct value", "1", action);

	}
}
