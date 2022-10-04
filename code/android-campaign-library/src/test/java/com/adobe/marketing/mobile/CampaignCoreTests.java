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
import org.junit.Test;
import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.*;

public class CampaignCoreTests extends BaseTest {

	private CampaignCore core;


	@Before
	public void testSetup() {
		super.beforeEach();
		core = new CampaignCore(eventHub, new ModuleDetails() {
			@Override
			public String getName() {
				return "TestCampaign";
			}

			@Override
			public String getVersion() {
				return "TestCampaignVersion";
			}

			@Override
			public Map<String, String> getAdditionalInfo() {
				return null;
			}
		});
	}

	@Test
	public void SetLinkageFields_when_LinkageFieldsNull() {
		// test
		core.setLinkageFields(null);

		// verify
		assertFalse("event is not dispatched", eventHub.isDispatchedCalled);
	}

	@Test
	public void SetLinkageFields_when_LinkageFieldsEmpty() {
		// test
		core.setLinkageFields(new HashMap<String, String>());

		// verify
		assertFalse("event is not dispatched", eventHub.isDispatchedCalled);
	}

	@Test
	public void SetLinkageFields_when_LinkageFieldsValid() {
		//setup
		HashMap<String, String> linkageFields = new HashMap<String, String>();
		linkageFields.put("key1", "value1");

		// test
		core.setLinkageFields(linkageFields);

		// verify
		assertTrue("event is dispatched", eventHub.isDispatchedCalled);

		assertEquals("event has correct name", "SetLinkageFields Event", eventHub.dispatchedEvent.getName());
		assertEquals("event has correct event type", EventType.CAMPAIGN, eventHub.dispatchedEvent.getEventType());
		assertEquals("event has correct event source", EventSource.REQUEST_IDENTITY, eventHub.dispatchedEvent.getEventSource());
		assertNotNull("event has eventData", eventHub.dispatchedEvent.getData());


		assertEquals("event has the correct linkage field data", linkageFields,
					 eventHub.dispatchedEvent.getData().optStringMap(CampaignTestConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, null));


	}

	@Test
	public void resetLinkageFields_happy() {
		// test
		core.resetLinkageFields();

		// verify
		assertTrue("event is dispatched", eventHub.isDispatchedCalled);

		assertEquals("event has correct name", "resetLinkageFields Event", eventHub.dispatchedEvent.getName());
		assertEquals("event has correct event type", EventType.CAMPAIGN, eventHub.dispatchedEvent.getEventType());
		assertEquals("event has correct event source", EventSource.REQUEST_RESET, eventHub.dispatchedEvent.getEventSource());
		assertEquals("event does not have eventData", 0, eventHub.dispatchedEvent.getData().size());




	}

}
