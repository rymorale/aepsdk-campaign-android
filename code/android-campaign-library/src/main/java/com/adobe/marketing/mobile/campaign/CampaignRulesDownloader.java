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

import static com.adobe.marketing.mobile.campaign.CampaignConstants.CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_CACHE_DIR;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_CONSEQUENCE_MESSAGE_TYPE;

import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.launch.rulesengine.download.RulesLoadResult;
import com.adobe.marketing.mobile.launch.rulesengine.download.RulesLoader;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CampaignRulesDownloader {
    private final static String SELF_TAG = "CampaignRulesDownloader";
    private final ExtensionApi extensionApi;
    private final LaunchRulesEngine campaignRulesEngine;
    private final RulesLoader rulesLoader;
    private final NamedCollection campaignNamedCollection;
    private boolean shouldLoadCache = false;

    CampaignRulesDownloader(final ExtensionApi extensionApi, final LaunchRulesEngine campaignRulesEngine, final RulesLoader rulesLoader, final NamedCollection campaignNamedCollection) {
        this.extensionApi = extensionApi;
        this.campaignRulesEngine = campaignRulesEngine;
        this.rulesLoader = rulesLoader;
        this.campaignNamedCollection = campaignNamedCollection;
    }

    boolean shouldLoadCache() {
        return shouldLoadCache;
    }

    /**
     * Loads cached rules from the previous rules download and registers those rules with the Campaign extension's {@link LaunchRulesEngine} instance.
     * <p>
     * This method reads the persisted {@value CampaignConstants#CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY} from the Campaign
     * named collection, gets cache path for the corresponding rules and loads then registers the rules with the {@link RulesLoader}.
     * <p>
     * If the Campaign {@link NamedCollection} is null or if rules directory does not exist in cache as determined from
     * {@link RulesLoader#getCacheName()}, then no rules are registered.
     */
    void loadCachedMessages() {
        if (campaignNamedCollection == null) {
            Log.error(LOG_TAG, SELF_TAG,
                    "Cannot load cached rules, Campaign Data store is not available.");
            return;
        }

        final String cachedUrl = campaignNamedCollection.getString(CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY, "");

        if (StringUtils.isNullOrEmpty(cachedUrl)) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "loadCachedMessages -  Cannot load cached rules, Campaign Data store does not have rules remote url.");
            return;
        }

        final String rulesDirectory = rulesLoader.getCacheName();

        if (!StringUtils.isNullOrEmpty(rulesDirectory)) {
            final RulesLoadResult result = rulesLoader.loadFromCache(cachedUrl);
            if (!result.getReason().equals(RulesLoadResult.Reason.NO_DATA)) {
                final List<LaunchRule> cachedRules = JSONRulesParser.parse(result.toString(), extensionApi);
                Log.trace(LOG_TAG, SELF_TAG, "loadCachedMessages -  Loading %s cached rule(s)", cachedRules.size());
                campaignRulesEngine.replaceRules(cachedRules);
            }
        }

        shouldLoadCache = false;
    }

    /**
     * Starts synchronous rules download from the provided {@code url}.
     * <p>
     * This method uses the {@link Networking} service to download the rules and the {@link CacheService}
     * to cache the downloaded Campaign rules. Once the rules are downloaded, they are registered with the Campaign extension's {@link LaunchRulesEngine} instance.
     * <p>
     * If the given {@code url} is null or empty no rules download happens.
     *
     * @param url           {@link String} containing Campaign rules download URL
     * @param linkageFields {@link String} containing optional linkage fields to include when downloading Campaign rules
     */
    void downloadRules(final String url, final String linkageFields) {
        if (StringUtils.isNullOrEmpty(url)) {
            Log.warning(LOG_TAG, SELF_TAG,
                    "Cannot download rules, provided url is null or empty. Cached rules will be used if present.");
            return;
        }

        final Map<String, String> requestProperties = new HashMap<>();

        if (!StringUtils.isNullOrEmpty(linkageFields)) {
            requestProperties.put(CampaignConstants.LINKAGE_FIELD_NETWORK_HEADER, linkageFields);
        }

        rulesLoader.loadFromUrl(url, requestProperties, rulesLoadResult -> {
            if (rulesLoadResult.getReason().equals(RulesLoadResult.Reason.NOT_MODIFIED)) {
                Log.trace(LOG_TAG, SELF_TAG, "Campaign rules are unmodified, will not register Campaign rules.");
                return;
            }
            onRulesDownloaded(url, rulesLoadResult);
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
     * @param rulesLoadResult {@link RulesLoadResult} containing the downloaded Campaign rules
     * @see #updateUrlInNamedCollection(String)
     * @see LaunchRulesEngine#replaceRules(List)
     * @see #cacheRemoteAssets(List)
     */
    private void onRulesDownloaded(final String url, final RulesLoadResult rulesLoadResult) {
        // save remotes url in Campaign Named Collection
        updateUrlInNamedCollection(url);

        // register all new rules
        final List<LaunchRule> campaignRules = JSONRulesParser.parse(rulesLoadResult.getData(), extensionApi);
        Log.trace(LOG_TAG, SELF_TAG, "Registering %s Campaign rule(s).", campaignRules.size());
        campaignRulesEngine.replaceRules(campaignRules);

        // cache any image assets present in each rule consequence
        cacheRemoteAssets(campaignRules);
    }

    /**
     * Parses the provided {@code List} of consequence Maps and downloads remote assets for them.
     * <p>
     * If a consequence in {@code consequences} does not represent a {@value CampaignConstants#MESSAGE_CONSEQUENCE_MESSAGE_TYPE}
     * consequence or if the consequence Id is not valid, no asset is downloaded for it.
     * <p>
     * This method also cleans up any cached files it has on disk for messages which are no longer loaded.
     *
     * @param campaignRules {@code List<LaunchRule>} of rules retrieved from the Campaign instance
     * @see CampaignMessage#downloadRemoteAssets(RuleConsequence)
     * @see #clearCachedAssetsForMessagesNotInList(List)
     */
    void cacheRemoteAssets(final List<LaunchRule> campaignRules) {
        if (campaignRules == null || campaignRules.isEmpty()) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "cacheRemoteAssets - Cannot load consequences, campaign rules list is null or empty.");
            return;
        }
        // generate a list of loaded message ids so we can clear cached files we no longer need
        final ArrayList<String> loadedMessageIds = new ArrayList<>();
        final ArrayList<RuleConsequence> ruleConsequences = new ArrayList<>();

        for (final LaunchRule rule : campaignRules) {
            ruleConsequences.addAll(rule.getConsequenceList());
        }

        if (ruleConsequences == null || ruleConsequences.isEmpty()) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "cacheRemoteAssets - Cannot load consequences, rule consequences list is null or empty.");
            return;
        }

        for (final RuleConsequence consequence : ruleConsequences) {
            final String consequenceType = consequence.getType();

            if (StringUtils.isNullOrEmpty(consequenceType)
                    || !consequenceType.equals(MESSAGE_CONSEQUENCE_MESSAGE_TYPE)) {
                continue;
            }

            final String consequenceId = consequence.getId();

            if (!StringUtils.isNullOrEmpty(consequenceId)) {
                CampaignMessage.downloadRemoteAssets(consequence);
                loadedMessageIds.add(consequenceId);
            } else {
                Log.debug(LOG_TAG, SELF_TAG, "loadConsequences -  Can't download assets, Consequence id is null");
            }
        }

        clearCachedAssetsForMessagesNotInList(loadedMessageIds);
    }

    /**
     * Deletes cached message assets for message Ids not listed in {@code activeMessageIds}.
     * <p>
     * If {@code CacheService} is not available, no cached assets are cleared.
     *
     * @param activeMessageIds {@code List<String>} containing the Ids of active messages
     */
    void clearCachedAssetsForMessagesNotInList(final List<String> activeMessageIds) {
        final CacheService cacheService = ServiceProvider.getInstance().getCacheService();
        if (cacheService == null) {
            Log.error(LOG_TAG, SELF_TAG, "Cannot clear cached assets, cache service is not available.");
            return;
        }

        for (final String messageId : activeMessageIds) {
            cacheService.remove(MESSAGE_CACHE_DIR, messageId);
        }
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
