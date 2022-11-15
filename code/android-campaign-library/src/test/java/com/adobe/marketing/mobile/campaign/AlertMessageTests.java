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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.AlertListener;
import com.adobe.marketing.mobile.services.ui.AlertSetting;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.util.DataReader;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceProvider.class, AlertMessage.class})
public class AlertMessageTests {
	private HashMap<String, Object> happyMessageMap;
	private HashMap<String, Object> happyDetailMap;

	@Mock
	UIService mockUIService;
	@Mock
	ServiceProvider mockServiceProvider;
	@Mock
	CampaignExtension mockCampaignExtension;

	@Before
	public void setup() {
		happyDetailMap = new HashMap<>();
		happyDetailMap.put("template", "alert");
		happyDetailMap.put("title", "Title");
		happyDetailMap.put("content", "content");
		happyDetailMap.put("confirm", "Y");
		happyDetailMap.put("cancel", "N");
		happyDetailMap.put("url", "http://www.adobe.com");

		happyMessageMap = new HashMap<>();
		happyMessageMap.put("id", "123");
		happyMessageMap.put("type", "iam");
		happyMessageMap.put("detail", happyDetailMap);

		// setup mocks
		mockStatic(ServiceProvider.class);
		when(ServiceProvider.getInstance()).thenReturn(mockServiceProvider);
		when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
	}

	RuleConsequence createRuleConsequence(Map<String, Object> consequenceMap) {
		String id = DataReader.optString(consequenceMap, "id", "");
		String type = DataReader.optString(consequenceMap, "type", "");
		Map details = DataReader.optTypedMap(Object.class, consequenceMap, "detail", null);
		return new RuleConsequence(id, type, details);
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_ConsequenceIsNull() throws Exception {
		//test
		new AlertMessage(mockCampaignExtension, null);
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_ConsequenceMapIsEmpty() throws Exception {
		//test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(new HashMap<>()));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_NoMessageId() throws Exception {
		// setup
		happyMessageMap.remove("id");

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_EmptyMessageId() throws Exception {
		// setup
		happyMessageMap.put("id", "");

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_NoMessageType() throws Exception {
		// setup
		happyMessageMap.remove("type");

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_EmptyMessageType() throws Exception {
		// setup
		happyMessageMap.put("type", "");

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_InvalidMessageType() throws Exception {
		// setup
		happyMessageMap.put("type", "invalid");

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapIsIncorrect() throws Exception {
		//Setup
		happyDetailMap.clear();
		happyDetailMap.put("blah", "skdjfh");
		happyMessageMap.put("detail", happyDetailMap);

		//test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapIsMissing() throws Exception {
		//Setup
		happyMessageMap.remove("detail");

		//test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapIsEmpty() throws Exception {
		//Setup
		happyMessageMap.put("detail", new HashMap<String, Object>());

		//test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapDoesNotContainTitle() throws Exception {
		// setup
		happyDetailMap.remove("title");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapContainsEmptyTitle() throws Exception {
		// setup
		happyDetailMap.put("title", "");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapDoesNotContainContent() throws Exception {
		// setup
		happyDetailMap.remove("content");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapContainsEmptyContent() throws Exception {
		// setup
		happyDetailMap.put("content", "");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapDoesNotContainCancel() throws Exception {
		// setup
		happyDetailMap.remove("cancel");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapContainsEmptyCancel() throws Exception {
		// setup
		happyDetailMap.put("cancel", "");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
	}

	@Test
	public void init_Success_When_DetailMapDoesNotContainConfirm() throws Exception {
		// setup
		happyDetailMap.remove("confirm");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		final AlertMessage message = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapContainsEmptyConfirm() throws Exception {
		// setup
		happyDetailMap.put("confirm", "");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		final AlertMessage message = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapDoesNotContainUrl() throws Exception {
		// setup
		happyDetailMap.remove("url");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		final AlertMessage message = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_DetailMapContainsEmptyUrl() throws Exception {
		// setup
		happyDetailMap.put("url", "");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		final AlertMessage message = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(message);
	}

	@Test
	public void init_Success_When_MessagePayloadIsValid() throws Exception {
		// test
		AlertMessage alertMessage = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));

		// Verify
		assertNotNull(alertMessage);
		assertEquals("123", alertMessage.messageId);
		assertEquals("Title", alertMessage.title);
		assertEquals("content", alertMessage.content);
		assertEquals("Y", alertMessage.confirmButtonText);
		assertEquals("N", alertMessage.cancelButtonText);
	}


	@Test
	public void showMessage_ShowsAlert_Happy() throws Exception {
		// setup
		ArgumentCaptor<AlertSetting> alertSettingArgumentCaptor = ArgumentCaptor.forClass(AlertSetting.class);
		ArgumentCaptor<AlertListener> alertListenerArgumentCaptor = ArgumentCaptor.forClass(AlertListener.class);
		// test
		AlertMessage alertMessage = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
		alertMessage.showMessage();

		// verify
		verify(mockUIService, times(1)).showAlert(alertSettingArgumentCaptor.capture(), alertListenerArgumentCaptor.capture());
		AlertSetting alertSetting = alertSettingArgumentCaptor.getValue();
		assertEquals("content", alertSetting.getMessage());
		assertEquals("Title", alertSetting.getTitle());
		assertEquals("N", alertSetting.getNegativeButtonText());
		assertEquals("Y", alertSetting.getPositiveButtonText());
		// verify that a valid listener was attached to the call.
		assertNotNull(alertListenerArgumentCaptor.getValue());
	}

	@Test
	public void showUrl_UIServiceShowUrlIsCalled_When_MessageContainsValidUrl() throws Exception {
		// setup
		ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
		// test
		AlertMessage alertMessage = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
		alertMessage.showUrl();

		// verify
		verify(mockUIService, times(1)).showUrl(stringArgumentCaptor.capture());
		assertEquals("http://www.adobe.com", stringArgumentCaptor.getValue());
	}

	@Test
	public void showUrl_UIServiceShowUrlNotCalled_When_MessageContainsNullUrl() throws Exception {
		// setup
		happyDetailMap.remove("url");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		AlertMessage alertMessage = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));
		alertMessage.showUrl();

		// verify
		verify(mockUIService, times(0)).showUrl(anyString());
	}

	@Test
	public void alertListener_TriggeredHitDispatched_When_onShowCalled() throws Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		AlertMessage.UIAlertMessageUIListener uiAlertMessageUIListener = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap)).new UIAlertMessageUIListener();

		// test
		uiAlertMessageUIListener.onShow();

		// verify
		verify(mockCampaignExtension, times(1)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		Map<String, Object> messageInteractionMap = mapArgumentCaptor.getValue();
		assertEquals(2, messageInteractionMap.size());
		assertEquals("1", messageInteractionMap.get("a.message.triggered"));
		assertEquals("123", messageInteractionMap.get("a.message.id"));
	}

	@Test
	public void alertListener_ViewedHitDispatched_When_onDismissCalled() throws Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		AlertMessage.UIAlertMessageUIListener uiAlertMessageUIListener = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap)).new UIAlertMessageUIListener();

		// test
		uiAlertMessageUIListener.onDismiss();

		// verify
		verify(mockCampaignExtension, times(1)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		Map<String, Object> messageInteractionMap = mapArgumentCaptor.getValue();
		assertEquals(2, messageInteractionMap.size());
		assertEquals("1", messageInteractionMap.get("a.message.viewed"));
		assertEquals("123", messageInteractionMap.get("a.message.id"));
	}

	@Test
	public void alertListener_ViewedHitDispatched_When_onNegativeResponseCalled() throws Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		AlertMessage.UIAlertMessageUIListener uiAlertMessageUIListener = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap)).new UIAlertMessageUIListener();

		// test
		uiAlertMessageUIListener.onNegativeResponse();

		// verify
		verify(mockCampaignExtension, times(1)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		Map<String, Object> messageInteractionMap = mapArgumentCaptor.getValue();
		assertEquals(2, messageInteractionMap.size());
		assertEquals("1", messageInteractionMap.get("a.message.viewed"));
		assertEquals("123", messageInteractionMap.get("a.message.id"));
	}

	@Test
	public void alertListener_ClickedHitDispatched_When_onPositiveResponseCalled() throws Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		AlertMessage.UIAlertMessageUIListener uiAlertMessageUIListener = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap)).new UIAlertMessageUIListener();

		// test
		uiAlertMessageUIListener.onPositiveResponse();

		// verify
		verify(mockCampaignExtension, times(1)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		Map<String, Object> messageInteractionMap = mapArgumentCaptor.getValue();
		assertEquals(3, messageInteractionMap.size());
		assertEquals("http://www.adobe.com", messageInteractionMap.get("url"));
		assertEquals("1", messageInteractionMap.get("a.message.clicked"));
		assertEquals("123", messageInteractionMap.get("a.message.id"));
	}

	@Test
	public void alertListener_ClickedHitDispatched_When_onPositiveResponseCalled_whenUrlNull() throws Exception {
		// setup
		happyDetailMap.remove("url");
		happyMessageMap.put("detail", happyDetailMap);
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		AlertMessage.UIAlertMessageUIListener uiAlertMessageUIListener = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap)).new UIAlertMessageUIListener();

		// test
		uiAlertMessageUIListener.onPositiveResponse();

		// verify
		verify(mockCampaignExtension, times(1)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		Map<String, Object> messageInteractionMap = mapArgumentCaptor.getValue();
		assertEquals(2, messageInteractionMap.size());
		assertNull(messageInteractionMap.get("url"));
		assertEquals("1", messageInteractionMap.get("a.message.clicked"));
		assertEquals("123", messageInteractionMap.get("a.message.id"));
	}

	@Test
	public void alertListener_ClickedHitDispatched_When_onPositiveResponseCalled_whenUrlEmpty() throws Exception {
		// setup
		happyDetailMap.put("url", "");
		happyMessageMap.put("detail", happyDetailMap);
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		AlertMessage.UIAlertMessageUIListener uiAlertMessageUIListener = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap)).new UIAlertMessageUIListener();

		// test
		uiAlertMessageUIListener.onPositiveResponse();

		// verify
		verify(mockCampaignExtension, times(1)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		Map<String, Object> messageInteractionMap = mapArgumentCaptor.getValue();
		assertEquals(2, messageInteractionMap.size());
		assertNull(messageInteractionMap.get("url"));
		assertEquals("1", messageInteractionMap.get("a.message.clicked"));
		assertEquals("123", messageInteractionMap.get("a.message.id"));
	}

	@Test
	public void alertListener_showUrlCalled_When_onPositiveResponseCalled() throws Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
		AlertMessage.UIAlertMessageUIListener uiAlertMessageUIListener = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap)).new UIAlertMessageUIListener();

		// test
		uiAlertMessageUIListener.onPositiveResponse();

		// verify
		verify(mockCampaignExtension, times(1)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		verify(mockUIService, times(1)).showUrl(stringArgumentCaptor.capture());
		Map<String, Object> messageInteractionMap = mapArgumentCaptor.getValue();
		assertEquals(3, messageInteractionMap.size());
		assertEquals("http://www.adobe.com", messageInteractionMap.get("url"));
		assertEquals("1", messageInteractionMap.get("a.message.clicked"));
		assertEquals("123", messageInteractionMap.get("a.message.id"));
		assertEquals("http://www.adobe.com", stringArgumentCaptor.getValue());
	}

	@Test
	public void shouldDownloadAssets_ReturnsFalse_happy() throws Exception {
		// setup
		AlertMessage alertMessage = new AlertMessage(mockCampaignExtension, createRuleConsequence(happyMessageMap));

		// test
		boolean shouldDownloadAssets = alertMessage.shouldDownloadAssets();

		// verify
		assertFalse(shouldDownloadAssets);
	}
}
