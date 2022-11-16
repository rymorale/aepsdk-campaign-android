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

package com.adobe.marketing.mobile.campaign;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class LocalNotificationMessageTests extends BaseTest {

	private CampaignExtension testExtension;
	private HashMap<String, Variant> happyMessageMap;
	private HashMap<String, Variant> happyDetailMap;
	private HashMap<String, Variant> happyUserDataMap;

	@Before
	public void setup() {
		super.beforeEach();
		happyUserDataMap = new HashMap<String, Variant>();
		happyUserDataMap.put("a", Variant.fromString("b"));
		happyUserDataMap.put("broadlogId", Variant.fromString("h3325"));
		happyUserDataMap.put("deliveryId", Variant.fromString("a2a1"));

		happyDetailMap = new HashMap<String, Variant>();
		happyDetailMap.put("template", Variant.fromString("local"));
		happyDetailMap.put("content", Variant.fromString("content"));
		happyDetailMap.put("wait", Variant.fromInteger(3));
		happyDetailMap.put("date", Variant.fromInteger(123456));
		happyDetailMap.put("adb_deeplink", Variant.fromString("http://www.adobe.com"));
		happyDetailMap.put("userData", Variant.fromVariantMap(happyUserDataMap));
		happyDetailMap.put("sound", Variant.fromString("sound"));
		happyDetailMap.put("title", Variant.fromString("title"));

		happyMessageMap = new HashMap<String, Variant>();
		happyMessageMap.put("id", Variant.fromString("123"));
		happyMessageMap.put("type", Variant.fromString("iam"));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		testExtension = new CampaignExtension(eventHub, platformServices);
	}

	CampaignRuleConsequence createCampaignRuleConsequence(Map<String, Variant> consequenceMap) {
		Variant consequenceAsVariant = Variant.fromVariantMap(consequenceMap);
		CampaignRuleConsequence consequence;

		try {
			consequence = consequenceAsVariant.getTypedObject(new CampaignRuleConsequenceSerializer());
		} catch (VariantException ex) {
			consequence = null;
		}

		return consequence;
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_ConsequenceIsNull() throws Exception {
		//test
		new LocalNotificationMessage(testExtension, platformServices, null);
	}

	@Test(expected = MissingPlatformServicesException.class)
	public void init_ExceptionThrown_When_PlatformServicesIsNull() throws Exception {
		//test
		new LocalNotificationMessage(testExtension, null, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_ConsequenceMapIsEmpty() throws Exception {
		//test
		new LocalNotificationMessage(testExtension, platformServices,
									 createCampaignRuleConsequence(new HashMap<String, Variant>()));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_NoMessageId() throws Exception {
		//Setup
		happyMessageMap.remove("id");

		//test
		new LocalNotificationMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_EmptyMessageId() throws Exception {
		// setup
		happyMessageMap.put("id", Variant.fromString(""));

		// test
		new LocalNotificationMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_NoMessageType() throws Exception {
		// setup
		happyMessageMap.remove("type");

		// test
		new LocalNotificationMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_EmptyMessageType() throws Exception {
		// setup
		happyMessageMap.put("type", Variant.fromString(""));

		// test
		new LocalNotificationMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_InvalidMessageType() throws Exception {
		// setup
		happyMessageMap.put("type", Variant.fromString("invalid"));

		// test
		new LocalNotificationMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_NoDetailMap() throws Exception {
		//Setup
		happyMessageMap.remove("detail");

		//test
		new LocalNotificationMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_EmptyDetailMap() throws Exception {
		//Setup
		happyMessageMap.put("detail", Variant.fromVariantMap(new HashMap<String, Variant>()));

		//test
		new LocalNotificationMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapIsIncorrect() throws Exception {
		//Setup
		happyDetailMap.clear();
		happyDetailMap.put("blah", Variant.fromString("skdjfh"));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		new LocalNotificationMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapDoesNotContainContent() throws Exception {
		//Setup
		happyDetailMap.remove("content");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		new LocalNotificationMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapContainsEmptyContent() throws Exception {
		//Setup
		happyDetailMap.remove("content");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		new LocalNotificationMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test
	public void init_ExceptionNotThrown_When_DetailMapDoesNotContainFireDate() throws Exception {
		//Setup
		happyDetailMap.remove("date");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_ExceptionNotThrown_When_DetailMapDoesNotContainWait() throws Exception {
		//Setup
		happyDetailMap.remove("wait");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_ExceptionNotThrown_When_DetailMapDoesNotContainFireDateOrWait() throws Exception {
		//Setup
		happyDetailMap.remove("date");
		happyDetailMap.remove("wait");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapDoesNotContainCategory() throws Exception {
		//Setup
		happyDetailMap.remove("category");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapDoesNotContainSound() throws Exception {
		//Setup
		happyDetailMap.remove("sound");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapContainsEmptySound() throws Exception {
		//Setup
		happyDetailMap.put("sound", Variant.fromString(""));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapDoesNotContainUserData() throws Exception {
		//Setup
		happyDetailMap.remove("userData");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapContainsEmptyUserData() throws Exception {
		//Setup
		happyDetailMap.put("userData", Variant.fromTypedObject(new HashMap<String, Object>(),
						   PermissiveVariantSerializer.DEFAULT_INSTANCE));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapDoesNotContainDeeplink() throws Exception {
		//Setup
		happyDetailMap.remove("adb_deeplink");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapContainsEmptyDeeplink() throws Exception {
		//Setup
		happyDetailMap.put("adb_deeplink", Variant.fromString(""));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapDoesNotContainTitle() throws Exception {
		//Setup
		happyDetailMap.remove("title");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapContainsEmptyTitle() throws Exception {
		//Setup
		happyDetailMap.put("title", Variant.fromString(""));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage message = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}


	@Test
	public void init_Success_When_ConsequenceIsValid() throws Exception {
		//test
		final LocalNotificationMessage localNotificationMessage = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		//Verify
		assertEquals("123", localNotificationMessage.messageId);
		assertEquals("content", localNotificationMessage.content);
		assertEquals(123456, localNotificationMessage.fireDate);
		assertEquals("b", localNotificationMessage.userdata.get("a"));
		assertEquals("h3325", localNotificationMessage.userdata.get("broadlogId"));
		assertEquals("a2a1", localNotificationMessage.userdata.get("deliveryId"));
		assertEquals("http://www.adobe.com", localNotificationMessage.deeplink);
		assertEquals("sound", localNotificationMessage.sound);
		assertEquals("title", localNotificationMessage.title);
	}

	@Test
	public void init_Success_When_DetailMapHasNoFireDate() throws Exception {
		// setup
		happyDetailMap.remove("date");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage localNotificationMessage = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		//Verify
		assertEquals("123", localNotificationMessage.messageId);
		assertEquals("content", localNotificationMessage.content);
		assertEquals(3, localNotificationMessage.localNotificationDelay);
		assertEquals("b", localNotificationMessage.userdata.get("a"));
		assertEquals("h3325", localNotificationMessage.userdata.get("broadlogId"));
		assertEquals("a2a1", localNotificationMessage.userdata.get("deliveryId"));
		assertEquals("http://www.adobe.com", localNotificationMessage.deeplink);
		assertEquals("sound", localNotificationMessage.sound);
		assertEquals("title", localNotificationMessage.title);
	}

	@Test
	public void init_Success_When_DetailMapHasInvalidFireDate() throws Exception {
		// setup
		happyDetailMap.put("date", Variant.fromLong(0));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage localNotificationMessage = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		//Verify
		assertEquals("123", localNotificationMessage.messageId);
		assertEquals("content", localNotificationMessage.content);
		assertEquals(3, localNotificationMessage.localNotificationDelay);
		assertEquals("b", localNotificationMessage.userdata.get("a"));
		assertEquals("h3325", localNotificationMessage.userdata.get("broadlogId"));
		assertEquals("a2a1", localNotificationMessage.userdata.get("deliveryId"));
		assertEquals("http://www.adobe.com", localNotificationMessage.deeplink);
		assertEquals("sound", localNotificationMessage.sound);
		assertEquals("title", localNotificationMessage.title);
	}

	@Test
	public void init_Success_When_DetailMapHasInvalidFireDateAndNoWait() throws Exception {
		// setup
		happyDetailMap.put("date", Variant.fromLong(-1));
		happyDetailMap.remove("wait");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		final LocalNotificationMessage localNotificationMessage = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		//Verify
		assertEquals("123", localNotificationMessage.messageId);
		assertEquals("content", localNotificationMessage.content);
		assertEquals(0, localNotificationMessage.localNotificationDelay);
		assertEquals("b", localNotificationMessage.userdata.get("a"));
		assertEquals("h3325", localNotificationMessage.userdata.get("broadlogId"));
		assertEquals("a2a1", localNotificationMessage.userdata.get("deliveryId"));
		assertEquals("http://www.adobe.com", localNotificationMessage.deeplink);
		assertEquals("sound", localNotificationMessage.sound);
		assertEquals("title", localNotificationMessage.title);
	}

	@Test
	public void showMessage_DispatchesTriggeredHitAndMessageInfoAndShowsNotification_happy() throws Exception {
		//test
		LocalNotificationMessage localNotificationMessage = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		localNotificationMessage.showMessage();

		//verify
		assertTrue(((FakeUIService)platformServices.getUIService()).showLocalNotificationWasCalled);
		assertEquals("content", ((FakeUIService)platformServices.getUIService()).showLocalNotificationContent);
		assertEquals("123", ((FakeUIService)platformServices.getUIService()).showLocalNotificationIdentifier);
		assertEquals("http://www.adobe.com", ((FakeUIService)platformServices.getUIService()).showLocalNotificationDeeplink);
		assertEquals("b", ((FakeUIService)platformServices.getUIService()).showLocalNotificationUserInfo.get("a"));
		assertEquals("h3325", ((FakeUIService)platformServices.getUIService()).showLocalNotificationUserInfo.get("broadlogId"));
		assertEquals("a2a1", ((FakeUIService)platformServices.getUIService()).showLocalNotificationUserInfo.get("deliveryId"));
		assertEquals(123456, ((FakeUIService)platformServices.getUIService()).showLocalNotificationFireDate);

		assertTrue(eventHub.isDispatchedCalled);
		assertEquals(eventHub.dispatchedEventList.size(), 2);

		// Event0 - Campaign response content
		assertEquals(EventSource.RESPONSE_CONTENT, eventHub.dispatchedEventList.get(0).getEventSource());
		assertEquals(EventType.CAMPAIGN, eventHub.dispatchedEventList.get(0).getEventType());
		EventData messageData0 = eventHub.dispatchedEventList.get(0).getData();
		assertEquals("Message data should only contain 2 items!", 2, messageData0.size());
		assertEquals("The key \"a.message.id\" should be equal to 123", "123",
					 messageData0.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, null));
		assertEquals("The value for the key \"a.message.triggered\" should be 1", "1",
					 messageData0.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_TRIGGERED, null));

		// Event1 - Generic data OS
		assertEquals(EventSource.OS, eventHub.dispatchedEventList.get(1).getEventSource());
		assertEquals(EventType.GENERIC_DATA, eventHub.dispatchedEventList.get(1).getEventType());
		EventData messageData1 = eventHub.dispatchedEventList.get(1).getData();
		assertEquals("Message data should only contain 3 items!", 3, messageData1.size());
		assertEquals("The key \"broadlogId\" should be equal to h3325", "h3325",
					 messageData1.optString(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID, null));
		assertEquals("The key \"deliveryId\" should be equal to a2a1", "a2a1",
					 messageData1.optString(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID, null));
		assertEquals("The key \"action\" should be equal to 7", "7",
					 messageData1.optString(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION, null));

	}

	@Test
	public void showMessage_DispatchesTriggeredHitAndMessageInfo_When_PlatformUIServicesIsNull() throws Exception {
		//Setup
		PlatformServices mockPlatformServices = new MockPlatformServices();
		CampaignExtension extension = new CampaignExtension(eventHub, mockPlatformServices);

		//test
		LocalNotificationMessage localNotificationMessage = new LocalNotificationMessage(extension, mockPlatformServices,
				createCampaignRuleConsequence(happyMessageMap));
		localNotificationMessage.showMessage();

		//verify
		assertTrue(eventHub.isDispatchedCalled);
		assertEquals(eventHub.dispatchedEventList.size(), 2);

		// Event0 - Campaign response content
		assertEquals(EventSource.RESPONSE_CONTENT, eventHub.dispatchedEventList.get(0).getEventSource());
		assertEquals(EventType.CAMPAIGN, eventHub.dispatchedEventList.get(0).getEventType());
		EventData messageData0 = eventHub.dispatchedEventList.get(0).getData();
		assertEquals("Message data should exactly contain 2 items!", 2, messageData0.size());
		assertEquals("The key \"a.message.id\" should be equal to 123", "123",
					 messageData0.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, null));
		assertEquals("The value for the key \"a.message.triggered\" should be 1", "1",
					 messageData0.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_TRIGGERED, null));

		// Event1 - Generic data OS
		assertEquals(EventSource.OS, eventHub.dispatchedEventList.get(1).getEventSource());
		assertEquals(EventType.GENERIC_DATA, eventHub.dispatchedEventList.get(1).getEventType());
		EventData messageData1 = eventHub.dispatchedEventList.get(1).getData();
		assertEquals("Message data should only contain 3 items!", 3, messageData1.size());
		assertEquals("The key \"broadlogId\" should be equal to h3325", "h3325",
					 messageData1.optString(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID, null));
		assertEquals("The key \"deliveryId\" should be equal to a2a1", "a2a1",
					 messageData1.optString(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID, null));
		assertEquals("The key \"action\" should be equal to 7", "7",
					 messageData1.optString(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION, null));
	}

	@Test
	public void shouldDownloadAssets_ReturnsFalse_happy() throws Exception {
		//Setup
		LocalNotificationMessage localNotificationMessage = new LocalNotificationMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		boolean shouldDownloadAssets = localNotificationMessage.shouldDownloadAssets();

		//verify
		assertFalse(shouldDownloadAssets);
	}
}
