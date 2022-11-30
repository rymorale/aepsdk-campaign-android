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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.rulesengine.Evaluable;
import com.adobe.marketing.mobile.services.DataQueue;
import com.adobe.marketing.mobile.services.DataQueuing;
import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.PersistentHitQueue;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.services.ui.AlertListener;
import com.adobe.marketing.mobile.services.ui.AlertSetting;
import com.adobe.marketing.mobile.services.ui.FullscreenMessageDelegate;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.NotificationSetting;
import com.adobe.marketing.mobile.services.ui.UIService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CampaignExtensionTests {

	private CampaignExtension campaignExtension;
	private File cacheDir;
	private String messageCacheDirString1;
	private String messageCacheDirString2;
	private String rulesCacheDirString;

	private static final String fakeMessageId1 = "d38a46f6-4f43-435a-a862-4038c27b90a1";
	private static final String fakeMessageId2 = "e38a46f6-4f43-435a-a862-4038c27b90a2";
	private static final String messageId = "07a1c997-2450-46f0-a454-537906404124";
	private static final String MESSAGES_CACHE = CampaignConstants.CACHE_BASE_DIR + File.separator + CampaignConstants.MESSAGE_CACHE_DIR + File.separator;

	private HashMap<String, String> metadataMap;

	@Mock
	ExtensionApi mockExtensionApi;
	@Mock
	ServiceProvider mockServiceProvider;
	@Mock
	UIService mockUIService;
	@Mock
	CacheService mockCacheService;
	@Mock
	CacheResult mockCacheResult;
	@Mock
	DataQueuing mockDataQueueService;
	@Mock
	DataQueue mockDataQueue;
	@Mock
	DeviceInforming mockDeviceInfoService;
	@Mock
	Networking mockNetworkService;
	@Mock
	DataStoring mockDataStoreService;
	@Mock
	NamedCollection mockNamedCollection;
	@Mock
	PersistentHitQueue mockPersistentHitQueue;
	@Mock
	LaunchRulesEngine mockRulesEngine;
	@Mock
	CampaignRulesDownloader mockCampaignRulesDownloader;
	@Mock
	Evaluable mockEvaluable;
	@Mock
	CampaignState mockCampaignState;

	@Before()
	public void setup() {
		cacheDir = new File("cache");
		cacheDir.mkdirs();
		cacheDir.setWritable(true);

		final String sha256HashForRemoteUrl = "fb0d3704b73d5fa012a521ea31013a61020e79610a3c27e8dd1007f3ec278195";
		final String cachedFileName = sha256HashForRemoteUrl + ".12345"; //12345 is just a random extension.
		metadataMap = new HashMap<>();
		metadataMap.put(CampaignConstants.METADATA_PATH, MESSAGES_CACHE + messageId + File.separator + cachedFileName);

		messageCacheDirString1 = CampaignConstants.MESSAGE_CACHE_DIR + File.separator + fakeMessageId1;
		messageCacheDirString2 = CampaignConstants.MESSAGE_CACHE_DIR + File.separator + fakeMessageId2;
		rulesCacheDirString = CampaignConstants.RULES_CACHE_FOLDER;
		
		campaignExtension = new CampaignExtension(mockExtensionApi, mockPersistentHitQueue, mockDataStoreService, mockRulesEngine, mockCampaignState, mockCacheService, mockCampaignRulesDownloader);
	}

	private void setupServiceProviderMockAndRunTest(Runnable testRunnable) {
		File assetFile = TestUtils.getResource("happy_test.html");
		try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
			serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
			when(mockCacheService.get(anyString(), eq("happy_test.html"))).thenReturn(mockCacheResult);
			when(mockCacheService.get(anyString(), eq("http://asset1-url00.jpeg"))).thenReturn(mockCacheResult);
			when(mockCacheResult.getData()).thenReturn(new FileInputStream(assetFile));
			when(mockCacheResult.getMetadata()).thenReturn(metadataMap);
			when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
			when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
			when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);
			when(mockServiceProvider.getDataStoreService()).thenReturn(mockDataStoreService);
			when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
			when(mockServiceProvider.getDataQueueService()).thenReturn(mockDataQueueService);
			when(mockDataStoreService.getNamedCollection(anyString())).thenReturn(mockNamedCollection);
			when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(cacheDir);
			when(mockDataQueueService.getDataQueue(anyString())).thenReturn(mockDataQueue);
			testRunnable.run();
		} catch (FileNotFoundException exception) {
			fail(exception.getMessage());
		}
	}

	@After
	public void teardown() {
		clearCacheFiles(cacheDir);
	}

	private SharedStateResult getConfigurationEventData(final Map<String, Object> customConfig) {
		final Map<String, Object> configData = new HashMap<>();
		configData.put(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "testServer");
		configData.put(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_PKEY_KEY, "testPkey");
		configData.put(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_MCIAS_KEY, "testMcias");
		configData.put(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_TIMEOUT, 10);
		configData.put(CampaignConstants.EventDataKeys.Configuration.PROPERTY_ID, "testPropertyId");
		configData.put(CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
		configData.put(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_DELAY_KEY, 30);
		configData.put(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_PAUSED_KEY, true);
		configData.putAll(customConfig);
		final SharedStateResult sharedStateResult = new SharedStateResult(SharedStateStatus.SET, configData);

		return sharedStateResult;
	}

	private SharedStateResult getIdentityEventData() {
		final Map<String, Object> identityData = new HashMap<>();
		identityData.put(CampaignConstants.EventDataKeys.Identity.VISITOR_ID_MID, "testExperienceCloudId");
		final SharedStateResult sharedStateResult = new SharedStateResult(SharedStateStatus.SET, identityData);

		return sharedStateResult;
	}

	private Map<String, Object> getLifecycleEventData() {
		final Map<String, Object> lifecycleData = new HashMap<>();
		lifecycleData.put(CampaignConstants.EventDataKeys.Lifecycle.LAUNCH_EVENT, "LaunchEvent");

		final Map<String, Object> lifecycleEventData = new HashMap<>();
		lifecycleData.put(CampaignConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA, lifecycleData);
		return lifecycleEventData;
	}

	private SharedStateResult getConfigurationEventDataWithCustomRegistrationDelay(final int registrationDelay) {
		final Map<String, Object> customConfig = new HashMap<>();
		customConfig.put(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_DELAY_KEY,
							   registrationDelay);

		return getConfigurationEventData(customConfig);
	}

	private SharedStateResult getConfigurationEventDataWithRegistrationPausedStatus(final boolean registrationPaused) {
		final Map<String, Object> customConfig = new HashMap<>();
		customConfig.put(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_PAUSED_KEY,
							   registrationPaused);

		return getConfigurationEventData(customConfig);
	}

	private SharedStateResult getConfigurationEventDataWithCustomRegistrationDelayAndPauseStatus(final int registrationDelay, final boolean registrationPaused) {
		final Map<String, Object> customConfig = new HashMap<>();
		customConfig.put(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_DELAY_KEY,
							   registrationDelay);
		customConfig.put(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_PAUSED_KEY,
							   registrationPaused);

		return getConfigurationEventData(customConfig);
	}

	private Map<String, Object> getMessageConsequenceEventData(final RuleConsequence consequence) {
		final Map<String, Object> triggeredConsequenceData = new HashMap<>();

		triggeredConsequenceData.put(CampaignConstants.EventDataKeys.RuleEngine.CONSEQUENCE_TRIGGERED,
				consequence);
		return triggeredConsequenceData;
	}

	private Map<String, Object> getMessageTrackEventData(final String broadlogId, final String deliveryId, final String action) {
		final Map<String, Object> trackData = new HashMap<>();
		trackData.put(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID, broadlogId);
		trackData.put(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID, deliveryId);
		trackData.put(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION, action);

		return trackData;
	}

	private Event getLifecycleEvent(final Map<String, Object> eventData) {
		return new Event.Builder("TEST", EventType.LIFECYCLE, EventSource.REQUEST_CONTENT)
			   .setEventData(eventData).build();
	}

	private void setupCachedMessageAssets(final String fakeMessageId1, final String fakeMessageId2) throws Exception {
		// setup
		final File existingCacheDir = new File(cacheDir, messageCacheDirString1);
		existingCacheDir.mkdirs();
		final File existingCachedFile = new
		File(existingCacheDir + File.separator +
			 "028dbbd3617ccfb5e302f4aa2df2eb312d1571ee40b3f4aa448658c9082b0411.1262304000000");
		existingCachedFile.createNewFile();

		final File existingCacheDir2 = new File(cacheDir, messageCacheDirString2);
		existingCacheDir2.mkdirs();
		final File existingCachedFile2 = new
		File(existingCacheDir2 + File.separator +
			 "028dbbd3617ccfb5e302f4aa2df2eb312d1571ee40b3f4aa448658c9082b0411.1262304000000");
		existingCachedFile2.createNewFile();
	}

	private File setupCachedRules() throws Exception {
		// setup
		final File existingCacheDir = new File(cacheDir, rulesCacheDirString);
		existingCacheDir.mkdirs();
		final File existingCachedFile = new
		File(existingCacheDir + File.separator +
			 "c3da84c61f5768a6a401d09baf43275f5dcdb6cf57f8ad5382fa6c3d9a6c4a75.1262304000000");
		existingCachedFile.createNewFile();
		return existingCachedFile;
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

	// =================================================================================================================
	// void handleWildcardEvents(final Event event)
	// =================================================================================================================

	@Test
	public void test_handleWildcardEvents_when_validEventForLocalNotification_happy() {
		// setup
		setupServiceProviderMockAndRunTest(() -> {
			ArgumentCaptor < NotificationSetting > notificationSettingArgumentCaptor = ArgumentCaptor.forClass(NotificationSetting.class);
			Map<String, Object> detail =
					new HashMap<String, Object>() {
						{
							put("template", "local");
							put("content", "messageContent");
						}
					};

			List<RuleConsequence> ruleConsequenceList = new ArrayList<RuleConsequence>() {
				{
					add(new RuleConsequence("id", "iam", detail));
				}
			};
			List<LaunchRule> triggeredRulesList = new ArrayList<LaunchRule>() {
				{
					add(new LaunchRule(mockEvaluable, ruleConsequenceList));
				}
			};

			Event testEvent = new Event.Builder("Test event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
					.setEventData(null)
					.build();

			when(mockRulesEngine.process(testEvent)).thenReturn(triggeredRulesList);

			// test
			campaignExtension.handleWildcardEvents(testEvent);

			// verify
			verify(mockUIService, times(1)).showLocalNotification(notificationSettingArgumentCaptor.capture());
			NotificationSetting notificationSetting = notificationSettingArgumentCaptor.getValue();
			assertEquals("messageContent", notificationSetting.getContent());
			assertEquals("id", notificationSetting.getIdentifier());
			assertEquals("", notificationSetting.getDeeplink());
			assertEquals(-1, notificationSetting.getFireDate());
			Map<String, Object> userInfo = notificationSetting.getUserInfo();
			assertEquals(null, userInfo);
		});
	}

	@Test
	public void test_handleWildcardEvents_when_validEventForAlert_happy() {
		// setup
		setupServiceProviderMockAndRunTest(() -> {
			ArgumentCaptor<AlertSetting> alertSettingArgumentCaptor = ArgumentCaptor.forClass(AlertSetting.class);
			Map<String, Object> detail =
					new HashMap<String, Object>() {
						{
							put("template", "alert");
							put("title", "messageTitle");
							put("content", "messageContent");
							put("cancel", "No");
						}
					};

			List<RuleConsequence> ruleConsequenceList = new ArrayList<RuleConsequence>() {
				{
					add(new RuleConsequence("id", "iam", detail));
				}
			};
			List<LaunchRule> triggeredRulesList = new ArrayList<LaunchRule>() {
				{
					add(new LaunchRule(mockEvaluable, ruleConsequenceList));
				}
			};

			Event testEvent = new Event.Builder("Test event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
					.setEventData(null)
					.build();

			when(mockRulesEngine.process(testEvent)).thenReturn(triggeredRulesList);

			// test
			campaignExtension.handleWildcardEvents(testEvent);

			// verify
			verify(mockUIService, times(1)).showAlert(alertSettingArgumentCaptor.capture(), any(AlertListener.class));
			AlertSetting alertSetting = alertSettingArgumentCaptor.getValue();
			assertEquals("messageContent", alertSetting.getMessage());
			assertEquals("messageTitle", alertSetting.getTitle());
			assertEquals("", alertSetting.getPositiveButtonText());
			assertEquals("No", alertSetting.getNegativeButtonText());
		});
	}

	@Test
	public void test_handleWildcardEvents_when_validEventForFullscreen_happy() {
		// setup
		setupServiceProviderMockAndRunTest(() -> {
			ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
			Map<String, Object> detail =
					new HashMap<String, Object>() {
						{
							put("template", "fullscreen");
							put("html", "happy_test.html");
						}
					};

			List<RuleConsequence> ruleConsequenceList = new ArrayList<RuleConsequence>() {
				{
					add(new RuleConsequence("id", "iam", detail));
				}
			};
			List<LaunchRule> triggeredRulesList = new ArrayList<LaunchRule>() {
				{
					add(new LaunchRule(mockEvaluable, ruleConsequenceList));
				}
			};

			Event testEvent = new Event.Builder("Test event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
					.setEventData(null)
					.build();

			when(mockRulesEngine.process(testEvent)).thenReturn(triggeredRulesList);

			// test
			campaignExtension.handleWildcardEvents(testEvent);

			// verify
			verify(mockUIService, times(1)).createFullscreenMessage(stringArgumentCaptor.capture(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class));
			String htmlContent = stringArgumentCaptor.getValue();
			assertEquals("<!DOCTYPE html>\n<html>\n<head><meta charset=\"UTF-8\"></head>\n<body>\neverything is awesome http://asset1-url00.jpeg\n</body>\n</html>", htmlContent);
		});
	}

	@Test
	public void test_handleWildcardEvents_when_nullDetailsPresentInConsequence_then_shouldNotShowMessage() {
		// setup
		setupServiceProviderMockAndRunTest(() -> {
			List<RuleConsequence> ruleConsequenceList = new ArrayList<RuleConsequence>() {
				{
					add(new RuleConsequence("id", "iam", null));
				}
			};
			List<LaunchRule> triggeredRulesList = new ArrayList<LaunchRule>() {
				{
					add(new LaunchRule(mockEvaluable, ruleConsequenceList));
				}
			};

			Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_CONTENT)
					.setEventData(null)
					.build();

			when(mockRulesEngine.process(testEvent)).thenReturn(triggeredRulesList);

			// test
			campaignExtension.handleWildcardEvents(testEvent);

			// verify
			verify(mockUIService, times(0)).createFullscreenMessage(anyString(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class));
			verify(mockUIService, times(0)).showAlert(any(AlertSetting.class), any(AlertListener.class));
			verify(mockUIService, times(0)).showLocalNotification(any(NotificationSetting.class));
		});
	}

	@Test
	public void test_handleWildcardEvents_when_emptyConsequenceListReturnedInTriggeredRules_then_shouldNotShowMessage() {
		// setup
		setupServiceProviderMockAndRunTest(() -> {
			List<RuleConsequence> ruleConsequenceList = new ArrayList<>();
			List<LaunchRule> triggeredRulesList = new ArrayList<LaunchRule>() {
				{
					add(new LaunchRule(mockEvaluable, ruleConsequenceList));
				}
			};

			Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_CONTENT)
					.setEventData(getMessageConsequenceEventData(null))
					.build();

			when(mockRulesEngine.process(testEvent)).thenReturn(triggeredRulesList);

			// test
			campaignExtension.handleWildcardEvents(testEvent);

			// verify
			verify(mockUIService, times(0)).createFullscreenMessage(anyString(), any(FullscreenMessageDelegate.class), anyBoolean(), any(MessageSettings.class));
			verify(mockUIService, times(0)).showAlert(any(AlertSetting.class), any(AlertListener.class));
			verify(mockUIService, times(0)).showLocalNotification(any(NotificationSetting.class));
		});
	}


	// =================================================================================================================
	// void setCampaignState(final Event event)
	// =================================================================================================================

	@Test
	public void test_setCampaignState_when_nullStateOwner_then_shouldDoNothing() {
		// test
		campaignExtension.setCampaignState(null);

		// verify
		verify(mockCampaignState, times(0)).setState(any(SharedStateResult.class), any(SharedStateResult.class));
	}

	@Test
	public void test_setCampaignState_when_configurationStateOwner_then_shouldSetCampaignState() {
		// setup
		Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
				.setEventData(getConfigurationEventData(new HashMap<>()).getValue())
				.build();
		when(mockExtensionApi.getSharedState(eq(CampaignConstants.EventDataKeys.Configuration.EXTENSION_NAME), any(Event.class), anyBoolean(), any(SharedStateResolution.class))).thenReturn(getConfigurationEventData(new HashMap<>()));
		// test
		campaignExtension.setCampaignState(testEvent);

		// verify setState called with the first argument being present. first argument is a config SharedStateResult.
		verify(mockCampaignState, times(1)).setState(any(SharedStateResult.class), eq(null));
	}

	@Test
	public void test_setCampaignState_when_identityStateOwner_then_shouldSetCampaignState() {
		// setup
		Event testEvent = new Event.Builder("Test event", EventType.IDENTITY, EventSource.RESPONSE_CONTENT)
				.setEventData(getConfigurationEventData(new HashMap<>()).getValue())
				.build();
		when(mockExtensionApi.getSharedState(eq(CampaignConstants.EventDataKeys.Identity.EXTENSION_NAME), any(Event.class), anyBoolean(), any(SharedStateResolution.class))).thenReturn(getIdentityEventData());
		// test
		campaignExtension.setCampaignState(testEvent);

		// verify setState called with the second argument being present. second argument is a identity SharedStateResult.
		verify(mockCampaignState, times(1)).setState(eq(null), any(SharedStateResult.class));
	}


	// =================================================================================================================
	// void handleLinkageFieldsEvent(final Event event)
	// =================================================================================================================
	@Test
	public void test_handleLinkageFieldsEvent_when_eventIsNull() {
		try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
			// test
			campaignExtension.handleLinkageFieldsEvent(null);

			// verify
			logMockedStatic.verify(() -> Log.debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()));
			verify(mockCacheService, times(0)).remove(eq(CampaignConstants.RULES_CACHE_FOLDER), eq(""));
			verify(mockCampaignRulesDownloader, times(0)).loadRulesFromUrl(anyString(), anyString());
		}
	}

	@Test
	public void test_handleLinkageFieldsEvent_when_eventDataIsEmpty() {
		try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
			// setup
			Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY)
					.build();

			// test
			campaignExtension.handleLinkageFieldsEvent(testEvent);

			// verify
			logMockedStatic.verify(() -> Log.debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()));
			verify(mockCacheService, times(0)).remove(eq(CampaignConstants.RULES_CACHE_FOLDER), eq(""));
			verify(mockCampaignRulesDownloader, times(0)).loadRulesFromUrl(anyString(), anyString());
		}
	}

	@Test
	public void test_handleLinkageFieldsEvent_when_linkageFieldsAreEmpty() {
		try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
			// setup
			HashMap<String, Object> eventData = new HashMap<>();
			Map<String, String> linkageFields = new HashMap<>();
			eventData.put(CampaignConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, linkageFields);

			Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY)
					.setEventData(eventData)
					.build();

			// test
			campaignExtension.handleLinkageFieldsEvent(testEvent);

			// verify
			logMockedStatic.verify(() -> Log.debug(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()));
			verify(mockCacheService, times(0)).remove(eq(CampaignConstants.RULES_CACHE_FOLDER), eq(""));
			verify(mockCampaignRulesDownloader, times(0)).loadRulesFromUrl(anyString(), anyString());
		}
	}

	@Test
	public void test_handleLinkageFieldsEvent_setValidLinkageFields() {
		// setup
		CampaignState campaignState = new CampaignState();
		campaignState.setState(getConfigurationEventData(new HashMap<>()), getIdentityEventData());
		campaignExtension = new CampaignExtension(mockExtensionApi, mockPersistentHitQueue, mockDataStoreService, mockRulesEngine, campaignState, mockCacheService, mockCampaignRulesDownloader);
		String expectedRulesDownloadUrl = "https://testMcias/testServer/testPropertyId/testExperienceCloudId/rules.zip";
		String expectedBase64EncodedLinkageFields = "eyJrZXkxIjoidmFsdWUxIn0=";
		HashMap<String, Object> eventData = new HashMap<>();
		Map<String, String> linkageFields = new HashMap<>();
		linkageFields.put("key1", "value1");
		eventData.put(CampaignConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, linkageFields);

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY)
		.setEventData(eventData)
		.build();

		// test
		campaignExtension.handleLinkageFieldsEvent(testEvent);

		// verify
		String encodedLinkageFields = campaignExtension.getLinkageFields();
		assertEquals(expectedBase64EncodedLinkageFields, encodedLinkageFields);
		verify(mockCacheService, times(1)).remove(eq(CampaignConstants.RULES_CACHE_FOLDER), eq(""));
		verify(mockCampaignRulesDownloader, times(1)).loadRulesFromUrl(eq(expectedRulesDownloadUrl), eq(encodedLinkageFields));
	}

	@Test
	public void test_handleLinkageFieldsEvent_setValidLinkageFields_when_campaignStateNotReady() {
		// setup
		String expectedBase64EncodedLinkageFields = "eyJrZXkxIjoidmFsdWUxIn0=";
		HashMap<String, Object> eventData = new HashMap<>();
		Map<String, String> linkageFields = new HashMap<>();
		linkageFields.put("key1", "value1");
		eventData.put(CampaignConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, linkageFields);

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY)
				.setEventData(eventData)
				.build();

		// test
		campaignExtension.handleLinkageFieldsEvent(testEvent);

		// verify
		String encodedLinkageFields = campaignExtension.getLinkageFields();
		assertEquals(expectedBase64EncodedLinkageFields, encodedLinkageFields);
		verify(mockCacheService, times(1)).remove(eq(CampaignConstants.RULES_CACHE_FOLDER), eq(""));
		verify(mockCampaignRulesDownloader, times(0)).loadRulesFromUrl(anyString(), anyString());
	}


	@Test
	public void test_handleResetLinkageFields_happy() {
		// setup
		CampaignState campaignState = new CampaignState();
		campaignState.setState(getConfigurationEventData(new HashMap<>()), getIdentityEventData());
		campaignExtension = new CampaignExtension(mockExtensionApi, mockPersistentHitQueue, mockDataStoreService, mockRulesEngine, campaignState, mockCacheService, mockCampaignRulesDownloader);
		String expectedRulesDownloadUrl = "https://testMcias/testServer/testPropertyId/testExperienceCloudId/rules.zip";

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_RESET)
		.build();

		// test
		campaignExtension.handleLinkageFieldsEvent(testEvent);

		// verify
		String linkageFields = campaignExtension.getLinkageFields();
		assertEquals("", linkageFields);
		verify(mockRulesEngine, times(1)).replaceRules(eq(null));
		verify(mockCacheService, times(1)).remove(eq(CampaignConstants.RULES_CACHE_FOLDER), eq(""));
		verify(mockCampaignRulesDownloader, times(1)).loadRulesFromUrl(eq(expectedRulesDownloadUrl), eq(""));
	}

	@Test
	public void test_handleResetLinkageFields_when_linkageFieldsSetPreviously() {
		// setup
		CampaignState campaignState = new CampaignState();
		campaignState.setState(getConfigurationEventData(new HashMap<>()), getIdentityEventData());
		campaignExtension = new CampaignExtension(mockExtensionApi, mockPersistentHitQueue, mockDataStoreService, mockRulesEngine, campaignState, mockCacheService, mockCampaignRulesDownloader);

		String expectedRulesDownloadUrl = "https://testMcias/testServer/testPropertyId/testExperienceCloudId/rules.zip";
		String expectedBase64EncodedLinkageFields = "eyJrZXkxIjoidmFsdWUxIn0=";
		HashMap<String, Object> eventData = new HashMap<>();
		Map<String, String> linkageFields = new HashMap<>();
		linkageFields.put("key1", "value1");
		eventData.put(CampaignConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, linkageFields);

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY)
				.setEventData(eventData)
				.build();

		// test
		campaignExtension.handleLinkageFieldsEvent(testEvent);

		// verify linkage fields are set
		String encodedLinkageFields = campaignExtension.getLinkageFields();
		assertEquals(expectedBase64EncodedLinkageFields, encodedLinkageFields);
		verify(mockCacheService, times(1)).remove(eq(CampaignConstants.RULES_CACHE_FOLDER), eq(""));
		verify(mockCampaignRulesDownloader, times(1)).loadRulesFromUrl(eq(expectedRulesDownloadUrl), eq(encodedLinkageFields));

		// setup reset event
		testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_RESET)
				.build();

		// test
		campaignExtension.handleLinkageFieldsEvent(testEvent);

		// verify linkage fields reset
		String linkageFieldsString = campaignExtension.getLinkageFields();
		assertEquals("", linkageFieldsString);
		verify(mockRulesEngine, times(1)).replaceRules(eq(null));
		verify(mockCacheService, times(2)).remove(eq(CampaignConstants.RULES_CACHE_FOLDER), eq(""));
		verify(mockCampaignRulesDownloader, times(1)).loadRulesFromUrl(eq(expectedRulesDownloadUrl), eq(""));
	}

	// =================================================================================================================
	// void processConfigurationResponse(final Event event)
	// =================================================================================================================
	@Test
	public void test_processConfiguration_When_PrivacyOptedIn() {
		// setup
		CampaignState campaignState = new CampaignState();
		campaignState.setState(getConfigurationEventData(new HashMap<>()), getIdentityEventData());
		campaignExtension = new CampaignExtension(mockExtensionApi, mockPersistentHitQueue, mockDataStoreService, mockRulesEngine, campaignState, mockCacheService, mockCampaignRulesDownloader);
		String expectedRulesDownloadUrl = "https://testMcias/testServer/testPropertyId/testExperienceCloudId/rules.zip";

		HashMap<String, Object> configData = new HashMap<>();
		configData.put(CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");

		Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setEventData(configData)
		.build();

		// test
		campaignExtension.processConfigurationResponse(testEvent);

		// verify
		verify(mockPersistentHitQueue, times(1)).handlePrivacyChange(eq(MobilePrivacyStatus.OPT_IN));
		verify(mockCampaignRulesDownloader, times(1)).loadRulesFromUrl(eq(expectedRulesDownloadUrl), eq(null));
	}

	@Test
	public void test_processConfiguration_When_PrivacyOptOut() {
		// setup
		when(mockDataStoreService.getNamedCollection(anyString())).thenReturn(mockNamedCollection);
		CampaignState campaignState = new CampaignState();
		HashMap<String, Object> configData = new HashMap<>();
		configData.put(CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
		campaignState.setState(getConfigurationEventData(configData), getIdentityEventData());
		campaignExtension = new CampaignExtension(mockExtensionApi, mockPersistentHitQueue, mockDataStoreService, mockRulesEngine, campaignState, mockCacheService, mockCampaignRulesDownloader);

		Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
				.setEventData(configData)
				.build();

		// test
		campaignExtension.processConfigurationResponse(testEvent);

		// verify
		verify(mockPersistentHitQueue, times(1)).handlePrivacyChange(eq(MobilePrivacyStatus.OPT_OUT));
		String linkageFields = campaignExtension.getLinkageFields();
		assertEquals("", linkageFields);
		verify(mockRulesEngine, times(1)).replaceRules(eq(null));
		verify(mockCacheService, times(1)).remove(eq(CampaignConstants.RULES_CACHE_FOLDER), eq(""));
		verify(mockNamedCollection, times(1)).removeAll();
	}

	@Test
	public void test_processConfiguration_When_PrivacyUnknown() {
		// setup
		CampaignState campaignState = new CampaignState();
		HashMap<String, Object> configData = new HashMap<>();
		configData.put(CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "unknown");
		campaignState.setState(getConfigurationEventData(configData), getIdentityEventData());
		campaignExtension = new CampaignExtension(mockExtensionApi, mockPersistentHitQueue, mockDataStoreService, mockRulesEngine, campaignState, mockCacheService, mockCampaignRulesDownloader);

		Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
				.setEventData(configData)
				.build();

		// test
		campaignExtension.processConfigurationResponse(testEvent);

		// verify
		verify(mockPersistentHitQueue, times(1)).handlePrivacyChange(eq(MobilePrivacyStatus.UNKNOWN));
		verify(mockCampaignRulesDownloader, times(0)).loadRulesFromUrl(anyString(), anyString());
	}

//	// =================================================================================================================
//	// void processMessageInformation(final Event event, final CampaignState campaignState)
//	// =================================================================================================================
//
//	@Test
//	public void test_processMessageInformation_when_campaignConfigured_then_shouldProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();
//
//		final String url = String.format(CampaignConstants.CAMPAIGN_TRACKING_URL, campaignState.getCampaignServer(),
//										 "h2347", "bb65", "2", campaignState.getExperienceCloudId());
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertTrue("CampaignHitsDatabase queue should be called",
//				   campaignHitsDatabase.queueWasCalled);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.GET);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, "");
//		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
//	}
//
//	@Test
//	public void test_processMessageInformation_when_noEventData_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_emptyEventData_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(new EventData()).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_campaignNotConfigured_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(new EventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_NoIdentity_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), new EventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_emptyBroadlogId_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), new EventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("", "bb65", "2")).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_nullBroadlogId_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), new EventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData(null, "bb65", "2")).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_emptyDeliveryId_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), new EventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("h2347", "", "2")).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_nullDeliveryId_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), new EventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("h2347", null, "2")).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_emptyAction_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), new EventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "")).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_nullAction_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), new EventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", null)).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_PrivacyOptOut_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		EventData configData = getConfigurationEventData();
//		configData.putString(CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
//
//		campaignState.setState(configData, getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_PrivacyUnknown_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		EventData configData = getConfigurationEventData();
//		configData.putString(CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");
//
//		campaignState.setState(configData, getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processMessageInformation_when_NullNetworkService_then_shouldNotCrash() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
//				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();
//
//		platformServices.mockNetworkService = null;
//
//		// test
//		campaignExtension.processMessageInformation(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	// =================================================================================================================
//	// void processLifecycleUpdate(final Event event, final CampaignState campaignState)
//	// =================================================================================================================
//
//	@Test
//	public void test_processLifecycleUpdate_when_campaignConfigured_then_shouldQueueHit() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();
//
//		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
//							   "\",\"pushPlatform\":\"gcm\"}";
//		final String url = String.format(CampaignConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
//										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertTrue("CampaignHitsDatabase queue should be called",
//				   campaignHitsDatabase.queueWasCalled);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
//		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
//	}
//
//	@Test
//	public void test_processLifecycleUpdate_when_campaignNotConfigured_then_shouldNotQueueHit() throws Exception {
//		// setup
//		campaignState.setState(new EventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processLifecycleUpdate_when_NoMid_then_shouldNotQueueHit() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), new EventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processLifecycleUpdate_when_PrivacyOptOut_then_shouldNotQueueHit() throws Exception {
//		// setup
//		EventData configData = getConfigurationEventData();
//		configData.putString(CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
//
//		campaignState.setState(configData, getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processLifecycleUpdate_when_PrivacyUnknown_then_shouldNotQueueHit() throws Exception {
//		// setup
//		EventData configData = getConfigurationEventData();
//		configData.putString(CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");
//
//		campaignState.setState(configData, getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processLifecycleUpdate_when_NullNetworkService_then_shouldNotCrash() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();
//
//		platformServices.mockNetworkService = null;
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void
//	test_processLifecycleUpdate_when_ecidIsChanged_and_registrationIsPaused_then_shouldNotQueueHit()
//	throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();
//
//		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
//							   "\",\"pushPlatform\":\"gcm\"}";
//		final String url = String.format(CampaignConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
//										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertTrue("CampaignHitsDatabase queue should be called",
//				   campaignHitsDatabase.queueWasCalled);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
//		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
//
//		// setup for second part of test
//		// add values to datastore to simulate a previous successful campaign registration request
//		// and set registration paused status to true
//		campaignState.setState(updateConfigurationEventDataWithRegistrationPausedStatus(getConfigurationEventData(), true),
//							   getIdentityEventData());
//		final long timestamp = System.currentTimeMillis();
//		campaignHitsDatabase.updateTimestampInDataStore(timestamp);
//		campaignHitsDatabase.queueWasCalled = false;
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processLifecycleUpdate_when_ecidIsChanged_then_shouldQueueHit() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();
//
//		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
//							   "\",\"pushPlatform\":\"gcm\"}";
//		final String url = String.format(CampaignConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
//										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertTrue("CampaignHitsDatabase queue should be called",
//				   campaignHitsDatabase.queueWasCalled);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
//		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
//
//		// setup for second part of test
//		// add values to datastore to simulate a previous successful campaign registration request
//		final long timestamp = System.currentTimeMillis();
//		campaignHitsDatabase.updateTimestampInDataStore(timestamp);
//		campaignHitsDatabase.queueWasCalled = false;
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertTrue("CampaignHitsDatabase queue should be called",
//				   campaignHitsDatabase.queueWasCalled);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
//		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
//	}
//
//	@Test
//	public void test_processLifecycleUpdate_when_customRegistrationDelayProvided_then_shouldQueueHit_ifDelayElapsed()
//	throws Exception {
//		// setup
//		// set registration delay to 1 day
//		campaignState.setState(getConfigurationEventDataWithCustomRegistrationDelay(getConfigurationEventData(), 1),
//							   getIdentityEventData());
//		// set timestamp on event to 2 days from now
//		final long futureTimestamp = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(2));
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(futureTimestamp).build();
//
//		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
//							   "\",\"pushPlatform\":\"gcm\"}";
//		final String url = String.format(CampaignConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
//										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
//		// add values to datastore to simulate a previous successful campaign registration request
//		campaignHitsDatabase.updateTimestampInDataStore(System.currentTimeMillis());
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertTrue("CampaignHitsDatabase queue should be called",
//				   campaignHitsDatabase.queueWasCalled);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
//		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
//	}
//
//	@Test
//	public void
//	test_processLifecycleUpdate_when_customRegistrationDelayOfZeroProvided_then_shouldQueueHit() throws
//		Exception {
//		// setup
//		// set registration delay to 0 days (should send a registration request every launch event)
//		campaignState.setState(getConfigurationEventDataWithCustomRegistrationDelay(getConfigurationEventData(), 0),
//							   getIdentityEventData());
//		final long currentTimestamp = System.currentTimeMillis();
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(currentTimestamp).build();
//
//		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
//							   "\",\"pushPlatform\":\"gcm\"}";
//		final String url = String.format(CampaignConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
//										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
//		// add values to datastore to simulate a previous successful campaign registration request
//		campaignHitsDatabase.updateTimestampInDataStore(currentTimestamp);
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertTrue("CampaignHitsDatabase queue should be called",
//				   campaignHitsDatabase.queueWasCalled);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
//		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
//	}
//
//	@Test
//	public void
//	test_processLifecycleUpdate_when_customRegistrationDelayProvided_then_shouldNotQueueHit_ifDelayNotElapsed() throws
//		Exception {
//		// setup
//		// set registration delay to 30 days
//		campaignState.setState(getConfigurationEventDataWithCustomRegistrationDelay(getConfigurationEventData(), 30),
//							   getIdentityEventData());
//
//		// set timestamp on event to 25 days from now
//		final long futureTimestamp = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(25));
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(futureTimestamp).build();
//
//		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
//							   "\",\"pushPlatform\":\"gcm\"}";
//		final String url = String.format(CampaignConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
//										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
//		// add values to datastore to simulate a previous successful campaign registration request
//		campaignHitsDatabase.updateTimestampInDataStore(System.currentTimeMillis());
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "testExperienceCloudId");
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void test_processLifecycleUpdate_when_registrationPausedIsEqualToFalse_then_shouldQueueHit() throws
//		Exception {
//		// setup
//		// set registration paused to false
//		campaignState.setState(updateConfigurationEventDataWithRegistrationPausedStatus(getConfigurationEventData(), false),
//							   getIdentityEventData());
//
//		// set timestamp on event to 80 days from now
//		final long futureTimestamp = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(80));
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(futureTimestamp).build();
//
//		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
//							   "\",\"pushPlatform\":\"gcm\"}";
//		final String url = String.format(CampaignConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
//										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
//
//		// add values to datastore to simulate a previous successful campaign registration request
//		campaignHitsDatabase.updateTimestampInDataStore(System.currentTimeMillis());
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertTrue("CampaignHitsDatabase queue should be called",
//				   campaignHitsDatabase.queueWasCalled);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
//		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
//		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
//	}
//
//	@Test
//	public void test_processLifecycleUpdate_when_registrationPausedIsEqualToTrue_then_shouldNotQueueHit() throws
//		Exception {
//		// setup
//		// set registration paused to true
//		campaignState.setState(updateConfigurationEventDataWithRegistrationPausedStatus(getConfigurationEventData(), true),
//							   getIdentityEventData());
//
//		// set timestamp on event to 80 days from now
//		final long futureTimestamp = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(80));
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(futureTimestamp).build();
//
//		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
//							   "\",\"pushPlatform\":\"gcm\"}";
//		final String url = String.format(CampaignConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
//										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
//
//		// add values to datastore to simulate a previous successful campaign registration request
//		campaignHitsDatabase.updateTimestampInDataStore(System.currentTimeMillis());
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	@Test
//	public void
//	test_processLifecycleUpdate_when_customRegistrationDelayProvided_and_registrationPausedIsEqualToTrue_then_shouldNotQueueHit()
//	throws Exception {
//		// setup
//		// set registration delay to 1 day
//		campaignState.setState(updateConfigurationEventDataWithCustomRegistrationDelayAndPauseStatus(
//								   getConfigurationEventData(),
//								   1, true), getIdentityEventData());
//		// set timestamp on event to 2 days from now
//		final long futureTimestamp = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(2));
//		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
//				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(futureTimestamp).build();
//
//		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
//							   "\",\"pushPlatform\":\"gcm\"}";
//		final String url = String.format(CampaignConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
//										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
//		// add values to datastore to simulate a previous successful campaign registration request
//		campaignHitsDatabase.updateTimestampInDataStore(System.currentTimeMillis());
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");
//
//		// test
//		campaignExtension.processLifecycleUpdate(testEvent, campaignState);
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertFalse("CampaignHitsDatabase queue should not be called",
//					campaignHitsDatabase.queueWasCalled);
//	}
//
//	// =================================================================================================================
//	// void triggerRulesDownload(final Event event, final CampaignState campaignState)
//	// =================================================================================================================
//
//	@Test
//	public void test_triggerRulesDownload__when_campaignConfigured_then_shouldTriggerRulesDownload() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION,
//				EventSource.RESPONSE_CONTENT).build();
//
//		final String url = String.format(CampaignConstants.CAMPAIGN_RULES_DOWNLOAD_URL, campaignState.getCampaignMcias(),
//										 campaignState.getCampaignServer(), campaignState.getPropertyId(), campaignState.getExperienceCloudId());
//
//		// test
//		campaignExtension.triggerRulesDownload(testEvent, campaignState);
//
//		waitForExecutor(campaignExtension.getExecutor(), 1);
//
//		// verify
//		assertTrue("Connect Url should be called to send rules download request.", mockNetworkService.connectUrlWasCalled);
//		assertEquals(mockNetworkService.connectUrlParametersUrl, url);
//		assertEquals(mockNetworkService.connectUrlParametersCommand, NetworkService.HttpCommand.GET);
//	}
//
//	@Test
//	public void test_triggerRulesDownload_when_campaignNotConfigured_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(new EventData(), getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION,
//				EventSource.RESPONSE_CONTENT).build();
//
//		// test
//		campaignExtension.triggerRulesDownload(testEvent, campaignState);
//
//		// verify
//		assertFalse("Connect Url should not be called.", mockNetworkService.connectUrlWasCalled);
//	}
//
//	@Test
//	public void test_triggerRulesDownload_when_NoMid_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		campaignState.setState(getConfigurationEventData(), new EventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION,
//				EventSource.RESPONSE_CONTENT).build();
//
//		// test
//		campaignExtension.triggerRulesDownload(testEvent, campaignState);
//
//		// verify
//		assertFalse("Connect Url should not be called.", mockNetworkService.connectUrlWasCalled);
//	}
//
//	@Test
//	public void test_triggerRulesDownload_when_PrivacyOptOut_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		EventData configData = getConfigurationEventData();
//		configData.putString(CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
//
//		campaignState.setState(configData, getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION,
//				EventSource.RESPONSE_CONTENT).build();
//
//		// test
//		campaignExtension.triggerRulesDownload(testEvent, campaignState);
//
//		// verify
//		assertFalse("Connect Url should not be called.", mockNetworkService.connectUrlWasCalled);
//	}
//
//	@Test
//	public void test_triggerRulesDownload_when_PrivacyUnknown_then_shouldNotProcessRequest() throws Exception {
//		// setup
//		EventData configData = getConfigurationEventData();
//		configData.putString(CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");
//
//		campaignState.setState(configData, getIdentityEventData());
//		final Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION,
//				EventSource.RESPONSE_CONTENT).build();
//
//		// test
//		campaignExtension.triggerRulesDownload(testEvent, campaignState);
//
//		// verify
//		assertFalse("Connect Url should not be called.", mockNetworkService.connectUrlWasCalled);
//	}
//
//	// =================================================================================================================
//	// void dispatchMessageInteraction(final Map<String, String> messageData)
//	// =================================================================================================================
//
//	@Test
//	public void test_dispatchMessageInteraction_happy() throws Exception {
//		// setup
//		final Map<String, String> testMessageData = new HashMap<String, String>();
//		testMessageData.put(CampaignConstants.ContextDataKeys.MESSAGE_ID, "testMessageId");
//		testMessageData.put(CampaignConstants.ContextDataKeys.MESSAGE_VIEWED, "1");
//
//		campaignExtension.campaignEventDispatcher = campaignDispatcher;
//
//		// test
//		campaignExtension.dispatchMessageInteraction(testMessageData);
//
//		// verify
//		assertTrue(campaignDispatcher.dispatchWasCalled);
//		assertEquals(campaignDispatcher.dispatchParameterMessageData, testMessageData);
//	}
//
//	@Test
//	public void test_dispatchMessageInteraction_whenNullDispatcher_then_shouldNotDispatch() throws Exception {
//		// setup
//		final Map<String, String> testMessageData = new HashMap<String, String>();
//		testMessageData.put(CampaignConstants.ContextDataKeys.MESSAGE_ID, "testMessageId");
//		testMessageData.put(CampaignConstants.ContextDataKeys.MESSAGE_VIEWED, "1");
//
//		campaignExtension.campaignEventDispatcher = null;
//
//		// test
//		campaignExtension.dispatchMessageInteraction(testMessageData);
//
//		// verify
//		assertFalse(campaignDispatcher.dispatchWasCalled);
//	}
//
//	// =================================================================================================================
//	// void dispatchMessageInfo(final String broadlogId, final String deliveryId, final String action)
//	// =================================================================================================================
//
//	@Test
//	public void test_dispatchMessageInfo_happy() throws Exception {
//		// setup
//		final String broadlogId = "h87a1";
//		final String deliveryId = "22dc";
//		final String action = "7";
//		campaignExtension.genericDataOSEventDispatcher = genericDataOSEventDispatcher;
//
//		// test
//		campaignExtension.dispatchMessageInfo(broadlogId, deliveryId, action);
//
//		// verify
//		assertTrue(genericDataOSEventDispatcher.dispatchWasCalled);
//		assertEquals(genericDataOSEventDispatcher.dispatchParameterBroadlogId, broadlogId);
//		assertEquals(genericDataOSEventDispatcher.dispatchParameterDeliveryId, deliveryId);
//		assertEquals(genericDataOSEventDispatcher.dispatchParameterAction, action);
//	}
//
//	@Test
//	public void test_dispatchMessageInfo_whenNullDispatcher_then_shouldNotDispatch() throws Exception {
//		// setup
//		final String broadlogId = "h87a1";
//		final String deliveryId = "22dc";
//		final String action = "7";
//		campaignExtension.genericDataOSEventDispatcher = null;
//
//		// test
//		campaignExtension.dispatchMessageInfo(broadlogId, deliveryId, action);
//
//		// verify
//		assertFalse(campaignDispatcher.dispatchWasCalled);
//	}
//
//	// =================================================================================================================
//	// void clearCachedAssetsForMessagesNotInList(final List<String> activeMessageIds)
//	// =================================================================================================================
//
//	@Test
//	public void test_clearCachedMessageAssets_When_EmptyList_Then_RemoveAllAssets() throws Exception {
//		// setup
//		setupCachedMessageAssets(fakeMessageId1, fakeMessageId2);
//
//		final String response = "abcd";
//		final int responseCode = 200;
//		final String responseMessage = "";
//		final HashMap<String, String> responseProperties = new HashMap<String, String>();
//		responseProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");
//
//		mockNetworkService.connectUrlAsyncCallbackParametersConnection =
//			new MockConnection(response, responseCode, responseMessage, responseProperties);
//
//		// pre-verify
//		final CacheManager manager = new CacheManager(mockSystemInfoService);
//		final File cachedFile = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
//								messageCacheDirString1, false);
//		assertNotNull("cachedFile should exist", cachedFile);
//		final File cachedFile2 = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
//								 messageCacheDirString2, false);
//		assertNotNull("cachedFile2 should exist", cachedFile2);
//
//		// execute
//		campaignExtension.clearCachedAssetsForMessagesNotInList(new ArrayList<String>());
//
//		// verify
//		final File cachedFile3 = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
//								 messageCacheDirString1, false);
//		assertNull("cachedFile3 should not exist", cachedFile3);
//		final File cachedFile4 = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
//								 messageCacheDirString2, false);
//		assertNull("cachedFile4 should not exist", cachedFile4);
//	}
//
//	@Test
//	public void test_clearCachedMessageAssets_When_MessageIdsInList_Then_RemoveAllAssetsNotInList() throws Exception {
//		// setup
//		final ArrayList<String> messageIds = new ArrayList<String>();
//		messageIds.add(fakeMessageId1);
//
//		setupCachedMessageAssets(fakeMessageId1, fakeMessageId2);
//
//		final String response = "abcd";
//		final int responseCode = 200;
//		final String responseMessage = "";
//		final HashMap<String, String> responseProperties = new HashMap<String, String>();
//		responseProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");
//
//		mockNetworkService.connectUrlAsyncCallbackParametersConnection =
//			new MockConnection(response, responseCode, responseMessage, responseProperties);
//
//		// execute
//		campaignExtension.clearCachedAssetsForMessagesNotInList(messageIds);
//
//		// verify
//		final CacheManager manager = new CacheManager(mockSystemInfoService);
//		final File cachedFile = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
//								messageCacheDirString1, false);
//		assertNotNull("Cached file for active message should still exist", cachedFile);
//		final File cachedFile2 = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
//								 messageCacheDirString2, false);
//		assertNull("Cached file for inactive message should be removed", cachedFile2);
//	}
//
//	// =================================================================================================================
//	// void clearRulesCacheDirectory()
//	// =================================================================================================================
//
//	@Test
//	public void test_clearRulesCacheDirectory_happy() throws Exception {
//		// setup
//		setupCachedRules();
//
//		final String response = "";
//		final int responseCode = 200;
//		final String responseMessage = "";
//		final HashMap<String, String> responseProperties = new HashMap<String, String>();
//		responseProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");
//
//		mockNetworkService.connectUrlReturnValue = ((NetworkService.HttpConnection)new MockConnection(response, responseCode,
//				responseMessage, responseProperties));
//
//		// pre-verify
//		final CacheManager manager = new CacheManager(mockSystemInfoService);
//		final File cachedFile = manager.getFileForCachedURL(
//									"https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip",
//									rulesCacheDirString, false);
//		assertNotNull("cachedFile should exist", cachedFile);
//
//		// execute
//		campaignExtension.clearRulesCacheDirectory();
//
//		// verify
//		final File cachedFile2 = manager.getFileForCachedURL(
//									 "https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip",
//									 rulesCacheDirString, false);
//		assertNull("cachedFile1 should not exist", cachedFile2);
//	}
//
//	// =================================================================================================================
//	// void loadCachedMessages()
//	// =================================================================================================================
//
//	@Test
//	public void test_loadCachedMessages_happy() throws Exception {
//		// setup
//		final File cacheFile = setupCachedRules();
//		final String rulesUrl =
//			"https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip";
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY, rulesUrl);
//
//		final MockCampaignRulesRemoteDownloader mockCampaignRulesRemoteDownloader = getMockCampaignRulesRemoteDownloader(
//					rulesUrl, rulesCacheDirString);
//		assertNotNull(mockCampaignRulesRemoteDownloader);
//
//		campaignExtension.getCampaignRulesRemoteDownloaderReturnValue = mockCampaignRulesRemoteDownloader;
//
//		// test
//		campaignExtension.loadCachedMessages();
//
//		// verify
//		assertTrue(mockCampaignRulesRemoteDownloader.getCachePathWasCalled);
//		assertNotNull(mockCampaignRulesRemoteDownloader.getCachePathReturnValue);
//		assertEquals(mockCampaignRulesRemoteDownloader.getCachePathReturnValue.getPath(), cacheFile.getPath());
//	}
//
//	@Test
//	public void test_loadCachedMessages_When_DataStoreIsEmpty_Then_ShouldNotCallGetCachePath() throws Exception {
//		// setup
//		setupCachedRules();
//		final String rulesUrl =
//			"https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip";
//		campaignDataStore.removeAll();
//
//		final MockCampaignRulesRemoteDownloader mockCampaignRulesRemoteDownloader = getMockCampaignRulesRemoteDownloader(
//					rulesUrl, rulesCacheDirString);
//		assertNotNull(mockCampaignRulesRemoteDownloader);
//
//		campaignExtension.getCampaignRulesRemoteDownloaderReturnValue = mockCampaignRulesRemoteDownloader;
//
//		// test
//		campaignExtension.loadCachedMessages();
//
//		// verify
//		assertFalse(mockCampaignRulesRemoteDownloader.getCachePathWasCalled);
//	}
//
//	@Test
//	public void test_loadCachedMessages_When_DataStoreHasEmptyRulesRemoteUrl_Then_ShouldNotCallGetCachePath() throws
//		Exception {
//		// setup
//		setupCachedRules();
//		final String rulesUrl =
//			"https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip";
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY, "");
//
//		final MockCampaignRulesRemoteDownloader mockCampaignRulesRemoteDownloader = getMockCampaignRulesRemoteDownloader(
//					rulesUrl, rulesCacheDirString);
//		assertNotNull(mockCampaignRulesRemoteDownloader);
//
//		campaignExtension.getCampaignRulesRemoteDownloaderReturnValue = mockCampaignRulesRemoteDownloader;
//
//		// test
//		campaignExtension.loadCachedMessages();
//
//		// verify
//		assertFalse(mockCampaignRulesRemoteDownloader.getCachePathWasCalled);
//	}
//
//	@Test
//	public void test_loadCachedMessages_When_NoCachedRulesForUrl_Then_ShouldGetNullCachePath() throws Exception {
//		// setup
//		setupCachedRules();
//		final String rulesUrl = "http://mock.com/rules.zip";
//		campaignDataStore.setString(CampaignConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY, rulesUrl);
//
//		final MockCampaignRulesRemoteDownloader mockCampaignRulesRemoteDownloader = getMockCampaignRulesRemoteDownloader(
//					rulesUrl, rulesCacheDirString);
//		assertNotNull(mockCampaignRulesRemoteDownloader);
//
//		campaignExtension.getCampaignRulesRemoteDownloaderReturnValue = mockCampaignRulesRemoteDownloader;
//
//		// test
//		campaignExtension.loadCachedMessages();
//
//		// verify
//		assertTrue(mockCampaignRulesRemoteDownloader.getCachePathWasCalled);
//		assertNull(mockCampaignRulesRemoteDownloader.getCachePathReturnValue);
//	}
//
//	// =================================================================================================================
//	// CampaignRulesRemoteDownloader getCampaignRulesRemoteDownloader(final String url,
//	//																  final Map<String, String> requestProperties)
//	// =================================================================================================================
//
//	@Test
//	public void test_getCampaignRulesRemoteDownloader_happy() throws Exception {
//		// setup
//		final String rulesUrl = "http://mock.com/rules.zip";
//
//		// test
//		CampaignRulesRemoteDownloader remoteDownloader = campaignExtension.getCampaignRulesRemoteDownloader(rulesUrl,
//				new HashMap<String, String>());
//
//		// verify
//		assertNotNull(remoteDownloader);
//	}
//
//	@Test
//	public void test_getCampaignRulesRemoteDownloader_When_NullPlatformServices_Then_ShouldReturnNull() throws Exception {
//		// setup
//		final String rulesUrl = "http://mock.com/rules.zip";
//		campaignExtension = new MockCampaignExtension(eventHub, null, campaignHitsDatabase);
//
//		// test
//		CampaignRulesRemoteDownloader remoteDownloader = campaignExtension.getCampaignRulesRemoteDownloader(rulesUrl,
//				new HashMap<String, String>());
//
//		// verify
//		assertNull(remoteDownloader);
//	}
//
//	// =================================================================================================================
//	// Test function dispatchMessageEvent, which gets called on Generic data OS events.
//	// =================================================================================================================
//
//	@Test
//	public void testDispatchMessageEventWithActionViewed() {
//
//		final String hexValueOfMessageId = "bbc6";
//		final String decimalValueOfMessageId = "48070";
//		campaignExtension.dispatchMessageEvent("1", hexValueOfMessageId);
//		Assert.assertTrue(eventHub.isDispatchedCalled);
//		Assert.assertEquals(eventHub.dispatchedEventList.size(), 1);
//		final Event event = eventHub.dispatchedEventList.get(0);
//
//		try {
//			Assert.assertEquals(event.getData().getString2(CampaignConstants.ContextDataKeys.MESSAGE_ID), decimalValueOfMessageId);
//			Assert.assertEquals(event.getData().getString2(CampaignConstants.ContextDataKeys.MESSAGE_VIEWED), "1");
//		} catch (ObjectException e) {
//			e.printStackTrace();
//			Assert.fail();
//		}
//	}
//
//	@Test
//	public void testDispatchMessageEventWithActionClicked() {
//
//		final String hexValueOfMessageId = "bbc6";
//		final String decimalValueOfMessageId = "48070";
//		campaignExtension.dispatchMessageEvent("2", hexValueOfMessageId);
//		Assert.assertTrue(eventHub.isDispatchedCalled);
//		Assert.assertEquals(eventHub.dispatchedEventList.size(), 1);
//		final Event event = eventHub.dispatchedEventList.get(0);
//
//		try {
//			Assert.assertEquals(event.getData().getString2(CampaignConstants.ContextDataKeys.MESSAGE_ID), decimalValueOfMessageId);
//			Assert.assertEquals(event.getData().getString2(CampaignConstants.ContextDataKeys.MESSAGE_CLICKED), "1");
//		} catch (ObjectException e) {
//			e.printStackTrace();
//			Assert.fail();
//		}
//
//	}
//
//	@Test
//	public void testDispatchMessageEventWithActionImpression() {
//
//		final String hexValueOfMessageId = "bbc6";
//		campaignExtension.dispatchMessageEvent("7", hexValueOfMessageId);
//		Assert.assertFalse(eventHub.isDispatchedCalled);
//	}
}
