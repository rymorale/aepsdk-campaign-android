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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CampaignListenerCampaignRequestContentTests extends BaseTest {

	private CampaignListenerCampaignRequestContent listener;
	private MockCampaignExtension campaignExtension;

	@Before
	public void testSetup() throws Exception {
		super.beforeEach();
		campaignExtension = new MockCampaignExtension(eventHub, platformServices);
		listener = new CampaignListenerCampaignRequestContent(campaignExtension, EventType.CAMPAIGN,
				EventSource.REQUEST_CONTENT);
	}

	@After
	public void afterEach() {
		super.afterEach();
	}

	@Test
	public void testHear_doesNotCallProcessMessageEvent_when_nullEventData() throws Exception {
		// setup
		final Event testEvent = new Event.Builder("test", EventType.CAMPAIGN,
				EventSource.REQUEST_CONTENT).setData(null).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse(campaignExtension.processMessageEventWasCalled);
	}

	@Test
	public void testHear_doesNotCallProcessMessageEvent_when_emptyEventData() throws Exception {
		// setup
		final EventData testEventData = new EventData();
		final Event testEvent = new Event.Builder("test", EventType.CAMPAIGN,
				EventSource.REQUEST_CONTENT).setData(testEventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse(campaignExtension.processMessageEventWasCalled);
	}

	@Test
	public void testHear_callsProcessMessageEvent_when_validEventData() throws Exception {
		// setup
		Map<String, Variant> testConsequenceMap = new HashMap<String, Variant>();
		testConsequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID,
							   Variant.fromString("1234"));
		testConsequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_TYPE,
							   Variant.fromString("iam"));
		testConsequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ASSETS_PATH,
							   Variant.fromString("test_assets_path/test.html"));

		Map<String, Variant> testDetailMap = new HashMap<String, Variant>();
		testDetailMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE,
						  Variant.fromString("fullscreen"));
		testDetailMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML,
						  Variant.fromString("test.html"));

		testConsequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL,
							   Variant.fromVariantMap(testDetailMap));

		final EventData testEventData = new EventData();
		testEventData.putVariantMap(CampaignTestConstants.EventDataKeys.RuleEngine.CONSEQUENCE_TRIGGERED, testConsequenceMap);
		final Event testEvent = new Event.Builder("test", EventType.CAMPAIGN,
				EventSource.REQUEST_CONTENT).setData(testEventData).build();

		// test
		listener.hear(testEvent);

		// verify
		assertTrue(campaignExtension.processMessageEventWasCalled);
		assertEquals(testEvent, campaignExtension.processMessageEventParameterEvent);
	}
}
