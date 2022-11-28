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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;

import java.util.HashMap;
import java.util.Map;

public class CampaignStateTests {

	private CampaignState campaignState;

	@Before
	public void setup() {
		campaignState = new CampaignState();
	}

	private SharedStateResult getConfigurationEventData() {
		final Map<String, Object> configData = new HashMap<>();
		configData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "testServer");
		configData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_PKEY_KEY, "testPkey");
		configData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_MCIAS_KEY, "testMcias");
		configData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_TIMEOUT, 10);
		configData.put(CampaignTestConstants.EventDataKeys.Configuration.PROPERTY_ID, "testPropertyId");
		configData.put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
		configData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_DELAY_KEY, 30);
		configData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_PAUSED_KEY, true);
		final SharedStateResult sharedStateResult = new SharedStateResult(SharedStateStatus.SET, configData);

		return sharedStateResult;
	}

	private SharedStateResult getIdentityEventData() {
		final Map<String, Object> identityData = new HashMap<>();
		identityData.put(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "testExperienceCloudId");
		final SharedStateResult sharedStateResult = new SharedStateResult(SharedStateStatus.SET, identityData);

		return sharedStateResult;
	}

	@Test
	public void testSetState_happy() {
		// test
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());

		// verify
		assertEquals("testServer", campaignState.getCampaignServer());
		assertEquals("testPkey", campaignState.getCampaignPkey());
		assertEquals("testMcias", campaignState.getCampaignMcias());
		assertEquals(10, campaignState.getCampaignTimeout());
		assertEquals("testPropertyId", campaignState.getPropertyId());
		assertEquals(MobilePrivacyStatus.OPT_IN, campaignState.getMobilePrivacyStatus());
		assertEquals(30, campaignState.getCampaignRegistrationDelay());
		assertEquals(true, campaignState.getCampaignRegistrationPaused());
	}

	@Test
	public void testSetState_SetsDefault_When_NoConfigOrIdentityData() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		Map<String, Object> testIdentityData = new HashMap<>();
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);
		SharedStateResult identitySharedStateResult = new SharedStateResult(SharedStateStatus.SET, testIdentityData);

		// test
		campaignState.setState(configSharedStateResult, identitySharedStateResult);

		// verify
		assertEquals("", campaignState.getCampaignServer());
		assertEquals("", campaignState.getCampaignPkey());
		assertEquals("", campaignState.getCampaignMcias());
		assertEquals(CampaignTestConstants.CAMPAIGN_TIMEOUT_DEFAULT, campaignState.getCampaignTimeout());
		assertEquals("", campaignState.getPropertyId());
		assertEquals(MobilePrivacyStatus.UNKNOWN, campaignState.getMobilePrivacyStatus());
		assertEquals(CampaignTestConstants.DEFAULT_REGISTRATION_DELAY_DAYS, campaignState.getCampaignRegistrationDelay());
		assertEquals(false, campaignState.getCampaignRegistrationPaused());
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_happy() {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertTrue(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_PrivacyOptOut() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_PrivacyUnknown() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_EmptyCampaignServer() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_NullCampaignServer() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, null);
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_EmptyCampaignMcias() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_MCIAS_KEY, "");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_NullCampaignMcias() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_MCIAS_KEY, null);
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_EmptyPropertyId() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.PROPERTY_ID, "");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_NullPropertyId() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.PROPERTY_ID, null);
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_EmptyExperienceCloudId() {
		// setup
		Map<String, Object> testIdentityData = new HashMap<>();
		testIdentityData.put(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "");
		SharedStateResult identitySharedStateResult = new SharedStateResult(SharedStateStatus.SET, testIdentityData);

		campaignState.setState(getConfigurationEventData(), identitySharedStateResult);

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_NullExperienceCloudId() {
		// setup
		Map<String, Object> testIdentityData = new HashMap<>();
		testIdentityData.put(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, null);
		SharedStateResult identitySharedStateResult = new SharedStateResult(SharedStateStatus.SET, testIdentityData);

		campaignState.setState(getConfigurationEventData(), identitySharedStateResult);

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanRegisterWithCurrentState_happy() {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertTrue(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_EmptyCampaignServer() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_NullCampaignServer() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, null);
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_EmptyCampaignPkey() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_PKEY_KEY, "");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_NullCampaignPkey() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_PKEY_KEY, null);
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_EmptyExperienceCloudId() {
		// setup
		Map<String, Object> testIdentityData = new HashMap<>();
		testIdentityData.put(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "");
		SharedStateResult identitySharedStateResult = new SharedStateResult(SharedStateStatus.SET, testIdentityData);

		campaignState.setState(getConfigurationEventData(), identitySharedStateResult);

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_NullExperienceCloudId() {
		// setup
		Map<String, Object> testIdentityData = new HashMap<>();
		testIdentityData.put(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, null);
		SharedStateResult identitySharedStateResult = new SharedStateResult(SharedStateStatus.SET, testIdentityData);

		campaignState.setState(getConfigurationEventData(), identitySharedStateResult);

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_PrivacyOptOut() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_PrivacyUnknown() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_happy() {
		// setup
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertTrue(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_EmptyCampaignServer() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_NullCampaignServer() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, null);
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_EmptyExperienceCloudId() {
		// setup
		Map<String, Object> testIdentityData = new HashMap<>();
		testIdentityData.put(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "");
		SharedStateResult identitySharedStateResult = new SharedStateResult(SharedStateStatus.SET, testIdentityData);

		campaignState.setState(getConfigurationEventData(), identitySharedStateResult);

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_NullExperienceCloudId() {
		// setup
		Map<String, Object> testIdentityData = new HashMap<>();
		testIdentityData.put(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, null);
		SharedStateResult identitySharedStateResult = new SharedStateResult(SharedStateStatus.SET, testIdentityData);

		campaignState.setState(getConfigurationEventData(), identitySharedStateResult);

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_PrivacyOptOut() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_PrivacyUnknown() {
		// setup
		Map<String, Object> testConfigData = new HashMap<>();
		testConfigData.put(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");
		SharedStateResult configSharedStateResult = new SharedStateResult(SharedStateStatus.SET, testConfigData);

		campaignState.setState(configSharedStateResult, getIdentityEventData());

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}
}
