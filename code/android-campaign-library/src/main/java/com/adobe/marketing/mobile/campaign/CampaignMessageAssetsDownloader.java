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

import static com.adobe.marketing.mobile.campaign.CampaignConstants.CACHE_BASE_DIR;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.CAMPAIGN_TIMEOUT_DEFAULT;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_CACHE_DIR;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.METADATA_PATH;

import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheExpiry;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.services.internal.caching.FileCacheService;
import com.adobe.marketing.mobile.util.UrlUtils;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assists in downloading and caching assets for {@code CampaignMessage}s.
 */
class CampaignMessageAssetsDownloader {
    private static final String SELF_TAG = "CampaignMessageAssetsDownloader";
    private final List<String> assetsCollection;
    private final Networking networkService;
    private final DeviceInforming deviceInfoService;
    private final CacheService cacheService;
    private final String messageId;
    private File assetDir;

    /**
     * Constructor.
     *
     * @param assets {@code ArrayList<String>} of assets to download and cache
     * @param parentMessageId {@link String} containing the message Id of the requesting message used as a cache subdirectory
     */
    CampaignMessageAssetsDownloader(final List<String> assets, final String parentMessageId) {
        this.assetsCollection = assets;
        this.networkService = ServiceProvider.getInstance().getNetworkService();
        this.deviceInfoService = ServiceProvider.getInstance().getDeviceInfoService();
        this.cacheService = new FileCacheService();
        this.messageId = parentMessageId;
        createMessageAssetCacheDirectory();
    }

    /**
     * Downloads and caches assets for a {@code CampaignMessage}.
     * <p>
     * Loops through {@link #assetsCollection} downloads and caches the collection of assets.
     * <p>
     * Attempts to purge assets that have previously been cached but are for messages that are no longer active.
     */
    void downloadAssetCollection() {
        final ArrayList<String> assetsToRetain = new ArrayList<>();

        if (assetsCollection != null && !assetsCollection.isEmpty()) {
            for (final String currentAsset : assetsCollection) {
                if (assetIsDownloadable(currentAsset)) {
                    assetsToRetain.add(currentAsset);
                }
            }
        }

        // clear old assets
        Utils.clearCachedAssetsNotInList(assetDir, assetsToRetain);

        // download assets within the assets to retain list
        for (final String url : assetsToRetain) {
            final NetworkRequest networkRequest = new NetworkRequest(url, HttpMethod.GET, null, null, CAMPAIGN_TIMEOUT_DEFAULT, CAMPAIGN_TIMEOUT_DEFAULT);
            networkService.connectAsync(networkRequest, httpConnecting -> {
                if (httpConnecting.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.debug(LOG_TAG, SELF_TAG, "downloadAssetCollection - Failed to download asset from URL: %s", url);
                    return;
                }
                cacheAssetData(httpConnecting.getInputStream(), url, messageId);
            });
        }
    }

    /**
     * Caches the provided {@link InputStream} containing the data downloaded from the given URL for the given message Id.
     *
     * @param assetData {@link InputStream} containing the download remote asset data.
     * @param key {@code String} The asset download URL. Used to name the cache folder.
     * @param messageId {@code String} The id of the message
     */
    private void cacheAssetData(final InputStream assetData, final String key, final String messageId) {
        // create message asset cache directory if needed
        if (!createDirectoryIfNeeded(messageId)) {
            Log.debug(LOG_TAG, SELF_TAG, "cacheAssetData - Cannot cache asset for message id %s, failed to create cache directory.", messageId);
            return;
        }

        final String assetCacheLocation = CACHE_BASE_DIR + File.separator + MESSAGE_CACHE_DIR + File.separator + messageId;

        // check if asset already cached
        if(cacheService.get(assetCacheLocation, key) != null) {
            Log.debug(LOG_TAG, SELF_TAG, "cacheAssetData - Found cached asset for message id %s.", key, messageId);
            return;
        }

        Log.debug(LOG_TAG, SELF_TAG, "cacheAssetData - Caching asset %s for message id %s.", key, messageId);
        final Map<String, String> metadata = new HashMap<>();
        metadata.put(METADATA_PATH, assetCacheLocation);
        final CacheEntry cacheEntry = new CacheEntry(assetData, CacheExpiry.never(), null);
        cacheService.set(assetCacheLocation, key, cacheEntry);
    }

    /**
     * Determine whether the provided {@code assetPath} is downloadable.
     * <p>
     * Checks that the {@code assetPath} is both a valid URL, and has a scheme of "http" or "https".
     *
     * @param assetPath {@link String} containing the asset path to check
     * @return {@code boolean} indicating whether the provided asset is downloadable
     */
    private boolean assetIsDownloadable(final String assetPath) {
        return UrlUtils.isValidUrl(assetPath) && (assetPath.startsWith("http") || assetPath.startsWith("https"));
    }

    /**
     * Creates assets cache directory for a {@code CampaignMessage}.
     * <p>
     * This method checks if the cache directory already exists in which case no new directory is created for assets.
     */
    private void createMessageAssetCacheDirectory() {
        try {
            assetDir = new File(deviceInfoService.getApplicationCacheDir() + File.separator + CACHE_BASE_DIR + File.separator
                    + MESSAGE_CACHE_DIR);

            if (!assetDir.exists() && !assetDir.mkdirs()) {
                Log.warning(CampaignConstants.LOG_TAG, SELF_TAG,
                        "createMessageAssetCacheDirectory - Unable to create directory for caching message assets");
            }
        } catch (final Exception ex) {
            Log.warning(CampaignConstants.LOG_TAG, SELF_TAG, "createMessageAssetCacheDirectory - An unexpected error occurred while managing assets cache directory: \n %s", ex);
        }
    }

    /**
     * Creates assets cache directory for a {@code CampaignMessage}.
     * <p>
     * This method checks if the cache directory already exists in which case no new directory is created for assets.
     * @param messageId {@code String} The id of the message
     * @return {@code boolean} if true, the asset cache directory for the message id was created successfully
     */
    private boolean createDirectoryIfNeeded(final String messageId) {
        if (!assetDir.exists()) {
            return false;
        }

        final File cacheDirectory = new File(assetDir + File.separator + messageId);
        if (!cacheDirectory.exists()) {
            return cacheDirectory.mkdir();
        }
        return true;
    }
}