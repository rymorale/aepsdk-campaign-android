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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.*;

public class CampaignMessageAssetsDownloaderTests extends BaseTest {
	static final String MESSAGE_CACHE_DIR = "messages";

	CampaignMessageAssetsDownloader downloader;
	MockNetworkService mockNetworkService;
	MockSystemInfoService mockSystemInfoService;
	ArrayList<String> assets;
	String fakeMessageId;
	File cacheDir;
	String messageCacheDirString;

	@Before
	public void setup() {
		super.beforeEach();
		// setup platform services
		mockNetworkService = platformServices.getMockNetworkService();
		mockSystemInfoService = platformServices.getMockSystemInfoService();
		cacheDir = new File("cache");
		cacheDir.mkdirs();
		cacheDir.setWritable(true);
		mockSystemInfoService.applicationCacheDir = cacheDir;

		// setup assets for testing
		assets = new ArrayList<String>();
		assets.add("https://www.pexels.com/photo/grey-fur-kitten-127028/");
		fakeMessageId = "d38a46f6-4f43-435a-a862-4038c27b90a1";
		messageCacheDirString = MESSAGE_CACHE_DIR + File.separator + fakeMessageId;

		// create CampaignMessageAssetsDownloader instance
		downloader = new CampaignMessageAssetsDownloader(platformServices, assets, fakeMessageId);
	}

	@After
	public void tearDown() {
		clearCacheFiles(cacheDir);
	}

	// ====================================================================================================
	// void downloadAssetCollection()
	// ====================================================================================================
	@Test
	public void testDownloadAssetCollection_when_assetNotInCache_then_assetIsCached() throws Exception {
		// setup
		final String response = "abcd";
		final int responseCode = 200;
		final String responseMessage = "";
		final HashMap<String, String> responseProperties = new HashMap<String, String>();
		responseProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");

		mockNetworkService.connectUrlAsyncCallbackParametersConnection =
			new MockConnection(response, responseCode, responseMessage, responseProperties);

		// execute
		downloader.downloadAssetCollection();

		// verify
		final CacheManager manager = new CacheManager(mockSystemInfoService);
		final File cachedFile = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
								messageCacheDirString, false);
		assertNotNull("Cached file should not be null", cachedFile);
	}

	@Test
	public void testDownloadAssetCollection_when_existingAssetInCache_then_assetIsCached() throws Exception {
		// setup
		final File existingCacheDir = new File("cache/messages/d38a46f6-4f43-435a-a862-4038c27b90a1");
		existingCacheDir.mkdirs();
		final File existingCachedFile = new
		File("cache/messages/d38a46f6-4f43-435a-a862-4038c27b90a1/028dbbd3617ccfb5e302f4aa2df2eb312d1571ee40b3f4aa448658c9082b0411.1262304000000");
		existingCachedFile.createNewFile();

		final String response = "abcd";
		final int responseCode = 200;
		final String responseMessage = "";
		final HashMap<String, String> responseProperties = new HashMap<String, String>();
		responseProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");

		mockNetworkService.connectUrlAsyncCallbackParametersConnection =
			new MockConnection(response, responseCode, responseMessage, responseProperties);

		// execute
		downloader.downloadAssetCollection();

		// verify
		final CacheManager manager = new CacheManager(mockSystemInfoService);
		final File cachedFile = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
								messageCacheDirString, false);
		assertNotNull("Cached file should not be null", cachedFile);
		assertEquals(existingCachedFile, cachedFile);
	}

	@Test
	public void testDownloadAssetCollection_when_assetInCacheIsNotForActiveMessage_then_cachedAssetIsDeleted() throws
		Exception {
		// setup
		final File existingCacheDir = new File("cache/messages/d38a46f6-4f43-435a-a862-4038c27b90a1");
		existingCacheDir.mkdirs();
		final File existingCachedFile = new
		File("cache/messages/d38a46f6-4f43-435a-a862-4038c27b90a1/028dbbd3617ccfb5e302f4aa2df2eb312d1571ee40b3f4aa448658c9082b0411.1262304000000");
		existingCachedFile.createNewFile();

		final String response = "abcd";
		final int responseCode = 200;
		final String responseMessage = "";
		final HashMap<String, String> responseProperties = new HashMap<String, String>();
		responseProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");

		mockNetworkService.connectUrlAsyncCallbackParametersConnection =
			new MockConnection(response, responseCode, responseMessage, responseProperties);

		downloader = new CampaignMessageAssetsDownloader(platformServices, new ArrayList<String>() {
			{
				add("https://www.pexels.com/photo/white-and-black-cat-156934/");
			}
		}, fakeMessageId);

		// pre-verify
		final CacheManager manager = new CacheManager(mockSystemInfoService);
		final File cachedFile = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
								messageCacheDirString, false);
		assertNotNull("Cached file should not be null", cachedFile);

		// execute
		downloader.downloadAssetCollection();

		// post-verify
		final File postCachedFileDoNotRetain =
			manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
										messageCacheDirString, false);
		assertNull("Cached file should be null", postCachedFileDoNotRetain);
		final File postCachedFileRetain =
			manager.getFileForCachedURL("https://www.pexels.com/photo/white-and-black-cat-156934/",
										messageCacheDirString, false);
		assertNotNull("Cached file should not be null", postCachedFileRetain);
	}

	@Test
	public void testDownloadAssetCollection_when_assetIsNotDownloadable_then_assetIsNotCached() throws Exception {
		// setup
		assets.clear();
		assets.add("not a url");
		assets.add("ftp://notvalidscheme");

		downloader = new CampaignMessageAssetsDownloader(platformServices, assets, fakeMessageId);

		// execute
		downloader.downloadAssetCollection();

		// post-verify
		final File messageCacheDir = new File(cacheDir, messageCacheDirString);
		assertEquals("cache dir should be empty", 0, messageCacheDir.list().length);
	}

	private void clearCacheFiles(final File file) {
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
}
