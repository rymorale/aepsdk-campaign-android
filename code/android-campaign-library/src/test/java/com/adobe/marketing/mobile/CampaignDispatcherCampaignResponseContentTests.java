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

public class CampaignDispatcherCampaignResponseContentTests extends BaseTest {
	private CampaignDispatcherCampaignResponseContent dispatcher;
	private static final String DATA_FOR_MESSAGE_REQUEST_EVENT_NAME = "DataForMessageRequest";

	@Before()
	public void setup() {
		super.beforeEach();
		dispatcher = new CampaignDispatcherCampaignResponseContent(eventHub, null);
	}

	@After
	public void afterEach() {
		super.afterEach();
	}

	@Test
	public void dispatch_ShouldNotDispatch_When_NullMessageData() {
		// test
		dispatcher.dispatch((Map<String, String>)null);

		// verify
		assertNull(eventHub.dispatchedEvent);
	}

	@Test
	public void dispatch_ShouldNotDispatch_When_EmptyMessageData() {
		//setup
		Map<String, String> messageData = new HashMap<String, String>();
		// test
		dispatcher.dispatch(messageData);

		// verify
		assertNull(eventHub.dispatchedEvent);
	}

	@Test
	public void dispatch_ShouldDispatch_When_ValidMessageData() {
		// setup
		Map<String, String> messageData = new HashMap<String, String>();
		messageData.put(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, "testMessageId");
		messageData.put(CampaignTestConstants.ContextDataKeys.MESSAGE_VIEWED, String.valueOf(1));

		// test
		dispatcher.dispatch(messageData);

		// verify
		assertNotNull(eventHub.dispatchedEvent);
		assertEquals(eventHub.dispatchedEvent.getEventType(), EventType.CAMPAIGN);
		assertEquals(eventHub.dispatchedEvent.getEventSource(), EventSource.RESPONSE_CONTENT);
		assertEquals(eventHub.dispatchedEvent.getName(), DATA_FOR_MESSAGE_REQUEST_EVENT_NAME);
		assertNotNull(eventHub.dispatchedEvent.getData().optString(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, null));
		assertEquals(eventHub.dispatchedEvent.getData().optString(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, null),
					 "testMessageId");
		assertNotNull(eventHub.dispatchedEvent.getData().optString(CampaignTestConstants.ContextDataKeys.MESSAGE_VIEWED, null));
		assertEquals(eventHub.dispatchedEvent.getData().optString(CampaignTestConstants.ContextDataKeys.MESSAGE_VIEWED, null),
					 String.valueOf(1));
	}

}
