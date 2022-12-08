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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
public class CampaignMessageAssetsDownloaderTests {

    private CampaignMessageAssetsDownloader campaignMessageAssetsDownloader;
    private ArrayList<String> assets;
    private String fakeMessageId;
    private String expectedCacheLocation;
    private String messageCacheDirString;
    private File cacheDir;
    private HashMap<String, String> metadataMap;
    private static final String messageId = "07a1c997-2450-46f0-a454-537906404124";
    private static final String assetUrl = "https://www.adobe.com/logo.png";
    private static final String MESSAGES_CACHE = CampaignConstants.CACHE_BASE_DIR + File.separator + CampaignConstants.MESSAGE_CACHE_DIR + File.separator;

    @Mock
    ServiceProvider mockServiceProvider;
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

        // setup assets for testing
        assets = new ArrayList<>();
        assets.add(assetUrl);
        fakeMessageId = "d38a46f6-4f43-435a-a862-4038c27b90a1";
        expectedCacheLocation = "cache/aepsdkcache/campaign/messages/" + fakeMessageId;
        messageCacheDirString = "campaign/messages/" + fakeMessageId;
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
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockCacheService.set(anyString(), anyString(), any(CacheEntry.class))).thenReturn(true);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
            when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);
            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(cacheDir);
            // create CampaignMessageAssetsDownloader instance
            campaignMessageAssetsDownloader = new CampaignMessageAssetsDownloader(assets, fakeMessageId);
            testRunnable.run();
        }
    }

    // ====================================================================================================
    // void downloadAssetCollection()
    // ====================================================================================================
    @Test
    public void testDownloadAssetCollection_when_assetNotInCache_then_assetIsCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {

            when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(mockHttpConnection.getInputStream()).thenReturn(new ByteArrayInputStream("assetData".getBytes(StandardCharsets.UTF_8)));
            doAnswer((Answer<Void>) invocation -> {
                NetworkCallback callback = invocation.getArgument(1);
                callback.call(mockHttpConnection);
                return null;
            }).when(mockNetworkService)
                    .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));

            try (MockedStatic<Utils> campaignUtilsMockedStatic = Mockito.mockStatic(Utils.class)) {
                // test
                campaignMessageAssetsDownloader.downloadAssetCollection();
                // verify
                campaignUtilsMockedStatic.verify(() -> Utils.clearCachedAssetsNotInList(any(), any()));
                verify(mockCacheService, times(1)).get(eq(expectedCacheLocation), eq(assetUrl));
                verify(mockNetworkService, times(1)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
                // verify asset cached
                verify(mockCacheService, times(1)).set(eq(messageCacheDirString), eq(assetUrl), any(CacheEntry.class));
            }
        });
    }

    @Test
    public void testDownloadAssetCollection_when_existingAssetInCache_then_assetIsNotCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {

            when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
            when(mockHttpConnection.getInputStream()).thenReturn(new ByteArrayInputStream("assetData".getBytes(StandardCharsets.UTF_8)));
            doAnswer((Answer<Void>) invocation -> {
                NetworkCallback callback = invocation.getArgument(1);
                callback.call(mockHttpConnection);
                return null;
            }).when(mockNetworkService)
                    .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));

            try (MockedStatic<Utils> campaignUtilsMockedStatic = Mockito.mockStatic(Utils.class)) {
                // test
                campaignMessageAssetsDownloader.downloadAssetCollection();
                // verify
                campaignUtilsMockedStatic.verify(() -> Utils.clearCachedAssetsNotInList(any(), any()));
                verify(mockCacheService, times(1)).get(eq(expectedCacheLocation), eq(assetUrl));
                verify(mockNetworkService, times(1)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
                // verify asset not cached
                verify(mockCacheService, times(0)).set(eq(messageCacheDirString), eq(assetUrl), any(CacheEntry.class));
            }
        });
    }

    @Test
    public void testDownloadAssetCollection_when_assetInCacheIsNotForActiveMessage_then_cachedAssetIsDeleted() throws
            Exception {
        // setup
        final File existingCacheDir = new File("cache/aepsdkcache/campaign/messages/d38a46f6-4f43-435a-a862-4038c27b90a1");
        existingCacheDir.mkdirs();
        final File existingCachedFile = new
                File("cache/aepsdkcache/campaign/messages/d38a46f6-4f43-435a-a862-4038c27b90a1/028dbbd3617ccfb5e302f4aa2df2eb312d1571ee40b3f4aa448658c9082b0411");
        existingCachedFile.createNewFile();

        setupServiceProviderMockAndRunTest(() -> {

            when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(mockHttpConnection.getInputStream()).thenReturn(new ByteArrayInputStream("assetData".getBytes(StandardCharsets.UTF_8)));
            doAnswer((Answer<Void>) invocation -> {
                NetworkCallback callback = invocation.getArgument(1);
                callback.call(mockHttpConnection);
                return null;
            }).when(mockNetworkService)
                    .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));

            // test
            campaignMessageAssetsDownloader.downloadAssetCollection();
            // verify
            verify(mockCacheService, times(1)).get(eq(expectedCacheLocation), eq(assetUrl));
            verify(mockNetworkService, times(1)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
            // verify new asset cached
            verify(mockCacheService, times(1)).set(eq(messageCacheDirString), eq(assetUrl), any(CacheEntry.class));
            // verify non matching cached asset deleted
            assertEquals(false, existingCachedFile.exists());
        });
    }

    @Test
    public void testDownloadAssetCollection_when_assetIsNotDownloadable_then_assetIsNotCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {

            when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
            doAnswer((Answer<Void>) invocation -> {
                NetworkCallback callback = invocation.getArgument(1);
                callback.call(mockHttpConnection);
                return null;
            }).when(mockNetworkService)
                    .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));

            try (MockedStatic<Utils> campaignUtilsMockedStatic = Mockito.mockStatic(Utils.class)) {
                // test
                campaignMessageAssetsDownloader.downloadAssetCollection();
                // verify
                campaignUtilsMockedStatic.verify(() -> Utils.clearCachedAssetsNotInList(any(), any()));
                verify(mockCacheService, times(1)).get(eq(expectedCacheLocation), eq(assetUrl));
                verify(mockNetworkService, times(1)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
                // verify asset not cached
                verify(mockCacheService, times(0)).set(eq(messageCacheDirString), eq(assetUrl), any(CacheEntry.class));
            }
        });
    }
}
