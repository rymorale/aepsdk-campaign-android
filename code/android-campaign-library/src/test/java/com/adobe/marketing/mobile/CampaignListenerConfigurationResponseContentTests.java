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

public class CampaignListenerConfigurationResponseContentTests extends BaseTest {

	private CampaignListenerConfigurationResponseContent listener;
	private MockCampaignExtension campaignExtension;

	@Before
	public void beforeEach() {
		super.beforeEach();
		campaignExtension = new MockCampaignExtension(eventHub, platformServices);
		listener = new CampaignListenerConfigurationResponseContent(campaignExtension, EventType.CONFIGURATION,
				EventSource.RESPONSE_CONTENT);
	}

	@After
	public void afterEach() {
		super.afterEach();
	}

	@Test
	public void testHear_doesNotCallProcessConfigurationResponse_when_nullEventData() throws Exception {
		// setup
		final Event testEvent = new Event.Builder("test", EventType.CONFIGURATION,
				EventSource.RESPONSE_CONTENT).setData(null).build();

		// test
		listener.hear(testEvent);

		// verify
		assertFalse(campaignExtension.processConfigurationResponseWasCalled);
	}

	@Test
	public void testHear_callsProcessConfigurationResponse_when_PrivacyOptOut() throws Exception {
		// setup
		final Map<String, Variant> configData = new HashMap<String, Variant>() {
			{
				put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, Variant.fromString("optedout"));
			}
		};
		Event testEvent = new Event.Builder("test", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(new EventData(configData)).build();

		// test
		listener.hear(testEvent);
		waitForExecutor(campaignExtension.getExecutor(), 1);

		// verify
		assertTrue(campaignExtension.processConfigurationResponseWasCalled);
		assertEquals(testEvent, campaignExtension.processConfigurationResponseParameterEvent);
	}

	@Test
	public void testHear_callsProcessConfigurationResponse_when_PrivacyOptUnknown() throws Exception {
		// setup
		final Map<String, Variant> configData = new HashMap<String, Variant>() {
			{
				put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, Variant.fromString("optunknown"));
			}
		};
		Event testEvent = new Event.Builder("test", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(new EventData(configData)).build();

		// test
		listener.hear(testEvent);
		waitForExecutor(campaignExtension.getExecutor(), 1);

		// verify
		assertTrue(campaignExtension.processConfigurationResponseWasCalled);
		assertEquals(testEvent, campaignExtension.processConfigurationResponseParameterEvent);
	}


	@Test
	public void testHear_callsProcessConfigurationResponse_when_PrivacyOptIn() throws Exception {
		// setup
		final Map<String, Variant> configData = new HashMap<String, Variant>() {
			{
				put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, Variant.fromString("optedin"));
			}
		};
		Event testEvent = new Event.Builder("test", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(new EventData(configData)).build();

		// test
		listener.hear(testEvent);
		waitForExecutor(campaignExtension.getExecutor(), 1);

		// verify
		assertTrue(campaignExtension.processConfigurationResponseWasCalled);
		assertEquals(testEvent, campaignExtension.processConfigurationResponseParameterEvent);
	}

	@Test
	public void testHear_callsProcessConfigurationResponse_when_InvalidPrivacy() throws Exception {
		// setup
		final Map<String, Variant> configData = new HashMap<String, Variant>() {
			{
				put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, Variant.fromString("invalid"));
			}
		};
		Event testEvent = new Event.Builder("test", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(new EventData(configData)).build();

		// test
		listener.hear(testEvent);
		waitForExecutor(campaignExtension.getExecutor(), 1);

		// verify
		assertTrue(campaignExtension.processConfigurationResponseWasCalled);
		assertEquals(testEvent, campaignExtension.processConfigurationResponseParameterEvent);
	}

}
