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

import static org.junit.Assert.*;

public class CampaignListenerCampaignRequestResetTests extends BaseTest {

	private CampaignListenerCampaignRequestReset listener;
	private MockCampaignExtension campaignExtension;

	@Before
	public void beforeEach() {
		super.beforeEach();
		campaignExtension = new MockCampaignExtension(eventHub, platformServices);
		listener = new CampaignListenerCampaignRequestReset(campaignExtension, EventType.CAMPAIGN,
				EventSource.REQUEST_RESET);
	}

	@After
	public void afterEach() {
		super.afterEach();
	}

	@Test
	public void testHear_happy() throws Exception {
		// setup
		final Event testEvent = new Event.Builder("test", EventType.CAMPAIGN,
				EventSource.REQUEST_RESET).build();

		// test
		listener.hear(testEvent);

		// verify
		assertTrue("The handler for resetLinkageFields should be called", campaignExtension.handleResetLinkageFieldsWasCalled);
		assertEquals("The handlers event parameter should match the event it was passed", testEvent,
					 campaignExtension.handleResetLinkageFieldsParameterEvent);
	}
}