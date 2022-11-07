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

import static com.adobe.marketing.mobile.campaign.CampaignConstants.CAMPAIGN_NAMED_COLLECTION_NAME;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.ContextDataKeys.MESSAGE_CLICKED;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.ContextDataKeys.MESSAGE_ID;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.ContextDataKeys.MESSAGE_VIEWED;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EXTENSION_NAME;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.CONSEQUENCE_TRIGGERED;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ASSETS_PATH;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_TYPE;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.FRIENDLY_NAME;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_CACHE_DIR;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.RULES_CACHE_FOLDER;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.RULES_CACHE_KEY;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.Lifecycle.LAUNCH_EVENT;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.FRIENDLY_NAME;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;

import android.util.Base64;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.DataEntity;
import com.adobe.marketing.mobile.services.DataQueue;
import com.adobe.marketing.mobile.services.DataQueuing;
import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.PersistentHitQueue;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The CampaignExtension class is responsible for showing Messages and downloading/caching their remote assets when applicable.
 * <p>
 * The {@link CampaignExtension} class listens for the following {@link Event}s:
 * <ul>
 *     <li>{@link EventType#CAMPAIGN} - {@link EventSource#REQUEST_CONTENT}</li>
 *     <li>{@code EventType.CAMPAIGN} - {@link EventSource#REQUEST_IDENTITY}</li>
 *     <li>{@link EventType#CONFIGURATION} - {@link EventSource#RESPONSE_CONTENT}</li>
 *     <li>{@link EventType#GENERIC_DATA} - {@link EventSource#OS}</li>
 *     <li>{@link EventType#HUB} - {@link EventSource#SHARED_STATE}</li>
 *     <li>{@link EventType#LIFECYCLE} - {@link EventSource#RESPONSE_CONTENT}</li>
 * </ul>
 * <p>
 * The {@code CampaignExtension} class dispatches the following {@code Event}s:
 * <ul>
 *     <li>{@link EventType#CAMPAIGN} - {@code EventSource.RESPONSE_CONTENT}</li>
 * </ul>
 */
public class CampaignExtension extends Extension {
    private static final String DATA_FOR_MESSAGE_REQUEST_EVENT_NAME = "DataForMessageRequest";
    private static final String INTERNAL_GENERIC_DATA_EVENT_NAME = "InternalGenericDataEvent";
    private final String SELF_TAG = "CampaignExtension";
    private final ExtensionApi extensionApi;
    private final RulesLoader rulesLoader;
    private final PersistentHitQueue campaignPersistentHitQueue;
    private final LaunchRulesEngine campaignRulesEngine;
    private final CacheService cacheService;
    private final CampaignRulesDownloader campaignRulesDownloader;
    private final CampaignState campaignState;
    private String linkageFields;
    private List<Map<String, Object>> loadedConsequencesList;
    private boolean initialCampaignRuleFetchCompleted = false;

    /**
     * Constructor.
     *
     * @param extensionApi {@link ExtensionApi} instance
     */
    public CampaignExtension(final ExtensionApi extensionApi) {
        super(extensionApi);
        this.extensionApi = extensionApi;

        // initialize campaign rules engine
        campaignRulesEngine = new LaunchRulesEngine(extensionApi);

        // initialize campaign rules downloader
        cacheService = ServiceProvider.getInstance().getCacheService();
        campaignRulesDownloader = new CampaignRulesDownloader(extensionApi, campaignRulesEngine, getNamedCollection(), cacheService);

        // setup persistent hit queue
        final DataQueuing dataQueuing = ServiceProvider.getInstance().getDataQueueService();
        final DataQueue campaignDataQueue = dataQueuing.getDataQueue(FRIENDLY_NAME);
        campaignPersistentHitQueue = new PersistentHitQueue(campaignDataQueue, new CampaignHitProcessor());

        // initialize the campaign state
        campaignState = new CampaignState();
    }

    @Override
    protected String getName() {
        return EXTENSION_NAME;
    }

    @Override
    protected String getFriendlyName() {
        return FRIENDLY_NAME;
    }

    @Override
    protected String getVersion() {
        return BuildConfig.LIB_VERSION;
    }

    @Override
    protected void onRegistered() {
        Log.debug(LOG_TAG, SELF_TAG, "Registered Campaign extension - version %s", getVersion());
        // register listeners
        getApi().registerEventListener(
                EventType.CAMPAIGN,
                EventSource.REQUEST_IDENTITY,
                this::handleLinkageFieldsEvent
        );
        getApi().registerEventListener(
                EventType.CONFIGURATION,
                EventSource.RESPONSE_CONTENT,
                this::processConfigurationResponse
        );
        getApi().registerEventListener(
                EventType.GENERIC_DATA,
                EventSource.OS,
                this::processMessageInformation
        );
        getApi().registerEventListener(
                EventType.LIFECYCLE,
                EventSource.RESPONSE_CONTENT,
                this::processLifecycleUpdate
        );
        getApi().registerEventListener(
                EventType.WILDCARD,
                EventSource.WILDCARD,
                this::handleWildcardEvents
        );
    }

    @Override
    public boolean readyForEvent(final Event event) {
        final String stateName = DataReader.optString(event.getEventData(), CampaignConstants.EventDataKeys.STATE_OWNER, "");

        if (CampaignConstants.EventDataKeys.Configuration.EXTENSION_NAME.equals(stateName) ||
                CampaignConstants.EventDataKeys.Identity.EXTENSION_NAME.equals(stateName)) {
            setCampaignState(event);
        }

        return getApi().getSharedState(CampaignConstants.EventDataKeys.Configuration.EXTENSION_NAME,
                event, false, SharedStateResolution.ANY).getStatus() == SharedStateStatus.SET && getApi().getSharedState(CampaignConstants.EventDataKeys.Identity.EXTENSION_NAME,
                event, false, SharedStateResolution.ANY).getStatus() == SharedStateStatus.SET &&
                campaignState.isStateSet();
    }

    // ========================================================================
    // package-private methods
    // ========================================================================

    /**
     * Processes all events dispatched to the {@code EventHub} to determine if any rules are matched.
     * <p>
     * If a rule is triggered then an appropriate {@link CampaignMessage} object is instantiated and the message is shown.
     *
     * @param event incoming {@link Event} object to be processed
     */
    private void handleWildcardEvents(final Event event) {
        List<LaunchRule> triggeredRules = campaignRulesEngine.process(event);
        final List<RuleConsequence> consequences = new ArrayList<>();

        for (final LaunchRule rule : triggeredRules) {
            consequences.addAll(rule.getConsequenceList());
        }

        if (consequences.isEmpty()) {
            return;
        }

        try {
            final CampaignMessage triggeredMessage = CampaignMessage.createMessageObject(this, consequences.get(0));

            if (triggeredMessage != null && ServiceProvider.getInstance().getUIService() != null) {
                triggeredMessage.showMessage();
            }
        } catch (final CampaignMessageRequiredFieldMissingException ex) {
            Log.error(LOG_TAG, SELF_TAG, "processRuleEngineResponse -  Error reading message definition: \n %s", ex);
        }
    }

    /**
     * Stores {@code Identity} and {@code Configuration} information.
     *
     * @param event incoming {@link Event} object to be processed
     */
    private void setCampaignState(final Event event) {
        final SharedStateResult configSharedStateResult = getApi().getSharedState(CampaignConstants.EventDataKeys.Configuration.EXTENSION_NAME,
                event, false, SharedStateResolution.LAST_SET);
        final SharedStateResult identitySharedStateResult = getApi().getSharedState(CampaignConstants.EventDataKeys.Identity.EXTENSION_NAME,
                event, false, SharedStateResolution.LAST_SET);

        campaignState.setState(configSharedStateResult, identitySharedStateResult);
    }

    /**
     * Processes {@code Configuration} response to handle any update to {@code MobilePrivacyStatus}, handle any update
     * to the number of days to delay or pause the sending of the Campaign registration request, and to trigger Campaign rules download.
     * <p>
     * If {@link MobilePrivacyStatus} is changed to {@link MobilePrivacyStatus#OPT_OUT},
     * invokes #processPrivacyOptOut() to handle privacy change. No event is queued for rules download in this case.
     *
     * @param event to be processed
     * @see #processPrivacyOptOut()
     */
    void processConfigurationResponse(final Event event) {
        if (event == null) {
            Log.debug(LOG_TAG, SELF_TAG, "processConfigurationResponse - Unable to process event, event received is null.");
            return;
        }

        final Map<String, Object> eventData = event.getEventData();
        if (eventData == null || eventData.isEmpty()) {
            Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "processConfigurationResponse - Configuration response event is null");
            return;
        }

        MobilePrivacyStatus privacyStatus = campaignState.getMobilePrivacyStatus();
        // notify campaign persistent hit queue of any privacy status changes
        //campaignPersistentHitQueue.handlePrivacyChange(privacyStatus);
        if (privacyStatus.equals(MobilePrivacyStatus.OPT_OUT)) {
            processPrivacyOptOut();
            return;
        }

        if (!initialCampaignRuleFetchCompleted) {
            triggerRulesDownload();
        }
    }

    /**
     * Processes {@code MobilePrivacyStatus} update to {@code MobilePrivacyStatus#OPT_OUT} on
     * {@code Configuration} response. And as a result,
     * <ul>
     *     <li>Clears stored {@link #linkageFields}.</li>
     *     <li>Unregisters previously registered rules.</li>
     *     <li>Clears directory containing any previously cached rules.</li>
     *     <li>Clears {@value CampaignConstants#CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY} in Campaign data store.</li>
     * </ul>
     */
    void processPrivacyOptOut() {
        Log.trace(LOG_TAG, SELF_TAG, "processPrivacyOptOut -  Clearing out cached data.");

        linkageFields = "";

        // unregister campaign rules
        campaignRulesEngine.replaceRules(null);

        // clear cached rules
        clearRulesCacheDirectory();

        // clear all keys in the Campaign Named Collection
        clearCampaignNamedCollection();
    }

    /**
     * Processes {@code Generic Data} events to send message tracking request to the configured {@code Campaign} server.
     * <p>
     * If the current {@code Configuration} properties do not allow sending track request, no request is sent.
     *
     * @param event {@link Event} object to be processed
     * @see CampaignState#canSendTrackInfoWithCurrentState()
     */
    void processMessageInformation(final Event event) {
        if (!campaignState.canSendTrackInfoWithCurrentState()) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "processMessageInformation -  Campaign extension is not configured to send message track request.");
            return;
        }

        if (event == null) {
            Log.debug(LOG_TAG, SELF_TAG, "processMessageInformation - Unable to process event, event received is null.");
            return;
        }

        final Map<String, Object> eventData = event.getEventData();

        if (eventData == null || eventData.isEmpty()) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "processMessageInformation -  Cannot send message track request, eventData is null.");
            return;
        }

        final String broadlogId = DataReader.optString(eventData, TRACK_INFO_KEY_BROADLOG_ID, "");
        final String deliveryId = DataReader.optString(eventData, TRACK_INFO_KEY_DELIVERY_ID, "");
        final String action = DataReader.optString(eventData, TRACK_INFO_KEY_ACTION, "");

        if (StringUtils.isNullOrEmpty(broadlogId) || StringUtils.isNullOrEmpty(deliveryId)
                || StringUtils.isNullOrEmpty(action)) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "processMessageInformation -  Cannot send message track request, %s %s %s null or empty.",
                    StringUtils.isNullOrEmpty(broadlogId) ? "broadlogId" : "", StringUtils.isNullOrEmpty(deliveryId) ? "deliveryId" : "",
                    StringUtils.isNullOrEmpty(action) ? "action" : "");
            return;
        }

        dispatchMessageEvent(action, deliveryId);

        final String url = buildTrackingUrl(campaignState.getCampaignServer(), broadlogId, deliveryId, action,
                campaignState.getExperienceCloudId());

        processRequest(url, "", campaignState, event);
    }

    /**
     * Dispatches an event with message id and action click. This is to mark that a notification is interacted by user.
     *
     * @param action     @{@link String} value represents user interaction action. Possible values are "7"(impression),"1"(open) and "2"(click).
     * @param deliveryId Hexadecimal value which is use to derive message id by converting it to Decimal.
     */
    void dispatchMessageEvent(final String action, final String deliveryId) {
        // Dispatch event only in case of action value "1"(open) and "2"(click).
        String actionKey = null;

        if ("2".equals(action)) {
            actionKey = MESSAGE_CLICKED;
        } else if ("1".equals(action)) {
            actionKey = MESSAGE_VIEWED;
        }

        if (actionKey == null) {
            Log.trace(LOG_TAG, SELF_TAG,
                    "dispatchMessageEvent -  Action received is other than viewed or clicked, so cannot dispatch Campaign response. ");
            return;
        }

        final int hashMapCapacity = 2;
        final Map<String, Object> contextData = new HashMap<>(hashMapCapacity);
        // Convert hex format deliveryId to base 10, which is message id.
        final int hexBase = 16;
        contextData.put(MESSAGE_ID, String.valueOf(Integer.parseInt(deliveryId,
                hexBase)));
        contextData.put(actionKey, String.valueOf(1));

        dispatchMessageInteraction(contextData);
    }


    /**
     * Processes {@code Lifecycle} event to send registration request to the configured {@code Campaign} server.
     * <p>
     * If current {@code Configuration} properties do not allow sending registration request, no request is sent.
     *
     * @param event The received Lifecycle response {@link Event}
     * @see CampaignState#canRegisterWithCurrentState()
     */
    void processLifecycleUpdate(final Event event) {
        if (event == null) {
            Log.debug(LOG_TAG, SELF_TAG, "processLifecycleUpdate - Unable to process event, event received is null.");
            return;
        }

        final Map<String, Object> eventData = event.getEventData();
        if (eventData == null || eventData.isEmpty()) {
            Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "processLifecycleUpdate - Ignoring event with null or empty EventData.");
            return;
        }

        // trigger rules download on non initial lifecycle launch events
        final Map<String, Object> lifecycleContextData = DataReader.optTypedMap(Object.class, eventData, LIFECYCLE_CONTEXT_DATA, null);
        if (lifecycleContextData != null
                && !lifecycleContextData.isEmpty()
                && (DataReader.optString(lifecycleContextData, LAUNCH_EVENT, null)).equals(LAUNCH_EVENT)
                && initialCampaignRuleFetchCompleted) {
            triggerRulesDownload();
        }

        if (!campaignState.canRegisterWithCurrentState()) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "processLifecycleUpdate -  Campaign extension is not configured to send registration request.");
            return;
        }

        final String url = buildRegistrationUrl(campaignState.getCampaignServer(), campaignState.getCampaignPkey(),
                campaignState.getExperienceCloudId());
        final String payload = buildRegistrationPayload("gcm", campaignState.getExperienceCloudId(),
                new HashMap<>());

        processRequest(url, payload, campaignState, event);
    }

    /**
     * Triggers rules download from the configured {@code Campaign} server.
     * <p>
     * If current {@code Configuration} properties do not allow downloading {@code Campaign} rules, no request is sent.
     *
     * @see CampaignState#canDownloadRulesWithCurrentState()
     * @see CampaignRulesDownloader#loadRulesFromUrl(String, String)
     */
    void triggerRulesDownload() {
        if (!campaignState.canDownloadRulesWithCurrentState()) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "triggerRulesDownload -  Campaign extension is not configured to download rules.");
            return;
        }

        final String rulesUrl = String.format(CampaignConstants.CAMPAIGN_RULES_DOWNLOAD_URL, campaignState.getCampaignMcias(),
                campaignState.getCampaignServer(), campaignState.getPropertyId(),
                campaignState.getExperienceCloudId());

        campaignRulesDownloader.loadRulesFromUrl(rulesUrl, getLinkageFields());
        initialCampaignRuleFetchCompleted = true;
    }

    /**
     * Processes campaign request identity event then queues a set linkage fields or reset linkage fields event.
     * <p>
     * If the event's data contains a map of linkage fields then the map will be stored in memory and subsequently used to download personalized rules.
     * If no linkage fields map is present in the event then any stored linkage fields are cleared from memory.
     *
     * @param event {@link Event} object to be processed.
     */
    void handleLinkageFieldsEvent(final Event event) {
        if (event == null) {
            Log.debug(LOG_TAG, SELF_TAG, "handleLinkageFieldsEvent - Unable to process event, event received is null.");
            return;
        }

        final Map<String, Object> eventData = event.getEventData();
        if (eventData == null || eventData.isEmpty()) {
            Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "handleLinkageFieldsEvent - Ignoring event with null or empty EventData.");
            return;
        }

        final Map<String, String> linkageFields = DataReader.optStringMap(eventData, CampaignConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, null);

        if (linkageFields == null || linkageFields.isEmpty()) {
            handleResetLinkageFields();
            return;
        }

        final Gson gson = new Gson();
        final String linkageFieldsJsonString = gson.toJson(linkageFields);

        if (StringUtils.isNullOrEmpty(linkageFieldsJsonString)) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "handleLinkageFieldsEvent -  Cannot set linkage fields, linkageFields JSON string is null or empty.");
            return;
        }
        this.linkageFields = Base64.encodeToString(linkageFieldsJsonString.getBytes(), Base64.NO_WRAP);

        if (StringUtils.isNullOrEmpty(this.linkageFields)) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "handleLinkageFieldsEvent -  Cannot set linkage fields, base64 encoded linkage fields string is empty.");
            return;
        }

        clearRulesCacheDirectory();
        triggerRulesDownload();
    }


    /**
     * Processes campaign request reset event then queues the event.
     * <p>
     * This event has no data but is used as a signal that the SDK should clear any persisted linkage fields and personalized rules
     * and subsequently download generic rules.
     */
    void handleResetLinkageFields() {
        linkageFields = "";

        campaignRulesEngine.replaceRules(null);

        clearRulesCacheDirectory();

        triggerRulesDownload();
    }

    /**
     * Clears the rules cache directory.
     * <p>
     * Creates a {@link CacheService} instance and invokes method on it to perform delete operation on
     * {@value CampaignConstants#RULES_CACHE_FOLDER} directory.
     */
    void clearRulesCacheDirectory() {
        if (cacheService != null) {
            cacheService.remove(CampaignConstants.RULES_CACHE_FOLDER, "");
        }
    }

    /**
     * Invokes the extension api dispatch function {@code EventType#CAMPAIGN}, {@code EventSource#RESPONSE_CONTENT} event
     * with the provided {@code messageData} Map.
     *
     * @param messageData {@link Map<String, Object>} containing message interaction data
     */
    void dispatchMessageInteraction(final Map<String, Object> messageData) {
        // Dispatch a campaign event to the event hub
        if (messageData == null || messageData.isEmpty()) {
            Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
                    "dispatchMessageInteraction -  Cannot dispatch Campaign response event, message interaction data is null or empty.");
            return;
        }

        final Event messageEvent = new Event.Builder(DATA_FOR_MESSAGE_REQUEST_EVENT_NAME,
                EventType.CAMPAIGN, EventSource.RESPONSE_CONTENT).setEventData(messageData).build();

        extensionApi.dispatch(messageEvent);
    }

    /**
     * Invokes the extension api dispatch function to dispatch {@code EventType.GENERIC_DATA}, {@code EventSource.OS} event
     * with the provided message info.
     *
     * @param broadlogId {@link String} containing message broadlogId
     * @param deliveryId {@code String} containing message deliveryId
     * @param action     {@code String} containing message action
     */
    void dispatchMessageInfo(final String broadlogId, final String deliveryId, final String action) {
        // Dispatch a generic data OS event to the event hub
        final Map<String, Object> eventData = new HashMap<>();

        eventData.put(TRACK_INFO_KEY_BROADLOG_ID, broadlogId);
        eventData.put(TRACK_INFO_KEY_DELIVERY_ID, deliveryId);
        eventData.put(TRACK_INFO_KEY_ACTION, action);

        final Event messageEvent = new Event.Builder(INTERNAL_GENERIC_DATA_EVENT_NAME,
                EventType.GENERIC_DATA, EventSource.OS).setEventData(eventData).build();

        extensionApi.dispatch(messageEvent);
    }

    /**
     * Returns this {@code linkageFields}.
     *
     * @return {@link String} containing this {@link #linkageFields}
     */
    String getLinkageFields() {
        return linkageFields;
    }

    // ========================================================================
    // private methods
    // ========================================================================

    /**
     * Returns {@code CampaignExtension}'s {@link NamedCollection}.
     * <p>
     * If {@link DataStoring} is unavailable this method returns null.
     *
     * @return {@link NamedCollection} object for this {@link CampaignExtension}
     */
    private NamedCollection getNamedCollection() {
        final DataStoring dataStoreService = ServiceProvider.getInstance().getDataStoreService();

        if (dataStoreService == null) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "getDataStore -  Cannot get Campaign Data store, data store service is not available.");
            return null;
        }

        return dataStoreService.getNamedCollection(CAMPAIGN_NAMED_COLLECTION_NAME);
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
        final NamedCollection campaignNamedCollection = getNamedCollection();

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

    /**
     * Updates {@value CampaignConstants#CAMPAIGN_NAMED_COLLECTION_EXPERIENCE_CLOUD_ID_KEY} in {@code CampaignExtension}'s {@link NamedCollection}.
     * <p>
     * If provided {@code ecid} is null or empty, {@value CampaignConstants#CAMPAIGN_NAMED_COLLECTION_EXPERIENCE_CLOUD_ID_KEY} key is removed from
     * the named collection.
     *
     * @param ecid {@code String} containing the last known experience cloud id.
     */
    private void updateEcidInNamedCollection(final String ecid) {
        final NamedCollection campaignNamedCollection = getNamedCollection();

        if (campaignNamedCollection == null) {
            Log.trace(LOG_TAG, SELF_TAG,
                    "updateEcidInNamedCollection - Campaign Named Collection is null, cannot store ecid.");
            return;
        }

        if (StringUtils.isNullOrEmpty(ecid)) {
            Log.trace(LOG_TAG, SELF_TAG,
                    "updateEcidInNamedCollection -  Removing experience cloud id key in Campaign Named Collection.");
            campaignNamedCollection.remove(CampaignConstants.CAMPAIGN_NAMED_COLLECTION_EXPERIENCE_CLOUD_ID_KEY);
        } else {
            Log.trace(LOG_TAG, SELF_TAG,
                    "updateEcidInNamedCollection -  Persisting experience cloud id (%s) in Campaign Named Collection.", ecid);
            campaignNamedCollection.setString(CampaignConstants.CAMPAIGN_NAMED_COLLECTION_EXPERIENCE_CLOUD_ID_KEY, ecid);
        }
    }

    /**
     * Queues a {@code Campaign} registration request by creating a {@link com.adobe.marketing.mobile.services.DataEntity} object and inserting it to
     * the Campaign {@link DataQueue} instance.
     * <p>
     * If the {@code payload} is null, empty, or if the {@link Networking} service is not available,
     * then the {@code Campaign} registration request is dropped.
     *
     * @param url           {@link String} containing the registration request URL
     * @param payload       {@link String} containing the registration request payload
     * @param campaignState {@link CampaignState} instance containing the current {@code Campaign} configuration
     * @param event         {@link Event} which triggered the queuing of the {@code Campaign} registration request
     */
    private void processRequest(final String url, final String payload, final CampaignState campaignState,
                                final Event event) {
        final Networking networkingService = ServiceProvider.getInstance().getNetworkService();

        if (networkingService == null) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "processRequest -  Cannot send request, Networking service is not available.");
            return;
        }

        // check if this request is a registration request by checking for the presence of a payload
        // and if it is a registration request, determine if it should be sent.
        if (!StringUtils.isNullOrEmpty(payload)
                && !shouldSendRegistrationRequest(campaignState, event.getTimestamp())) {
            return;
        }

        // create a data entity and add it to the data queue
        final CampaignHit campaignHit = new CampaignHit(url, payload, campaignState.getCampaignTimeout());
        final DataEntity dataEntity = new DataEntity(campaignHit.toString());
        Log.debug(LOG_TAG, SELF_TAG, "processRequest - Campaign Request Queued with url (%s) and body (%s)", url, payload);
        //campaignPersistentHitQueue.queue(dataEntity);
    }

    /**
     * Clears all the keys stored in the {@code CampaignExtension}'s {@link NamedCollection}.
     */
    private void clearCampaignNamedCollection() {
        final NamedCollection campaignNamedCollection = getNamedCollection();

        if (campaignNamedCollection == null) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "clearCampaignNamedCollection -  Campaign Named Collection is not available to be cleared.");
            return;
        }

        campaignNamedCollection.removeAll();
    }

    /**
     * Creates a Campaign track request URL with the provided {@code Campaign} properties.
     *
     * @param server            {@link String} containing the configured Campaign server
     * @param experienceCloudId {@code String} containing the Experience Cloud Id
     * @return {@code String} containing the Campaign message tracking URL
     */
    private String buildTrackingUrl(final String server,
                                    final String broadlogId,
                                    final String deliveryId,
                                    final String action,
                                    final String experienceCloudId) {
        return String.format(CampaignConstants.CAMPAIGN_TRACKING_URL, server, broadlogId, deliveryId, action,
                experienceCloudId);
    }

    /**
     * Creates a Campaign registration request URL with the provided {@code Campaign} properties.
     *
     * @param server            {@link String} containing the configured Campaign server
     * @param pkey              {@code String} containing the configured Campaign pkey
     * @param experienceCloudId {@code String} containing the Experience Cloud Id
     * @return {@code String} containing the Campaign registration URL
     */
    private String buildRegistrationUrl(final String server,
                                        final String pkey,
                                        final String experienceCloudId) {
        return String.format(CampaignConstants.CAMPAIGN_REGISTRATION_URL, server, pkey, experienceCloudId);
    }


    /**
     * Creates payload {@code String} for Campaign registration request.
     *
     * @param platform          {@link String} specifying the push platform "gcm" or "apns"
     * @param experienceCloudId {@code String} containing the Experience Cloud Id
     * @param data              {@code Map<String, String>} containing additional key-value pairs to be sent in the request
     * @return {@code String} containing the Campaign registration payload
     */
    private String buildRegistrationPayload(final String platform,
                                            final String experienceCloudId,
                                            final Map<String, String> data) {
        Map<String, String> profileData = new HashMap<>(data);
        profileData.put(CampaignConstants.CAMPAIGN_PUSH_PLATFORM, platform);
        profileData.put(CampaignConstants.EXPERIENCE_CLOUD_ID, experienceCloudId);

        JSONObject bodyJSON = new JSONObject(profileData);

        if (bodyJSON == null) {
            return "";
        }

        return bodyJSON.toString();
    }

    /**
     * Determines if a registration request should be sent to Campaign.
     *
     * @param campaignState  {@link CampaignState} instance containing the current {@code Campaign} configuration
     * @param eventTimestamp {@link Long} containing the registration event's timestamp.
     * @return the {@code boolean} indicating if a registration request should be sent.
     */
    private boolean shouldSendRegistrationRequest(final CampaignState campaignState, final long eventTimestamp) {
        // quick out if registration requests should be ignored
        final boolean shouldPauseRegistration = campaignState.getCampaignRegistrationPaused();

        if (shouldPauseRegistration) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "shouldSendRegistrationRequest -  Registration requests are paused.");
            return false;
        }

        final String retrievedEcid = getNamedCollection().getString(
                CampaignConstants.CAMPAIGN_NAMED_COLLECTION_EXPERIENCE_CLOUD_ID_KEY, "");
        final String currentEcid = campaignState.getExperienceCloudId();
        final long retrievedTimestamp = getNamedCollection().getLong(
                CampaignConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY, CampaignConstants.DEFAULT_TIMESTAMP_VALUE);
        final int registrationDelay = campaignState.getCampaignRegistrationDelay();
        final long registrationDelayInMilliseconds = TimeUnit.DAYS.toMillis(registrationDelay);

        if (!retrievedEcid.equals(currentEcid)) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "shouldSendRegistrationRequest - The current ecid (%s) is new, sending the registration request.",
                    currentEcid);
            updateEcidInNamedCollection(currentEcid);
            return true;

        }

        if (eventTimestamp - retrievedTimestamp >= registrationDelayInMilliseconds) {
            Log.debug(LOG_TAG, SELF_TAG,
                    "shouldSendRegistrationRequest -  Registration delay of (%d) days has elapsed. Sending the Campaign registration request.",
                    registrationDelay);
            return true;
        }

        Log.debug(LOG_TAG, SELF_TAG,
                "shouldSendRegistrationRequest - The registration request will not be sent because the registration delay of (%d) days has not elapsed.",
                registrationDelay);
        return false;
    }
}