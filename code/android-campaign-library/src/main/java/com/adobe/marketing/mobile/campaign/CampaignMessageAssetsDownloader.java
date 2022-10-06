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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Assists in downloading and caching assets for {@code CampaignMessage}s.
 */
class CampaignMessageAssetsDownloader {

	private final List<String> assetsCollection;
	private final NetworkService networkService;
	private final SystemInfoService systemInfoService;
	private final String cacheSubDirectory;

	/**
	 * Constructor.
	 *
	 * @param platformServices {@link PlatformServices} instance
	 * @param assets {@code ArrayList<String>} of assets to download and cache
	 * @param parentMessageId {@link String} containing the message Id of the requesting message used as a cache subdirectory
	 */
	CampaignMessageAssetsDownloader(final PlatformServices platformServices, final List<String> assets,
									final String parentMessageId) {
		this.assetsCollection = assets;
		this.networkService = platformServices.getNetworkService();
		this.systemInfoService = platformServices.getSystemInfoService();
		this.cacheSubDirectory = CampaignConstants.MESSAGE_CACHE_DIR + File.separator + parentMessageId;
		createMessageAssetCacheDirectory();
	}

	/**
	 * Downloads and caches assets for a {@code CampaignMessage}.
	 * <p>
	 * Loops through {@link #assetsCollection} and creates a {@link RemoteDownloader} to handle downloading and
	 * caching the collection of assets.
	 * <p>
	 * Attempts to purge assets that have previously been cached but are for messages that are no longer active.
	 */
	void downloadAssetCollection() {
		final ArrayList<String> assetsToRetain = new ArrayList<String>();

		if (assetsCollection != null && !assetsCollection.isEmpty()) {
			for (final String currentAsset : assetsCollection) {
				if (assetIsDownloadable(currentAsset)) {
					assetsToRetain.add(currentAsset);
				}
			}
		}

		// clear old assets
		try {
			new CacheManager(systemInfoService).deleteFilesNotInList(assetsToRetain, cacheSubDirectory);
		} catch (final MissingPlatformServicesException ex) {
			Log.debug(CampaignConstants.LOG_TAG, "downloadAssetCollection -  Unable to delete cache for old messages \n (%s)", ex);
		}

		for (final String currentAsset : assetsToRetain) {
			try {
				final RemoteDownloader downloader = getRemoteDownloader(currentAsset);
				downloader.startDownload();
			} catch (MissingPlatformServicesException e) {
				Log.debug(CampaignConstants.LOG_TAG,
						  "downloadAssetCollection -  Cannot download assets without platform services \n (%s)", e);
			}
		}
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
		return StringUtils.stringIsUrl(assetPath) && (assetPath.startsWith("http") || assetPath.startsWith("https"));
	}

	/**
	 * Returns a {@code RemoteDownloader} object for the provided {@code currentAssetUrl}.
	 *
	 * @param currentAssetUrl {@link String} containing the URL of the asset to be downloaded
	 * @return A {@link RemoteDownloader} configured with the {@code currentAssetUrl}
	 * @throws MissingPlatformServicesException when {@link #networkService} or {@link #systemInfoService} are null
	 */
	private RemoteDownloader getRemoteDownloader(final String currentAssetUrl) throws MissingPlatformServicesException {
		return new RemoteDownloader(networkService, systemInfoService, currentAssetUrl, cacheSubDirectory) {
			@Override
			protected void onDownloadComplete(final File downloadedFile) {
				if (downloadedFile != null) {
					Log.trace(CampaignConstants.LOG_TAG, "%s has been downloaded and cached.", currentAssetUrl);
				} else {
					Log.debug(CampaignConstants.LOG_TAG, "Failed to download asset from %s.", currentAssetUrl);
				}
			}
		};
	}

	/**
	 * Creates assets cache directory for a {@code CampaignMessage}.
	 * <p>
	 * This method checks if the cache directory already exists in which case no new directory is created for assets.
	 */
	private void createMessageAssetCacheDirectory() {
		try {
			final File assetDir = new File(systemInfoService.getApplicationCacheDir() + File.separator
										   + CampaignConstants.MESSAGE_CACHE_DIR);

			if (!assetDir.exists() && !assetDir.mkdirs()) {
				Log.warning(CampaignConstants.LOG_TAG,
							"Unable to create directory for caching message assets");
			}
		} catch (final Exception ex) {
			Log.warning(CampaignConstants.LOG_TAG, "An unexpected error occurred while managing assets cache directory: \n %s", ex);
		}
	}
}
