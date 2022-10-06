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
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class CampaignRulesRemoteDownloader extends RemoteDownloader {

	private static final String LOG_TAG = RulesBundleNetworkProtocolHandler.class.getSimpleName();
	private RulesBundleNetworkProtocolHandler protocolHandler;
	private static final String FORWARD_SLASH = "/";
	private static final String REGEX_PERIOD = "\\.";
	private static final int FILE_NAME_ARRAY_SIZE_WITH_ETAG = 3;

	/**
	 * This is the contract for a concrete implementation that would support
	 * processing a Rules Engine bundle downloaded from the end-point configured.
	 * <p>
	 *
	 * The interface allows the handling of the actual file type downloaded to be changed / plugged -in/out.
	 * The implementation is responsible for: <br>
	 *
	 * <ul>
	 *     <li>
	 *         Processing the downloaded bundle (file)
	 *     </li>
	 *     <li>
	 *         Recording the {@link Metadata} for the bundle. This metadata will be used by the
	 *         {@link CampaignRulesRemoteDownloader} while performing a conditional fetch later.
	 *     </li>
	 * </ul>
	 *
	 */
	interface RulesBundleNetworkProtocolHandler {
		/**
		 * Retrieve the {@code Metadata} for the original downloaded Rules bundle (file).
		 * <p>
		 *
		 *  The metadata should be recorded when this implementation processes the downloaded bundle the first time.
		 *
		 * @param cachedBundlePath The path for the <b>processed</b> bundle.
		 * @return The {@link Metadata} corresponding to the <b>un-processed</b> rules bundle.
		 *
		 * @see #processDownloadedBundle(File, String, long, String)
		 */
		Metadata getMetadata(File cachedBundlePath);

		/**
		 * Process the file that was downloaded by the {@code RulesRemoteDownloader}.
		 * <p>
		 *
		 * The implementation is free to process the file as it wishes. The processed contents should be stored in the
		 * {@code outputPath} path.
		 * <br>
		 *
		 * This method is also responsible to record the {@code downloadedBundle} metadata before the processing is complete.
		 * The metadata will then be used by the {@link CampaignRulesRemoteDownloader} when querying for the same bundle the next time.
		 *
		 * @param downloadedBundle The file that was downloaded by the {@code RulesRemoteDownloader}
		 * @param outputPath The absolute path of the output folder. The implementation is free to create sub-folders underneath.
		 * @param lastModifiedDateForBundle The last modified date obtained from the downloaded resource.
		 * @param etag The ETag obtained from the downloaded resource.
		 *
		 * @return Indication of whether the processing was successful.
		 */
		boolean processDownloadedBundle(File downloadedBundle, String outputPath, long lastModifiedDateForBundle, String etag);
	}

	interface Metadata {
		long getLastModifiedDate();
		long getSize();
		String getETag();
	}

	/**
	 * Constructor.
	 * <p>
	 * The default {@link RulesBundleNetworkProtocolHandler} will be set here. The default is {@link CampaignZipBundleHandler}.
	 * If the {@link CompressedFileService} is not available on he platform, the {@code CampaignZipBundleHandler} will not be used, and
	 * this downloader will discard the downloaded files.
	 *
	 * @param networkService The platform {@link NetworkService} implementation
	 * @param systemInfoService The platform {@link SystemInfoService} implementation
	 * @param compressedFileService The platform {@link CompressedFileService} implementation
	 * @param url The URL to download from
	 * @param directoryOverride The cache directory override.
	 * @param requestProperties {@code Map<String, String>} containing any additional key value pairs to be used while requesting a
	 *                        connection to the url
	 * @throws MissingPlatformServicesException If the required platform services are not available on the platform. The {@link CompressedFileService} is <b>not</b> a
	 * required platform service. If it is not available, the {@link CampaignZipBundleHandler} will not be used as the {@code RulesBundleNetworkProtocolHandler}
	 *
	 * @see #setRulesBundleProtocolHandler(RulesBundleNetworkProtocolHandler)
	 */
	CampaignRulesRemoteDownloader(final NetworkService networkService, final SystemInfoService systemInfoService,
								  final CompressedFileService compressedFileService,
								  final String url, final String directoryOverride,
								  final Map<String, String> requestProperties) throws MissingPlatformServicesException {
		super(networkService, systemInfoService, url, directoryOverride, requestProperties);

		try {
			//The default file type is Zip.
			protocolHandler = new CampaignZipBundleHandler(compressedFileService);
		} catch (MissingPlatformServicesException ex) {
			Log.trace(LOG_TAG, "Will not be using Zip Protocol to download rules (%s)", ex);
		}
	}

	/**
	 * Constructor.
	 * <p>
	 * The default {@link RulesBundleNetworkProtocolHandler} will be set here. The default is {@link CampaignZipBundleHandler}.
	 * If the {@link CompressedFileService} is not available on he platform, the {@code CampaignZipBundleHandler} will not be used, and
	 * this downloader will discard the downloaded files.
	 *
	 * @param networkService The platform {@link NetworkService} implementation
	 * @param systemInfoService The platform {@link SystemInfoService} implementation
	 * @param compressedFileService The platform {@link CompressedFileService} implementation
	 * @param url The URL to download from
	 * @param directoryOverride The cache directory override.
	 * @throws MissingPlatformServicesException If the required platform services are not available on the platform. The {@link CompressedFileService} is <b>not</b> a
	 * required platform service. If it is not available, the {@link CampaignZipBundleHandler} will not be used as the {@code RulesBundleNetworkProtocolHandler}
	 *
	 * @see #setRulesBundleProtocolHandler(RulesBundleNetworkProtocolHandler)
	 */
	CampaignRulesRemoteDownloader(final NetworkService networkService, final SystemInfoService systemInfoService,
								  final CompressedFileService compressedFileService,
								  final String url, final String directoryOverride) throws MissingPlatformServicesException {
		super(networkService, systemInfoService, url, directoryOverride);

		try {
			//The default file type is Zip.
			protocolHandler = new CampaignZipBundleHandler(compressedFileService);
		} catch (MissingPlatformServicesException ex) {
			Log.trace(LOG_TAG, "Will not be using Zip Protocol to download rules (%s)", ex);
		}
	}

	/**
	 * Testing constructor - Solely used for unit testing purposes.
	 *
	 * @param networkService   {@link NetworkService} instance
	 * @param systemInfoService {@link SystemInfoService} instance
	 * @param url {@link String} containing URL to downloaded from
	 * @param directoryOverride {@code String} containing cache directory override
	 * @param cacheManager {@link CacheManager} instance used by the downloader
	 * @throws MissingPlatformServicesException  if the downloader is initialized with null {@code NetworkService}
	 * or {@code SystemInfoService}.
	 */
	CampaignRulesRemoteDownloader(final NetworkService networkService, final SystemInfoService systemInfoService,
								  final String url, final String directoryOverride,
								  final CacheManager cacheManager) throws MissingPlatformServicesException {
		super(networkService, systemInfoService, url, directoryOverride, cacheManager);
	}


	/**
	 * Testing constructor - Solely used for unit testing purposes.
	 *
	 * @param networkService   {@link NetworkService} instance
	 * @param systemInfoService {@link SystemInfoService} instance
	 * @param url {@link String} containing URL to downloaded from
	 * @param cacheManager {@link CacheManager} instance used by the downloader
	 * @throws MissingPlatformServicesException  if the downloader is initialized with null {@code NetworkService}
	 * or {@code SystemInfoService}.
	 */
	CampaignRulesRemoteDownloader(final NetworkService networkService, final SystemInfoService systemInfoService,
								  final String url,
								  final CacheManager cacheManager) throws MissingPlatformServicesException {
		super(networkService, systemInfoService, url, cacheManager);
	}

	@Override
	protected HashMap<String, String> getRequestParameters(final File cacheFile) {
		//Get the request parameters from the unzipped folder
		HashMap<String, String> requestParameters = new HashMap<String, String>();

		if (cacheFile == null || protocolHandler == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getRequestParameters -  The passed Cached File or Protocol Handler is null, so returning empty request parameters.");
			return requestParameters;
		}

		Metadata metadata = protocolHandler.getMetadata(cacheFile);

		if (metadata != null) {
			final Long date = metadata.getLastModifiedDate();
			final String etag = metadata.getETag();
			String lastModifiedDate = null;

			if (date != 0L) {
				final SimpleDateFormat rfc2822Formatter = createRFC2822Formatter();
				lastModifiedDate = rfc2822Formatter.format(date);
				requestParameters.put("If-Modified-Since", lastModifiedDate);
			}

			// Add support for conditional GET request
			// The "If-Range" header uses an ETag or the last modified date (not both).
			// See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/If-Range for more info.
			if (etag != null) {
				requestParameters.put("If-Range", etag);
				requestParameters.put("If-None-Match", etag);
			} else {
				requestParameters.put("If-Range", lastModifiedDate);
			}

			String rangeRequestString = String.format(Locale.US, "bytes=%d-", metadata.getSize());
			requestParameters.put("Range", rangeRequestString);
		}

		return requestParameters;
	}

	/**
	 * Process the downloaded file and return the destination path.
	 *
	 * @param downloadedFile The file that was downloaded by this.
	 * @return The destination path where the processed file contents were stored. Will be null if the processing failed.
	 */
	protected File processBundle(final File downloadedFile) {
		File processedFile = null;
		String etag = null;

		if (downloadedFile == null) {
			return null;
		}

		// save the ETag if present in the rules cache file name
		String[] splitFilenameArray = downloadedFile.getPath().split(REGEX_PERIOD);

		// if the ETag is present, it will be in the second to last index. we also need to check that there are no forward slashes in the extracted ETag
		// to fix an android functional test. for more background on this, this check was added because the android functional tests have more periods
		// present in the filename. this causes a false positive when checking for the presence of an ETag when one wasn't actually provided.
		if (splitFilenameArray.length >= FILE_NAME_ARRAY_SIZE_WITH_ETAG
				&& !splitFilenameArray[splitFilenameArray.length - 2].contains(FORWARD_SLASH)) {
			// the ETag value is hex encoded
			etag = HexStringUtil.hexToString(splitFilenameArray[splitFilenameArray.length - 2]);
		}

		if (downloadedFile.isDirectory()) {
			processedFile = downloadedFile;
		} else {
			//The folder that has been processed will be sent out to the method (not the zip file)
			final Long date = cacheManager != null ? cacheManager.getLastModifiedOfFile(downloadedFile.getPath()) : 0L;
			String outputPath = cacheManager != null ? cacheManager.getBaseFilePath(url, directory) : null;

			if (outputPath != null && protocolHandler.processDownloadedBundle(downloadedFile, outputPath, date, etag)) {
				processedFile = new File(outputPath);
			}
		}

		return processedFile;
	}

	/**
	 * Set an alternative {@code RulesBundleNetworkProtocolHandler} implementation.
	 *
	 * @param protocolHandler The {@link RulesBundleNetworkProtocolHandler} implementation to be used.
	 */
	void setRulesBundleProtocolHandler(final RulesBundleNetworkProtocolHandler protocolHandler) {
		this.protocolHandler = protocolHandler;
	}

	/**
	 * Downloads the rules bundle file if needed and processes the file.
	 * <p>
	 *
	 * The file will be processed using the {@link RulesBundleNetworkProtocolHandler} set.
	 * @return The processed file. If the download was not required, then returns the cached directory.
	 */
	@Override
	public File startDownloadSync() {
		File downloadedBundle =  super.startDownloadSync();
		//The downloaded file will be a zip / or something else.
		//We will need to convert that into a folder.
		File processedBundlePath = null;

		if (downloadedBundle != null && protocolHandler != null) {
			processedBundlePath = processBundle(downloadedBundle);
		}

		if (processedBundlePath == null) {
			Log.debug(CampaignConstants.LOG_TAG, "startDownloadSync -  Unable to unzip rules bundle.");
			//Purge the file since we do not know what to do with this
			cacheManager.deleteCachedDataForURL(url, directory);
		}

		return processedBundlePath;
	}

	/**
	 * Returns the directory in cache where contents for the {@code RemoteDownloader.url} are stored
	 *
	 * @return {@link File} destination where the {@link RemoteDownloader#url} contents are stored in cache
	 * @see CacheManager#getFileForCachedURL(String, String, boolean)
	 */
	File getCachePath() {
		Log.trace(CampaignConstants.LOG_TAG, "getCachePath -  Locating cache file path for url: '%s'", url);

		return cacheManager.getFileForCachedURL(url, directory, true);
	}
}

