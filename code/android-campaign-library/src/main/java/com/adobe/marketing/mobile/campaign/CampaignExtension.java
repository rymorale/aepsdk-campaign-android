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
import static com.adobe.marketing.mobile.campaign.CampaignConstants.CAMPAIGN_NAMED_COLLECTION_NAME;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.CAMPAIGN_TIMEOUT_DEFAULT;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_TYPE;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EXTENSION_NAME;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.FRIENDLY_NAME;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.RULES_CACHE_FOLDER;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.RULES_CACHE_KEY;

import android.util.Base64;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.download.RulesLoadResult;
import com.adobe.marketing.mobile.launch.rulesengine.download.RulesLoader;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import com.adobe.marketing.mobile.services.DataQueue;
import com.adobe.marketing.mobile.services.DataQueuing;
import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheExpiry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.StringUtils;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The CampaignExtension class is responsible for showing Messages and downloading/caching their remote assets when applicable.
 *
 * The {@link CampaignExtension} class listens for the following {@link Event}s:
 * <ul>
 *     <li>{@link EventType#CAMPAIGN} - {@link EventSource#REQUEST_CONTENT}</li>
 *     <li>{@code EventType.CAMPAIGN} - {@link EventSource#REQUEST_IDENTITY}</li>
 *     <li>{@code EventType.CAMPAIGN} - {@link EventSource#REQUEST_RESET}</li>
 *     <li>{@link EventType#CONFIGURATION} - {@link EventSource#RESPONSE_CONTENT}</li>
 *     <li>{@link EventType#HUB} - {@link EventSource#SHARED_STATE}</li>
 *     <li>{@link EventType#LIFECYCLE} - {@link EventSource#RESPONSE_CONTENT}</li>
 * </ul>
 *
 * The {@code CampaignExtension} class dispatches the following {@code Event}s:
 * <ul>
 *     <li>{@link EventType#CAMPAIGN} - {@code EventSource.RESPONSE_CONTENT}</li>
 * </ul>

 */
public class CampaignExtension extends Extension {
	private final String SELF_TAG = "CampaignExtension";
	private static final String DATA_FOR_MESSAGE_REQUEST_EVENT_NAME = "DataForMessageRequest";
	private static final String INTERNAL_GENERIC_DATA_EVENT_NAME = "InternalGenericDataEvent";
	private CampaignState campaignState;
	private String linkageFields;
	private List<Map<String, Object>> loadedConsequencesList;
	private boolean shouldLoadCache = true;
	private ExecutorService executorService;
	private final Object executorMutex = new Object();
	private final ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
	private final ExtensionApi extensionApi;
	private final RulesLoader rulesLoader;
	private final LaunchRulesEngine launchRulesEngine;

	/**
	 * Constructor.
	 *
	 * @param extensionApi {@link ExtensionApi} instance
	 */
	public CampaignExtension(final ExtensionApi extensionApi) {
		super(extensionApi);
		this.extensionApi = extensionApi;
		rulesLoader = new RulesLoader(CACHE_BASE_DIR);
		launchRulesEngine = new LaunchRulesEngine(extensionApi);

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
		Log.debug(LOG_TAG, SELF_TAG,"Registered Campaign extension - version %s", getVersion());
		registerEventListeners();
	}

	private void registerEventListeners() {
		final ListenerHubSharedState listenerHubSharedState = new ListenerHubSharedState(this);
		final ListenerCampaignRequestContent listenerCampaignRequestContent = new ListenerCampaignRequestContent(this);
		final ListenerLifecycleResponseContent listenerLifecycleResponseContent = new ListenerLifecycleResponseContent(this);
		final ListenerConfigurationResponseContent listenerConfigurationResponseContent = new ListenerConfigurationResponseContent(this);
		final ListenerCampaignRequestIdentity listenerCampaignRequestIdentity = new ListenerCampaignRequestIdentity(this);
		final ListenerCampaignRequestReset listenerCampaignRequestReset = new ListenerCampaignRequestReset(this);
		final ListenerGenericDataOS listenerGenericDataOS = new ListenerGenericDataOS(this);

		extensionApi.registerEventListener(EventType.HUB, EventSource.SHARED_STATE, listenerHubSharedState);
		extensionApi.registerEventListener(EventType.CAMPAIGN, EventSource.REQUEST_CONTENT, listenerCampaignRequestContent);
		extensionApi.registerEventListener(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT, listenerLifecycleResponseContent);
		extensionApi.registerEventListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT, listenerConfigurationResponseContent);
		extensionApi.registerEventListener(EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY, listenerCampaignRequestIdentity);
		extensionApi.registerEventListener(EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY, listenerCampaignRequestReset);
		extensionApi.registerEventListener(EventType.GENERIC_DATA, EventSource.OS, listenerGenericDataOS);
	}

	@Override
	public boolean readyForEvent(final Event event) {
		return getApi().getSharedState(CampaignConstants.EventDataKeys.Configuration.EXTENSION_NAME,
				event, false, SharedStateResolution.ANY).getStatus() == SharedStateStatus.SET;
	}

	// ========================================================================
	// package-private methods
	// ========================================================================
	/**
	 * This method queues the provided event in {@link #eventQueue}.
	 *
	 * <p>
	 * The queued events are then processed in an orderly fashion.
	 * No action is taken if the provided event's value is null.
	 *
	 * @param event The {@link Event} thats needs to be queued
	 */
	void queueEvent(final Event event) {
		if (event == null) {
			return;
		}

		eventQueue.add(event);
	}

	/**
	 * Processes the queued event one by one until queue is empty.
	 *
	 * <p>
	 * Suspends processing of the events in the queue if the configuration or identity shared state is not ready.
	 * Processed events are polled out of the {@link #eventQueue}.
	 */
	void processEvents() {
		while (!eventQueue.isEmpty()) {
			Event eventToProcess = eventQueue.peek();

			if (eventToProcess == null) {
				Log.debug(LOG_TAG, SELF_TAG, "Unable to process event, event received is null.");
				return;
			}

			setCampaignState(eventToProcess);
			// configuration and identity are mandatory when processing a event, so if shared state is not set for either stop processing
			if (!campaignState.isStateSet()) {
				Log.warning(LOG_TAG, SELF_TAG,
						"Could not process event, necessary campaign state is pending");
				return;
			}

			// if this is a lifecycle event, process the event
			if (eventToProcess.getType() == EventType.LIFECYCLE) {
				processLifecycleUpdate(eventToProcess, campaignState);
			} else if (eventToProcess.getType() == EventType.CONFIGURATION
					|| eventToProcess.getType() == EventType.CAMPAIGN) {
				if (eventToProcess.getSource() == EventSource.REQUEST_IDENTITY) {
					clearRulesCacheDirectory();
				}
				triggerRulesDownload();
			} else if (eventToProcess.getType() == EventType.GENERIC_DATA) {
				processMessageInformation(eventToProcess, campaignState);
			}

			// pop the current event
			eventQueue.poll();
		}
	}

	private void setCampaignState(final Event event) {
		ExtensionErrorCallback<ExtensionError> configurationErrorCallback = extensionError -> {
			if (extensionError != null) {
				Log.warning(LOG_TAG, SELF_TAG, "CampaignExtension : Could not process event, an error occurred while retrieving configuration shared state: %s",
						extensionError.getErrorName());
			}
		};

		ExtensionErrorCallback<ExtensionError> edgeIdentityErrorCallback = extensionError -> {
			if (extensionError != null) {
				Log.warning(LOG_TAG, SELF_TAG, "CampaignExtension : Could not process event, an error occurred while retrieving edge identity shared state: %s",
						extensionError.getErrorName());
			}
		};

		final Map<String, Object> configSharedState = getApi().getSharedEventState(CampaignConstants.EventDataKeys.Configuration.EXTENSION_NAME,
				event, configurationErrorCallback);
		final Map<String, Object> identitySharedState = getApi().getSharedEventState(CampaignConstants.EventDataKeys.Identity.EXTENSION_NAME,
				event, edgeIdentityErrorCallback);

		campaignState.setState(configSharedState, identitySharedState);
	}

	/**
	 * Processes Campaign request event to display messages.
	 * <p>
	 * If {@value CampaignConstants.EventDataKeys.RuleEngine#CONSEQUENCE_TRIGGERED} key is present in {@code EventData},
	 * it creates a {@code CampaignMessage} object from the corresponding {@code consequence} to show the message.
	 *
	 * @param event incoming {@link Event} instance to be processed
	 */
	void processMessageEvent(final Event event) {
		getExecutor().execute(() -> {
			Log.trace(LOG_TAG, "processMessageEvent -  processing event %s type: %s source: %s", event.getName(),
					event.getType(), event.getSource());
			final Map<String, Object> eventData = event.getEventData();

			if (eventData == null || eventData.isEmpty()) {
				Log.debug(LOG_TAG, SELF_TAG,
						"processMessageEvent -  Cannot process Campaign request event, eventData is null.");
				return;
			}

			// handle triggered consequence
			Map<String, Object> consequenceData = (Map<String, Object>) eventData.get(CampaignConstants.EventDataKeys.RuleEngine.CONSEQUENCE_TRIGGERED);

			try {
				final CampaignMessage triggeredMessage = CampaignMessage.createMessageObject(CampaignExtension.this,
						consequenceData);

				if (triggeredMessage != null && ServiceProvider.getInstance().getUIService() != null) {
					triggeredMessage.showMessage();
				}
			} catch (final CampaignMessageRequiredFieldMissingException ex) {
				Log.error(LOG_TAG, SELF_TAG,"processMessageEvent -  Error reading message definition: \n %s", ex);
			}
		});

	}

	/**
	 * Processes shared state update {@code Event} for the given {@code stateOwner}.
	 * <p>
	 * If the shared state owner is {@code com.adobe.module.configuration} or {@code com.adobe.module.identity}, then
	 * kicks off processing {@link #eventQueue}.
	 *
	 * @param stateOwner {@link String} containing name of the shared state that changed
	 */
	void processSharedStateUpdate(final String stateOwner) {
		if (CampaignConstants.EventDataKeys.Configuration.EXTENSION_NAME.equals(stateOwner) ||
				CampaignConstants.EventDataKeys.Identity.EXTENSION_NAME.equals(stateOwner)) {
			getExecutor().execute(() -> processEvents());
		}
	}

	/**
	 * Processes {@code Configuration} response to handle any update to {@code MobilePrivacyStatus}, handle any update
	 * to the number of days to delay or pause the sending of the Campaign registration request, and to queue then process
	 * events for Campaign rules download.
	 * <p>
	 * Initially on App launch when {@link #shouldLoadCache} is true, {@link #loadCachedMessages()} is invoked to
	 * register previously downloaded and cached Campaign rules before re-attempting rules download.
	 * <p>
	 * If {@link MobilePrivacyStatus} is changed to {@link MobilePrivacyStatus#OPT_OUT},
	 * invokes #processPrivacyOptOut() to handle privacy change. No event is queued for rules download in this case.
	 *
	 * @param event to be processed
	 * @see #processPrivacyOptOut()
	 * @see #processEvents()
	 */
	void processConfigurationResponse(final Event event) {
		if (event == null || event.getEventData() == null || event.getEventData().isEmpty()) {
			Log.debug(LOG_TAG, SELF_TAG, "processConfigurationResponse -  Configuration response event is null");
			return;
		}

		if (shouldLoadCache) {
			Log.debug(LOG_TAG, SELF_TAG, "processConfigurationResponse -  Attempting to load cached rules.");
			loadCachedMessages();
		}

		getExecutor().execute(() -> {
			MobilePrivacyStatus privacyStatus = campaignState.getMobilePrivacyStatus();
			// notify campaign data queue of any privacy status changes
			final DataQueuing dataQueuing = ServiceProvider.getInstance().getDataQueueService();
			final DataQueue campaignDataQueue = dataQueuing.getDataQueue(FRIENDLY_NAME);

			if (privacyStatus.equals(MobilePrivacyStatus.OPT_OUT)) {
				if (campaignDataQueue != null) {
					campaignDataQueue.clear();
				} else {
					Log.warning(LOG_TAG, SELF_TAG,
							"Campaign data queue is not initialized. Unable to update data queue status.");
				}
				processPrivacyOptOut();
				return;
			}

			queueEvent(event);
			processEvents();
		});
	}

	/**
	 * Loads cached rules from the previous rules download and registers those rules with the {@link LaunchRulesEngine}.
	 * <p>
	 * This method reads the persisted {@value CampaignConstants#CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY} from the Campaign
	 * named collection, gets cache path for the corresponding rules and loads then registers the rules with the {@link RulesLoader}.
	 * <p>
	 * If {@link #getNamedCollection()} returns null or if rules directory does not exist in cache as determined from
	 * {@link RulesLoader#getCacheName()}, then no rules are registered.
	 */
	void loadCachedMessages() {
		final NamedCollection campaignNamedCollection = getNamedCollection();

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
			Log.trace(LOG_TAG, "loadCachedMessages -  Loading cached rules from: '%s'",
					rulesDirectory);

			final RulesLoadResult result = rulesLoader.loadFromCache(cachedUrl);
			launchRulesEngine.replaceRules(JSONRulesParser.parse(result.toString(), extensionApi));
		}

		shouldLoadCache = false;
	}

	/**
	 * Processes {@code MobilePrivacyStatus} update to {@code MobilePrivacyStatus#OPT_OUT} on
	 * {@code Configuration} response. And as a result,
	 * <ul>
	 *     <li>Clears stored {@link #linkageFields}.</li>
	 *     <li>Clears any queued event in {@link #eventQueue}.</li>
	 *     <li>Unregisters previously registered rules.</li>
	 *     <li>Clears directory containing any previously cached rules.</li>
	 *     <li>Clears {@value CampaignConstants#CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY} in Campaign data store.</li>
	 * </ul>
	 */
	void processPrivacyOptOut() {
		Log.trace(LOG_TAG, SELF_TAG, "processPrivacyOptOut -  Clearing out cached data.");

		linkageFields = "";

		// clear any queued event
		clearWaitingEvents();

		// unregister rules
		launchRulesEngine.replaceRules(null);

		// clear cached rules
		clearRulesCacheDirectory();

		// clear all keys in the Campaign Named Collection
		clearCampaignNamedCollection();
	}

	/**
	 * Processes {@code Generic Data} event to send message tracking request to the configured {@code Campaign} server.
	 * <p>
	 * If the current {@code Configuration} properties do not allow sending track request, no request is sent.
	 *
	 * @param event {@link Event} object to be processed
	 * @param campaignState {@link CampaignState} instance containing the current Campaign configuration
	 * @see CampaignState#canSendTrackInfoWithCurrentState()
	 */
	void processMessageInformation(final Event event, final CampaignState campaignState) {
		if (!campaignState.canSendTrackInfoWithCurrentState()) {
			Log.debug(LOG_TAG, SELF_TAG,
					"processMessageInformation -  Campaign extension is not configured to send message track request.");
			return;
		}

		final Map<String, Object> eventData = event.getEventData();

		if (eventData == null || eventData.isEmpty()) {
			Log.debug(LOG_TAG, SELF_TAG,
					"processMessageInformation -  Cannot send message track request, eventData is null.");
			return;
		}

		final String broadlogId = (String) eventData.get(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID);
		final String deliveryId = (String) eventData.get(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID);
		final String action = (String) eventData.get(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION);

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
	 * @param action                  @{@link String} value represents user interaction action. Possible values are "7"(impression),"1"(open) and "2"(click).
	 * @param deliveryId              Hexadecimal value which is use to derive message id by converting it to Decimal.
	 */
	void dispatchMessageEvent(final String action, final String deliveryId) {
		//Dispatch event only in case of action value "1"(open) and "2"(click).
		String actionKey = null;

		if ("2".equals(action)) {
			actionKey = CampaignConstants.ContextDataKeys.MESSAGE_CLICKED;
		} else if ("1".equals(action)) {
			actionKey = CampaignConstants.ContextDataKeys.MESSAGE_VIEWED;
		}

		if (actionKey == null) {
			Log.trace(LOG_TAG, SELF_TAG,
					"dispatchMessageEvent -  Action received is other than viewed or clicked, so cannot dispatch Campaign response. ");
			return;
		}

		final int hashMapCapacity = 2;
		final Map<String, Object> contextData = new HashMap<>(hashMapCapacity);
		//Convert hex format deliveryId to base 10, which is message id.
		final int hexBase = 16;
		contextData.put(CampaignConstants.ContextDataKeys.MESSAGE_ID, String.valueOf(Integer.parseInt(deliveryId,
				hexBase)));
		contextData.put(actionKey, String.valueOf(1));

		dispatchMessageInteraction(contextData);
	}


	/**
	 * Processes {@code Lifecycle} event to send registration request to the configured {@code Campaign} server.
	 * <p>
	 * If current {@code Configuration} properties do not allow sending registration request, no request is sent.
	 *
	 * @param event {@link Event} object to be processed
	 * @param campaignState {@link CampaignState} instance containing the current Campaign configuration
	 * @see CampaignState#canRegisterWithCurrentState()
	 */
	void processLifecycleUpdate(final Event event, final CampaignState campaignState) {
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
	 * @see #downloadRules(String)
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

		downloadRules(rulesUrl);
	}

	/**
	 * Processes campaign request identity event then queues the event.
	 * <p>
	 * This event's data contains a map of linkage fields which will be stored in memory and subsequently used to download personalized rules.
	 *
	 * @param event {@link Event} object to be processed.
	 * @param linkageFieldsMap {@code Map<String, String>} of linkageFields that were already extracted and null-checked by the caller of this method.
	 */
	void handleSetLinkageFields(final Event event, final Map<String, String> linkageFieldsMap) {
		getExecutor().execute(() -> {
			final Gson gson = new Gson();
			final String linkageFieldsJsonString = gson.toJson(linkageFieldsMap);

			if (StringUtils.isNullOrEmpty(linkageFieldsJsonString)) {
				Log.debug(LOG_TAG, SELF_TAG,
						"handleSetLinkageFields -  Cannot set linkage fields, linkageFields JSON string is null or empty.");
				return;
			}
			linkageFields = Base64.encodeToString(linkageFieldsJsonString.getBytes(), Base64.NO_WRAP);

			if (linkageFields.isEmpty()) {
				Log.debug(LOG_TAG, SELF_TAG,
						"handleSetLinkageFields -  Cannot set linkage fields, base64 encoded linkage fields string is empty.");
				return;
			}

			queueEvent(event);
			processEvents();
		});
	}

	/**
	 * Processes campaign request reset event then queues the event.
	 * <p>
	 * This event has no data but is used as a signal that the SDK should clear any persisted linkage fields and personalized rules
	 * and subsequently download generic rules.
	 *
	 * @param event {@link Event} object to be processed
	 */
	void handleResetLinkageFields(final Event event) {
		getExecutor().execute(() -> {
			linkageFields = "";

			launchRulesEngine.replaceRules(null);

			clearRulesCacheDirectory();

			queueEvent(event);
			processEvents();
		});
	}

	/**
	 * Clears the rules cache directory.
	 * <p>
	 * Creates a {@link CacheService} instance and invokes method on it to perform delete operation on
	 * {@value CampaignConstants#RULES_CACHE_FOLDER} directory.
	 */
	void clearRulesCacheDirectory() {
		final CacheService cacheService = ServiceProvider.getInstance().getCacheService();

		if (cacheService != null) {
			cacheService.remove(CampaignConstants.RULES_CACHE_FOLDER, "");
		}
	}

	/**
	 * Parses the provided {@code List} of consequence Maps into {@code CampaignRuleConsequence} instances and downloads
	 * remote assets for them.
	 * <p>
	 * If a consequence in {@code consequences} does not represent a {@value CampaignConstants#MESSAGE_CONSEQUENCE_MESSAGE_TYPE}
	 * consequence or if the consequence Id is not valid, no asset is downloaded for it.
	 * <p>
	 * This method also cleans up any cached files it has on disk for messages which are no longer loaded.
	 *
	 * @param consequences {@code List<Map<String, Object>>} of all rules consequences loaded by the {@code Rules} module
	 * @see CampaignMessage#downloadRemoteAssets(CampaignExtension, Map)
	 * @see #clearCachedAssetsForMessagesNotInList(List)
	 */
	void loadConsequences(final List<Map<String, Object>> consequences) {
		// generate a list of loaded message ids so we can clear cached files we no longer need
		final ArrayList<String> loadedMessageIds = new ArrayList<String>();

		if (consequences == null || consequences.isEmpty()) {
			Log.debug(LOG_TAG, SELF_TAG,
					"loadConsequences -  Cannot load consequences, consequences list is null or empty.");
			return;
		}

		for (final Map<String, Object> consequence : consequences) {

			final String consequenceType = (String) consequence.get(MESSAGE_CONSEQUENCE_TYPE);

			if (StringUtils.isNullOrEmpty(consequenceType)
					|| !consequenceType.equals(CampaignConstants.MESSAGE_CONSEQUENCE_MESSAGE_TYPE)) {
				continue;
			}

			final String consequenceId = (String) consequence.get(MESSAGE_CONSEQUENCE_ID);

			if (!StringUtils.isNullOrEmpty(consequenceId)) {
				CampaignMessage.downloadRemoteAssets(this, consequence);
				loadedMessageIds.add(consequenceId);
			} else {
				Log.debug(LOG_TAG, SELF_TAG, "loadConsequences -  Can't download assets, Consequence id is null");
			}
		}

		clearCachedAssetsForMessagesNotInList(loadedMessageIds);
	}

	// TODO: migrate to using core 2.0 CacheService
	/**
	 * Deletes cached message assets for message Ids not listed in {@code activeMessageIds}.
	 * <p>
	 * If {@code SystemInfoService} is not available, no cached assets are cleared.
	 *
	 * @param activeMessageIds {@code List<String>} containing the Ids of active messages
	 */
	void clearCachedAssetsForMessagesNotInList(final List<String> activeMessageIds) {
//		try {
//			final DeviceInforming deviceInforming = ServiceProvider.getInstance().getDeviceInfoService();
//
//			if (deviceInforming == null) {
//				Log.error(LOG_TAG, SELF_TAG, "Cannot clear cached assets, device informing is not available.");
//				return;
//			}
//
//			final CacheManager cacheManager = new CacheManager(getPlatformServices().getSystemInfoService());
//			cacheManager.deleteFilesNotInList(activeMessageIds, CampaignConstants.MESSAGE_CACHE_DIR, true);
//		} catch (final MissingPlatformServicesException ex) {
//			Log.warning(LOG_TAG,
//						"Unable to clear cached message assets \n (%s).", ex);
//		}
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
	 * @param action {@code String} containing message action
	 */
	void dispatchMessageInfo(final String broadlogId, final String deliveryId, final String action) {
		// Dispatch a generic data OS event to the event hub
		final Map<String, Object> eventData = new HashMap<>();

		eventData.put(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID, broadlogId);
		eventData.put(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID, deliveryId);
		eventData.put(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION, action);

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
	 * If {@link DataStoring} are unavailable this method returns null.
	 *
	 * @return {@link NamedCollection} object for this {@link CampaignExtension}
	 */
	private NamedCollection getNamedCollection() {
		final DataStoring dataStoring = ServiceProvider.getInstance().getDataStoreService();

		if (dataStoring == null) {
			Log.debug(LOG_TAG, SELF_TAG,
					"getDataStore -  Cannot get Campaign Data store, Local storage service is not available.");
			return null;
		}

		return dataStoring.getNamedCollection(CAMPAIGN_NAMED_COLLECTION_NAME);
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

		if (StringUtils.isNullOrEmpty(url)) {
			Log.trace(LOG_TAG, SELF_TAG,
					"updateUrlInNamedCollection -  Removing remotes URL key in Campaign Named Collection.");
			campaignNamedCollection.remove(CAMPAIGN_NAMED_COLLECTION_REMOTES_URL_KEY);
		} else {
			Log.trace(LOG_TAG, SELF_TAG,
					"updateUrlInDataStore -  Persisting remotes URL (%s) in in Campaign Named Collection.", url);
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
	 * Clears the queue of events waiting to be processed.
	 */
	private void clearWaitingEvents() {
		eventQueue.clear();
	}

	/**
	 * Creates a Campaign track request URL with the provided {@code Campaign} properties.
	 *
	 * @param server {@link String} containing the configured Campaign server
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
	 * @param server {@link String} containing the configured Campaign server
	 * @param pkey {@code String} containing the configured Campaign pkey
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
	 * @param platform {@link String} specifying the push platform "gcm" or "apns"
	 * @param experienceCloudId {@code String} containing the Experience Cloud Id
	 * @param data {@code Map<String, String>} containing additional key-value pairs to be sent in the request
	 * @return {@code String} containing the Campaign registration payload
	 */
	private String buildRegistrationPayload(final String platform,
											final String experienceCloudId,
											final Map<String, String> data) {
		Map<String, String> profileData = new HashMap<String, String>(data);
		profileData.put(CampaignConstants.CAMPAIGN_PUSH_PLATFORM, platform);
		profileData.put(CampaignConstants.EXPERIENCE_CLOUD_ID, experienceCloudId);

		JSONObject bodyJSON = new JSONObject(profileData);

		if (bodyJSON == null) {
			return "";
		}

		return bodyJSON.toString();
	}

	// TODO: migrate to use core 2.0 HitQueueing
	/**
	 * Queues a {@code Campaign} registration request by creating a {@link com.adobe.marketing.mobile.services.DataEntity} object and inserting it to
	 * the Campaign {@link DataQueue} instance.
	 * <p>
	 * If the {@code payload} is null, empty, or if the {@link Networking} service is not available,
	 * then the {@code Campaign} registration request is dropped.
	 *
	 * @param url {@link String} containing the registration request URL
	 * @param payload {@link String} containing the registration request payload
	 * @param campaignState {@link CampaignState} instance containing the current {@code Campaign} configuration
	 * @param event {@link Event} which triggered the queuing of the {@code Campaign} registration request
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

		final DataQueuing dataQueuing = ServiceProvider.getInstance().getDataQueueService();

		if (dataQueuing != null) {
			final DataQueue dataQueue = dataQueuing.getDataQueue(FRIENDLY_NAME);
			if (dataQueue != null) {
				// create a data entity and add it to the data queue
//				CampaignHit campaignHit = new CampaignHit();
//				campaignHit.url = url;
//				campaignHit.body = payload;
//				campaignHit.timeout = campaignState.getCampaignTimeout();
//				Log.debug(LOG_TAG,
//						"processRequest - Campaign Request Queued with url (%s) and body (%s)", url, payload);
//				dataQueue.add(); // add
			}
		} else {
			Log.warning(LOG_TAG, SELF_TAG,
					"Campaign data queue is not initialized. Unable to queue Campaign Request.");
		}
	}

	/**
	 * Starts synchronous rules download from the provided {@code url}.
	 * <p>
	 * This method uses the {@link Networking} service to download the rules and the {@link CacheService}
	 * to cache the downloaded Campaign rules. Once the rules are downloaded, they are registered with the {@link LaunchRulesEngine}.
	 * <p>
	 * If the given {@code url} is null or empty no rules download happens.
	 *
	 * @param url {@link String} containing Campaign rules download URL
	 */
	private void downloadRules(final String url) {
		if (StringUtils.isNullOrEmpty(url)) {
			Log.warning(LOG_TAG, SELF_TAG,
					"Cannot download rules, provided url is null or empty. Cached rules will be used if present.");
			return;
		}

		final Map<String, String> requestProperties = new HashMap<>();

		if (linkageFields != null && !linkageFields.isEmpty()) {
			requestProperties.put(CampaignConstants.LINKAGE_FIELD_NETWORK_HEADER, linkageFields);
		}
		final Networking networking = ServiceProvider.getInstance().getNetworkService();
		if (networking != null) {
			final NetworkRequest networkRequest = new NetworkRequest(url, HttpMethod.GET, null,null, CAMPAIGN_TIMEOUT_DEFAULT, CAMPAIGN_TIMEOUT_DEFAULT);
			networking.connectAsync(networkRequest, httpConnecting -> {
				final CacheEntry downloadedRules = new CacheEntry(httpConnecting.getInputStream(), CacheExpiry.never(), null);
				final CacheService cacheService = ServiceProvider.getInstance().getCacheService();
				if (cacheService.set(RULES_CACHE_FOLDER, RULES_CACHE_KEY, downloadedRules)) {
					onRulesDownloaded(url);
				}
			});
		}
	}

	/**
	 * Invoked when rules have finished downloading.
	 * <p>
	 * This method takes the following actions once rules are downloaded:
	 * <ul>
	 *     <li>Unregister any previously registered rules.</li>
	 *     <li>Persist the provided remotes {@code url} in Campaign data store.</li>
	 *     <li>Register downloaded rules with the {@code EventHub}.</li>
	 *     <li>Download and cache remote assets for the {@link #loadedConsequencesList}.</li>
	 * </ul>
	 * If {@link #loadRulesFromDirectory(CacheResult)} cannot parse {@value CampaignConstants#RULES_JSON_FILE_NAME} in the
	 * provided {@code rulesDirectory} and returns empty {@code List<LaunchRule>}, then no rules are registered.
	 *
	 * @param url {@link String} containing Campaign rules download URL
	 * @see #updateUrlInNamedCollection(String)
	 * @see LaunchRulesEngine#replaceRules(List)
	 * @see #loadConsequences(List)
	 */
	private void onRulesDownloaded(final String url) {
		// save remotes url in Campaign Named Collection
		updateUrlInNamedCollection(url);

		loadedConsequencesList = new ArrayList<>();
		// register all new rules
		launchRulesEngine.replaceRules(loadRulesFromDirectory(ServiceProvider.getInstance().getCacheService().get(RULES_CACHE_FOLDER, RULES_CACHE_KEY)));

		loadConsequences(loadedConsequencesList);
		loadedConsequencesList = null; // We don't need it anymore.
	}

	/**
	 * Parses {@value CampaignConstants#RULES_JSON_FILE_NAME} in the provided cache {@code rulesDirectory}
	 * and returns the parsed {@code List<LaunchRule>}.
	 * <p>
	 * If provided {@code rulesDirectory} is null or empty then an empty {@code List} is returned.
	 *
	 * @param cachedRules {@link CacheResult} object containing cached rules
	 * @return a {@code List} of {@link LaunchRule} objects that were parsed from the {@value CampaignConstants#RULES_JSON_FILE_NAME} in
	 * the provided {@code rulesDirectory}
	 */
	private List<LaunchRule> loadRulesFromDirectory(final CacheResult cachedRules) {
		List<LaunchRule> rulesList = new ArrayList<>();

		// check if we downloaded a valid file
		if (cachedRules == null) {
			Log.debug(LOG_TAG, SELF_TAG, "loadRulesFromDirectory -  No valid rules directory found in cache.");
			// clear existing rules
			launchRulesEngine.replaceRules(null);
			return rulesList;
		}

		try {
			final String jsonString = Utils.inputStreamToString(cachedRules.getData());
			return JSONRulesParser.parse(jsonString, extensionApi);
		} catch (final IOException ioException) {
			Log.warning(LOG_TAG, SELF_TAG, "loadRulesFromDirectory -  Failed to convert cached rules to string: %s", ioException.getMessage());
			return null;
		}
	}

	/**
	 * Determines if a registration request should be sent to Campaign.
	 * @param campaignState {@link CampaignState} instance containing the current {@code Campaign} configuration
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

	// ========================================================================================
	// Getters for private members
	// ========================================================================================
	/**
	 * Getter for the {@link #executorService}. Access to which is mutex protected.
	 *
	 * @return A non-null {@link ExecutorService} instance
	 */
	ExecutorService getExecutor() {
		synchronized (executorMutex) {
			if (executorService == null) {
				executorService = Executors.newSingleThreadExecutor();
			}

			return executorService;
		}
	}
}