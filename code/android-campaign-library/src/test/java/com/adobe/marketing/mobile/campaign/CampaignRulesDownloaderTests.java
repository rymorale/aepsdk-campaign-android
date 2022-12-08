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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.services.ui.UIService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CampaignRulesDownloaderTests {

	private CampaignRulesDownloader campaignRulesDownloader;

	private File cacheDir;
	private File zipFile = TestUtils.getResource("campaign_rules.zip");
	private File ruleJsonFile = TestUtils.getResource("rules.json");
	private HashMap<String, String> metadataMap;
	private static final String messageId = "07a1c997-2450-46f0-a454-537906404124";
	private static final String MESSAGES_CACHE = CampaignConstants.CACHE_BASE_DIR + File.separator + CampaignConstants.MESSAGE_CACHE_DIR + File.separator;
    private FakeNamedCollection fakeNamedCollection = new FakeNamedCollection();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Mock
	ExtensionApi mockExtensionApi;
	@Mock
	ServiceProvider mockServiceProvider;
	@Mock
	LaunchRulesEngine mockRulesEngine;
	@Mock
	UIService mockUIService;
	@Mock
	CacheService mockCacheService;
	@Mock
	CacheResult mockCacheResult;
	@Mock
	DeviceInforming mockDeviceInfoService;
	@Mock
	Networking mockNetworkService;
	@Mock
	HttpConnecting mockHttpConnection;

	@Before
	public void setup() {
		String sha256HashForRemoteUrl = "fb0d3704b73d5fa012a521ea31013a61020e79610a3c27e8dd1007f3ec278195";
		String cachedFileName = sha256HashForRemoteUrl + ".12345"; //12345 is just a random extension.
		metadataMap = new HashMap<>();
		metadataMap.put(CampaignConstants.METADATA_PATH, MESSAGES_CACHE + messageId + File.separator + cachedFileName);
	}

	@After
	public void tearDown() {
		clearCacheFiles(cacheDir);
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

	private void setupServiceProviderMockAndRunTest(Runnable testRunnable) {
		cacheDir = new File("cache");
		cacheDir.mkdirs();
		cacheDir.setWritable(true);
		try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
			serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
			when(mockCacheResult.getData()).thenReturn(new FileInputStream(ruleJsonFile));
			when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
			when(mockCacheService.set(anyString(), anyString(), any(CacheEntry.class))).thenReturn(true);
			when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
			when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
			when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
			when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);
			when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(cacheDir);
			// create CampaignRulesDownloader instance
			campaignRulesDownloader = new CampaignRulesDownloader(mockExtensionApi, mockRulesEngine, fakeNamedCollection, mockCacheService);
			testRunnable.run();
		} catch (FileNotFoundException e) {
			fail(e.getMessage());
		}
	}

	private File setupCachedRules() {
		// setup
		final File existingCacheDir = new File(cacheDir, CampaignConstants.RULES_CACHE_FOLDER);
		existingCacheDir.mkdirs();
		final File existingCachedFile = new
				File(existingCacheDir + File.separator +
				"c3da84c61f5768a6a401d09baf43275f5dcdb6cf57f8ad5382fa6c3d9a6c4a75");
		try {
			existingCachedFile.createNewFile();
		} catch (IOException exception) {
			fail(exception.getMessage());
		}
		return existingCachedFile;
	}

	// =================================================================================================================
	// void loadCachedMessages()
	// =================================================================================================================
	@Test
	public void test_loadCachedMessages_happy() {
		// setup
		setupServiceProviderMockAndRunTest(() -> {
			when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
			try {
				when(mockHttpConnection.getInputStream()).thenReturn(new FileInputStream(zipFile));
			} catch (FileNotFoundException e) {
				fail(e.getMessage());
			}
			doAnswer((Answer<Void>) invocation -> {
				NetworkCallback callback = invocation.getArgument(1);
				callback.call(mockHttpConnection);
				return null;
			}).when(mockNetworkService)
					.connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
			String rulesUrl =
					"https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip";

			// test
			campaignRulesDownloader.loadRulesFromUrl(rulesUrl, null);

			// verify
			verify(mockNetworkService, times(1)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
			// verify extracted rules json is cached
			verify(mockCacheService, times(1)).set(eq(CampaignConstants.CACHE_BASE_DIR + File.separator + CampaignConstants.RULES_CACHE_FOLDER), eq("rules.json"), any(CacheEntry.class));
			// verify rules json is retrieved to be loaded into the rules engine
			verify(mockCacheService, times(1)).get(eq(CampaignConstants.CACHE_BASE_DIR + File.separator + CampaignConstants.RULES_CACHE_FOLDER), eq(CampaignConstants.RULES_JSON_FILE_NAME));
			// verify rules remote url added to named collection
			assertEquals(rulesUrl, fakeNamedCollection.getString(CampaignConstants.CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY, ""));
			// verify rules loaded into the rules engine
			verify(mockRulesEngine, times(1)).replaceRules(any());
		});
	}

//
//	@Test
//	public void test_loadCachedMessages_When_NoCachedRulesForUrl_Then_ShouldGetNullCachePath() {
//		// setup
//		setupCachedRules();
//		String rulesUrl = "http://mock.com/rules.zip";
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY, rulesUrl);
//
//		MockCampaignRulesRemoteDownloader mockCampaignRulesRemoteDownloader = getMockCampaignRulesRemoteDownloader(
//				rulesUrl, rulesCacheDirString);
//		assertNotNull(mockCampaignRulesRemoteDownloader);
//
//		campaignExtension.getCampaignRulesRemoteDownloaderReturnValue = mockCampaignRulesRemoteDownloader;
//
//		// test
//		campaignExtension.loadCachedMessages();
//
//		// verify
//		assertTrue(mockCampaignRulesRemoteDownloader.getCachePathWasCalled);
//		assertNull(mockCampaignRulesRemoteDownloader.getCachePathReturnValue);
//	}
//
//	@Test
//	public void startDownloadSync_When_ProtocolHandlerIsNull_ThenBundlePathNull()throws Exception {
//		//Setup
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl("http://mock.com", cacheManager);
//		Map<String, String> requestProperties = new HashMap<String, String>();
//		requestProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");
//
//		platformServices.mockNetworkService.connectUrlReturnValue = new MockConnection("test response", 200, "",
//				requestProperties);
//		//test
//		File bundle = rulesRemoteDownloader.startDownloadSync();
//		//verify
//		assertNull(bundle);
//		File sdkCacheDir = new File(temporaryFolder.getRoot(), DEFAULT_CACHE_DIR);
//		assertEquals(0, sdkCacheDir.list().length);
//	}
//
//	@Test
//	public void startDownloadSync_When_ProtocolHandlerIsNotNull_ThenBundlePathValid() throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//		File expectedBundleFile = new File(temporaryFolder.getRoot(),
//										   DEFAULT_CACHE_DIR + "/" + cacheManager.sha2hash(mockUrl));
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(mockUrl, cacheManager);
//		Map<String, String> requestProperties = new HashMap<String, String>();
//		requestProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");
//
//		platformServices.mockNetworkService.connectUrlReturnValue = new MockConnection("test response", 200, "",
//				requestProperties);
//
//		//set a valid protocol handler
//		rulesRemoteDownloader.setRulesBundleProtocolHandler(
//			getProtocolHandler(1234568989987L, 1234, true, null));
//
//		//test
//		File bundle = rulesRemoteDownloader.startDownloadSync();
//		//verify
//		assertEquals(expectedBundleFile.getAbsolutePath(), bundle.getAbsolutePath());
//		File sdkCacheDir = new File(temporaryFolder.getRoot(), DEFAULT_CACHE_DIR);
//		assertEquals(1, sdkCacheDir.list().length);
//	}
//
//	@Test
//	public void
//	startDownloadSyncWithETagPresentInResponseHeaders_When_ProtocolHandlerIsNotNull_ThenBundlePathValid_And_FilenameContainsETag()
//	throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//		File expectedBundleFile = new File(temporaryFolder.getRoot(),
//										   DEFAULT_CACHE_DIR + "/" + cacheManager.sha2hash(mockUrl));
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(mockUrl, cacheManager);
//		Map<String, String> requestProperties = new HashMap<String, String>();
//		requestProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");
//		requestProperties.put("ETag", ETAG);
//
//		platformServices.mockNetworkService.connectUrlReturnValue = new MockConnection("test response", 200, "",
//				requestProperties);
//
//		//set a valid protocol handler
//		rulesRemoteDownloader.setRulesBundleProtocolHandler(
//			getProtocolHandler(1234568989987L, 1234, true, ETAG));
//
//		//test
//		File bundle = rulesRemoteDownloader.startDownloadSync();
//		//verify
//		assertEquals(expectedBundleFile.getAbsolutePath(), bundle.getAbsolutePath());
//		File sdkCacheDir = new File(temporaryFolder.getRoot(), DEFAULT_CACHE_DIR);
//		assertEquals(1, sdkCacheDir.list().length);
//		String[] fileNameTokens = sdkCacheDir.list()[0].split("\\.");
//		assertEquals(ETAG, HexStringUtil.hexToString(fileNameTokens[fileNameTokens.length - 2]));
//	}
//
//	@Test
//	public void
//	startDownloadSyncWithWeakETagPresentInResponseHeaders_When_ProtocolHandlerIsNotNull_ThenBundlePathValid_And_FilenameContainsWeakETag()
//	throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//		File expectedBundleFile = new File(temporaryFolder.getRoot(),
//										   DEFAULT_CACHE_DIR + "/" + cacheManager.sha2hash(mockUrl));
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(mockUrl, cacheManager);
//		Map<String, String> requestProperties = new HashMap<String, String>();
//		requestProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");
//		requestProperties.put("ETag", WEAK_ETAG);
//
//		platformServices.mockNetworkService.connectUrlReturnValue = new MockConnection("test response", 200, "",
//				requestProperties);
//
//		//set a valid protocol handler
//		rulesRemoteDownloader.setRulesBundleProtocolHandler(
//			getProtocolHandler(1234568989987L, 1234, true, WEAK_ETAG));
//
//		//test
//		File bundle = rulesRemoteDownloader.startDownloadSync();
//		//verify
//		assertEquals(expectedBundleFile.getAbsolutePath(), bundle.getAbsolutePath());
//		File sdkCacheDir = new File(temporaryFolder.getRoot(), DEFAULT_CACHE_DIR);
//		assertEquals(1, sdkCacheDir.list().length);
//		String[] fileNameTokens = sdkCacheDir.list()[0].split("\\.");
//		assertEquals(WEAK_ETAG, HexStringUtil.hexToString(fileNameTokens[fileNameTokens.length - 2]));
//	}
//
//	@Test
//	public void startDownloadSync_When_ProtocolHandlerCannotProcessBundle_ThenBundlePathValid() throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(mockUrl, cacheManager);
//		Map<String, String> requestProperties = new HashMap<String, String>();
//		requestProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");
//
//		platformServices.mockNetworkService.connectUrlReturnValue = new MockConnection("test response", 200, "",
//				requestProperties);
//
//		//set a valid protocol handler
//		rulesRemoteDownloader.setRulesBundleProtocolHandler(
//			getProtocolHandler(1234568989987L, 1234, false, null));
//
//		//test
//		File bundle = rulesRemoteDownloader.startDownloadSync();
//		//verify
//		assertNull(bundle);
//		File sdkCacheDir = new File(temporaryFolder.getRoot(), DEFAULT_CACHE_DIR);
//		assertEquals(0, sdkCacheDir.list().length);
//	}
//
//	@Test
//	public void startDownloadSync_When_DownloadedFileNull_ThenBundlePathNull() throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(mockUrl, cacheManager);
//
//		//This will cause the remote download to return null file
//		platformServices.mockNetworkService.connectUrlReturnValue = null;
//
//		//set a valid protocol handler
//		rulesRemoteDownloader.setRulesBundleProtocolHandler(
//			getProtocolHandler(1234568989987L, 1234, true, null));
//
//		//test
//		File bundle = rulesRemoteDownloader.startDownloadSync();
//		//verify
//		assertNull(bundle);
//		File sdkCacheDir = new File(temporaryFolder.getRoot(), DEFAULT_CACHE_DIR);
//		assertEquals(0, sdkCacheDir.list().length);
//	}
//
//	@Test
//	public void startDownloadSync_When_DownloadedFileIsDirectory_ThenBundlePathSameAsDownloadedPath() throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//		File cachedDirectoryFile = new File(temporaryFolder.getRoot(),
//											DEFAULT_CACHE_DIR + "/" + cacheManager.sha2hash(mockUrl));
//		assertTrue(cachedDirectoryFile.mkdirs());
//		//
//		File dummyRules = new File(cachedDirectoryFile, "rules.json");
//		dummyRules.createNewFile();
//		//
//
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(mockUrl, cacheManager);
//		Map<String, String> requestProperties = new HashMap<String, String>();
//		requestProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");
//
//		//This will cause the remote download to return null file
//		platformServices.mockNetworkService.connectUrlReturnValue = new MockConnection("test response", 416, "",
//				requestProperties);
//
//		//set a valid protocol handler
//		rulesRemoteDownloader.setRulesBundleProtocolHandler(
//			getProtocolHandler(1234568989987L, 1234, true, null));
//
//		//test
//		File bundle = rulesRemoteDownloader.startDownloadSync();
//		//verify
//		assertEquals(cachedDirectoryFile.getAbsolutePath(), bundle.getAbsolutePath());
//		assertEquals(1, cachedDirectoryFile.list().length);
//		assertEquals("rules.json", cachedDirectoryFile.list()[0]);
//	}
//
//	@Test
//	public void getCachePath_happy() throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//
//		File cachedDirectoryFile = new File(temporaryFolder.getRoot(),
//											RULES_CACHE_DIR + "/" + cacheManager.sha2hash(mockUrl));
//		assertTrue(cachedDirectoryFile.mkdirs());
//
//		File dummyRules = new File(cachedDirectoryFile, "rules.json");
//		dummyRules.createNewFile();
//
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(mockUrl, RULES_CACHE_DIR, cacheManager);
//
//		//test
//		File rulesDirectory = rulesRemoteDownloader.getCachePath();
//
//		//verify
//		assertEquals(cachedDirectoryFile.getAbsolutePath(), rulesDirectory.getAbsolutePath());
//		assertEquals(1, rulesDirectory.list().length);
//		assertEquals("rules.json", rulesDirectory.list()[0]);
//	}
//
//	@Test
//	public void getCachePath_When_EmptyUrl_ThenShouldReturnNull() throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//
//		File cachedDirectoryFile = new File(temporaryFolder.getRoot(),
//											RULES_CACHE_DIR + "/" + cacheManager.sha2hash(mockUrl));
//		assertTrue(cachedDirectoryFile.mkdirs());
//
//		File dummyRules = new File(cachedDirectoryFile, "rules.json");
//		dummyRules.createNewFile();
//
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl("", RULES_CACHE_DIR, cacheManager);
//
//		//test
//		File rulesDirectory = rulesRemoteDownloader.getCachePath();
//
//		//verify
//		assertNull(rulesDirectory);
//	}
//
//	@Test
//	public void getCachePath_When_NullUrl_ThenShouldReturnNull() throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//
//		File cachedDirectoryFile = new File(temporaryFolder.getRoot(),
//											RULES_CACHE_DIR + "/" + cacheManager.sha2hash(mockUrl));
//		assertTrue(cachedDirectoryFile.mkdirs());
//
//		File dummyRules = new File(cachedDirectoryFile, "rules.json");
//		dummyRules.createNewFile();
//
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(null, RULES_CACHE_DIR, cacheManager);
//
//		//test
//		File rulesDirectory = rulesRemoteDownloader.getCachePath();
//
//		//verify
//		assertNull(rulesDirectory);
//	}
//
//	@Test
//	public void getCachePath_When_UrlNotCached_ThenShouldReturnNull() throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(mockUrl, RULES_CACHE_DIR, cacheManager);
//
//		//test
//		File rulesDirectory = rulesRemoteDownloader.getCachePath();
//
//		//verify
//		assertNull(rulesDirectory);
//	}
//
//	@Test
//	public void getCachePath_When_EmptyDirectoryOverride_ThenShouldUseDefaultCacheDirectory() throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//
//		File cachedDirectoryFile = new File(temporaryFolder.getRoot(),
//											DEFAULT_CACHE_DIR + "/" + cacheManager.sha2hash(mockUrl));
//		assertTrue(cachedDirectoryFile.mkdirs());
//
//		File dummyRules = new File(cachedDirectoryFile, "rules.json");
//		dummyRules.createNewFile();
//
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(mockUrl, "", cacheManager);
//
//		//test
//		File rulesDirectory = rulesRemoteDownloader.getCachePath();
//
//		//verify
//		assertEquals(cachedDirectoryFile.getAbsolutePath(), rulesDirectory.getAbsolutePath());
//		assertEquals(1, rulesDirectory.list().length);
//		assertEquals("rules.json", rulesDirectory.list()[0]);
//	}
//
//	@Test
//	public void getCachePath_When_NullDirectoryOverride_ThenShouldUseDefaultCacheDirectory() throws Exception {
//		//Setup
//		String mockUrl = "http://mock.com";
//		CacheManager cacheManager = new CacheManager(platformServices.mockSystemInfoService);
//
//		File cachedDirectoryFile = new File(temporaryFolder.getRoot(),
//											DEFAULT_CACHE_DIR + "/" + cacheManager.sha2hash(mockUrl));
//		assertTrue(cachedDirectoryFile.mkdirs());
//
//		File dummyRules = new File(cachedDirectoryFile, "rules.json");
//		dummyRules.createNewFile();
//
//		platformServices.mockSystemInfoService.applicationCacheDir = temporaryFolder.getRoot();
//		rulesRemoteDownloader = getVerifiableRulesRemoteDownloaderForUrl(mockUrl, null, cacheManager);
//
//		//test
//		File rulesDirectory = rulesRemoteDownloader.getCachePath();
//
//		//verify
//		assertEquals(cachedDirectoryFile.getAbsolutePath(), rulesDirectory.getAbsolutePath());
//		assertEquals(1, rulesDirectory.list().length);
//		assertEquals("rules.json", rulesDirectory.list()[0]);
//	}
//
//	private CampaignRulesRemoteDownloader getVerifiableRulesRemoteDownloaderForUrl(final String url,
//			final String directoryOverride,
//			final CacheManager cacheManager) {
//		try {
//			return new CampaignRulesRemoteDownloader(platformServices.getMockNetworkService(),
//					platformServices.mockSystemInfoService, url, directoryOverride, cacheManager);
//		} catch (Exception e) {
//			fail("Could not create the RulesRemoteDownloader instance - " + e);
//		}
//
//		return null;
//	}
//
//	private CampaignRulesRemoteDownloader getVerifiableRulesRemoteDownloaderForUrl(final String url,
//			final CacheManager cacheManager) {
//		try {
//			return new CampaignRulesRemoteDownloader(platformServices.getMockNetworkService(),
//					platformServices.mockSystemInfoService, url, cacheManager);
//		} catch (Exception e) {
//			fail("Could not create the RulesRemoteDownloader instance - " + e);
//		}
//
//		return null;
//	}
//
//	private CampaignRulesRemoteDownloader.RulesBundleNetworkProtocolHandler getProtocolHandler(long lastModifiedDate,
//			long bundleSize,
//			boolean protocolDownloadedBundleReturnValue,
//			String etag) {
//		MockMetadata mockMetadata = new MockMetadata();
//		mockMetadata.getLastModifiedDateReturn = lastModifiedDate;
//		mockMetadata.getSizeReturn = bundleSize;
//		mockMetadata.getEtagReturn = etag;
//
//		MockProtocolHandler protocolHandler = new MockProtocolHandler();
//		protocolHandler.getMetadataReturn = mockMetadata;
//		protocolHandler.processDownloadedBundleReturn = protocolDownloadedBundleReturnValue;
//
//		return protocolHandler;
//	}
}