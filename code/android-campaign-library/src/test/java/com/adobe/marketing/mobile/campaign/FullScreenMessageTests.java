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
import org.mockito.Mock;

import java.io.File;
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

import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.util.DataReader;

public class FullScreenMessageTests {
	private HashMap<String, Object> happyMessageMap;
	private HashMap<String, Object> happyDetailMap;
	private ArrayList<ArrayList<String>> happyRemoteAssets;
	private ArrayList<String> remoteAssetOne;
	private ArrayList<String> remoteAssetTwo;
	private String assetPath;
	private final String messageId = "07a1c997-2450-46f0-a454-537906404124";

	@Mock
	CampaignExtension mockCampaignExtension;

	@Before
	public void setup() {
		remoteAssetOne = new ArrayList<>();
		remoteAssetOne.add("http://asset1-url00.jpeg");
		remoteAssetOne.add("http://asset1-url01.jpeg");
		remoteAssetOne.add("01.jpeg");

		remoteAssetTwo = new ArrayList<String>();
		remoteAssetTwo.add("http://asset2-url10.jpeg");
		remoteAssetTwo.add("http://asset2-url11.jpeg");

		happyRemoteAssets = new ArrayList<>();
		happyRemoteAssets.add(remoteAssetOne);
		happyRemoteAssets.add(remoteAssetTwo);

		happyDetailMap = new HashMap<String, Object>();
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
	}

	@After
	public void cleanUp() {
		deleteCacheDir();
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
	public void FullScreenMessage_ShouldThrowException_When_MissingAssetsPath() throws
		CampaignMessageRequiredFieldMissingException {
		// setup
		happyMessageMap.remove("assetsPath");

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void FullScreenMessage_ShouldThrowException_When_AssetsPathIsEmpty() throws
		CampaignMessageRequiredFieldMissingException {
		// setup
		happyMessageMap.put("assetsPath", "");

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void FullScreenMessage_ShouldThrowException_When_MissingHTMLInJsonPayload() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyDetailMap.remove("html");
		happyMessageMap.put("detail", Object.fromVariantMap(happyDetailMap));

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test(expected = CampaignMessageRequiredFieldMissingException.class)
	public void FullScreenMessage_ShouldThrowException_When_EmptyHTMLInJsonPayload() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyDetailMap.put("html", Object.fromString(""));
		happyMessageMap.put("detail", Object.fromVariantMap(happyDetailMap));

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	@Test
	public void FullScreenMessage_ShouldNotThrowException_When_MissingAssetsInJsonPayload() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyDetailMap.remove("remoteAssets");
		happyMessageMap.put("detail", Object.fromVariantMap(happyDetailMap));

		// test
		new FullScreenMessage(mockCampaignExtension,TestUtils.createRuleConsequence(happyMessageMap));
	}

	//extractAsset
	@Test
	public void FullScreenMessage_ShouldNotCreateAssets_When_AssetJsonIsNull() throws
		CampaignMessageRequiredFieldMissingException,
		MissingPlatformServicesException {
		// setup
		happyDetailMap.remove("remoteAssets");
		happyMessageMap.put("detail", Object.fromVariantMap(happyDetailMap));

		// test
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));

		// verify
		assertNull(fullScreenMessage.assets);
	}

	@Test
	public void FullScreenMessage_ShouldCreateAssets_When_AssetJsonIsValid() throws
		CampaignMessageRequiredFieldMissingException,
		MissingPlatformServicesException {
		// test
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));

		// verify
		assertNotNull(fullScreenMessage.assets);
		assertEquals(fullScreenMessage.assets.size(), 2);
	}

	//showMessage
	@Test
	public void showMessage_ShouldCallUIServiceWithOriginalHTML_When_AssetsAreNull() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		happyDetailMap.remove("remoteAssets");
		happyMessageMap.put("detail", Object.fromVariantMap(happyDetailMap));
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));

		// test
		fullScreenMessage.showMessage();

		// verify
		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		assertTrue(mockUIService.createFullscreenMessageWasCalled);
		assertEquals(mockUIService.createFullscreenMessageHtml,
					 "<!DOCTYPE html>\n<html>\n<head><meta charset=\"UTF-8\"></head>\n<body>\neverything is awesome http://asset1-url00.jpeg\n</body>\n</html>");
	}

	@Test
	public void showMessage_ShouldCallUIServiceWithReplacedHTML_When_AssetsAreNotNull() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));

		// test
		fullScreenMessage.showMessage();

		// verify
		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		assertTrue(mockUIService.createFullscreenMessageWasCalled);
		assertEquals(mockUIService.createFullscreenMessageHtml,
					 "<!DOCTYPE html>\n<html>\n<head><meta charset=\"UTF-8\"></head>\n<body>\neverything is awesome 01.jpeg\n</body>\n</html>");
	}

	@Test
	public void showMessage_Should_Call_setLocalAssetsMap_With_Empty_Map() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// setup
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = new MockUIFullScreenMessageUI();

		// test
		fullScreenMessage.showMessage();

		MockUIFullScreenMessageUI mockUIFullScreenMessage = (MockUIFullScreenMessageUI)
				mockUIService.createUIFullScreenMessageReturn;

		// verify
		assertTrue(mockUIFullScreenMessage.setLocalAssetsMapCalled);
		assertNull(mockUIFullScreenMessage.setLocalAssetsMapParameterAssetMap);
	}

	@Test
	public void showMessage_Should_Call_setLocalAssetsMap_with_a_Non_Empty_Map() throws
		CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {

		final String remoteUrl = "http://asset1-url00.jpeg"; //url to cache.
		final String sha256HashForRemoteUrl = "fb0d3704b73d5fa012a521ea31013a61020e79610a3c27e8dd1007f3ec278195";
		final String cachedFileName = sha256HashForRemoteUrl + ".12345"; //12345 is just a random extension.

		createCachedDir(CampaignTestConstants.CACHE_DIR);
		createCachedFile(CampaignTestConstants.MESSAGE_CACHE_DIR + File.separator + messageId,
						 cachedFileName);

		// setup
		FullScreenMessage fullScreenMessage = new MockFullscreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = new MockUIFullScreenMessageUI();

		// test
		fullScreenMessage.showMessage();

		MockUIFullScreenMessageUI mockUIFullScreenMessage = (MockUIFullScreenMessageUI)
				mockUIService.createUIFullScreenMessageReturn;

		// verify
		assertTrue(mockUIFullScreenMessage.setLocalAssetsMapCalled);
		assertNotNull(mockUIFullScreenMessage.setLocalAssetsMapParameterAssetMap);
		assertEquals(mockUIFullScreenMessage.setLocalAssetsMapParameterAssetMap.size(), 1);
		assertTrue(mockUIFullScreenMessage.setLocalAssetsMapParameterAssetMap.containsKey(remoteUrl));
		assertTrue(mockUIFullScreenMessage.setLocalAssetsMapParameterAssetMap.get(remoteUrl).endsWith(cachedFileName));
	}

	/**
	 * Creates a cache directory with name {@code cachedDirName} and assign the the reference of corresponding {@link File} instance to {@link MockSystemInfoService#applicationCacheDir}.
	 *
	 * @param cachedDirName name of cache directory.
	 */
	private void createCachedDir(final String cachedDirName) {
		MockSystemInfoService mockSystemInfoService = platformServices.getMockSystemInfoService();
		File cacheDir = new File(cachedDirName);
		cacheDir.mkdirs();
		cacheDir.setWritable(true);
		mockSystemInfoService.applicationCacheDir = cacheDir;
	}

	/**
	 * Create a cache file with name {@code fileName} inside {@code messageDirectoryPath}.
	 *
	 * @param messageDirectoryPath path where file need to create.
	 * @param fileName name of file.
	 */
	private void createCachedFile(final String messageDirectoryPath, final String fileName) {
		try {
			File messageDir = new File(platformServices.getMockSystemInfoService().applicationCacheDir, messageDirectoryPath);
			messageDir.mkdirs();
			new File(messageDir, fileName).createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Delete the cache directory if present.
	 */
	private void deleteCacheDir() {
		File cacheDir = platformServices.getMockSystemInfoService().applicationCacheDir;

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

	@Test
	public void fullscreenListenerOverrideUrlLoad_ShouldNotCallRemoveViewedClickedWithData_When_URLInvalid() throws
		Exception {
		MockUIFullScreenMessageUI mockFullscreenMessageUI = new MockUIFullScreenMessageUI();

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = mockFullscreenMessageUI;

		//Allow creation of the listener through showMessage()
		MockFullscreenMessage fullScreenMessage = new MockFullscreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));
		fullScreenMessage.showMessage();

		//Test
		mockUIService.createFullscreenMessageUIFullScreenListener.overrideUrlLoad(mockFullscreenMessageUI,
				"http://www.adobe.com");

		//Verify
		assertFalse(fullScreenMessage.clickedWithDataWasCalled);
		assertFalse(fullScreenMessage.viewedWasCalled);
		assertFalse(mockFullscreenMessageUI.removeCalled);
	}

	@Test
	public void
	fullscreenListenerOverrideUrlLoad_ShouldNotCallRemoveViewedClickedWithData_When_URLDoesNotContainValidScheme()
	throws
		Exception {

		MockUIFullScreenMessageUI mockFullscreenMessageUI = new MockUIFullScreenMessageUI();

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = mockFullscreenMessageUI;
		//Allow creation of the listener through showMessage()
		MockFullscreenMessage fullScreenMessage = new MockFullscreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));
		fullScreenMessage.showMessage();

		//Test
		mockUIService.createFullscreenMessageUIFullScreenListener.overrideUrlLoad(mockFullscreenMessageUI,
				"adbapp://confirm/?id=h11901a,86f10d,3&url=https://www.adobe.com");

		//Verify
		assertFalse(fullScreenMessage.clickedWithDataWasCalled);
		assertFalse(fullScreenMessage.viewedWasCalled);
		assertFalse(mockFullscreenMessageUI.removeCalled);
	}

	@Test
	public void fullscreenListenerOverrideUrlLoad_ShouldCallRemoveViewedClickedWithData_When_URLIsConfirmTagId3() throws
		Exception {
		MockUIFullScreenMessageUI mockFullscreenMessageUI = new MockUIFullScreenMessageUI();

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = mockFullscreenMessageUI;
		//Allow creation of the listener through showMessage()
		MockFullscreenMessage fullScreenMessage = new MockFullscreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));
		fullScreenMessage.showMessage();

		//Test
		mockUIService.createFullscreenMessageUIFullScreenListener.overrideUrlLoad(mockFullscreenMessageUI,
				"adbinapp://confirm/?id=h11901a,86f10d,3&url=https://www.adobe.com");

		//Verify
		assertTrue(fullScreenMessage.clickedWithDataWasCalled);
		assertNotNull(fullScreenMessage.clickedWithDataParameterData);
		assertEquals(fullScreenMessage.clickedWithDataParameterData.size(), 3);
		assertEquals(fullScreenMessage.clickedWithDataParameterData.get("type"), "confirm");
		assertEquals(fullScreenMessage.clickedWithDataParameterData.get("id"), "h11901a,86f10d,3");
		assertEquals(fullScreenMessage.clickedWithDataParameterData.get("url"), "https://www.adobe.com");
		assertTrue(fullScreenMessage.viewedWasCalled);
		assertTrue(mockFullscreenMessageUI.removeCalled);
	}

	@Test
	public void fullscreenListenerOverrideUrlLoad_ShouldCallRemoveViewedClickedWithData_When_URLIsConfirmTagId4() throws
		Exception {
		MockUIFullScreenMessageUI mockFullscreenMessageUI = new MockUIFullScreenMessageUI();

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = mockFullscreenMessageUI;
		//Allow creation of the listener through showMessage()
		MockFullscreenMessage fullScreenMessage = new MockFullscreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));
		fullScreenMessage.showMessage();

		//Test
		mockUIService.createFullscreenMessageUIFullScreenListener.overrideUrlLoad(mockFullscreenMessageUI,
				"adbinapp://confirm/?id=h11901a,86f10d,4&url=https://www.adobe.com");

		//Verify
		assertTrue(fullScreenMessage.clickedWithDataWasCalled);
		assertNotNull(fullScreenMessage.clickedWithDataParameterData);
		assertEquals(fullScreenMessage.clickedWithDataParameterData.size(), 3);
		assertEquals(fullScreenMessage.clickedWithDataParameterData.get("type"), "confirm");
		assertEquals(fullScreenMessage.clickedWithDataParameterData.get("id"), "h11901a,86f10d,4");
		assertEquals(fullScreenMessage.clickedWithDataParameterData.get("url"), "https://www.adobe.com");
		assertTrue(fullScreenMessage.viewedWasCalled);
		assertTrue(mockFullscreenMessageUI.removeCalled);
	}

	@Test
	public void
	fullscreenListenerOverrideUrlLoad_ShouldCallRemoveViewedClickedWithData_When_URLIsConfirmTagId3WithoutRedirectUrl()
	throws
		Exception {
		MockUIFullScreenMessageUI mockFullscreenMessageUI = new MockUIFullScreenMessageUI();

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = mockFullscreenMessageUI;
		//Allow creation of the listener through showMessage()
		MockFullscreenMessage fullScreenMessage = new MockFullscreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));
		fullScreenMessage.showMessage();

		//Test
		mockUIService.createFullscreenMessageUIFullScreenListener.overrideUrlLoad(mockFullscreenMessageUI,
				"adbinapp://confirm/?id=h11901a,86f10d,3");

		//Verify
		assertTrue(fullScreenMessage.clickedWithDataWasCalled);
		assertTrue(fullScreenMessage.viewedWasCalled);
		assertTrue(mockFullscreenMessageUI.removeCalled);
	}

	@Test
	public void
	fullscreenListenerOverrideUrlLoad_ShouldCallRemoveViewedClickedWithData_When_URLIsConfirmTag4WithoutRedirectUrl()
	throws
		Exception {
		MockUIFullScreenMessageUI mockFullscreenMessageUI = new MockUIFullScreenMessageUI();

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = mockFullscreenMessageUI;
		//Allow creation of the listener through showMessage()
		MockFullscreenMessage fullScreenMessage = new MockFullscreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));
		fullScreenMessage.showMessage();

		//Test
		mockUIService.createFullscreenMessageUIFullScreenListener.overrideUrlLoad(mockFullscreenMessageUI,
				"adbinapp://confirm/?id=h11901a,86f10d,4");

		//Verify
		assertTrue(fullScreenMessage.clickedWithDataWasCalled);
		assertTrue(fullScreenMessage.viewedWasCalled);
		assertTrue(mockFullscreenMessageUI.removeCalled);
	}

	@Test
	public void fullscreenListenerOverrideUrlLoad_ShouldCallRemoveViewedClickedWithData_When_URLIsCancelTagId5() throws
		Exception {
		MockUIFullScreenMessageUI mockFullscreenMessageUI = new MockUIFullScreenMessageUI();

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = mockFullscreenMessageUI;
		//Allow creation of the listener through showMessage()
		MockFullscreenMessage fullScreenMessage = new MockFullscreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));
		fullScreenMessage.showMessage();

		//Test
		mockUIService.createFullscreenMessageUIFullScreenListener.overrideUrlLoad(mockFullscreenMessageUI,
				"adbinapp://cancel?id=h11901a,86f10d,5");

		//Verify
		assertTrue(fullScreenMessage.clickedWithDataWasCalled);
		assertTrue(fullScreenMessage.viewedWasCalled);
		assertTrue(mockFullscreenMessageUI.removeCalled);
	}


	@Test
	public void fullscreenListenerOnDismiss_happy() throws Exception {
		MockUIFullScreenMessageUI mockFullscreenMessageUI = new MockUIFullScreenMessageUI();

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = mockFullscreenMessageUI;
		//Allow creation of the listener through showMessage()
		MockFullscreenMessage fullScreenMessage = new MockFullscreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));
		fullScreenMessage.showMessage();

		//Test
		mockUIService.createFullscreenMessageUIFullScreenListener.onDismiss(mockFullscreenMessageUI);

		//verify
		assertTrue(fullScreenMessage.viewedWasCalled);
	}

	@Test
	public void fullscreenListenerOnShow_happy() throws Exception {
		MockUIFullScreenMessageUI mockFullscreenMessageUI = new MockUIFullScreenMessageUI();

		FakeUIService mockUIService = (FakeUIService) platformServices.getUIService();
		mockUIService.createUIFullScreenMessageReturn = mockFullscreenMessageUI;
		//Allow creation of the listener through showMessage()
		MockFullscreenMessage fullScreenMessage = new MockFullscreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));
		fullScreenMessage.showMessage();

		//Test
		mockUIService.createFullscreenMessageUIFullScreenListener.onShow(mockFullscreenMessageUI);

		//verify
		assertTrue(fullScreenMessage.triggeredWasCalled);
	}


	@Test
	public void shouldDownloadAssets_ReturnsTrue_happy() throws Exception {
		//Setup
		FullScreenMessage fullScreenMessage = new FullScreenMessage(mockCampaignExtension, platformServices,
				TestUtils.createRuleConsequence(happyMessageMap));

		// test
		boolean shouldDownloadAssets = fullScreenMessage.shouldDownloadAssets();

		//verify
		assertTrue(shouldDownloadAssets);
	}

}
