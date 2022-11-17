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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.services.ui.AlertSetting;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.FullscreenMessageDelegate;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.util.DataReader;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceProvider.class, FullScreenMessage.class})
public class FullScreenMessageTests {
	private HashMap<String, Object> happyMessageMap;
	private HashMap<String, Object> happyDetailMap;
	private HashMap<String, String> metadataMap;
	private ArrayList<ArrayList<String>> happyRemoteAssets;
	private ArrayList<String> remoteAssetOne;
	private ArrayList<String> remoteAssetTwo;
	private String assetPath;
	private final String messageId = "07a1c997-2450-46f0-a454-537906404124";
	private final String MESSAGES_CACHE = CampaignConstants.CACHE_BASE_DIR + File.separator + CampaignConstants.MESSAGE_CACHE_DIR + File.separator;

	@Mock
	UIService mockUIService;
	@Mock
	CacheService mockCacheService;
	@Mock
	CacheResult mockCacheResult;
	@Mock
	ServiceProvider mockServiceProvider;
	@Mock
	CampaignExtension mockCampaignExtension;
	@Mock
	FullscreenMessage mockFullscreenMessage;
	@Mock
	FullScreenMessage mockCampaignFullScreenMessage;

	@Before
	public void setup() throws Exception {
		remoteAssetOne = new ArrayList<>();
		remoteAssetOne.add("http://asset1-url00.jpeg");
		remoteAssetOne.add("http://asset1-url01.jpeg");
		remoteAssetOne.add("01.jpeg");

		remoteAssetTwo = new ArrayList<>();
		remoteAssetTwo.add("http://asset2-url10.jpeg");
		remoteAssetTwo.add("http://asset2-url11.jpeg");

		happyRemoteAssets = new ArrayList<>();
		happyRemoteAssets.add(remoteAssetOne);
		happyRemoteAssets.add(remoteAssetTwo);

		happyDetailMap = new HashMap<>();
		happyDetailMap.put("template", "fullscreen");
		happyDetailMap.put("html", "happy_test.html");
		happyDetailMap.put("remoteAssets", happyRemoteAssets);

		File assetFile = TestUtils.getResource("happy_test.html");

		if (assetFile != null) {
			assetPath = assetFile.getParent();
		}

		happyMessageMap = new HashMap<>();
		happyMessageMap.put("id", messageId);
		happyMessageMap.put("type", "iam");
		happyMessageMap.put("assetsPath", assetPath);
		happyMessageMap.put("detail", happyDetailMap);

		final String sha256HashForRemoteUrl = "fb0d3704b73d5fa012a521ea31013a61020e79610a3c27e8dd1007f3ec278195";
		final String cachedFileName = sha256HashForRemoteUrl + ".12345"; //12345 is just a random extension.
		metadataMap = new HashMap<>();
		metadataMap.put(CampaignConstants.METADATA_PATH, MESSAGES_CACHE + messageId + File.separator + cachedFileName);

		// setup mocks
		mockStatic(ServiceProvider.class);
		when(ServiceProvider.getInstance()).thenReturn(mockServiceProvider);
		when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
		when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
		when(mockCacheService.get(anyString(), eq("happy_test.html"))).thenReturn(mockCacheResult);
		when(mockCacheService.get(anyString(), eq("http://asset1-url00.jpeg"))).thenReturn(mockCacheResult);
		when(mockCacheResult.getData()).thenReturn(new FileInputStream(assetFile));
		when(mockCacheResult.getMetadata()).thenReturn(metadataMap);
		when(mockUIService.createFullscreenMessage(anyString(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class))).thenReturn(mockFullscreenMessage);
	}

	@After
	public void cleanUp() {
		deleteCacheDir();
	}

	/**
	 * Delete the cache directory if present.
	 */
	private void deleteCacheDir() {
		File cacheDir = new File(MESSAGES_CACHE);

		if (cacheDir != null && cacheDir.exists()) {
			clearCacheFiles(cacheDir);
		}
	}

	/**
	 * Deletes the directory and all files inside it.
	 *
	 * @param file instance of {@link File} points to the directory need to be deleted.
	 */
	private static void clearCacheFiles(final File file) {
		// clear files from directory first
		if (file.isDirectory()) {
			String[] children = file.list();

			if (children != null) {
				for (final String child : children) {
					final File childFile = new File(file, child);
					clearCacheFiles(childFile);
				}
			}
		}

		file.delete(); // delete file or empty directory
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void FullScreenMessage_ShouldThrowException_When_NullConsequence() throws
		CampaignMessageRequiredFieldMissingException {
		new FullScreenMessage(mockCampaignExtension, null);
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_ConsequenceMapIsEmpty() throws Exception {
		//test
		new FullScreenMessage(mockCampaignExtension, TestUtils.createRuleConsequence(new HashMap<String, Object>()));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_NoMessageId() throws Exception {
		// setup
		happyMessageMap.remove("id");

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_EmptyMessageId() throws Exception {
		// setup
		happyMessageMap.put("id", "");

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_NoMessageType() throws Exception {
		// setup
		happyMessageMap.remove("type");

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_EmptyMessageType() throws Exception {
		// setup
		happyMessageMap.put("type", "");

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_InvalidMessageType() throws Exception {
		// setup
		happyMessageMap.put("type", "invalid");

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapIsIncorrect() throws Exception {
		//Setup
		happyDetailMap.clear();
		happyDetailMap.put("blah", "skdjfh");
		happyMessageMap.put("detail", happyDetailMap);

		//test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapIsMissing() throws Exception {
		//Setup
		happyMessageMap.remove("detail");

		//test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void init_ExceptionThrown_When_DetailMapIsEmpty() throws Exception {
		//Setup
		happyMessageMap.put("detail", new HashMap<String, Object>());

		//test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void FullScreenMessage_ShouldThrowException_When_MissingHTMLInJsonPayload() throws
		CampaignMessageRequiredFieldMissingException {
		// setup
		happyDetailMap.remove("html");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void FullScreenMessage_ShouldThrowException_When_EmptyHTMLInJsonPayload() throws
		CampaignMessageRequiredFieldMissingException {
		// setup
		happyDetailMap.put("html", "");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test
	public void FullScreenMessage_ShouldNotThrowException_When_MissingAssetsInJsonPayload() throws
		CampaignMessageRequiredFieldMissingException {
		// setup
		happyDetailMap.remove("remoteAssets");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	// extractAsset
	@Test
	public void FullScreenMessage_ShouldNotCreateAssets_When_AssetJsonIsNull() throws
		CampaignMessageRequiredFieldMissingException {
		// setup
		happyDetailMap.remove("remoteAssets");
		happyMessageMap.put("detail", happyDetailMap);

		// test
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension, TestUtils.createRuleConsequence(happyMessageMap));

		// verify
		assertEquals(new ArrayList<>(), fullScreenMessage.getAssetsList());
	}

	@Test
	public void FullScreenMessage_ShouldCreateAssets_When_AssetJsonIsValid() throws
		CampaignMessageRequiredFieldMissingException {
		// test
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(fullScreenMessage.getAssetsList());
		assertEquals(fullScreenMessage.getAssetsList().size(), 2);
	}

	// showMessage
	@Test
	public void showMessage_ShouldCallUIServiceWithOriginalHTML_When_AssetsAreNull() throws
		CampaignMessageRequiredFieldMissingException {
		// setup
		ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
		happyDetailMap.remove("remoteAssets");
		happyMessageMap.put("detail", happyDetailMap);
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap));

		// test
		fullScreenMessage.showMessage();

		// verify
		verify(mockUIService, times(1)).createFullscreenMessage(stringArgumentCaptor.capture(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class));
		String htmlContent = stringArgumentCaptor.getValue();
		assertEquals("<!DOCTYPE html>\n<html>\n<head><meta charset=\"UTF-8\"></head>\n<body>\neverything is awesome http://asset1-url00.jpeg\n</body>\n</html>", htmlContent);
	}

	@Test
	public void showMessage_Should_Call_setLocalAssetsMap_With_EmptyMap_WhenNoAssets() throws
		CampaignMessageRequiredFieldMissingException {
		// setup
		happyDetailMap.remove("remoteAssets");
		happyMessageMap.put("detail", happyDetailMap);
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap));

		// test
		fullScreenMessage.showMessage();

		// verify
		verify(mockFullscreenMessage, times(1)).setLocalAssetsMap(new HashMap<>());
	}

	@Test
	public void showMessage_Should_Call_setLocalAssetsMap_with_a_Non_Empty_Map_WhenAssetsPresent() throws CampaignMessageRequiredFieldMissingException {
		// setup
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap));
		Map<String, String> expectedMap = new HashMap<>();
		expectedMap.put("http://asset1-url00.jpeg", "campaign/messages/07a1c997-2450-46f0-a454-537906404124/fb0d3704b73d5fa012a521ea31013a61020e79610a3c27e8dd1007f3ec278195.12345");

		// test
		fullScreenMessage.showMessage();

		// verify
		verify(mockFullscreenMessage, times(1)).setLocalAssetsMap(expectedMap);
	}

	@Test
	public void fullscreenListenerOverrideUrlLoad_ShouldNotCallRemoveViewedClickedWithData_When_URLInvalid() throws
		Exception {
		// setup
		whenNew(FullScreenMessage.class).withAnyArguments().thenReturn(mockCampaignFullScreenMessage);
		FullScreenMessage.FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap)).new FullScreenMessageUiListener();

		// test
		fullScreenMessageUiListener.overrideUrlLoad(mockFullscreenMessage, "http://www.adobe.com");

		// verify
		verify(mockCampaignFullScreenMessage, times(0)).clickedWithData(anyMap());
		verify(mockCampaignFullScreenMessage, times(0)).viewed();
		verify(mockFullscreenMessage, times(0)).dismiss();
	}

	@Test
	public void
	fullscreenListenerOverrideUrlLoad_ShouldNotCallRemoveViewedClickedWithData_When_URLDoesNotContainValidScheme()
	throws
		Exception {
		// setup
		whenNew(FullScreenMessage.class).withAnyArguments().thenReturn(mockCampaignFullScreenMessage);
		FullScreenMessage.FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap)).new FullScreenMessageUiListener();

		// test
		fullScreenMessageUiListener.overrideUrlLoad(mockFullscreenMessage, "adbapp://confirm/?id=h11901a,86f10d,3&url=https://www.adobe.com");

		// verify
		verify(mockCampaignFullScreenMessage, times(0)).clickedWithData(anyMap());
		verify(mockCampaignFullScreenMessage, times(0)).viewed();
		verify(mockFullscreenMessage, times(0)).dismiss();
	}

	@Test
	public void fullscreenListenerOverrideUrlLoad_ShouldCallRemoveViewedClickedWithData_When_URLIsConfirmTagId3() throws
		Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		FullScreenMessage.FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap)).new FullScreenMessageUiListener();

		// test
		fullScreenMessageUiListener.overrideUrlLoad(mockFullscreenMessage, "adbinapp://confirm/?id=h11901a,86f10d,3&url=https://www.adobe.com");

		// verify
		verify(mockCampaignExtension, times(2)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		List<Map<String, Object>> messageInteractions = mapArgumentCaptor.getAllValues();
		assertEquals(2, messageInteractions.size());
		Map<String, Object> clickedDataMap = messageInteractions.get(0);
		assertEquals("confirm", clickedDataMap.get("type"));
		assertEquals("h11901a,86f10d,3", clickedDataMap.get("id"));
		assertEquals("https://www.adobe.com", clickedDataMap.get("url"));
		assertEquals("1", clickedDataMap.get("a.message.clicked"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" , clickedDataMap.get("a.message.id"));
		Map<String, Object> viewedDataMap = messageInteractions.get(1);
		assertEquals("1", viewedDataMap.get("a.message.viewed"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" ,viewedDataMap.get("a.message.id"));
		verify(mockUIService, times(1)).showUrl("https://www.adobe.com");
		verify(mockFullscreenMessage, times(1)).dismiss();
	}

	@Test
	public void fullscreenListenerOverrideUrlLoad_ShouldCallRemoveViewedClickedWithData_When_URLIsConfirmTagId4() throws
		Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		FullScreenMessage.FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap)).new FullScreenMessageUiListener();

		// test
		fullScreenMessageUiListener.overrideUrlLoad(mockFullscreenMessage, "adbinapp://confirm/?id=h11901a,86f10d,4&url=https://www.adobe.com");

		// verify
		verify(mockCampaignExtension, times(2)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		List<Map<String, Object>> messageInteractions = mapArgumentCaptor.getAllValues();
		assertEquals(2, messageInteractions.size());
		Map<String, Object> clickedDataMap = messageInteractions.get(0);
		assertEquals("confirm", clickedDataMap.get("type"));
		assertEquals("h11901a,86f10d,4", clickedDataMap.get("id"));
		assertEquals("https://www.adobe.com", clickedDataMap.get("url"));
		assertEquals("1", clickedDataMap.get("a.message.clicked"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" , clickedDataMap.get("a.message.id"));
		Map<String, Object> viewedDataMap = messageInteractions.get(1);
		assertEquals("1", viewedDataMap.get("a.message.viewed"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" ,viewedDataMap.get("a.message.id"));
		verify(mockUIService, times(1)).showUrl("https://www.adobe.com");
		verify(mockFullscreenMessage, times(1)).dismiss();
	}

	@Test
	public void
	fullscreenListenerOverrideUrlLoad_ShouldCallRemoveViewedClickedWithData_When_URLIsConfirmTagId3WithoutRedirectUrl()
	throws
		Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		FullScreenMessage.FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap)).new FullScreenMessageUiListener();

		// test
		fullScreenMessageUiListener.overrideUrlLoad(mockFullscreenMessage, "adbinapp://confirm/?id=h11901a,86f10d,3");

		// verify
		verify(mockCampaignExtension, times(2)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		List<Map<String, Object>> messageInteractions = mapArgumentCaptor.getAllValues();
		assertEquals(2, messageInteractions.size());
		Map<String, Object> clickedDataMap = messageInteractions.get(0);
		assertEquals("confirm", clickedDataMap.get("type"));
		assertEquals("h11901a,86f10d,3", clickedDataMap.get("id"));
		assertNull(clickedDataMap.get("url"));
		assertEquals("1", clickedDataMap.get("a.message.clicked"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" , clickedDataMap.get("a.message.id"));
		Map<String, Object> viewedDataMap = messageInteractions.get(1);
		assertEquals("1", viewedDataMap.get("a.message.viewed"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" ,viewedDataMap.get("a.message.id"));
		verify(mockUIService, times(0)).showUrl(anyString());
		verify(mockFullscreenMessage, times(1)).dismiss();
	}

	@Test
	public void
	fullscreenListenerOverrideUrlLoad_ShouldCallRemoveViewedClickedWithData_When_URLIsConfirmTag4WithoutRedirectUrl()
	throws
		Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		FullScreenMessage.FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap)).new FullScreenMessageUiListener();

		// test
		fullScreenMessageUiListener.overrideUrlLoad(mockFullscreenMessage, "adbinapp://confirm/?id=h11901a,86f10d,4&url=https://www.adobe.com");

		// verify
		verify(mockCampaignExtension, times(2)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		List<Map<String, Object>> messageInteractions = mapArgumentCaptor.getAllValues();
		assertEquals(2, messageInteractions.size());
		Map<String, Object> clickedDataMap = messageInteractions.get(0);
		assertEquals("confirm", clickedDataMap.get("type"));
		assertEquals("h11901a,86f10d,4", clickedDataMap.get("id"));
		assertEquals("https://www.adobe.com", clickedDataMap.get("url"));
		assertEquals("1", clickedDataMap.get("a.message.clicked"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" , clickedDataMap.get("a.message.id"));
		Map<String, Object> viewedDataMap = messageInteractions.get(1);
		assertEquals("1", viewedDataMap.get("a.message.viewed"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" ,viewedDataMap.get("a.message.id"));
		verify(mockUIService, times(1)).showUrl(eq("https://www.adobe.com"));
		verify(mockFullscreenMessage, times(1)).dismiss();
	}

	@Test
	public void fullscreenListenerOverrideUrlLoad_ShouldCallRemoveViewedClickedWithData_When_URLIsCancelTagId5() throws
		Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		FullScreenMessage.FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap)).new FullScreenMessageUiListener();

		// test
		fullScreenMessageUiListener.overrideUrlLoad(mockFullscreenMessage, "adbinapp://cancel?id=h11901a,86f10d,5");

		// verify
		verify(mockCampaignExtension, times(2)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		List<Map<String, Object>> messageInteractions = mapArgumentCaptor.getAllValues();
		assertEquals(2, messageInteractions.size());
		Map<String, Object> clickedDataMap = messageInteractions.get(0);
		assertEquals("cancel", clickedDataMap.get("type"));
		assertEquals("h11901a,86f10d,5", clickedDataMap.get("id"));
		assertNull(clickedDataMap.get("url"));
		assertEquals("1", clickedDataMap.get("a.message.clicked"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" , clickedDataMap.get("a.message.id"));
		Map<String, Object> viewedDataMap = messageInteractions.get(1);
		assertEquals("1", viewedDataMap.get("a.message.viewed"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" ,viewedDataMap.get("a.message.id"));
		verify(mockUIService, times(0)).showUrl(anyString());
		verify(mockFullscreenMessage, times(1)).dismiss();
	}


	@Test
	public void fullscreenListenerOnDismiss_happy() throws Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		FullScreenMessage.FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap)).new FullScreenMessageUiListener();

		// test
		fullScreenMessageUiListener.onDismiss(mockFullscreenMessage);

		// verify
		verify(mockCampaignExtension, times(1)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		List<Map<String, Object>> messageInteractions = mapArgumentCaptor.getAllValues();
		assertEquals(1, messageInteractions.size());
		Map<String, Object> dismissDataMap = messageInteractions.get(0);
		assertEquals("1", dismissDataMap.get("a.message.viewed"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" , dismissDataMap.get("a.message.id"));
	}

	@Test
	public void fullscreenListenerOnShow_happy() throws Exception {
		// setup
		ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
		FullScreenMessage.FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap)).new FullScreenMessageUiListener();

		// test
		fullScreenMessageUiListener.onShow(mockFullscreenMessage);

		// verify
		verify(mockCampaignExtension, times(1)).dispatchMessageInteraction(mapArgumentCaptor.capture());
		List<Map<String, Object>> messageInteractions = mapArgumentCaptor.getAllValues();
		assertEquals(1, messageInteractions.size());
		Map<String, Object> dismissDataMap = messageInteractions.get(0);
		assertEquals("1", dismissDataMap.get("a.message.triggered"));
		assertEquals("07a1c997-2450-46f0-a454-537906404124" , dismissDataMap.get("a.message.id"));
	}


	@Test
	public void shouldDownloadAssets_ReturnsTrue_happy() throws Exception {
		//Setup
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension,
				TestUtils.createRuleConsequence(happyMessageMap));

		// test
		boolean shouldDownloadAssets = fullScreenMessage.shouldDownloadAssets();

		//verify
		assertTrue(shouldDownloadAssets);
	}
}
