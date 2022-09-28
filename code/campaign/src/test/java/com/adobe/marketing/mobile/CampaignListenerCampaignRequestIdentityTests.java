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

public class CampaignListenerCampaignRequestIdentityTests extends BaseTest {

	private CampaignListenerCampaignRequestIdentity listener;
	private MockCampaignExtension campaignExtension;

	@Before
	public void beforeEach() {
		super.beforeEach();
		campaignExtension = new MockCampaignExtension(eventHub, platformServices);
		listener = new CampaignListenerCampaignRequestIdentity(campaignExtension, EventType.CAMPAIGN,
				EventSource.REQUEST_IDENTITY);
	}

	@After
	public void afterEach() {
		super.afterEach();
	}

	@Test
	public void testHear_when_nullEventData_then_shouldNotCallHandleSetLinkageFields() throws Exception {
		// setup
		final Event testEvent = new Event.Builder("test", EventType.CAMPAIGN,
				EventSource.REQUEST_IDENTITY).setData(null).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse("The handler for setLinkageFields should not be called", campaignExtension.handleSetLinkageFieldsWasCalled);
	}

	@Test
	public void testHear_when_emptyEventData_then_shouldNotCallHandleSetLinkageFields() throws Exception {
		// setup
		final EventData testEventData = new EventData();
		final Event testEvent = new Event.Builder("test", EventType.CAMPAIGN,
				EventSource.REQUEST_IDENTITY).setData(testEventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse("The handler for setLinkageFields should not be called", campaignExtension.handleSetLinkageFieldsWasCalled);
	}

	@Test
	public void testHear_when_LinkageFieldsNullInEventData_then_shouldNotCallHandleSetLinkageFields()
	throws Exception {
		// setup
		final EventData testEventData = new EventData();
		testEventData.putStringMap(CampaignTestConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, null);
		final Event testEvent = new Event.Builder("test", EventType.CAMPAIGN,
				EventSource.REQUEST_IDENTITY).setData(testEventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse("The handler for setLinkageFields should not be called", campaignExtension.handleSetLinkageFieldsWasCalled);

	}

	@Test
	public void testHear_when_LinkageFieldsEmptyInEventData_then_shouldNotCallHandleSetLinkageFields()
	throws Exception {
		// setup
		final EventData testEventData = new EventData();
		final Map<String, String> testMap = new HashMap<String, String>();
		testEventData.putStringMap(CampaignTestConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, testMap);
		final Event testEvent = new Event.Builder("test", EventType.CAMPAIGN,
				EventSource.REQUEST_IDENTITY).setData(testEventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse("The handler for setLinkageFields should not be called", campaignExtension.handleSetLinkageFieldsWasCalled);

	}

	@Test
	public void testHear_when_LinkageFieldsInEventData_then_shouldCallHandleSetLinkageFields()
	throws Exception {
		// setup
		final EventData testEventData = new EventData();
		final Map<String, String> testMap = new HashMap<String, String>();
		testMap.put("key1", "value1");
		testEventData.putStringMap(CampaignTestConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, testMap);
		final Event testEvent = new Event.Builder("test", EventType.CAMPAIGN,
				EventSource.REQUEST_IDENTITY).setData(testEventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertTrue("The handler for setLinkageFields should be called", campaignExtension.handleSetLinkageFieldsWasCalled);
		assertEquals("The handlers event parameter should match the event it was passed", testEvent,
					 campaignExtension.handleSetLinkageFieldsParameterEvent);
		assertEquals("The handlers linkageFields map parameter should match the map it was passed", testMap,
					 campaignExtension.handleSetLinkageFieldsParameterLinkageFields);
	}
}
