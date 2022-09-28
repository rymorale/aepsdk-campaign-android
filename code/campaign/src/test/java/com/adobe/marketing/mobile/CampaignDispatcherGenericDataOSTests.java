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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CampaignDispatcherGenericDataOSTests extends BaseTest {
	private CampaignDispatcherGenericDataOS dispatcher;
	private static final String INTERNAL_GENERIC_DATA_EVENT_NAME = "InternalGenericDataEvent";

	@Before()
	public void setup() {
		super.beforeEach();
		dispatcher = new CampaignDispatcherGenericDataOS(eventHub, null);
	}

	@After
	public void afterEach() {
		super.afterEach();
	}


	@Test
	public void dispatch_ShouldDispatch_When_ValidMessageData() {
		// setup
		final String broadlogId = "h331a";
		final String deliveryId = "131d";
		final String action = "7";

		// test
		dispatcher.dispatch(broadlogId, deliveryId, action);

		// verify
		assertNotNull(eventHub.dispatchedEvent);
		assertEquals(eventHub.dispatchedEvent.getEventType(), EventType.GENERIC_DATA);
		assertEquals(eventHub.dispatchedEvent.getEventSource(), EventSource.OS);
		assertEquals(eventHub.dispatchedEvent.getName(), INTERNAL_GENERIC_DATA_EVENT_NAME);
		assertEquals(eventHub.dispatchedEvent.getData().optString(
						 CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID, null),
					 broadlogId);
		assertEquals(eventHub.dispatchedEvent.getData().optString(
						 CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID, null),
					 deliveryId);
		assertEquals(eventHub.dispatchedEvent.getData().optString(
						 CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION, null),
					 action);
	}

}