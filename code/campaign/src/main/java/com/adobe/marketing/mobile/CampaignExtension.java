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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * CampaignExtension class is responsible for showing Messages and downloading/caching their remote assets when applicable.
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
 *
 * The {@code CampaignExtension} class has dependency on the following {@link PlatformServices}:
 * <ul>
 *     <li>{@link CompressedFileService}</li>
 *     <li>{@link DatabaseService}</li>
 *     <li>{@link JsonUtilityService}</li>
 *     <li>{@link LocalStorageService}</li>
 *     <li>{@link NetworkService}</li>
 *     <li>{@link SystemInfoService}</li>
 *     <li>{@link UIService}</li>
 * </ul>
 */
class CampaignExtension extends InternalModule {

	protected ConcurrentLinkedQueue<Event> waitingEvents = new ConcurrentLinkedQueue<Event>();
	protected CampaignDispatcherCampaignResponseContent campaignEventDispatcher;
	protected CampaignDispatcherGenericDataOS genericDataOSEventDispatcher;
	private String linkageFields;
	private List<Map<String, Variant>> loadedConsequencesList;
	private CampaignHitsDatabase hitsDatabase = null;
	private boolean shouldLoadCache = true;

	/**
	 * Constructor.
	 *
	 * Registers {@link Event} listeners and creates dispatchers.
	 *
	 * @param hub {@link EventHub} instance that owns this module
	 * @param services {@link PlatformServices} instance
	 */
	public CampaignExtension(final EventHub hub, final PlatformServices services) {
		super(CampaignConstants.EventDataKeys.Campaign.EXTENSION_NAME, hub, services);

		// Register Listeners
		this.registerListener(EventType.CAMPAIGN, EventSource.REQUEST_CONTENT, CampaignListenerCampaignRequestContent.class);
		this.registerListener(EventType.LIFECYCLE, EventSource.RESPONSE_CONTENT,
							  CampaignListenerLifecycleResponseContent.class);
		this.registerListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT,
							  CampaignListenerConfigurationResponseContent.class);
		this.registerListener(EventType.HUB, EventSource.SHARED_STATE, CampaignListenerHubSharedState.class);
		this.registerListener(EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY, CampaignListenerCampaignRequestIdentity.class);
		this.registerListener(EventType.CAMPAIGN, EventSource.REQUEST_RESET, CampaignListenerCampaignRequestReset.class);
		this.registerListener(EventType.GENERIC_DATA, EventSource.OS, CampaignListenerGenericDataOS.class);

		// Register Dispatchers
		campaignEventDispatcher = this.createDispatcher(CampaignDispatcherCampaignResponseContent.class);
		genericDataOSEventDispatcher = this.createDispatcher(CampaignDispatcherGenericDataOS.class);
	}

	/**
	 * Constructor for testing purposes.
	 *
	 * @param hub {@link EventHub} instance that owns this module
	 * @param services {@link PlatformServices} instance
	 * @param database {@link CampaignHitsDatabase} instance
	 */
	CampaignExtension(final EventHub hub, final PlatformServices services, final CampaignHitsDatabase database) {
		this(hub, services);
		this.hitsDatabase = database;
	}

	// ========================================================================
	// package-private methods
	// ========================================================================

	/**
	 * Processes Campaign request event to display messages.
	 * <p>
	 * If {@value CampaignConstants.EventDataKeys.RuleEngine#CONSEQUENCE_TRIGGERED} key is present in {@code EventData},
	 * it creates a {@code CampaignMessage} object from the corresponding {@code consequence} to show the message.
	 * <p>
	 * Invokes {@link #shouldShowMessage()} to determine if the current message can be displayed.
	 * <p>
	 * No message is displayed if provided {@code consequence} is null or invalid, or if {@link PlatformServices}
	 * are not available.
	 *
	 * @param event incoming {@link Event} instance to be processed
	 */
	void processMessageEvent(final Event event) {
		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				Log.trace(CampaignConstants.LOG_TAG, "processMessageEvent -  processing event %s type: %s source: %s", event.getName(),
						  event.getType(), event.getSource());
				final EventData eventData = event.getData();

				if (eventData == null) {
					Log.debug(CampaignConstants.LOG_TAG,
							  "processMessageEvent -  Cannot process Campaign request event, eventData is null.");
					return;
				}

				// handle triggered consequence
				CampaignRuleConsequence triggeredConsequence = eventData.optTypedObject(
							CampaignConstants.EventDataKeys.RuleEngine.CONSEQUENCE_TRIGGERED,
							null, new CampaignRuleConsequenceSerializer());

				if (triggeredConsequence == null) {
					Log.debug(CampaignConstants.LOG_TAG,
							  "processMessageEvent -  Cannot process Campaign request event, failed to parse triggered consequence.");
					return;
				}

				try {
					final CampaignMessage triggeredMessage = CampaignMessage.createMessageObject(CampaignExtension.this,
							getPlatformServices(),
							triggeredConsequence);

					if (triggeredMessage != null && shouldShowMessage()) {
						triggeredMessage.showMessage();
					}
				} catch (final CampaignMessageRequiredFieldMissingException ex) {
					Log.error(CampaignConstants.LOG_TAG, "processMessageEvent -  Error reading message definition: \n %s", ex);
				} catch (final MissingPlatformServicesException ex) {
					Log.error(CampaignConstants.LOG_TAG, "processMessageEvent -  Error reading message definition: \n %s", ex);
				}
			}
		});

	}

	/**
	 * Processes shared state update {@code Event} for the given {@code stateOwner}.
	 * <p>
	 * If the shared state owner is {@code com.adobe.module.configuration} or {@code com.adobe.module.identity}, then
	 * kicks off processing {@link #waitingEvents}.
	 *
	 * @param stateOwner {@link String} containing name of the shared state that changed
	 */
	void processSharedStateUpdate(final String stateOwner) {
		if (CampaignConstants.EventDataKeys.Configuration.EXTENSION_NAME.equals(stateOwner) ||
				CampaignConstants.EventDataKeys.Identity.EXTENSION_NAME.equals(stateOwner)) {
			getExecutor().execute(new Runnable() {
				@Override
				public void run() {
					processQueuedEvents();
				}
			});
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
	 * If {@link MobilePrivacyStatus} is changed to {@link MobilePrivacyStatus#OPT_OUT}, invokes #processPrivacyOptOut()
	 * to handle privacy change. No event is queued for rules download in this case.
	 *
	 * @param event to be processed
	 * @see #processPrivacyOptOut()
	 * @see #queueAndProcessEvent(Event)
	 */
	void processConfigurationResponse(final Event event) {
		if (event == null) {
			Log.debug(CampaignConstants.LOG_TAG, "processConfigurationResponse -  Configuration response event is null");
			return;
		}

		final EventData configData = event.getData();
		final EventData identityData = getSharedEventState(CampaignConstants.EventDataKeys.Identity.EXTENSION_NAME, event);

		final CampaignState campaignState = new CampaignState();
		campaignState.setState(configData, identityData);

		if (shouldLoadCache) {
			Log.debug(CampaignConstants.LOG_TAG, "processConfigurationResponse -  Attempting to load cached rules.");
			loadCachedMessages();
		}

		getExecutor().execute(new Runnable() {
			@Override
			public void run() {

				MobilePrivacyStatus privacyStatus = campaignState.getMobilePrivacyStatus();

				// notify campaign hits database of any privacy status changes
				final CampaignHitsDatabase database = getCampaignHitsDatabase();

				if (database != null) {
					database.updatePrivacyStatus(privacyStatus);
				} else {
					Log.warning(CampaignConstants.LOG_TAG,
								"Campaign database is not initialized. Unable to update privacy status.");
				}

				if (privacyStatus.equals(MobilePrivacyStatus.OPT_OUT)) {
					processPrivacyOptOut();
					return;
				}

				queueAndProcessEvent(event);
			}
		});
	}

	/**
	 * Loads cached rules from the previous rules download and registers those rules with the {@code EventHub}.
	 * <p>
	 * This method reads the persisted {@value CampaignConstants#CAMPAIGN_DATA_STORE_REMOTES_URL_KEY} from the Campaign
	 * data store, gets cache path for the corresponding rules and loads then registers the rules with the {@link EventHub}.
	 * <p>
	 * If {@link #getDataStore()} returns null or if rules directory does not exist in cache as determined from
	 * {@link CampaignRulesRemoteDownloader#getCachePath()}, then no rules are registered.
	 */
	void loadCachedMessages() {
		final LocalStorageService.DataStore dataStore = getDataStore();

		if (dataStore == null) {
			Log.error(CampaignConstants.LOG_TAG,
					  "Cannot load cached rules, Campaign Data store is not available.");
			return;
		}

		final String cachedUrl = dataStore.getString(CampaignConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY, "");

		if (StringUtils.isNullOrEmpty(cachedUrl)) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "loadCachedMessages -  Cannot load cached rules, Campaign Data store does not have rules remote url.");
			return;
		}

		final CampaignRulesRemoteDownloader remoteDownloader = getCampaignRulesRemoteDownloader(cachedUrl,  null);

		if (remoteDownloader == null) {
			Log.error(CampaignConstants.LOG_TAG,
					  "Cannot load cached rules, getCampaignRulesRemoteDownloader returned null.");
			return;
		}

		final File rulesDirectory = remoteDownloader.getCachePath();

		if (rulesDirectory != null) {
			Log.trace(CampaignConstants.LOG_TAG, "loadCachedMessages -  Loading cached rules from: '%s'",
					  rulesDirectory.toString());

			replaceRules(loadRulesFromDirectory(rulesDirectory));
		}

		shouldLoadCache = false;
	}

	/**
	 * Processes {@code MobilePrivacyStatus} update to {@code MobilePrivacyStatus#OPT_OUT} on
	 * {@code Configuration} response. And as a result,
	 * <ul>
	 *     <li>Clears stored {@link #linkageFields}.</li>
	 *     <li>Clears any queued event in {@link #waitingEvents} queue.</li>
	 *     <li>Unregisters previously registered rules with {@link EventHub}.</li>
	 *     <li>Clears directory containing any previously cached rules.</li>
	 *     <li>Clears {@value CampaignConstants#CAMPAIGN_DATA_STORE_REMOTES_URL_KEY} in Campaign data store.</li>
	 * </ul>
	 */
	void processPrivacyOptOut() {
		Log.trace(CampaignConstants.LOG_TAG, "processPrivacyOptOut -  Clearing out cached data.");

		linkageFields = "";

		// clear any queued event
		clearWaitingEvents();

		// unregister rules with EventHub
		unregisterAllRules();

		// clear cached rules
		clearRulesCacheDirectory();

		// clear all keys in the Campaign Data Store
		clearCampaignDatastore();
	}

	/**
	 * Adds an {@code Event} object to the {@link #waitingEvents} queue, then processes the queued {@code Event}s.
	 *
	 * @param event {@link Event} instance to be processed
	 * @see #processQueuedEvents()
	 */
	void queueAndProcessEvent(final Event event) {
		if (event == null) {
			Log.debug(CampaignConstants.LOG_TAG, "queueAndProcessEvent -  Called with null event.");
			return;
		}

		// add current event to the queue
		waitingEvents.add(event);

		// process queued events
		processQueuedEvents();
	}

	/**
	 * Loops through the existing list of {@code waitingEvents} and processes them.
	 * <p>
	 * Once processed, events are popped from the {@link #waitingEvents} queue. Events are not processed if
	 * {@code Configuration} or {@code Identity} shared states are unavailable or if {@code waitingEvents} queue is empty.
	 */
	void processQueuedEvents() {
		// process all of our waiting events if we can
		while (!waitingEvents.isEmpty()) {
			// get the top event from our list
			final Event currentEvent = waitingEvents.peek();

			if (currentEvent == null) {
				Log.debug(CampaignConstants.LOG_TAG, "processQueuedEvents -  Event queue is empty.");
				break;
			}

			final EventData configState = getSharedEventState(CampaignConstants.EventDataKeys.Configuration.EXTENSION_NAME,
										  currentEvent);

			final EventData identityState = getSharedEventState(CampaignConstants.EventDataKeys.Identity.EXTENSION_NAME,
											currentEvent);

			// Check if configuration or identity is pending. We want to keep the event in the queue if we expect an update here.
			if (configState == EventHub.SHARED_STATE_PENDING || identityState == EventHub.SHARED_STATE_PENDING) {
				Log.debug(CampaignConstants.LOG_TAG,
						  "processQueuedEvents -  Pending Configuration or Identity update, so not processing queued event.");
				break;
			}

			CampaignState campaignState = new CampaignState();
			campaignState.setState(configState, identityState);

			// if this is a lifecycle event, process the event
			if (currentEvent.getEventType() == EventType.LIFECYCLE) {
				processLifecycleUpdate(currentEvent, campaignState);
			} else if (currentEvent.getEventType() == EventType.CONFIGURATION
					   || currentEvent.getEventType() == EventType.CAMPAIGN) {

				if (currentEvent.getEventSource() == EventSource.REQUEST_IDENTITY) {
					clearRulesCacheDirectory();
				}

				triggerRulesDownload(currentEvent, campaignState);
			} else if (currentEvent.getEventType() == EventType.GENERIC_DATA) {
				processMessageInformation(currentEvent, campaignState);
			}

			// pop the current event
			waitingEvents.poll();
		}
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
			Log.debug(CampaignConstants.LOG_TAG,
					  "processMessageInformation -  Campaign extension is not configured to send message track request.");
			return;
		}

		final EventData eventData = event.getData();

		if (eventData == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "processMessageInformation -  Cannot send message track request, eventData is null.");
			return;
		}

		final String broadlogId = eventData.optString(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID,
								  null);
		final String deliveryId = eventData.optString(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID,
								  null);
		final String action = eventData.optString(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION, null);

		if (StringUtils.isNullOrEmpty(broadlogId) || StringUtils.isNullOrEmpty(deliveryId)
				|| StringUtils.isNullOrEmpty(action)) {
			Log.debug(CampaignConstants.LOG_TAG,
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
			Log.trace(CampaignConstants.LOG_TAG,
					  "dispatchMessageEvent -  Action received is other than viewed or clicked, so cannot dispatch Campaign response. ");
			return;
		}

		final int hashMapCapacity = 2;
		final Map<String, String> contextData = new HashMap<String, String>(hashMapCapacity);
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
			Log.debug(CampaignConstants.LOG_TAG,
					  "processLifecycleUpdate -  Campaign extension is not configured to send registration request.");
			return;
		}

		final String url = buildRegistrationUrl(campaignState.getCampaignServer(), campaignState.getCampaignPkey(),
												campaignState.getExperienceCloudId());
		final String payload = buildRegistrationPayload("gcm", campaignState.getExperienceCloudId(),
							   new HashMap<String, String>());

		processRequest(url, payload, campaignState, event);

	}

	/**
	 * Triggers rules download from the configured {@code Campaign} server.
	 * <p>
	 * If current {@code Configuration} properties do not allow downloading {@code Campaign} rules, no request is sent.
	 *
	 * @param event {@link Event} object to be processed
	 * @param campaignState {@link CampaignState} instance containing the current Campaign configuration
	 * @see CampaignState#canDownloadRulesWithCurrentState()
	 * @see #downloadRules(String)
	 */
	void triggerRulesDownload(final Event event, final CampaignState campaignState) {

		if (!campaignState.canDownloadRulesWithCurrentState()) {
			Log.debug(CampaignConstants.LOG_TAG,
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

		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				final JsonUtilityService jsonUtilityService = getJsonUtilityService();

				if (jsonUtilityService == null) {
					Log.debug(CampaignConstants.LOG_TAG,
							  "handleSetLinkageFields -  Cannot set linkage fields, JSON Utility service is not available.");
					return;
				}

				final EncodingService encodingService = getEncodingService();

				if (encodingService == null) {
					Log.debug(CampaignConstants.LOG_TAG,
							  "handleSetLinkageFields -  Cannot set linkage fields, Encoding service is not available.");
					return;
				}


				final JsonUtilityService.JSONObject linkageFieldsJsonObject = jsonUtilityService.createJSONObject(linkageFieldsMap);

				if (linkageFieldsJsonObject == null) {
					Log.debug(CampaignConstants.LOG_TAG,
							  "handleSetLinkageFields -  Cannot set linkage fields, failed to create JSON object from provided linkage fields.");
					return;
				}

				final String linkageFieldsJsonString = linkageFieldsJsonObject.toString();


				if (linkageFieldsJsonString == null || linkageFieldsJsonString.isEmpty()) {
					Log.debug(CampaignConstants.LOG_TAG,
							  "handleSetLinkageFields -  Cannot set linkage fields, linkageFields JSON string is null or empty.");
					return;
				}

				try {
					// The base64Encode method accepts and returns an array of bytes.
					// So the linkageFieldsJsonString must be converted into a byte[].
					byte[] base64EncodedLinkageFieldsBytes = encodingService.base64Encode(linkageFieldsJsonString.getBytes(
								StringUtils.CHARSET_UTF_8));

					if (base64EncodedLinkageFieldsBytes == null) {
						Log.debug(CampaignConstants.LOG_TAG,
								  "handleSetLinkageFields -  Cannot set linkage fields, base64Encode returned null for provided linkage fields JSON string.");
						return;
					}

					// The resulting byte[] must be converted back into a String.
					linkageFields = new String(base64EncodedLinkageFieldsBytes, StringUtils.CHARSET_UTF_8);

				} catch (UnsupportedEncodingException ex) {
					Log.debug(CampaignConstants.LOG_TAG,
							  "handleSetLinkageFields -  Cannot set linkage fields, failed to base64 encode linkage fields JSON string (%s).", ex);
					return;
				}

				if (linkageFields.isEmpty()) {
					Log.debug(CampaignConstants.LOG_TAG,
							  "handleSetLinkageFields -  Cannot set linkage fields, base64 encoded linkage fields string is empty.");
					return;
				}

				queueAndProcessEvent(event);

			}
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

		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				linkageFields = "";

				unregisterAllRules();

				clearRulesCacheDirectory();

				queueAndProcessEvent(event);
			}
		});
	}

	/**
	 * Clears the rules cache directory.
	 * <p>
	 * Creates a {@link CacheManager} instance and invokes method on it to perform delete operation on
	 * {@value CampaignConstants#RULES_CACHE_FOLDER} directory.
	 *
	 * @see CacheManager#deleteFilesNotInList(List, String, boolean)
	 */
	void clearRulesCacheDirectory() {
		CacheManager cacheManager = null;

		try {
			cacheManager = new CacheManager(getSystemInfoService());
		} catch (final MissingPlatformServicesException ex) {
			Log.warning(CampaignConstants.LOG_TAG, "Cannot delete rules cache directory \n (%s).", ex);
		}

		if (cacheManager != null) {
			cacheManager.deleteFilesNotInList(new ArrayList<String>(), CampaignConstants.RULES_CACHE_FOLDER, true);
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
	 * @param consequences {@code List<Map<String, Variant>>} of all rules consequences loaded by the {@code Rules} module
	 * @see CampaignMessage#downloadRemoteAssets(CampaignExtension, PlatformServices, CampaignRuleConsequence)
	 * @see #clearCachedAssetsForMessagesNotInList(List)
	 */
	void loadConsequences(final List<Map<String, Variant>> consequences) {
		// generate a list of loaded message ids so we can clear cached files we no longer need
		final ArrayList<String> loadedMessageIds = new ArrayList<String>();

		if (consequences == null || consequences.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "loadConsequences -  Cannot load consequences, consequences list is null or empty.");
			return;
		}

		for (final Map<String, Variant> consequence : consequences) {
			Variant consequenceAsVariant = Variant.fromVariantMap(consequence);

			try {
				final CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(
							new CampaignRuleConsequenceSerializer());

				if (campaignConsequence != null) {

					final String consequenceType = campaignConsequence.getType();

					if (StringUtils.isNullOrEmpty(consequenceType)
							|| !consequenceType.equals(CampaignConstants.MESSAGE_CONSEQUENCE_MESSAGE_TYPE)) {
						continue;
					}

					final String consequenceId = campaignConsequence.getId();

					if (!StringUtils.isNullOrEmpty(consequenceId)) {
						CampaignMessage.downloadRemoteAssets(this, getPlatformServices(), campaignConsequence);
						loadedMessageIds.add(consequenceId);
					} else {
						Log.debug(CampaignConstants.LOG_TAG, "loadConsequences -  Can't download assets, Consequence id is null");
					}

				}
			} catch (VariantException ex) {
				// shouldn't ever happen, but just in case
				Log.warning(CampaignConstants.LOG_TAG, "Unable to convert consequence json object to a variant.");
			}
		}

		clearCachedAssetsForMessagesNotInList(loadedMessageIds);
	}

	/**
	 * Deletes cached message assets for message Ids not listed in {@code activeMessageIds}.
	 * <p>
	 * If {@code SystemInfoService} is not available, no cached assets are cleared.
	 *
	 * @param activeMessageIds {@code List<String>} containing the Ids of active messages
	 * @see CacheManager#deleteFilesNotInList(List, String, boolean)
	 */
	void clearCachedAssetsForMessagesNotInList(final List<String> activeMessageIds) {
		try {

			final SystemInfoService systemInfoService = getSystemInfoService();

			if (systemInfoService == null) {
				Log.error(CampaignConstants.LOG_TAG, "Cannot clear cached assets, System Info service is not available.");
				return;
			}

			final CacheManager cacheManager = new CacheManager(getPlatformServices().getSystemInfoService());
			cacheManager.deleteFilesNotInList(activeMessageIds, CampaignConstants.MESSAGE_CACHE_DIR, true);
		} catch (final MissingPlatformServicesException ex) {
			Log.warning(CampaignConstants.LOG_TAG,
						"Unable to clear cached message assets \n (%s).", ex);
		}
	}

	/**
	 * Invokes the dispatcher to dispatch {@code EventType#CAMPAIGN}, {@code EventSource#RESPONSE_CONTENT} event
	 * with the provided {@code messageData} Map.
	 *
	 * @param messageData {@link EventData} containing message interaction data
	 * @see CampaignDispatcherCampaignResponseContent#dispatch(Map)
	 */
	void dispatchMessageInteraction(final Map<String, String> messageData) {
		// Dispatch a campaign event to the event hub
		if (campaignEventDispatcher != null) {
			campaignEventDispatcher.dispatch(messageData);
		}
	}

	/**
	 * Invokes the dispatcher to dispatch {@code EventType.GENERIC_DATA}, {@code EventSource.OS} event
	 * with the provided message info.
	 *
	 * @param broadlogId {@link String} containing message broadlogId
	 * @param deliveryId {@code String} containing message deliveryId
	 * @param action {@code String} containing message action
	 *
	 * @see CampaignDispatcherGenericDataOS#dispatch(String, String, String)
	 */
	void dispatchMessageInfo(final String broadlogId, final String deliveryId, final String action) {
		// Dispatch a generic data OS event to the event hub
		if (genericDataOSEventDispatcher != null) {
			genericDataOSEventDispatcher.dispatch(broadlogId, deliveryId, action);
		}
	}

	/**
	 * Creates an instance of {@code CampaignRulesRemoteDownloader} with the provided {@code url}, {@code requestProperties}
	 * and Campaign cache sub-folder {@value CampaignConstants#RULES_CACHE_FOLDER}.
	 * <p>
	 * This method invokes a different {@link CampaignRulesRemoteDownloader} constructor based on whether {@code requestProperties}
	 * are present.
	 *
	 * @param url {@link String} containing Campaign rules download URL
	 * @param requestProperties {@code Map<String, String>} containing header key-value pairs to be sent in the network request
	 * @return {@code CampaignRulesRemoteDownloader} instance
	 * @see CampaignRulesRemoteDownloader#CampaignRulesRemoteDownloader(NetworkService, SystemInfoService, CompressedFileService, String, String)
	 * @see CampaignRulesRemoteDownloader#CampaignRulesRemoteDownloader(NetworkService, SystemInfoService, CompressedFileService, String, String, Map)
	 */
	CampaignRulesRemoteDownloader getCampaignRulesRemoteDownloader(final String url,
			final Map<String, String> requestProperties) {
		CampaignRulesRemoteDownloader campaignRulesRemoteDownloader = null;

		final PlatformServices platformServices = getPlatformServices();

		if (platformServices == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getCampaignRulesRemoteDownloader - Cannot instantiate CampaignRulesRemoteDownloader, Platform services are not available");
			return campaignRulesRemoteDownloader;
		}

		try {
			if (requestProperties != null && !requestProperties.isEmpty()) {
				campaignRulesRemoteDownloader = new CampaignRulesRemoteDownloader(
					platformServices.getNetworkService(),
					platformServices.getSystemInfoService(),
					platformServices.getCompressedFileService(),
					url, CampaignConstants.RULES_CACHE_FOLDER,
					requestProperties);
			} else {
				campaignRulesRemoteDownloader = new CampaignRulesRemoteDownloader(
					platformServices.getNetworkService(),
					platformServices.getSystemInfoService(),
					platformServices.getCompressedFileService(),
					url, CampaignConstants.RULES_CACHE_FOLDER);
			}
		} catch (final MissingPlatformServicesException e) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getCampaignRulesRemoteDownloader - Cannot instantiate CampaignRulesRemoteDownloader (%s)", e);
		}

		return campaignRulesRemoteDownloader;
	}

	/**
	 * Returns this {@code linkageFields}.
	 *
	 * @return {@link String} containing this {@link #linkageFields}
	 */
	String getLinkageFields() {
		return linkageFields;
	}


	/**
	 * Creates an instance of {@code CampaignHitsDatabase} with the {@code PlatformServices} if needed.
	 * <p>
	 * This method returns null if the {@link PlatformServices} are not available.
	 *
	 * @return {@code CampaignHitsDatabase} instance
	 * @see CampaignHitsDatabase#CampaignHitsDatabase(PlatformServices, com.adobe.marketing.mobile.LocalStorageService.DataStore)
	 */
	CampaignHitsDatabase getCampaignHitsDatabase() {
		if (hitsDatabase != null) {
			return hitsDatabase;
		}

		final PlatformServices platformServices = getPlatformServices();

		try {
			hitsDatabase = new CampaignHitsDatabase(platformServices);
		} catch (MissingPlatformServicesException e) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getCampaignHitsDatabase -  Cannot instantiate CampaignHitsDatabase \n (%s).", e);
			return null;
		}

		Log.debug(CampaignConstants.LOG_TAG,
				  "getCampaignHitsDatabase -  CampaignHitsDatabase created.");
		return hitsDatabase;
	}
	// ========================================================================
	// private methods
	// ========================================================================
	/**
	 * Returns platform {@code NetworkService} instance.
	 *
	 * @return {@link NetworkService} or null if {@link PlatformServices} are unavailable
	 */
	private NetworkService getNetworkService() {
		final PlatformServices platformServices = getPlatformServices();

		if (platformServices == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getNetworkService -  Cannot get Network Service, Platform services are not available.");
			return null;
		}

		return platformServices.getNetworkService();
	}

	/**
	 * Returns platform {@code SystemInfoService} instance.
	 *
	 * @return {@link SystemInfoService} or null if {@link PlatformServices} are unavailable
	 */
	private SystemInfoService getSystemInfoService() {
		final PlatformServices platformServices = getPlatformServices();

		if (platformServices == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getSystemInfoService -  Cannot get System Info Service, Platform services are not available.");
			return null;
		}

		return platformServices.getSystemInfoService();
	}

	/**
	 * Returns platform {@code JsonUtilityService} instance.
	 *
	 * @return {@link JsonUtilityService} or null if {@link PlatformServices} are unavailable
	 */
	private JsonUtilityService getJsonUtilityService() {
		final PlatformServices platformServices = getPlatformServices();

		if (platformServices == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getJsonUtilityService -  Cannot get JsonUtility Service, Platform services are not available.");
			return null;
		}

		return platformServices.getJsonUtilityService();
	}

	/**
	 * Returns platform {@code EncodingService} instance.
	 *
	 * @return {@link EncodingService} or null if {@link PlatformServices} are unavailable
	 */
	private EncodingService getEncodingService() {
		final PlatformServices platformServices = getPlatformServices();

		if (platformServices == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getEncodingService - Cannot get Encoding Service, Platform services are not available.");
			return null;
		}

		return platformServices.getEncodingService();
	}

	/**
	 * Returns {@code CampaignExtension}'s {@code DataStore}.
	 * <p>
	 * If {@link PlatformServices} are unavailable or if {@link LocalStorageService} is unavailable, this method returns null.
	 *
	 * @return {@link LocalStorageService.DataStore} object for this {@link CampaignExtension}
	 */
	private LocalStorageService.DataStore getDataStore() {
		final PlatformServices platformServices = getPlatformServices();

		if (platformServices == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getDataStore -  Cannot get Campaign Data store, Platform services are not available.");
			return null;
		}

		final LocalStorageService localStorageService = platformServices.getLocalStorageService();

		if (localStorageService == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getDataStore -  Cannot get Campaign Data store, Local storage service is not available.");
			return null;
		}

		return localStorageService.getDataStore(CampaignConstants.CAMPAIGN_DATA_STORE_NAME);
	}

	/**
	 * Updates {@value CampaignConstants#CAMPAIGN_DATA_STORE_REMOTES_URL_KEY} in {@code CampaignExtension}'s {@code DataStore}.
	 * <p>
	 * If provided {@code url} is null or empty, {@value CampaignConstants#CAMPAIGN_DATA_STORE_REMOTES_URL_KEY} key is removed from
	 * the data store.
	 * <p>
	 * If {@link #getDataStore()} returns null, this method does nothing.
	 *
	 * @param url {@code String} containing a Campaign rules download remotes URL.
	 */
	private void updateUrlInDataStore(final String url) {
		final LocalStorageService.DataStore dataStore = getDataStore();

		if (dataStore == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "updateUrlInDataStore -  Campaign Data store is not available to update.");
			return;
		}

		if (StringUtils.isNullOrEmpty(url)) {
			Log.trace(CampaignConstants.LOG_TAG,
					  "updateUrlInDataStore -  Removing remotes URL key in Campaign Data Store.");
			dataStore.remove(CampaignConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY);
		} else {
			Log.trace(CampaignConstants.LOG_TAG,
					  "updateUrlInDataStore -  Persisting remotes URL (%s) in Campaign Data Store.", url);
			dataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY, url);
		}
	}

	/**
	 * Updates {@value CampaignConstants#CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY} in {@code CampaignExtension}'s {@code DataStore}.
	 * <p>
	 * If provided {@code ecid} is null or empty, {@value CampaignConstants#CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY} key is removed from
	 * the data store.
	 * <p>
	 * If {@link #getDataStore()} returns null, this method does nothing.
	 *
	 * @param ecid {@code String} containing the last known experience cloud id.
	 */
	private void updateEcidInDataStore(final String ecid) {
		final LocalStorageService.DataStore dataStore = getDataStore();

		if (dataStore == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "updateEcidInDataStore -  Campaign Data store is not available to update.");
			return;
		}

		if (StringUtils.isNullOrEmpty(ecid)) {
			Log.trace(CampaignConstants.LOG_TAG,
					  "updateEcidInDataStore -  Removing experience cloud id key in Campaign Data Store.");
			dataStore.remove(CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY);
		} else {
			Log.trace(CampaignConstants.LOG_TAG,
					  "updateEcidInDataStore -  Persisting experience cloud id (%s) in Campaign Data Store.", ecid);
			dataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, ecid);
		}
	}

	/**
	 * Clears all the keys stored in the {@code CampaignExtension}'s {@code DataStore}.
	 */
	private void clearCampaignDatastore() {
		final LocalStorageService.DataStore dataStore = getDataStore();

		if (dataStore == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "clearCampaignDatastore -  Campaign Data store is not available to be cleared.");
			return;
		}

		dataStore.removeAll();
	}

	/**
	 * Clears the queue of events waiting to be processed.
	 */
	private void clearWaitingEvents() {
		waitingEvents.clear();
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
	 * <p>
	 * This method returns null if {@link JsonUtilityService} is unavailable or if {@code JsonUtilityService} failed to
	 * create JSON object to construct the payload {@code String}.
	 *
	 * @param platform {@link String} specifying the push platform "gcm" or "apns"
	 * @param experienceCloudId {@code String} containing the Experience Cloud Id
	 * @param data {@code Map<String, String>} containing additional key-value pairs to be sent in the request
	 * @return {@code String} containing the Campaign registration payload
	 */
	private String buildRegistrationPayload(final String platform,
											final String experienceCloudId,
											final Map<String, String> data) {
		final JsonUtilityService jsonUtilityService = getJsonUtilityService();

		if (jsonUtilityService == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "buildRegistrationPayload -  Cannot send request, JsonUtility service is not available.");
			return "";
		}

		Map<String, String> profileData = new HashMap<String, String>(data);
		profileData.put(CampaignConstants.CAMPAIGN_PUSH_PLATFORM, platform);
		profileData.put(CampaignConstants.EXPERIENCE_CLOUD_ID, experienceCloudId);

		JsonUtilityService.JSONObject bodyJSON = jsonUtilityService.createJSONObject(profileData);

		if (bodyJSON == null) {
			return "";
		}

		return bodyJSON.toString();
	}

	/**
	 * Queues a {@code Campaign} registration request by creating a {@code CampaignHit} object and passing it to
	 * the {@code CampaignHitsDatabase} instance.
	 * <p>
	 * If the {@code payload} is null, empty, or if the {@link NetworkService} is not available,
	 * then the {@code Campaign} registration request is dropped.
	 *
	 * @param url {@link String} containing the registration request URL
	 * @param payload {@link String} containing the registration request payload
	 * @param campaignState {@link CampaignState} instance containing the current {@code Campaign} configuration
	 * @param event {@link Event} which triggered the queuing of the {@code Campaign} registration request
	 */
	private void processRequest(final String url, final String payload, final CampaignState campaignState,
								final Event event) {
		final NetworkService networkService = getNetworkService();

		if (networkService == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "processRequest -  Cannot send request, Network service is not available.");
			return;
		}

		// check if this request is a registration request by checking for the presence of a payload
		// and if it is a registration request, determine if it should be sent.
		if (!StringUtils.isNullOrEmpty(payload)
				&& !shouldSendRegistrationRequest(campaignState, event.getTimestamp())) {
			return;
		}

		final CampaignHitsDatabase database = getCampaignHitsDatabase();

		if (database != null) {
			// create then queue the campaign hit
			CampaignHit campaignHit = new CampaignHit();
			campaignHit.url = url;
			campaignHit.body = payload;
			campaignHit.timeout = campaignState.getCampaignTimeout();
			database.queue(campaignHit, event.getTimestamp(), campaignState.getMobilePrivacyStatus());

			Log.debug(CampaignConstants.LOG_TAG,
					  "processRequest - Campaign Request Queued with url (%s) and body (%s)", url, payload);
		} else {
			Log.warning(CampaignConstants.LOG_TAG,
						"Campaign database is not initialized. Unable to queue Campaign Request.");
		}
	}

	/**
	 * Determines if a {@code CampaignMessage} should be shown.
	 * <p>
	 * This method returns false if {@link UIService} instance is not available or if another message is already being
	 * displayed.
	 *
	 * @return {@code boolean} indicating whether the message should be shown
	 * @see UIService#isMessageDisplayed()
	 */
	private boolean shouldShowMessage() {
		final PlatformServices platformServices = getPlatformServices();

		if (platformServices == null) {
			Log.error(CampaignConstants.LOG_TAG, "Cannot show message, Platform services are not available");
			return false;
		}

		UIService uiService =  platformServices.getUIService();
		return (uiService != null && !uiService.isMessageDisplayed());
	}

	/**
	 * Starts synchronous rules download from the provided {@code url}.
	 * <p>
	 * This method instantiates {@code CampaignRulesRemoteDownloader} and invokes method on it to download and
	 * cache Campaign rules. Once the rules are downloaded, they are registered with the {@link EventHub}.
	 * <p>
	 * If the given {@code url} is null or empty or, if {@link #getCampaignRulesRemoteDownloader(String, Map)}
	 * returns null, no rules download happens.
	 *
	 * @param url {@link String} containing Campaign rules download URL
	 * @see CampaignRulesRemoteDownloader#startDownloadSync()
	 * @see #onRulesDownloaded(File, String)
	 */
	private void downloadRules(final String url) {

		if (StringUtils.isNullOrEmpty(url)) {
			Log.warning(CampaignConstants.LOG_TAG,
						"Cannot download rules, provided url is null or empty. Cached rules will be used if present.");
			return;
		}

		final Map<String, String> requestProperties = new HashMap<String, String>();

		if (linkageFields != null && !linkageFields.isEmpty()) {
			requestProperties.put(CampaignConstants.LINKAGE_FIELD_NETWORK_HEADER, linkageFields);
		}

		final CampaignRulesRemoteDownloader remoteDownloader = getCampaignRulesRemoteDownloader(url, requestProperties);

		if (remoteDownloader != null) {
			final File outputFile = remoteDownloader.startDownloadSync();
			onRulesDownloaded(outputFile, url);
		}
	}

	/**
	 * Invoked when rules have finished downloading.
	 * <p>
	 * This method takes the following actions once rules are downloaded:
	 * <ul>
	 *     <li>Unregister any previously registered rules with {@link EventHub}.</li>
	 *     <li>Persist the provided remotes {@code url} in Campaign data store.</li>
	 *     <li>Register downloaded rules with the {@code EventHub}.</li>
	 *     <li>Download and cache remote assets for the {@link #loadedConsequencesList}.</li>
	 * </ul>
	 * If {@link #loadRulesFromDirectory(File)} cannot parse {@value CampaignConstants#RULES_JSON_FILE_NAME} in the
	 * provided {@code rulesDirectory} and returns empty {@code List<Rule>}, then no rules are registered.
	 *
	 * @param rulesDirectory {@link File} object containing the directory that the rules bundle is in
	 * @param url {@link String} containing Campaign rules download URL
	 * @see Module#unregisterAllRules()
	 * @see #updateUrlInDataStore(String)
	 * @see Module#replaceRules(List)
	 * @see #loadConsequences(List)
	 */
	private void onRulesDownloaded(final File rulesDirectory, final String url) {
		// save remotes url in Campaign Data Store
		updateUrlInDataStore(url);

		loadedConsequencesList = new ArrayList<Map<String, Variant>>();
		// register all new rules
		replaceRules(loadRulesFromDirectory(rulesDirectory));

		loadConsequences(loadedConsequencesList);
		loadedConsequencesList = null; // We don't need it anymore.
	}

	/**
	 * Parses {@value CampaignConstants#RULES_JSON_FILE_NAME} in the provided cache {@code rulesDirectory}
	 * and returns the parsed {@code List<Rule>}.
	 * <p>
	 * If provided {@code rulesDirectory} is null or empty or, if {@code JsonUtilityService} is null then an empty {@code List}
	 * is returned.
	 *
	 * @param rulesDirectory {@link File} object containing the directory that the rules bundle is in
	 * @return a {@code List} of {@code Rule} objects that were parsed from the {@value CampaignConstants#RULES_JSON_FILE_NAME} in
	 * the provided {@code rulesDirectory}
	 * @see #parseRulesFromJsonObject(JsonUtilityService.JSONObject, File)
	 */
	private List<Rule> loadRulesFromDirectory(final File rulesDirectory) {
		List<Rule> rulesList = new ArrayList<Rule>();

		// check if we downloaded a valid file
		if (rulesDirectory == null || !rulesDirectory.isDirectory()) {
			Log.debug(CampaignConstants.LOG_TAG, "loadRulesFromDirectory -  No valid rules directory found in cache.");
			// clear existing rules
			unregisterAllRules();
			return rulesList;
		}

		final String rulesFilePath = rulesDirectory.getPath() + File.separator + CampaignConstants.RULES_JSON_FILE_NAME;
		final File rulesFile = new File(rulesFilePath);
		final String jsonString = readFromFile(rulesFile);

		// read new object
		JsonUtilityService jsonUtilityService = getJsonUtilityService();

		if (jsonUtilityService != null) {
			final JsonUtilityService.JSONObject rulesJsonObject = jsonUtilityService.createJSONObject(jsonString);
			rulesList = parseRulesFromJsonObject(rulesJsonObject, rulesDirectory);
		}

		return rulesList;
	}

	/**
	 * Reads a file from disk and returns its contents as {@code String}.
	 *
	 * @param rulesJsonFile the {@link File} object to read
	 * @return the {@code rulesJsonFile} contents as {@code String}
	 */
	private String readFromFile(final File rulesJsonFile) {
		String json = null;

		if (rulesJsonFile != null) {
			FileInputStream rulesJsonIS = null;

			try {
				rulesJsonIS = new FileInputStream(rulesJsonFile);
				json = StringUtils.streamToString(rulesJsonIS);
			} catch (IOException ex) {
				Log.debug(CampaignConstants.LOG_TAG, "readFromFile -  Could not read the rules json file! (%s)", ex);
			} finally {
				try {
					if (rulesJsonIS != null) {
						rulesJsonIS.close();
					}
				} catch (Exception e) {
					Log.trace(CampaignConstants.LOG_TAG, "readFromFile -  Failed to close stream for %s", rulesJsonFile);
				}
			}
		}

		return json;
	}

	/**
	 * Parses all rules from the provided {@code jsonObject} into a list of {@code Rules}s.
	 * <p>
	 * If input {@code jsonObject} is null or, if there is an error reading {@link JsonUtilityService.JSONArray} from it,
	 * empty {@code List<Rule>} is returned.
	 *
	 * @param jsonObject {@code JSONObject} containing the list of rules and consequences
	 * @param rulesDirectory {@link File} instance representing the Campaign rules cache directory
	 * @return a {@code List} of {@code Rule} objects that were parsed from the input {@code jsonObject}
	 */
	private List<Rule> parseRulesFromJsonObject(final JsonUtilityService.JSONObject jsonObject, final File rulesDirectory) {
		final List<Rule> parsedRules = new ArrayList<Rule>();

		if (jsonObject == null) {
			Log.debug(CampaignConstants.LOG_TAG, "parseRulesFromJsonObject -  Unable to parse rules, input jsonObject is null.");
			return parsedRules;
		}

		JsonUtilityService.JSONArray rulesJsonArray;

		try {
			rulesJsonArray = jsonObject.getJSONArray(CampaignConstants.RULES_JSON_KEY);
		} catch (final JsonException e) {
			Log.debug(CampaignConstants.LOG_TAG, "parseRulesFromJsonObject -  Unable to parse rules (%s)", e);
			return parsedRules;
		}

		// loop through each rule definition
		for (int i = 0; i < rulesJsonArray.length(); i++) {
			try {
				// get individual rule json object
				final JsonUtilityService.JSONObject ruleObject = rulesJsonArray.getJSONObject(i);
				// get rule condition
				final JsonUtilityService.JSONObject ruleConditionJsonObject = ruleObject.getJSONObject(
							CampaignConstants.RULES_JSON_CONDITION_KEY);
				final RuleCondition condition = RuleCondition.ruleConditionFromJson(ruleConditionJsonObject);
				// get consequences
				final List<Event> consequences = generateConsequenceEvents(ruleObject.getJSONArray(
													 CampaignConstants.RULES_JSON_CONSEQUENCES_KEY), rulesDirectory);

				parsedRules.add(new Rule(condition, consequences));
			} catch (final JsonException e) {
				Log.debug(CampaignConstants.LOG_TAG, "parseRulesFromJsonObject -  Unable to parse individual rule json (%s)", e);
			} catch (final UnsupportedConditionException e) {
				Log.debug(CampaignConstants.LOG_TAG, "parseRulesFromJsonObject -  Unable to parse individual rule conditions (%s)", e);
			} catch (final IllegalArgumentException e) {
				Log.debug(CampaignConstants.LOG_TAG, "parseRulesFromJsonObject -  Unable to create rule object (%s)", e);
			}
		}

		return parsedRules;
	}

	/**
	 * Parses {@code CampaignRuleConsequence} objects from the given {@code consequenceJsonArray} and converts them
	 * into a list of {@code Event}s.
	 * <p>
	 * Any consequence of type {@value CampaignConstants#MESSAGE_CONSEQUENCE_MESSAGE_TYPE} is used to generate a
	 * {@link EventType#CAMPAIGN}, {@link EventSource#REQUEST_CONTENT}. All other consequences generate a
	 * {@link EventType#RULES_ENGINE}, {@link EventSource#RESPONSE_CONTENT}.
	 * <p>
	 * For {@value CampaignConstants#MESSAGE_CONSEQUENCE_MESSAGE_TYPE} type consequences, {@code assetsPath} key is added to
	 * consequence before serializing it to {@link Variant} to be added in {@code EventData}.
	 * <p>
	 * An empty {@code List<Event>} is returned if {@code consequenceJsonArray} is empty or null or, if
	 * {@code JsonUtilityService} is not available.
	 *
	 * @param consequenceJsonArray {@link JsonUtilityService.JSONArray} object containing 1 or more rule consequence definitions
	 * @param rulesDirectory {@link File} instance representing the Campaign rules cache directory
	 * @return a {@code List} of consequence {@code Event} objects.
	 *
	 * @throws JsonException if errors occur during parsing
	 */
	private List<Event> generateConsequenceEvents(final JsonUtilityService.JSONArray consequenceJsonArray,
			final File rulesDirectory) throws
		JsonException {
		final List<Event> parsedEvents = new ArrayList<Event>();

		if (consequenceJsonArray == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "generateConsequenceEvents -  The passed in consequence array is null, so returning an empty consequence events list.");
			return  parsedEvents;
		}

		JsonUtilityService jsonUtilityService = getJsonUtilityService();

		if (jsonUtilityService == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "generateConsequenceEvents -  JsonUtility service is not available, returning empty consequence events list.");
			return parsedEvents;
		}

		for (int i = 0; i < consequenceJsonArray.length(); i++) {
			try {
				final Variant consequenceAsVariant = Variant.fromTypedObject(consequenceJsonArray.getJSONObject(i),
													 new JsonObjectVariantSerializer(jsonUtilityService));

				CampaignRuleConsequence consequence = consequenceAsVariant.getTypedObject(new CampaignRuleConsequenceSerializer());

				if (consequence != null) {
					// Add assetsPath to consequenceVariantMap
					final String assetsPath = rulesDirectory + File.separator + CampaignConstants.ASSETS_DIR;
					consequence.setAssetsPath(assetsPath);

					final Map<String, Variant> consequenceVariantMap = Variant.fromTypedObject(consequence,
							new CampaignRuleConsequenceSerializer()).getVariantMap();

					if (loadedConsequencesList != null) {
						loadedConsequencesList.add(consequenceVariantMap);
					}

					EventData eventData = new EventData();
					eventData.putVariantMap(CampaignConstants.EventDataKeys.RuleEngine.CONSEQUENCE_TRIGGERED,
											consequenceVariantMap);

					final Event event;

					if (consequence.getType().equals(CampaignConstants.MESSAGE_CONSEQUENCE_MESSAGE_TYPE)) {
						event = new Event.Builder("Rules Event", EventType.CAMPAIGN, EventSource.REQUEST_CONTENT)
						.setData(eventData)
						.build();

					} else {
						event = new Event.Builder("Rules Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
						.setData(eventData)
						.build();
					}

					parsedEvents.add(event);
				}
			} catch (VariantException ex) {
				// shouldn't ever happen, but just in case
				Log.warning(CampaignConstants.LOG_TAG,
							"Unable to convert consequence json object to a variant.");
			}
		}

		return parsedEvents;
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
			Log.debug(CampaignConstants.LOG_TAG,
					  "shouldSendRegistrationRequest -  Registration requests are paused.");
			return false;
		}

		final String retrievedEcid = getDataStore().getString(
										 CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "");
		final String currentEcid = campaignState.getExperienceCloudId();
		final long retrievedTimestamp = getDataStore().getLong(
											CampaignConstants.CAMPAIGN_DATA_STORE_REGISTRATION_TIMESTAMP_KEY, CampaignConstants.DEFAULT_TIMESTAMP_VALUE);
		final int registrationDelay = campaignState.getCampaignRegistrationDelay();
		final long registrationDelayInMilliseconds = TimeUnit.DAYS.toMillis(registrationDelay);

		if (!retrievedEcid.equals(currentEcid)) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "shouldSendRegistrationRequest - The current ecid (%s) is new, sending the registration request.",
					  currentEcid);
			updateEcidInDataStore(currentEcid);
			return true;

		}

		if (eventTimestamp - retrievedTimestamp >= registrationDelayInMilliseconds) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "shouldSendRegistrationRequest -  Registration delay of (%d) days has elapsed. Sending the Campaign registration request.",
					  registrationDelay);
			return true;
		}

		Log.debug(CampaignConstants.LOG_TAG,
				  "shouldSendRegistrationRequest - The registration request will not be sent because the registration delay of (%d) days has not elapsed.",
				  registrationDelay);
		return false;
	}
}
