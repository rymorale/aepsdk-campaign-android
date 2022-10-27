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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class CampaignExtensionTests extends BaseTest {

	private MockCampaignExtension testExtension;
	private CampaignState campaignState;
	private MockCampaignDispatcherCampaignResponseContent campaignDispatcher;
	private MockCampaignDispatcherGenericDataOS genericDataOSEventDispatcher;
	private MockCampaignHitsDatabase campaignHitsDatabase;
	private MockNetworkService mockNetworkService;
	private MockSystemInfoService mockSystemInfoService;
	private FakeJsonUtilityService fakeJsonUtilityService;
	private FakeEncodingService fakeEncodingService;
	private FakeLocalStorageService fakeLocalStorageService;
	private MockCompressedFileService mockCompressedFileService;
	private FakeDataStore campaignDataStore;
	private FakeUIService mockUIService;
	private File cacheDir;

	private String messageCacheDirString1;
	private String messageCacheDirString2;

	private String rulesCacheDirString;

	private static final String fakeMessageId1 = "d38a46f6-4f43-435a-a862-4038c27b90a1";
	private static final String fakeMessageId2 = "e38a46f6-4f43-435a-a862-4038c27b90a2";

	@Before()
	public void setup() {
		super.beforeEach();

		mockSystemInfoService = platformServices.getMockSystemInfoService();
		cacheDir = new File("cache");
		cacheDir.mkdirs();
		cacheDir.setWritable(true);
		mockSystemInfoService.applicationCacheDir = cacheDir;

		messageCacheDirString1 = CampaignTestConstants.MESSAGE_CACHE_DIR + File.separator + fakeMessageId1;
		messageCacheDirString2 = CampaignTestConstants.MESSAGE_CACHE_DIR + File.separator + fakeMessageId2;

		rulesCacheDirString = CampaignTestConstants.RULES_CACHE_FOLDER;

		mockNetworkService = platformServices.getMockNetworkService();

		mockUIService = platformServices.getMockUIService();

		fakeLocalStorageService = (FakeLocalStorageService) platformServices.getLocalStorageService();

		campaignDataStore = (FakeDataStore) fakeLocalStorageService.getDataStore(
								CampaignTestConstants.CAMPAIGN_DATA_STORE_NAME);

		fakeJsonUtilityService = (FakeJsonUtilityService) platformServices.getJsonUtilityService();
		fakeEncodingService = (FakeEncodingService) platformServices.getEncodingService();

		campaignState = new CampaignState();

		mockCompressedFileService = (MockCompressedFileService) platformServices.getCompressedFileService();

		campaignDispatcher = new MockCampaignDispatcherCampaignResponseContent(eventHub, testExtension);
		genericDataOSEventDispatcher = new MockCampaignDispatcherGenericDataOS(eventHub, testExtension);

		campaignHitsDatabase = getCampaignHitsDatabase(platformServices);

		testExtension = new MockCampaignExtension(eventHub, platformServices, campaignHitsDatabase);
	}

	@After
	public void teardown() {
		super.afterEach();
		clearCacheFiles(cacheDir);
	}

	private EventData getConfigurationEventData() {
		final EventData configData = new EventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "testServer");
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_PKEY_KEY, "testPkey");
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_MCIAS_KEY, "testMcias");
		configData.putInteger(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_TIMEOUT, 10);
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.PROPERTY_ID, "testPropertyId");
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");

		return configData;
	}

	private EventData getConfigurationEventDataWithCustomRegistrationDelay(final EventData configState,
			final int registrationDelay) {
		configState.putInteger(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_DELAY_KEY,
							   registrationDelay);

		return configState;
	}

	private EventData updateConfigurationEventDataWithRegistrationPausedStatus(final EventData configState,
			final boolean registrationPaused) {
		configState.putBoolean(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_PAUSED_KEY,
							   registrationPaused);

		return configState;
	}

	private EventData updateConfigurationEventDataWithCustomRegistrationDelayAndPauseStatus(final EventData configState,
			final int registrationDelay, final boolean registrationPaused) {
		configState.putInteger(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_DELAY_KEY,
							   registrationDelay);
		configState.putBoolean(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_PAUSED_KEY,
							   registrationPaused);

		return configState;
	}

	private EventData getIdentityEventData() {
		final EventData identityData = new EventData();
		identityData.putString(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "testExperienceCloudId");

		return identityData;
	}

	private EventData getLifecycleEventData() {
		Map<String, String> lifecycleMap = new HashMap<String, String>();
		lifecycleMap.put(CampaignTestConstants.EventDataKeys.Lifecycle.LAUNCH_EVENT, "LaunchEvent");

		final EventData lifecycleData = new EventData();
		lifecycleData.putStringMap(CampaignTestConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA, lifecycleMap);
		return lifecycleData;
	}

	private EventData getMessageTrackEventData(final String broadlogId, final String deliveryId, final String action) {
		final EventData trackData = new EventData();
		trackData.putString(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID, broadlogId);
		trackData.putString(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID, deliveryId);
		trackData.putString(CampaignTestConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION, action);

		return trackData;
	}

	private EventData getMessageConsequenceEventData(final CampaignRuleConsequence consequence) {
		final EventData triggeredConsequenceData = new EventData();

		try {
			triggeredConsequenceData.putTypedObject(CampaignTestConstants.EventDataKeys.RuleEngine.CONSEQUENCE_TRIGGERED,
													consequence, new CampaignRuleConsequenceSerializer());
		} catch (VariantException ex) {
		}

		return triggeredConsequenceData;
	}

	private Event getLifecycleEvent(final EventData eventData) {
		return new Event.Builder("TEST", EventType.LIFECYCLE, EventSource.REQUEST_CONTENT)
			   .setData(eventData).build();
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

	private MockCampaignRulesRemoteDownloader getMockCampaignRulesRemoteDownloader(final String url,
			final String directoryOverride) {
		try {
			return new MockCampaignRulesRemoteDownloader(mockNetworkService, mockSystemInfoService, mockCompressedFileService,
					url, directoryOverride);
		} catch (Exception e) {
			fail("Could not create the CampaignRulesRemoteDownloader instance (%s)." + e);
		}

		return null;
	}

	private MockCampaignHitsDatabase getCampaignHitsDatabase(final PlatformServices services) {
		try {
			return new MockCampaignHitsDatabase(services);
		} catch (MissingPlatformServicesException e) {
			return null;
		}
	}

	// =================================================================================================================
	// void processMessageEvent(final Event event)
	// =================================================================================================================

	@Test
	public void test_processMessageEvent_when_validEventForLocalNotification_happy() throws Exception {
		// setup
		CampaignRuleConsequence consequence = new CampaignRuleConsequence("testId", "iam", "",
		new HashMap<String, Variant>() {
			{
				put("template", Variant.fromString("local"));
				put("content", Variant.fromString("messageContent"));
			}
		});

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_CONTENT)
		.setData(getMessageConsequenceEventData(consequence))
		.build();

		mockUIService.isMessageDisplayedReturnValue = false;

		// test
		testExtension.processMessageEvent(testEvent);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue(mockUIService.showLocalNotificationWasCalled);
		assertEquals(mockUIService.showLocalNotificationContent, "messageContent");
		assertEquals(mockUIService.showLocalNotificationDelaySeconds, 0);
		assertEquals(mockUIService.showLocalNotificationFireDate, 0);
		assertEquals(mockUIService.showLocalNotificationDeeplink, null);
		assertEquals(mockUIService.showLocalNotificationSound, null);
		assertEquals(mockUIService.showLocalNotificationUserInfo, (Map<String, Object>) null);
	}

	@Test
	public void test_processMessageEvent_when_validEventForAlert_happy() throws Exception {
		// setup
		CampaignRuleConsequence consequence = new CampaignRuleConsequence("testId", "iam", "",
		new HashMap<String, Variant>() {
			{
				put("template", Variant.fromString("alert"));
				put("title", Variant.fromString("messageTitle"));
				put("content", Variant.fromString("messageContent"));
				put("cancel", Variant.fromString("No"));
			}
		});

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_CONTENT)
		.setData(getMessageConsequenceEventData(consequence))
		.build();

		mockUIService.isMessageDisplayedReturnValue = false;

		// test
		testExtension.processMessageEvent(testEvent);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue(mockUIService.showAlertWasCalled);
		assertEquals(mockUIService.showAlertTitle, "messageTitle");
		assertEquals(mockUIService.showAlertMessage, "messageContent");
		assertEquals(mockUIService.showAlertNegativeButtonText, "No");
		assertEquals(mockUIService.showAlertPositiveButtonText, null);
	}

	@Test
	public void test_processMessageEvent_when_validEventForFullscreen_happy() throws Exception {
		// setup
		File assetFile = getResource("happy_test.html");
		CampaignRuleConsequence consequence = new CampaignRuleConsequence("testId", "iam", assetFile.getParent(),
		new HashMap<String, Variant>() {
			{
				put("template", Variant.fromString("fullscreen"));
				put("html", Variant.fromString("happy_test.html"));
			}
		});

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_CONTENT)
		.setData(getMessageConsequenceEventData(consequence))
		.build();

		mockUIService.isMessageDisplayedReturnValue = false;
		MockUIFullScreenMessageUI mockUIFullScreenMessage = new MockUIFullScreenMessageUI();
		mockUIService.createUIFullScreenMessageReturn = mockUIFullScreenMessage;

		// test
		testExtension.processMessageEvent(testEvent);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue(mockUIService.createFullscreenMessageWasCalled);
		assertEquals(mockUIService.createFullscreenMessageHtml,
					 "<!DOCTYPE html>\n<html>\n<head><meta charset=\"UTF-8\"></head>\n<body>\neverything is awesome http://asset1-url00.jpeg\n</body>\n</html>");
		assertTrue(mockUIFullScreenMessage.showCalled);
	}

	@Test
	public void test_processMessageEvent_when_validNullConsequenceInEvent_then_shouldNotShowMessage() throws Exception {
		// setup
		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_CONTENT)
		.setData(getMessageConsequenceEventData(null))
		.build();

		mockUIService.isMessageDisplayedReturnValue = false;

		// test
		testExtension.processMessageEvent(testEvent);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertFalse(mockUIService.showLocalNotificationWasCalled);
	}

	@Test
	public void test_processMessageEvent_when_anotherMessageIsShowing_then_shouldNotShowMessage() throws Exception {
		// setup
		CampaignRuleConsequence consequence = new CampaignRuleConsequence("testId", "iam", "", new HashMap<String, Variant>() {
			{
				put("template", Variant.fromString("local"));
				put("content", Variant.fromString("messageContent"));
			}
		});

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_CONTENT)
		.setData(getMessageConsequenceEventData(consequence))
		.build();

		mockUIService.isMessageDisplayedReturnValue = true;

		// test
		testExtension.processMessageEvent(testEvent);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertFalse(mockUIService.showLocalNotificationWasCalled);
	}

	// =================================================================================================================
	// void processSharedStateUpdate(final String stateOwner)
	// =================================================================================================================

	@Test
	public void test_processSharedStateUpdate_when_nullStateOwner_then_shouldDoNothing() throws Exception {
		// test
		testExtension.processSharedStateUpdate(null);

		// verify
		assertFalse("Process Queued Events should not be called", testExtension.processQueuedEventsWasCalled);
	}

	@Test
	public void test_processSharedStateUpdate_when_emptyStateOwner_then_shouldDoNothing() throws Exception {
		// test
		testExtension.processSharedStateUpdate("");

		// verify
		assertFalse("Process Queued Events should not be called", testExtension.processQueuedEventsWasCalled);
	}

	@Test
	public void test_processSharedStateUpdate_when_invalidStateOwner_then_shouldDoNothing() throws Exception {
		// test
		testExtension.processSharedStateUpdate("blah");

		// verify
		assertFalse("Process Queued Events should not be called", testExtension.processQueuedEventsWasCalled);
	}

	@Test
	public void test_processSharedStateUpdate_when_configurationStateOwner_then_shouldProcessQueuedEvents() throws
		Exception {
		// test
		testExtension.processSharedStateUpdate(CampaignTestConstants.EventDataKeys.Configuration.EXTENSION_NAME);

		waitForExecutor(testExtension.getExecutor(), 1);
		// verify
		assertTrue("Process Queued Events should be called", testExtension.processQueuedEventsWasCalled);
	}

	@Test
	public void test_processSharedStateUpdate_when_identityStateOwner_then_shouldProcessQueuedEvents() throws Exception {
		// test
		testExtension.processSharedStateUpdate(CampaignTestConstants.EventDataKeys.Identity.EXTENSION_NAME);

		waitForExecutor(testExtension.getExecutor(), 1);
		// verify
		assertTrue("Process Queued Events should be called", testExtension.processQueuedEventsWasCalled);
	}


	// =================================================================================================================
	// void handleSetLinkageFields(final Event event, final Map<String, String> linkageFields)
	// =================================================================================================================
	@Test
	public void test_handleSetLinkageFields_When_NullJsonUtilityService() throws Exception {

		//setup
		platformServices.fakeJsonUtilityService = null;
		EventData eventData = new EventData();
		final Map<String, String> linkageFields = new HashMap<String, String>();
		linkageFields.put("key1", "value1");
		eventData.putStringMap(CampaignTestConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, linkageFields);

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY)
		.setData(eventData)
		.build();

		// test
		testExtension.handleSetLinkageFields(testEvent, linkageFields);

		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertEquals("The handler should not have queued the event", 0, testExtension.waitingEvents.size());
		assertEquals("The handler should not have queued the correct event", null, testExtension.waitingEvents.peek());
		assertFalse("Process Queued Events should not be called", testExtension.processQueuedEventsWasCalled);
		assertEquals("The handler should not have changed the linkage fields", null, testExtension.getLinkageFields());
	}

	@Test
	public void test_handleSetLinkageFields_When_NullEncodingService() throws Exception {

		//setup
		platformServices.fakeEncodingService = null;
		EventData eventData = new EventData();
		final Map<String, String> linkageFields = new HashMap<String, String>();
		linkageFields.put("key1", "value1");
		eventData.putStringMap(CampaignTestConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, linkageFields);

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY)
		.setData(eventData)
		.build();

		// test
		testExtension.handleSetLinkageFields(testEvent, linkageFields);

		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertEquals("The handler should not have queued the event", 0, testExtension.waitingEvents.size());
		assertEquals("The handler should not have queued the correct event", null, testExtension.waitingEvents.peek());
		assertFalse("Process Queued Events should not be called", testExtension.processQueuedEventsWasCalled);
		assertEquals("The handler should not have changed the linkage fields", null, testExtension.getLinkageFields());
	}

	@Test
	public void test_handleSetLinkageFields_Happy() throws Exception {

		EventData eventData = new EventData();
		final Map<String, String> linkageFields = new HashMap<String, String>();
		linkageFields.put("key1", "value1");
		eventData.putStringMap(CampaignTestConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, linkageFields);

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY)
		.setData(eventData)
		.build();

		// test
		testExtension.handleSetLinkageFields(testEvent, linkageFields);

		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertEquals("The handler should've queued the event", 1, testExtension.waitingEvents.size());
		assertEquals("The handler should've queued the correct event", testEvent, testExtension.waitingEvents.peek());

		//This assertion allows us to see how what linkage fields look like before and after going through setLinkageFields
		assertEquals("The handler should've stored linkageFields", "eyJrZXkxIjoidmFsdWUxIn0=",
					 testExtension.getLinkageFields());

		assertTrue("Process Queued Events should be called", testExtension.processQueuedEventsWasCalled);
	}


	// =================================================================================================================
	// void handleResetLinkageFields(final Event event)
	// =================================================================================================================

	@Test
	public void test_handleResetLinkageFields_happy() throws Exception {

		//setup
		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_RESET)
		.build();

		// test
		testExtension.handleResetLinkageFields(testEvent);

		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertEquals("The handler should've clear the stored linkageFields", "", testExtension.getLinkageFields());
		assertTrue("The handler should've called the clearRulesCacheDirectory method",
				   testExtension.clearRulesCacheDirectoryWasCalled);

		assertEquals("The handler should've queued the event", 1, testExtension.waitingEvents.size());
		assertEquals("The handler should've queued the correct event", testEvent, testExtension.waitingEvents.peek());
		assertTrue("Process Queued Events should be called", testExtension.processQueuedEventsWasCalled);

	}

	@Test
	public void test_handleResetLinkageFields_when_linkageFieldsSetPreviously() throws Exception {

		//setup
		EventData eventData = new EventData();
		final Map<String, String> linkageFields = new HashMap<String, String>();
		linkageFields.put("key1", "value1");
		eventData.putStringMap(CampaignTestConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, linkageFields);

		Event testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY)
		.setData(eventData)
		.build();

		testExtension.handleSetLinkageFields(testEvent, linkageFields);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify linkage fields are set
		assertEquals("The handler should've stored linkageFields", "eyJrZXkxIjoidmFsdWUxIn0=",
					 testExtension.getLinkageFields());
		testExtension.waitingEvents.clear();

		testEvent = new Event.Builder("Test event", EventType.CAMPAIGN, EventSource.REQUEST_RESET)
		.build();

		// test
		testExtension.handleResetLinkageFields(testEvent);

		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertEquals("The handler should've clear the stored linkageFields", "", testExtension.getLinkageFields());
		assertTrue("The handler should've called the clearRulesCacheDirectory method",
				   testExtension.clearRulesCacheDirectoryWasCalled);

		assertEquals("The handler should've queued the event", 1, testExtension.waitingEvents.size());
		assertEquals("The handler should've queued the correct event", testEvent, testExtension.waitingEvents.peek());
		assertTrue("Process Queued Events should be called", testExtension.processQueuedEventsWasCalled);

	}

	// =================================================================================================================
	// void processConfigurationResponse(final Event event)
	// =================================================================================================================

	@Test
	public void test_processConfiguration_When_PrivacyOptedIn() throws Exception {
		// setup
		EventData configuration = new EventData();
		configuration.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");

		Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(configuration)
		.build();

		// test
		testExtension.processConfigurationResponse(testEvent);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertEquals("Event should be queued", testExtension.waitingEvents.size(), 1);
		assertFalse(testExtension.clearRulesCacheDirectoryWasCalled);
		assertTrue(testExtension.processQueuedEventsWasCalled);
		assertTrue(campaignHitsDatabase.updatePrivacyStatusWasCalled);
		assertEquals(campaignHitsDatabase.updatePrivacyStatusParameterMobilePrivacyStatus, MobilePrivacyStatus.OPT_IN);
	}

	@Test
	public void test_processConfiguration_When_PrivacyOptOut() throws Exception {
		// setup
		EventData configuration = new EventData();
		configuration.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");

		Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(configuration)
		.build();

		// test
		testExtension.processConfigurationResponse(testEvent);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertEquals("Event should not be queued", testExtension.waitingEvents.size(), 0);
		assertTrue(testExtension.clearRulesCacheDirectoryWasCalled);
		assertFalse(testExtension.processQueuedEventsWasCalled);
		assertTrue(campaignHitsDatabase.updatePrivacyStatusWasCalled);
		assertEquals(campaignHitsDatabase.updatePrivacyStatusParameterMobilePrivacyStatus, MobilePrivacyStatus.OPT_OUT);
		assertEquals(campaignDataStore.getString(CampaignTestConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY, ""), "");
	}

	@Test
	public void test_processConfiguration_When_PrivacyUnknown() throws Exception {
		// setup
		EventData configuration = new EventData();
		configuration.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");

		Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
		.setData(configuration)
		.build();

		// test
		testExtension.processConfigurationResponse(testEvent);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertEquals("Event should be queued", testExtension.waitingEvents.size(), 1);
		assertFalse(testExtension.clearRulesCacheDirectoryWasCalled);
		assertTrue(testExtension.processQueuedEventsWasCalled);
		assertTrue(campaignHitsDatabase.updatePrivacyStatusWasCalled);
		assertEquals(campaignHitsDatabase.updatePrivacyStatusParameterMobilePrivacyStatus, MobilePrivacyStatus.UNKNOWN);
	}

	// =================================================================================================================
	// void queueAndProcessEvent(final Event event)
	// =================================================================================================================

	@Test
	public void test_queueEvent_when_eventIsNull_then_shouldDoNothing() {
		// test
		testExtension.queueAndProcessEvent(null);

		// verify
		assertEquals("Event should not be queued", 0, testExtension.waitingEvents.size());
		assertFalse("Process Queued Events should not be called", testExtension.processQueuedEventsWasCalled);
	}

	@Test
	public void test_queueEvent_when_validEvent_then_shouldQueueAndProcessEvents() throws Exception {
		// setup
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).build();

		// test
		testExtension.queueAndProcessEvent(testEvent);

		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertEquals("Event should be queued", 1, testExtension.waitingEvents.size());
		assertEquals("Event should be correct", testEvent, testExtension.waitingEvents.peek());
		assertTrue("Process Queued Events should be called", testExtension.processQueuedEventsWasCalled);
	}

	// =================================================================================================================
	// protected void processQueuedEvents()
	// =================================================================================================================

	@Test
	public void test_processQueuedEvents_when_happy_then_shouldLoopThroughAndSubmitSignalForAllWaitingEvents() throws
		Exception {
		// setup
		final Event lifecycleEvent = getLifecycleEvent(getLifecycleEventData());
		testExtension.waitingEvents.add(lifecycleEvent);

		eventHub.setSharedState(CampaignTestConstants.EventDataKeys.Configuration.EXTENSION_NAME, getConfigurationEventData());
		eventHub.setSharedState(CampaignTestConstants.EventDataKeys.Identity.EXTENSION_NAME, getIdentityEventData());

		// test
		testExtension.processQueuedEvents();

		// verify
		assertEquals("waiting events queue should be empty", 0, testExtension.waitingEvents.size());
		assertTrue("process lifecycle update should be called", testExtension.processLifecycleUpdateWasCalled);
	}

	@Test
	public void test_processQueuedEvents_when_noConfiguration_then_shouldNotProcessEvents() throws Exception {
		// setup
		final Event lifecycleEvent = getLifecycleEvent(getLifecycleEventData());
		testExtension.waitingEvents.add(lifecycleEvent);

		eventHub.setSharedState(CampaignTestConstants.EventDataKeys.Configuration.EXTENSION_NAME, null);
		eventHub.setSharedState(CampaignTestConstants.EventDataKeys.Identity.EXTENSION_NAME, getIdentityEventData());

		// test
		testExtension.processQueuedEvents();

		// verify
		assertEquals("waiting events queue should be unchanged", 1, testExtension.waitingEvents.size());
		assertFalse("process lifecycle update should not be called", testExtension.processLifecycleUpdateWasCalled);
	}

	@Test
	public void test_processQueuedEvents_when_noIdentity_then_shouldNotProcessEvents() throws Exception {
		// setup
		final Event lifecycleEvent = getLifecycleEvent(getLifecycleEventData());
		testExtension.waitingEvents.add(lifecycleEvent);

		eventHub.setSharedState(CampaignTestConstants.EventDataKeys.Configuration.EXTENSION_NAME, getConfigurationEventData());
		eventHub.setSharedState(CampaignTestConstants.EventDataKeys.Identity.EXTENSION_NAME, null);

		// test
		testExtension.processQueuedEvents();

		// verify
		assertEquals("waiting events queue should be unchanged", 1, testExtension.waitingEvents.size());
		assertFalse("process lifecycle update should not be called", testExtension.processLifecycleUpdateWasCalled);
	}

	// =================================================================================================================
	// void processMessageInformation(final Event event, final CampaignState campaignState)
	// =================================================================================================================

	@Test
	public void test_processMessageInformation_when_campaignConfigured_then_shouldProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();

		final String url = String.format(CampaignTestConstants.CAMPAIGN_TRACKING_URL, campaignState.getCampaignServer(),
										 "h2347", "bb65", "2", campaignState.getExperienceCloudId());

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue("CampaignHitsDatabase queue should be called",
				   campaignHitsDatabase.queueWasCalled);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.GET);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, "");
		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
	}

	@Test
	public void test_processMessageInformation_when_noEventData_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_emptyEventData_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(new EventData()).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_campaignNotConfigured_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(new EventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_NoIdentity_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), new EventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_emptyBroadlogId_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), new EventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("", "bb65", "2")).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_nullBroadlogId_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), new EventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData(null, "bb65", "2")).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_emptyDeliveryId_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), new EventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("h2347", "", "2")).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_nullDeliveryId_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), new EventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("h2347", null, "2")).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_emptyAction_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), new EventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "")).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_nullAction_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), new EventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", null)).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_PrivacyOptOut_then_shouldNotProcessRequest() throws Exception {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");

		campaignState.setState(configData, getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_PrivacyUnknown_then_shouldNotProcessRequest() throws Exception {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");

		campaignState.setState(configData, getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processMessageInformation_when_NullNetworkService_then_shouldNotCrash() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.GENERIC_DATA,
				EventSource.OS).setData(getMessageTrackEventData("h2347", "bb65", "2")).build();

		platformServices.mockNetworkService = null;

		// test
		testExtension.processMessageInformation(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	// =================================================================================================================
	// void processLifecycleUpdate(final Event event, final CampaignState campaignState)
	// =================================================================================================================

	@Test
	public void test_processLifecycleUpdate_when_campaignConfigured_then_shouldQueueHit() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();

		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
							   "\",\"pushPlatform\":\"gcm\"}";
		final String url = String.format(CampaignTestConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);

		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue("CampaignHitsDatabase queue should be called",
				   campaignHitsDatabase.queueWasCalled);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
	}

	@Test
	public void test_processLifecycleUpdate_when_campaignNotConfigured_then_shouldNotQueueHit() throws Exception {
		// setup
		campaignState.setState(new EventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processLifecycleUpdate_when_NoMid_then_shouldNotQueueHit() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), new EventData());
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processLifecycleUpdate_when_PrivacyOptOut_then_shouldNotQueueHit() throws Exception {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");

		campaignState.setState(configData, getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processLifecycleUpdate_when_PrivacyUnknown_then_shouldNotQueueHit() throws Exception {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");

		campaignState.setState(configData, getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processLifecycleUpdate_when_NullNetworkService_then_shouldNotCrash() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();

		platformServices.mockNetworkService = null;

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void
	test_processLifecycleUpdate_when_ecidIsChanged_and_registrationIsPaused_then_shouldNotQueueHit()
	throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();

		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
							   "\",\"pushPlatform\":\"gcm\"}";
		final String url = String.format(CampaignTestConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue("CampaignHitsDatabase queue should be called",
				   campaignHitsDatabase.queueWasCalled);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());

		// setup for second part of test
		// add values to datastore to simulate a previous successful campaign registration request
		// and set registration paused status to true
		campaignState.setState(updateConfigurationEventDataWithRegistrationPausedStatus(getConfigurationEventData(), true),
							   getIdentityEventData());
		final long timestamp = System.currentTimeMillis();
		campaignHitsDatabase.updateTimestampInDataStore(timestamp);
		campaignHitsDatabase.queueWasCalled = false;
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertFalse("CampaignHitsDatabase queue should be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processLifecycleUpdate_when_ecidIsChanged_then_shouldQueueHit() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).build();

		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
							   "\",\"pushPlatform\":\"gcm\"}";
		final String url = String.format(CampaignTestConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue("CampaignHitsDatabase queue should be called",
				   campaignHitsDatabase.queueWasCalled);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());

		// setup for second part of test
		// add values to datastore to simulate a previous successful campaign registration request
		final long timestamp = System.currentTimeMillis();
		campaignHitsDatabase.updateTimestampInDataStore(timestamp);
		campaignHitsDatabase.queueWasCalled = false;
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue("CampaignHitsDatabase queue should be called",
				   campaignHitsDatabase.queueWasCalled);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
	}

	@Test
	public void test_processLifecycleUpdate_when_customRegistrationDelayProvided_then_shouldQueueHit_ifDelayElapsed()
	throws Exception {
		// setup
		// set registration delay to 1 day
		campaignState.setState(getConfigurationEventDataWithCustomRegistrationDelay(getConfigurationEventData(), 1),
							   getIdentityEventData());
		// set timestamp on event to 2 days from now
		final long futureTimestamp = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(2));
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(futureTimestamp).build();

		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
							   "\",\"pushPlatform\":\"gcm\"}";
		final String url = String.format(CampaignTestConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
		// add values to datastore to simulate a previous successful campaign registration request
		campaignHitsDatabase.updateTimestampInDataStore(System.currentTimeMillis());
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue("CampaignHitsDatabase queue should be called",
				   campaignHitsDatabase.queueWasCalled);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
	}

	@Test
	public void
	test_processLifecycleUpdate_when_customRegistrationDelayOfZeroProvided_then_shouldQueueHit() throws
		Exception {
		// setup
		// set registration delay to 0 days (should send a registration request every launch event)
		campaignState.setState(getConfigurationEventDataWithCustomRegistrationDelay(getConfigurationEventData(), 0),
							   getIdentityEventData());
		final long currentTimestamp = System.currentTimeMillis();
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(currentTimestamp).build();

		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
							   "\",\"pushPlatform\":\"gcm\"}";
		final String url = String.format(CampaignTestConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
		// add values to datastore to simulate a previous successful campaign registration request
		campaignHitsDatabase.updateTimestampInDataStore(currentTimestamp);
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue("CampaignHitsDatabase queue should be called",
				   campaignHitsDatabase.queueWasCalled);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
	}

	@Test
	public void
	test_processLifecycleUpdate_when_customRegistrationDelayProvided_then_shouldNotQueueHit_ifDelayNotElapsed() throws
		Exception {
		// setup
		// set registration delay to 30 days
		campaignState.setState(getConfigurationEventDataWithCustomRegistrationDelay(getConfigurationEventData(), 30),
							   getIdentityEventData());

		// set timestamp on event to 25 days from now
		final long futureTimestamp = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(25));
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(futureTimestamp).build();

		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
							   "\",\"pushPlatform\":\"gcm\"}";
		final String url = String.format(CampaignTestConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
		// add values to datastore to simulate a previous successful campaign registration request
		campaignHitsDatabase.updateTimestampInDataStore(System.currentTimeMillis());
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "testExperienceCloudId");

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void test_processLifecycleUpdate_when_registrationPausedIsEqualToFalse_then_shouldQueueHit() throws
		Exception {
		// setup
		// set registration paused to false
		campaignState.setState(updateConfigurationEventDataWithRegistrationPausedStatus(getConfigurationEventData(), false),
							   getIdentityEventData());

		// set timestamp on event to 80 days from now
		final long futureTimestamp = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(80));
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(futureTimestamp).build();

		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
							   "\",\"pushPlatform\":\"gcm\"}";
		final String url = String.format(CampaignTestConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());

		// add values to datastore to simulate a previous successful campaign registration request
		campaignHitsDatabase.updateTimestampInDataStore(System.currentTimeMillis());
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue("CampaignHitsDatabase queue should be called",
				   campaignHitsDatabase.queueWasCalled);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.url, url);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.getHttpCommand(), NetworkService.HttpCommand.POST);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.timeout, 10);
		assertEquals(campaignHitsDatabase.queueParameterCampaignHit.body, payload);
		assertEquals(campaignHitsDatabase.queueParameterTimestamp, testEvent.getTimestamp());
	}

	@Test
	public void test_processLifecycleUpdate_when_registrationPausedIsEqualToTrue_then_shouldNotQueueHit() throws
		Exception {
		// setup
		// set registration paused to true
		campaignState.setState(updateConfigurationEventDataWithRegistrationPausedStatus(getConfigurationEventData(), true),
							   getIdentityEventData());

		// set timestamp on event to 80 days from now
		final long futureTimestamp = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(80));
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(futureTimestamp).build();

		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
							   "\",\"pushPlatform\":\"gcm\"}";
		final String url = String.format(CampaignTestConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());

		// add values to datastore to simulate a previous successful campaign registration request
		campaignHitsDatabase.updateTimestampInDataStore(System.currentTimeMillis());
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	@Test
	public void
	test_processLifecycleUpdate_when_customRegistrationDelayProvided_and_registrationPausedIsEqualToTrue_then_shouldNotQueueHit()
	throws Exception {
		// setup
		// set registration delay to 1 day
		campaignState.setState(updateConfigurationEventDataWithCustomRegistrationDelayAndPauseStatus(
								   getConfigurationEventData(),
								   1, true), getIdentityEventData());
		// set timestamp on event to 2 days from now
		final long futureTimestamp = System.currentTimeMillis() + (TimeUnit.DAYS.toMillis(2));
		final Event testEvent = new Event.Builder("Test event", EventType.LIFECYCLE,
				EventSource.RESPONSE_CONTENT).setData(getLifecycleEventData()).setTimestamp(futureTimestamp).build();

		final String payload = "{\"marketingCloudId\":\"" + campaignState.getExperienceCloudId() +
							   "\",\"pushPlatform\":\"gcm\"}";
		final String url = String.format(CampaignTestConstants.CAMPAIGN_REGISTRATION_URL, campaignState.getCampaignServer(),
										 campaignState.getCampaignPkey(), campaignState.getExperienceCloudId());
		// add values to datastore to simulate a previous successful campaign registration request
		campaignHitsDatabase.updateTimestampInDataStore(System.currentTimeMillis());
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY, "newExperienceCloudId");

		// test
		testExtension.processLifecycleUpdate(testEvent, campaignState);
		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertFalse("CampaignHitsDatabase queue should not be called",
					campaignHitsDatabase.queueWasCalled);
	}

	// =================================================================================================================
	// void triggerRulesDownload(final Event event, final CampaignState campaignState)
	// =================================================================================================================

	@Test
	public void test_triggerRulesDownload__when_campaignConfigured_then_shouldTriggerRulesDownload() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION,
				EventSource.RESPONSE_CONTENT).build();

		final String url = String.format(CampaignTestConstants.CAMPAIGN_RULES_DOWNLOAD_URL, campaignState.getCampaignMcias(),
										 campaignState.getCampaignServer(), campaignState.getPropertyId(), campaignState.getExperienceCloudId());

		// test
		testExtension.triggerRulesDownload(testEvent, campaignState);

		waitForExecutor(testExtension.getExecutor(), 1);

		// verify
		assertTrue("Connect Url should be called to send rules download request.", mockNetworkService.connectUrlWasCalled);
		assertEquals(mockNetworkService.connectUrlParametersUrl, url);
		assertEquals(mockNetworkService.connectUrlParametersCommand, NetworkService.HttpCommand.GET);
	}

	@Test
	public void test_triggerRulesDownload_when_campaignNotConfigured_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(new EventData(), getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION,
				EventSource.RESPONSE_CONTENT).build();

		// test
		testExtension.triggerRulesDownload(testEvent, campaignState);

		// verify
		assertFalse("Connect Url should not be called.", mockNetworkService.connectUrlWasCalled);
	}

	@Test
	public void test_triggerRulesDownload_when_NoMid_then_shouldNotProcessRequest() throws Exception {
		// setup
		campaignState.setState(getConfigurationEventData(), new EventData());
		final Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION,
				EventSource.RESPONSE_CONTENT).build();

		// test
		testExtension.triggerRulesDownload(testEvent, campaignState);

		// verify
		assertFalse("Connect Url should not be called.", mockNetworkService.connectUrlWasCalled);
	}

	@Test
	public void test_triggerRulesDownload_when_PrivacyOptOut_then_shouldNotProcessRequest() throws Exception {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");

		campaignState.setState(configData, getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION,
				EventSource.RESPONSE_CONTENT).build();

		// test
		testExtension.triggerRulesDownload(testEvent, campaignState);

		// verify
		assertFalse("Connect Url should not be called.", mockNetworkService.connectUrlWasCalled);
	}

	@Test
	public void test_triggerRulesDownload_when_PrivacyUnknown_then_shouldNotProcessRequest() throws Exception {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");

		campaignState.setState(configData, getIdentityEventData());
		final Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION,
				EventSource.RESPONSE_CONTENT).build();

		// test
		testExtension.triggerRulesDownload(testEvent, campaignState);

		// verify
		assertFalse("Connect Url should not be called.", mockNetworkService.connectUrlWasCalled);
	}

	// =================================================================================================================
	// void dispatchMessageInteraction(final Map<String, String> messageData)
	// =================================================================================================================

	@Test
	public void test_dispatchMessageInteraction_happy() throws Exception {
		// setup
		final Map<String, String> testMessageData = new HashMap<String, String>();
		testMessageData.put(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, "testMessageId");
		testMessageData.put(CampaignTestConstants.ContextDataKeys.MESSAGE_VIEWED, "1");

		testExtension.campaignEventDispatcher = campaignDispatcher;

		// test
		testExtension.dispatchMessageInteraction(testMessageData);

		// verify
		assertTrue(campaignDispatcher.dispatchWasCalled);
		assertEquals(campaignDispatcher.dispatchParameterMessageData, testMessageData);
	}

	@Test
	public void test_dispatchMessageInteraction_whenNullDispatcher_then_shouldNotDispatch() throws Exception {
		// setup
		final Map<String, String> testMessageData = new HashMap<String, String>();
		testMessageData.put(CampaignTestConstants.ContextDataKeys.MESSAGE_ID, "testMessageId");
		testMessageData.put(CampaignTestConstants.ContextDataKeys.MESSAGE_VIEWED, "1");

		testExtension.campaignEventDispatcher = null;

		// test
		testExtension.dispatchMessageInteraction(testMessageData);

		// verify
		assertFalse(campaignDispatcher.dispatchWasCalled);
	}

	// =================================================================================================================
	// void dispatchMessageInfo(final String broadlogId, final String deliveryId, final String action)
	// =================================================================================================================

	@Test
	public void test_dispatchMessageInfo_happy() throws Exception {
		// setup
		final String broadlogId = "h87a1";
		final String deliveryId = "22dc";
		final String action = "7";
		testExtension.genericDataOSEventDispatcher = genericDataOSEventDispatcher;

		// test
		testExtension.dispatchMessageInfo(broadlogId, deliveryId, action);

		// verify
		assertTrue(genericDataOSEventDispatcher.dispatchWasCalled);
		assertEquals(genericDataOSEventDispatcher.dispatchParameterBroadlogId, broadlogId);
		assertEquals(genericDataOSEventDispatcher.dispatchParameterDeliveryId, deliveryId);
		assertEquals(genericDataOSEventDispatcher.dispatchParameterAction, action);
	}

	@Test
	public void test_dispatchMessageInfo_whenNullDispatcher_then_shouldNotDispatch() throws Exception {
		// setup
		final String broadlogId = "h87a1";
		final String deliveryId = "22dc";
		final String action = "7";
		testExtension.genericDataOSEventDispatcher = null;

		// test
		testExtension.dispatchMessageInfo(broadlogId, deliveryId, action);

		// verify
		assertFalse(campaignDispatcher.dispatchWasCalled);
	}

	// =================================================================================================================
	// void clearCachedAssetsForMessagesNotInList(final List<String> activeMessageIds)
	// =================================================================================================================

	@Test
	public void test_clearCachedMessageAssets_When_EmptyList_Then_RemoveAllAssets() throws Exception {
		// setup
		setupCachedMessageAssets(fakeMessageId1, fakeMessageId2);

		final String response = "abcd";
		final int responseCode = 200;
		final String responseMessage = "";
		final HashMap<String, String> responseProperties = new HashMap<String, String>();
		responseProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");

		mockNetworkService.connectUrlAsyncCallbackParametersConnection =
			new MockConnection(response, responseCode, responseMessage, responseProperties);

		// pre-verify
		final CacheManager manager = new CacheManager(mockSystemInfoService);
		final File cachedFile = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
								messageCacheDirString1, false);
		assertNotNull("cachedFile should exist", cachedFile);
		final File cachedFile2 = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
								 messageCacheDirString2, false);
		assertNotNull("cachedFile2 should exist", cachedFile2);

		// execute
		testExtension.clearCachedAssetsForMessagesNotInList(new ArrayList<String>());

		// verify
		final File cachedFile3 = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
								 messageCacheDirString1, false);
		assertNull("cachedFile3 should not exist", cachedFile3);
		final File cachedFile4 = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
								 messageCacheDirString2, false);
		assertNull("cachedFile4 should not exist", cachedFile4);
	}

	@Test
	public void test_clearCachedMessageAssets_When_MessageIdsInList_Then_RemoveAllAssetsNotInList() throws Exception {
		// setup
		final ArrayList<String> messageIds = new ArrayList<String>();
		messageIds.add(fakeMessageId1);

		setupCachedMessageAssets(fakeMessageId1, fakeMessageId2);

		final String response = "abcd";
		final int responseCode = 200;
		final String responseMessage = "";
		final HashMap<String, String> responseProperties = new HashMap<String, String>();
		responseProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");

		mockNetworkService.connectUrlAsyncCallbackParametersConnection =
			new MockConnection(response, responseCode, responseMessage, responseProperties);

		// execute
		testExtension.clearCachedAssetsForMessagesNotInList(messageIds);

		// verify
		final CacheManager manager = new CacheManager(mockSystemInfoService);
		final File cachedFile = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
								messageCacheDirString1, false);
		assertNotNull("Cached file for active message should still exist", cachedFile);
		final File cachedFile2 = manager.getFileForCachedURL("https://www.pexels.com/photo/grey-fur-kitten-127028/",
								 messageCacheDirString2, false);
		assertNull("Cached file for inactive message should be removed", cachedFile2);
	}

	// =================================================================================================================
	// void clearRulesCacheDirectory()
	// =================================================================================================================

	@Test
	public void test_clearRulesCacheDirectory_happy() throws Exception {
		// setup
		setupCachedRules();

		final String response = "";
		final int responseCode = 200;
		final String responseMessage = "";
		final HashMap<String, String> responseProperties = new HashMap<String, String>();
		responseProperties.put("Last-Modified", "Fri, 1 Jan 2010 00:00:00 UTC");

		mockNetworkService.connectUrlReturnValue = ((NetworkService.HttpConnection)new MockConnection(response, responseCode,
				responseMessage, responseProperties));

		// pre-verify
		final CacheManager manager = new CacheManager(mockSystemInfoService);
		final File cachedFile = manager.getFileForCachedURL(
									"https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip",
									rulesCacheDirString, false);
		assertNotNull("cachedFile should exist", cachedFile);

		// execute
		testExtension.clearRulesCacheDirectory();

		// verify
		final File cachedFile2 = manager.getFileForCachedURL(
									 "https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip",
									 rulesCacheDirString, false);
		assertNull("cachedFile1 should not exist", cachedFile2);
	}

	// =================================================================================================================
	// void loadCachedMessages()
	// =================================================================================================================

	@Test
	public void test_loadCachedMessages_happy() throws Exception {
		// setup
		final File cacheFile = setupCachedRules();
		final String rulesUrl =
			"https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip";
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY, rulesUrl);

		final MockCampaignRulesRemoteDownloader mockCampaignRulesRemoteDownloader = getMockCampaignRulesRemoteDownloader(
					rulesUrl, rulesCacheDirString);
		assertNotNull(mockCampaignRulesRemoteDownloader);

		testExtension.getCampaignRulesRemoteDownloaderReturnValue = mockCampaignRulesRemoteDownloader;

		// test
		testExtension.loadCachedMessages();

		// verify
		assertTrue(mockCampaignRulesRemoteDownloader.getCachePathWasCalled);
		assertNotNull(mockCampaignRulesRemoteDownloader.getCachePathReturnValue);
		assertEquals(mockCampaignRulesRemoteDownloader.getCachePathReturnValue.getPath(), cacheFile.getPath());
	}

	@Test
	public void test_loadCachedMessages_When_DataStoreIsEmpty_Then_ShouldNotCallGetCachePath() throws Exception {
		// setup
		setupCachedRules();
		final String rulesUrl =
			"https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip";
		campaignDataStore.removeAll();

		final MockCampaignRulesRemoteDownloader mockCampaignRulesRemoteDownloader = getMockCampaignRulesRemoteDownloader(
					rulesUrl, rulesCacheDirString);
		assertNotNull(mockCampaignRulesRemoteDownloader);

		testExtension.getCampaignRulesRemoteDownloaderReturnValue = mockCampaignRulesRemoteDownloader;

		// test
		testExtension.loadCachedMessages();

		// verify
		assertFalse(mockCampaignRulesRemoteDownloader.getCachePathWasCalled);
	}

	@Test
	public void test_loadCachedMessages_When_DataStoreHasEmptyRulesRemoteUrl_Then_ShouldNotCallGetCachePath() throws
		Exception {
		// setup
		setupCachedRules();
		final String rulesUrl =
			"https://mcias-va7.cloud.adobe.io/mcias/mcias.campaign-demo.adobe.com/PR146b40abd1be4a0ab224c16cbdc04bff/37922783516695133647566171476397216484/rules.zip";
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY, "");

		final MockCampaignRulesRemoteDownloader mockCampaignRulesRemoteDownloader = getMockCampaignRulesRemoteDownloader(
					rulesUrl, rulesCacheDirString);
		assertNotNull(mockCampaignRulesRemoteDownloader);

		testExtension.getCampaignRulesRemoteDownloaderReturnValue = mockCampaignRulesRemoteDownloader;

		// test
		testExtension.loadCachedMessages();

		// verify
		assertFalse(mockCampaignRulesRemoteDownloader.getCachePathWasCalled);
	}

	@Test
	public void test_loadCachedMessages_When_NoCachedRulesForUrl_Then_ShouldGetNullCachePath() throws Exception {
		// setup
		setupCachedRules();
		final String rulesUrl = "http://mock.com/rules.zip";
		campaignDataStore.setString(CampaignTestConstants.CAMPAIGN_DATA_STORE_REMOTES_URL_KEY, rulesUrl);

		final MockCampaignRulesRemoteDownloader mockCampaignRulesRemoteDownloader = getMockCampaignRulesRemoteDownloader(
					rulesUrl, rulesCacheDirString);
		assertNotNull(mockCampaignRulesRemoteDownloader);

		testExtension.getCampaignRulesRemoteDownloaderReturnValue = mockCampaignRulesRemoteDownloader;

		// test
		testExtension.loadCachedMessages();

		// verify
		assertTrue(mockCampaignRulesRemoteDownloader.getCachePathWasCalled);
		assertNull(mockCampaignRulesRemoteDownloader.getCachePathReturnValue);
	}

	// =================================================================================================================
	// CampaignRulesRemoteDownloader getCampaignRulesRemoteDownloader(final String url,
	//																  final Map<String, String> requestProperties)
	// =================================================================================================================

	@Test
	public void test_getCampaignRulesRemoteDownloader_happy() throws Exception {
		// setup
		final String rulesUrl = "http://mock.com/rules.zip";

		// test
		CampaignRulesRemoteDownloader remoteDownloader = testExtension.getCampaignRulesRemoteDownloader(rulesUrl,
				new HashMap<String, String>());

		// verify
		assertNotNull(remoteDownloader);
	}

	@Test
	public void test_getCampaignRulesRemoteDownloader_When_NullPlatformServices_Then_ShouldReturnNull() throws Exception {
		// setup
		final String rulesUrl = "http://mock.com/rules.zip";
		testExtension = new MockCampaignExtension(eventHub, null, campaignHitsDatabase);

		// test
		CampaignRulesRemoteDownloader remoteDownloader = testExtension.getCampaignRulesRemoteDownloader(rulesUrl,
				new HashMap<String, String>());

		// verify
		assertNull(remoteDownloader);
	}

	// =================================================================================================================
	// Test function dispatchMessageEvent, which gets called on Generic data OS events.
	// =================================================================================================================

	@Test
	public void testDispatchMessageEventWithActionViewed() {

		final String hexValueOfMessageId = "bbc6";
		final String decimalValueOfMessageId = "48070";
		testExtension.dispatchMessageEvent("1", hexValueOfMessageId);
		Assert.assertTrue(eventHub.isDispatchedCalled);
		Assert.assertEquals(eventHub.dispatchedEventList.size(), 1);
		final Event event = eventHub.dispatchedEventList.get(0);

		try {
			Assert.assertEquals(event.getData().getString2(CampaignConstants.ContextDataKeys.MESSAGE_ID), decimalValueOfMessageId);
			Assert.assertEquals(event.getData().getString2(CampaignConstants.ContextDataKeys.MESSAGE_VIEWED), "1");
		} catch (VariantException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testDispatchMessageEventWithActionClicked() {

		final String hexValueOfMessageId = "bbc6";
		final String decimalValueOfMessageId = "48070";
		testExtension.dispatchMessageEvent("2", hexValueOfMessageId);
		Assert.assertTrue(eventHub.isDispatchedCalled);
		Assert.assertEquals(eventHub.dispatchedEventList.size(), 1);
		final Event event = eventHub.dispatchedEventList.get(0);

		try {
			Assert.assertEquals(event.getData().getString2(CampaignConstants.ContextDataKeys.MESSAGE_ID), decimalValueOfMessageId);
			Assert.assertEquals(event.getData().getString2(CampaignConstants.ContextDataKeys.MESSAGE_CLICKED), "1");
		} catch (VariantException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	@Test
	public void testDispatchMessageEventWithActionImpression() {

		final String hexValueOfMessageId = "bbc6";
		testExtension.dispatchMessageEvent("7", hexValueOfMessageId);
		Assert.assertFalse(eventHub.isDispatchedCalled);
	}
}