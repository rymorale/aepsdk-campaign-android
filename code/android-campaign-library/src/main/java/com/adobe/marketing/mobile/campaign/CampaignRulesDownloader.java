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
import static com.adobe.marketing.mobile.campaign.CampaignConstants.CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.CAMPAIGN_TIMEOUT_DEFAULT;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.HTTP_HEADER_ETAG;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.HTTP_HEADER_IF_MODIFIED_SINCE;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.HTTP_HEADER_IF_NONE_MATCH;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.HTTP_HEADER_LAST_MODIFIED;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_CACHE_DIR;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_CONSEQUENCE_MESSAGE_TYPE;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.RULES_JSON_FILE_NAME;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.ZIP_HANDLE;

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.internal.util.StringEncoder;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.launch.rulesengine.download.RulesLoadResult;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheExpiry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StreamUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.TimeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

class CampaignRulesDownloader {
    private final static String SELF_TAG = "CampaignRulesDownloader";
    private final ExtensionApi extensionApi;
    private final LaunchRulesEngine campaignRulesEngine;
    private final NamedCollection campaignNamedCollection;
    private final CacheService cacheService;
    private final Networking networkService;
    private static final String TEMP_RULES_DIR = "campaign_temp";
    private CampaignMessageAssetsDownloader campaignMessageAssetsDownloader;

    CampaignRulesDownloader(final ExtensionApi extensionApi, final LaunchRulesEngine campaignRulesEngine, final NamedCollection campaignNamedCollection, final CacheService cacheService) {
        this.extensionApi = extensionApi;
        this.campaignRulesEngine = campaignRulesEngine;
        this.campaignNamedCollection = campaignNamedCollection;
        this.cacheService = cacheService;
        this.networkService = ServiceProvider.getInstance().getNetworkService();
    }

    /**
     * Starts async rules download from the provided {@code url}.
     * <p>
     * This method uses the {@link Networking} service to download the rules and the {@link CacheService}
     * to cache the downloaded Campaign rules. Once the rules are downloaded, they are registered with the Campaign extension's {@link LaunchRulesEngine} instance.
     * <p>
     * If the given {@code url} is null or empty no rules download happens.
     *
     * @param url           {@link String} containing Campaign rules download URL
     * @param linkageFields {@link String} containing optional linkage fields to include when downloading Campaign rules
     */
    void loadRulesFromUrl(final String url, final String linkageFields) {
        if (networkService == null) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "loadRulesFromUrl - Cannot download rules, the network service is unavailable.");
            return;
        }

        if (StringUtils.isNullOrEmpty(url)) {
            Log.warning(LOG_TAG, SELF_TAG,
                    "loadRulesFromUrl - Cannot download rules, provided url is null or empty. Cached rules will be used if present.");
            return;
        }

        // 304 - Not Modified support
        Map<String, String> requestProperties = new HashMap<>();
        final CacheResult cachedRules = cacheService.get(CACHE_BASE_DIR, ZIP_HANDLE);
        if (cachedRules != null) {
            requestProperties = extractHeadersFromCache(cachedRules);
        }

        if (!StringUtils.isNullOrEmpty(linkageFields)) {
            requestProperties.put(CampaignConstants.LINKAGE_FIELD_NETWORK_HEADER, linkageFields);
        }

        final NetworkRequest networkRequest = new NetworkRequest(
                url,
                HttpMethod.GET,
                null,
                requestProperties,
                CAMPAIGN_TIMEOUT_DEFAULT,
                CAMPAIGN_TIMEOUT_DEFAULT
        );
        networkService.connectAsync(networkRequest, httpConnecting -> {
            onRulesDownloaded(url, httpConnecting);
        });
    }

    /**
     * Invoked when rules have finished downloading.
     * <p>
     * This method takes the following actions once rules are downloaded:
     * <ul>
     *     <li>Unregister any previously registered rules.</li>
     *     <li>Persist the provided remotes {@code url} in Campaign data store.</li>
     *     <li>Register downloaded rules with the {@code CampaignRulesEngine}.</li>
     * </ul>
     *
     * @param response {@link HttpConnecting} containing the downloaded Campaign rules
     * @see #updateUrlInNamedCollection(String)
     * @see LaunchRulesEngine#replaceRules(List)
     * @see #cacheRemoteAssets(List)
     */
    private void onRulesDownloaded(final String url, final HttpConnecting response) {
        // process the downloaded bundle
        RulesLoadResult rulesLoadResult;
        switch (response.getResponseCode()) {
            case HttpURLConnection.HTTP_OK:
                Log.trace(LOG_TAG, SELF_TAG, "Registering new Campaign rules downloaded from %s.", url);
                rulesLoadResult = extractRules(url, response.getInputStream(), extractMetadataFromResponse(response));
                // save remotes url in Campaign Named Collection
                updateUrlInNamedCollection(url);
                break;
            case HttpURLConnection.HTTP_NOT_MODIFIED:
                Log.trace(LOG_TAG, SELF_TAG, "Campaign rules are not modified, retrieving rules from cache.");
                rulesLoadResult = new RulesLoadResult(StreamUtils.readAsString(cacheService.get(CACHE_BASE_DIR, RULES_JSON_FILE_NAME).getData()), RulesLoadResult.Reason.NOT_MODIFIED);
                break;
            case HttpURLConnection.HTTP_NOT_FOUND:
            default:
                Log.trace(LOG_TAG, SELF_TAG, "Received download response: %s", response.getResponseCode());
                return;
        }

        // register rules
        final List<LaunchRule> campaignRules = JSONRulesParser.parse(Objects.requireNonNull(rulesLoadResult.getData()), extensionApi);
        if (campaignRules != null) {
            Log.trace(LOG_TAG, SELF_TAG, "Registering %s Campaign rule(s).", campaignRules.size());
            campaignRulesEngine.replaceRules(campaignRules);
        }

        // cache any image assets present in each rule consequence
        cacheRemoteAssets(campaignRules);
    }

    /**
     * Parses the provided {@code List} of consequence Maps and downloads remote assets for them.
     * <p>
     * If a consequence in a {@code LaunchRule} does not represent a {@value CampaignConstants#MESSAGE_CONSEQUENCE_MESSAGE_TYPE}
     * consequence or if the consequence Id is not valid, no asset is downloaded for it.
     * <p>
     * This method also cleans up any cached files it has on disk for messages which are no longer loaded.
     *
     * @param campaignRules {@code List<LaunchRule>} of rules retrieved from the Campaign instance
     * @see Utils#clearCachedAssetsNotInList(File, List)
     */
    void cacheRemoteAssets(final List<LaunchRule> campaignRules) {
        if (campaignRules == null || campaignRules.isEmpty()) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "cacheRemoteAssets - Cannot load consequences, campaign rules list is null or empty.");
            return;
        }
        // generate a list of loaded message ids so we can clear cached files we no longer need
        final ArrayList<String> loadedMessageIds = new ArrayList<>();

        for (final LaunchRule rule : campaignRules) {
            for (final RuleConsequence consequence : rule.getConsequenceList()) {
                final String consequenceType = consequence.getType();

                final Map<String, Object> details = consequence.getDetail();
                final String templateType = DataReader.optString(details, MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "");
                if (StringUtils.isNullOrEmpty(consequenceType)
                        || !consequenceType.equals(MESSAGE_CONSEQUENCE_MESSAGE_TYPE)
                        || !templateType.equals("fullscreen")) {
                    continue;
                }

                final String consequenceId = consequence.getId();
                if (!StringUtils.isNullOrEmpty(consequenceId)) {
                    loadedMessageIds.add(consequenceId);
                    final List<String> assetUrls = createAssetUrlList(details);
                    if (assetUrls == null || assetUrls.isEmpty()) {
                        Log.debug(LOG_TAG, SELF_TAG, "cacheRemoteAssets - Can't download assets, no remote assets found in consequence for message id %s", consequence.getId());
                        break;
                    }
                    campaignMessageAssetsDownloader = new CampaignMessageAssetsDownloader(assetUrls, consequenceId);
                    campaignMessageAssetsDownloader.downloadAssetCollection();
                } else {
                    Log.debug(LOG_TAG, SELF_TAG, "cacheRemoteAssets - Can't download assets, Consequence id is null");
                }
            }
        }

        final File messageCacheDir = new File(ServiceProvider.getInstance().getDeviceInfoService().getApplicationCacheDir() + File.separator + CACHE_BASE_DIR + File.separator
                + MESSAGE_CACHE_DIR);
        Utils.clearCachedAssetsNotInList(messageCacheDir, loadedMessageIds);
    }

    /**
     * Responsible for reading and extracting {@code zipContentStream} and returning a {@code RulesDownloadResult}
     *  with rules. if successful. If the extraction is unsuccessful, returns a {@code RulesDownloadResult} with the
     *  error reason.
     *
     * @param key              the key that will be used for e
     * @param zipContentStream the zip stream that will need to be processed
     * @param metadata         any metadata associated with the zipContentStream
     */
    private RulesLoadResult extractRules(final String key,
                                         final InputStream zipContentStream,
                                         final Map<String, String> metadata) {

        if (zipContentStream == null) {
            Log.debug(LOG_TAG, CACHE_BASE_DIR, "Zip content stream is null");
            return new RulesLoadResult(null, RulesLoadResult.Reason.NO_DATA);
        }

        // Attempt to create a temporary directory for copying the zipContentStream
        final File tempDirectory = getTemporaryDirectory(key);
        if (!tempDirectory.exists() && !tempDirectory.mkdirs()) {
            Log.debug(LOG_TAG, SELF_TAG, "Cannot access application cache directory to create temp dir.");
            return new RulesLoadResult(null, RulesLoadResult.Reason.CANNOT_CREATE_TEMP_DIR);
        }

        // Copy the content of zipContentStream into the previously created temporary folder
        if(!FileUtils.readInputStreamIntoFile(getZipFileHandle(key), zipContentStream, false)) {
            Log.debug(LOG_TAG, SELF_TAG, "Couldn't extract zip contents to temp directory.");
            return new RulesLoadResult(null, RulesLoadResult.Reason.CANNOT_STORE_IN_TEMP_DIR);
        }

        // Extract the rules zip
        if(!FileUtils.extractFromZip(getZipFileHandle(key), tempDirectory.getPath())){
            Log.debug(LOG_TAG, SELF_TAG, "Failed to extract rules response zip into temp dir.");
            return new RulesLoadResult(null, RulesLoadResult.Reason.ZIP_EXTRACTION_FAILED);
        }

        // Cache the extracted contents
        final boolean cached = cacheExtractedFiles(tempDirectory, metadata);
        if (!cached) {
            Log.debug(LOG_TAG, CACHE_BASE_DIR, "Could not cache rules from source %s", key);
        }

        // Delete the temporary directory created for processing
        deleteTemporaryDirectory(key);

        final CacheResult cachedRulesJson = cacheService.get(CACHE_BASE_DIR, RULES_JSON_FILE_NAME);
        final String rulesJsonString = StreamUtils.readAsString(cachedRulesJson.getData());
        return new RulesLoadResult(rulesJsonString, RulesLoadResult.Reason.SUCCESS);
    }

    private List<String> createAssetUrlList(final Map<String, Object> detailMap) {
        final List<List<String>> assets = (List<List<String>>) detailMap.get(MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS);
        if (assets == null || assets.isEmpty()) {
            return null;
        }
        final List<String> assetsToDownload = new ArrayList<>();
        for(final List<String> asset : assets) {
            assetsToDownload.addAll(asset);
        }
        return assetsToDownload;
    }

    private File getTemporaryDirectory(final String tag) {
        final String hash = StringEncoder.sha2hash(tag);
        final String tempDir = ServiceProvider.getInstance().getDeviceInfoService().getApplicationCacheDir().getPath()
                + File.separator + TEMP_RULES_DIR
                + File.separator + hash;
        return new File(tempDir);
    }

    private File getZipFileHandle(final String tag) {
        return new File(getTemporaryDirectory(tag).getPath() + File.separator + ZIP_HANDLE);
    }

    private void deleteTemporaryDirectory(final String tag) {
        if (StringUtils.isNullOrEmpty(tag)) return;
        FileUtils.deleteFile(getTemporaryDirectory(tag), true);
    }

    private boolean cacheExtractedFiles(final File tempDirectory, final Map<String, String> metadata) {
        for (final File fileEntry : tempDirectory.listFiles()) {
            if (fileEntry.isDirectory()) {
                cacheExtractedFiles(fileEntry, metadata);
            } else {
                try {
                    final String fileName = fileEntry.getName();
                    Log.trace(LOG_TAG, SELF_TAG, "Caching file (%s)", fileName);
                    cacheService.set(CACHE_BASE_DIR, fileName, new CacheEntry(new FileInputStream(fileEntry), CacheExpiry.never(), metadata));
                } catch (final FileNotFoundException exception) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Extracts the response properties (like {@code HTTP_HEADER_ETAG} , {@code HTTP_HEADER_LAST_MODIFIED}
     * that are useful as cache metadata.
     *
     * @param response the {@code HttpConnecting} from where the response properties should be extracted from
     * @return a map of metadata keys and their values as obrained from the {@code response}
     */
    private Map<String, String> extractMetadataFromResponse(final HttpConnecting response) {
        final HashMap<String, String> metadata = new HashMap<>();

        final String lastModifiedProp = response.getResponsePropertyValue(HTTP_HEADER_LAST_MODIFIED);
        final Date lastModifiedDate = TimeUtils.parseRFC2822Date(
                lastModifiedProp, TimeZone.getTimeZone("GMT"), Locale.US);
        final String lastModifiedMetadata = lastModifiedDate == null
                ? String.valueOf(new Date(0L).getTime())
                : String.valueOf(lastModifiedDate.getTime());
        metadata.put(HTTP_HEADER_LAST_MODIFIED, lastModifiedMetadata);

        final String eTagProp = response.getResponsePropertyValue(HTTP_HEADER_ETAG);
        metadata.put(HTTP_HEADER_ETAG, eTagProp == null ? "" : eTagProp);

        return metadata;
    }

    /**
     * Creates http headers for conditional fetching, based on the metadata of the
     * {@code CacheResult} provided.
     *
     * @param cacheResult the cache result whose metadata should be used for finding headers
     * @return a map of headers (HTTP_HEADER_IF_MODIFIED_SINCE, HTTP_HEADER_IF_NONE_MATCH)
     * that can be used while fetching any modified content.
     */
    private Map<String, String> extractHeadersFromCache(final CacheResult cacheResult) {
        final Map<String, String> headers = new HashMap<>();
        if (cacheResult == null) {
            return headers;
        }

        final Map<String, String> metadata = cacheResult.getMetadata();
        final String eTag = metadata == null ? "" : metadata.get(HTTP_HEADER_ETAG);
        headers.put(HTTP_HEADER_IF_NONE_MATCH, eTag != null ? eTag : "");

        // Last modified in cache metadata is stored in epoch string. So Convert it to RFC-2822 date format.
        final String lastModified = metadata == null ? null : metadata.get(HTTP_HEADER_LAST_MODIFIED);
        long lastModifiedEpoch;
        try {
            lastModifiedEpoch = lastModified != null ? Long.parseLong(lastModified) : 0L;
        } catch (final NumberFormatException e) {
            lastModifiedEpoch = 0L;
        }

        final String ifModifiedSince = TimeUtils.getRFC2822Date(lastModifiedEpoch,
                TimeZone.getTimeZone("GMT"), Locale.US);
        headers.put(HTTP_HEADER_IF_MODIFIED_SINCE, ifModifiedSince);
        return headers;
    }

    /**
     * Updates {@value CampaignConstants#CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY} in {@code CampaignExtension}'s {@link NamedCollection}.
     * <p>
     * If provided {@code url} is null or empty, {@value CampaignConstants#CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY} key is removed from
     * the collecyion.
     *
     * @param url {@code String} containing a Campaign rules download remotes URL.
     */
    private void updateUrlInNamedCollection(final String url) {
        if (campaignNamedCollection == null) {
            Log.trace(LOG_TAG, SELF_TAG,
                    "updateUrlInNamedCollection - Campaign Named Collection is null, cannot store url.");
            return;
        }

        if (StringUtils.isNullOrEmpty(url)) {
            Log.trace(LOG_TAG, SELF_TAG,
                    "updateUrlInNamedCollection - Removing remotes URL key in Campaign Named Collection.");
            campaignNamedCollection.remove(CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY);
        } else {
            Log.trace(LOG_TAG, SELF_TAG,
                    "updateUrlInDataStore - Persisting remotes URL (%s) in in Campaign Named Collection.", url);
            campaignNamedCollection.setString(CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY, url);
        }
    }
}