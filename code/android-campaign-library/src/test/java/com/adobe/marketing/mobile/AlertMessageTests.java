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
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class AlertMessageTests extends BaseTest {
	private CampaignExtension testExtension;
	private HashMap<String, Variant> happyMessageMap;
	private HashMap<String, Variant> happyDetailMap;

	@Before
	public void setup() {
		super.beforeEach();
		happyDetailMap = new HashMap<String, Variant>();
		happyDetailMap.put("template", Variant.fromString("alert"));
		happyDetailMap.put("title", Variant.fromString("Title"));
		happyDetailMap.put("content", Variant.fromString("content"));
		happyDetailMap.put("confirm", Variant.fromString("Y"));
		happyDetailMap.put("cancel", Variant.fromString("N"));
		happyDetailMap.put("url", Variant.fromString("http://www.adobe.com"));

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
		new AlertMessage(testExtension, platformServices, null);
	}

	@Test(expected = MissingPlatformServicesException.class)
	public void init_ExceptionThrown_When_PlatformServicesIsNull() throws Exception {
		//test
		new AlertMessage(testExtension, null, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_ConsequenceMapIsEmpty() throws Exception {
		//test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(new HashMap<String, Variant>()));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_NoMessageId() throws Exception {
		// setup
		happyMessageMap.remove("id");

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_EmptyMessageId() throws Exception {
		// setup
		happyMessageMap.put("id", Variant.fromString(""));

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_NoMessageType() throws Exception {
		// setup
		happyMessageMap.remove("type");

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_EmptyMessageType() throws Exception {
		// setup
		happyMessageMap.put("type", Variant.fromString(""));

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_InvalidMessageType() throws Exception {
		// setup
		happyMessageMap.put("type", Variant.fromString("invalid"));

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapIsIncorrect() throws Exception {
		//Setup
		happyDetailMap.clear();
		happyDetailMap.put("blah", Variant.fromString("skdjfh"));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapIsMissing() throws Exception {
		//Setup
		happyMessageMap.remove("detail");

		//test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapIsEmpty() throws Exception {
		//Setup
		happyMessageMap.put("detail", Variant.fromVariantMap(new HashMap<String, Variant>()));

		//test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapDoesNotContainTitle() throws Exception {
		// setup
		happyDetailMap.remove("title");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapContainsEmptyTitle() throws Exception {
		// setup
		happyDetailMap.put("title", Variant.fromString(""));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapDoesNotContainContent() throws Exception {
		// setup
		happyDetailMap.remove("content");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapContainsEmptyContent() throws Exception {
		// setup
		happyDetailMap.put("content", Variant.fromString(""));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapDoesNotContainCancel() throws Exception {
		// setup
		happyDetailMap.remove("cancel");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapContainsEmptyCancel() throws Exception {
		// setup
		happyDetailMap.put("cancel", Variant.fromString(""));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		new AlertMessage(testExtension, platformServices, createCampaignRuleConsequence(happyMessageMap));
	}

	@Test
	public void init_Success_When_DetailMapDoesNotContainConfirm() throws Exception {
		// setup
		happyDetailMap.remove("confirm");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		final AlertMessage message = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapContainsEmptyConfirm() throws Exception {
		// setup
		happyDetailMap.put("confirm", Variant.fromString(""));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		final AlertMessage message = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapDoesNotContainUrl() throws Exception {
		// setup
		happyDetailMap.remove("url");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		final AlertMessage message = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapContainsEmptyUrl() throws Exception {
		// setup
		happyDetailMap.put("url", Variant.fromString(""));
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		// test
		final AlertMessage message = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_MessagePayloadIsValid() throws Exception {
		//test
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		//Verify
		assertNotNull(alertMessage);
		assertEquals("123", alertMessage.messageId);
		assertEquals("Title", alertMessage.title);
		assertEquals("content", alertMessage.content);
		assertEquals("Y", alertMessage.confirmButtonText);
		assertEquals("N", alertMessage.cancelButtonText);
	}


	@Test
	public void showMessage_ShowsAlert_Happy() throws Exception {
		//test
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		alertMessage.showMessage();


		//verify
		assertTrue(((FakeUIService)platformServices.getUIService()).showAlertWasCalled);
		assertEquals("content", ((FakeUIService)platformServices.getUIService()).showAlertMessage);
		assertEquals("Title", ((FakeUIService)platformServices.getUIService()).showAlertTitle);
		assertEquals("N", ((FakeUIService)platformServices.getUIService()).showAlertNegativeButtonText);
		assertEquals("Y", ((FakeUIService)platformServices.getUIService()).showAlertPositiveButtonText);
		//verify that a valid listener was attached to the call.
		assertNotNull(((FakeUIService)platformServices.getUIService()).showAlertUIAlertListener);
	}

	@Test
	public void showUrl_UIServiceShowUrlIsCalled_When_MessageContainsValidUrl() throws Exception {
		//test
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		alertMessage.showUrl();

		//verify
		assertTrue(((FakeUIService)platformServices.getUIService()).showUrlWasCalled);
		assertEquals("http://www.adobe.com", ((FakeUIService)platformServices.getUIService()).showUrlUrl);
	}

	@Test
	public void showUrl_UIServiceShowUrlNotCalled_When_MessageContainsNullUrl() throws Exception {
		//Setup
		happyDetailMap.remove("url");
		happyMessageMap.put("detail", Variant.fromVariantMap(happyDetailMap));

		//test
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		alertMessage.showUrl();

		//verify
		assertFalse(((FakeUIService)platformServices.getUIService()).showUrlWasCalled);
	}

	@Test
	public void showUrl_HandlesFalseReturn_When_UIServiceShowUrlReturnsFalse() throws Exception {
		//Setup
		((FakeUIService)platformServices.getUIService()).showUrlReturn = false;

		//test
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		alertMessage.showUrl();

		//verify
		assertTrue(((FakeUIService)platformServices.getUIService()).showUrlWasCalled);
	}

	@Test
	public void alertListener_TriggeredHitDispatched_When_onShowCalled() throws Exception {
		//Setup
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		alertMessage.showMessage();

		//test
		UIService.UIAlertListener UIAlertListener = ((FakeUIService)platformServices.getUIService()).showAlertUIAlertListener;
		UIAlertListener.onShow();

		//verify
		assertTrue(eventHub.isDispatchedCalled);
		assertEquals(EventSource.RESPONSE_CONTENT, eventHub.dispatchedEvent.getEventSource());
		assertEquals(EventType.CAMPAIGN, eventHub.dispatchedEvent.getEventType());

		EventData messageData = eventHub.dispatchedEvent.getData();
		assertEquals("Message data should only contain 2 items!", 2, messageData.size());
		assertEquals("The key \"a.message.id\" should be equal to 123", "123",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, null));
		assertEquals("The value for the key \"a.message.viewed\" should be 1", "1",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_TRIGGERED, null));
	}

	@Test
	public void alertListener_ViewedHitDispatched_When_onDismissCalled() throws Exception {
		//Setup
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		alertMessage.showMessage();

		//test
		UIService.UIAlertListener UIAlertListener = ((FakeUIService)platformServices.getUIService()).showAlertUIAlertListener;
		UIAlertListener.onDismiss();

		//verify
		assertTrue(eventHub.isDispatchedCalled);
		assertEquals(EventSource.RESPONSE_CONTENT, eventHub.dispatchedEvent.getEventSource());
		assertEquals(EventType.CAMPAIGN, eventHub.dispatchedEvent.getEventType());

		EventData messageData = eventHub.dispatchedEvent.getData();
		assertEquals("Message data should only contain 2 items!", 2, messageData.size());
		assertEquals("The key \"a.message.id\" should be equal to 123", "123",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, null));
		assertEquals("The value for the key \"a.message.viewed\" should be 1", "1",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_VIEWED, null));
	}

	@Test
	public void alertListener_ViewedHitDispatched_When_onNegativeResponseCalled() throws Exception {
		//Setup
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		alertMessage.showMessage();

		//test
		UIService.UIAlertListener UIAlertListener = ((FakeUIService)platformServices.getUIService()).showAlertUIAlertListener;
		UIAlertListener.onNegativeResponse();

		//verify
		assertTrue(eventHub.isDispatchedCalled);
		assertEquals(EventSource.RESPONSE_CONTENT, eventHub.dispatchedEvent.getEventSource());
		assertEquals(EventType.CAMPAIGN, eventHub.dispatchedEvent.getEventType());

		EventData messageData = eventHub.dispatchedEvent.getData();
		assertEquals("Message data should only contain 2 items!", 2, messageData.size());
		assertEquals("The key \"a.message.id\" should be equal to 123", "123",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, null));
		assertEquals("The value for the key \"a.message.viewed\" should be 1", "1",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_VIEWED, null));
	}

	@Test
	public void alertListener_ClickedHitDispatched_When_onPositiveResponseCalled() throws Exception {
		//Setup
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		alertMessage.showMessage();

		//test
		UIService.UIAlertListener UIAlertListener = ((FakeUIService)platformServices.getUIService()).showAlertUIAlertListener;
		UIAlertListener.onPositiveResponse();

		//verify
		assertTrue(eventHub.isDispatchedCalled);
		assertEquals(EventSource.RESPONSE_CONTENT, eventHub.dispatchedEvent.getEventSource());
		assertEquals(EventType.CAMPAIGN, eventHub.dispatchedEvent.getEventType());

		EventData messageData = eventHub.dispatchedEvent.getData();
		assertEquals("The key \"url\" should be equal to http://www.adobe.com", "http://www.adobe.com",
					 messageData.optString(CampaignTestConstants.CAMPAIGN_INTERACTION_URL, null));
		assertEquals("Message data should only contain 3 items!", 3, messageData.size());
		assertEquals("The key \"a.message.id\" should be equal to 123", "123",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, null));
		assertEquals("The key \"url\" should be equal to http://www.adobe.com", "http://www.adobe.com",
					 messageData.optString(CampaignTestConstants.CAMPAIGN_INTERACTION_URL, null));
	}

	@Test
	public void alertListener_ClickedHitDispatched_When_onPositiveResponseCalled_whenUrlNull() throws Exception {
		//Setup
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		alertMessage.url = null;
		alertMessage.showMessage();

		//test
		UIService.UIAlertListener UIAlertListener = ((FakeUIService)platformServices.getUIService()).showAlertUIAlertListener;
		UIAlertListener.onPositiveResponse();

		//verify
		assertTrue(eventHub.isDispatchedCalled);
		assertEquals(EventSource.RESPONSE_CONTENT, eventHub.dispatchedEvent.getEventSource());
		assertEquals(EventType.CAMPAIGN, eventHub.dispatchedEvent.getEventType());

		EventData messageData = eventHub.dispatchedEvent.getData();
		assertEquals("Message data should only contain 2 items!", 2, messageData.size());
		assertEquals("The key \"a.message.id\" should be equal to 123", "123",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, null));
		assertEquals("The value for the key \"a.message.clicked\" should be 1", "1",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_CLICKED, null));
	}

	@Test
	public void alertListener_ClickedHitDispatched_When_onPositiveResponseCalled_whenUrlEmpty() throws Exception {
		//Setup
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		alertMessage.url = "";
		alertMessage.showMessage();

		//test
		UIService.UIAlertListener UIAlertListener = ((FakeUIService) platformServices.getUIService()).showAlertUIAlertListener;
		UIAlertListener.onPositiveResponse();

		//verify
		assertTrue(eventHub.isDispatchedCalled);
		assertEquals(EventSource.RESPONSE_CONTENT, eventHub.dispatchedEvent.getEventSource());
		assertEquals(EventType.CAMPAIGN, eventHub.dispatchedEvent.getEventType());

		EventData messageData = eventHub.dispatchedEvent.getData();
		assertEquals("Message data should only contain 2 items!", 2, messageData.size());
		assertEquals("The key \"a.message.id\" should be equal to 123", "123",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, null));
		assertEquals("The value for the key \"a.message.clicked\" should be 1", "1",
					 messageData.optString(CampaignTestConstants.ContextDataKeys.MESSAGE_CLICKED, null));
	}

	@Test
	public void alertListener_showUrlCalled_When_onPositiveResponseCalled() throws Exception {
		//Setup
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));
		alertMessage.showMessage();

		//test
		UIService.UIAlertListener UIAlertListener = ((FakeUIService)platformServices.getUIService()).showAlertUIAlertListener;
		UIAlertListener.onPositiveResponse();

		//verify
		assertTrue(((FakeUIService)platformServices.getUIService()).showUrlWasCalled);
		assertEquals("http://www.adobe.com", ((FakeUIService)platformServices.getUIService()).showUrlUrl);
	}

	@Test
	public void shouldDownloadAssets_ReturnsFalse_happy() throws Exception {
		//Setup
		AlertMessage alertMessage = new AlertMessage(testExtension, platformServices,
				createCampaignRuleConsequence(happyMessageMap));

		// test
		boolean shouldDownloadAssets = alertMessage.shouldDownloadAssets();

		//verify
		assertFalse(shouldDownloadAssets);
	}
}
