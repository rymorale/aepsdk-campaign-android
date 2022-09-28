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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CampaignStateTests {

	private CampaignState campaignState;

	@Before
	public void setup() throws Exception {
		campaignState = new CampaignState();
	}

	private EventData getConfigurationEventData() {
		final EventData configData = new EventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "testServer");
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_PKEY_KEY, "testPkey");
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_MCIAS_KEY, "testMcias");
		configData.putInteger(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_TIMEOUT, 10);
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.PROPERTY_ID, "testPropertyId");
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedin");
		configData.putInteger(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_DELAY_KEY, 30);
		configData.putBoolean(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_PAUSED_KEY, true);

		return configData;
	}

	private EventData getIdentityEventData() {
		final EventData identityData = new EventData();
		identityData.putString(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "testExperienceCloudId");

		return identityData;
	}

	@Test
	public void testSetState_happy() {
		// test
		campaignState.setState(getConfigurationEventData(), getIdentityEventData());

		// verify
		assertEquals(campaignState.getCampaignServer(), "testServer");
		assertEquals(campaignState.getCampaignPkey(), "testPkey");
		assertEquals(campaignState.getCampaignMcias(), "testMcias");
		assertEquals(campaignState.getCampaignTimeout(), 10);
		assertEquals(campaignState.getPropertyId(), "testPropertyId");
		assertEquals(campaignState.getMobilePrivacyStatus(), MobilePrivacyStatus.OPT_IN);
		assertEquals(campaignState.getCampaignRegistrationDelay(), 30);
		assertEquals(campaignState.getCampaignRegistrationPaused(), true);
	}

	@Test
	public void testSetState_SetsDefault_When_NoConfigOrIdentityData() {
		// setup
		EventData testConfigData = new EventData();
		EventData testIdentityData = new EventData();

		// test
		campaignState.setState(testConfigData, testIdentityData);

		// verify
		assertEquals(campaignState.getCampaignServer(), "");
		assertEquals(campaignState.getCampaignPkey(), "");
		assertEquals(campaignState.getCampaignMcias(), "");
		assertEquals(campaignState.getCampaignTimeout(), CampaignTestConstants.CAMPAIGN_TIMEOUT_DEFAULT);
		assertEquals(campaignState.getPropertyId(), "");
		assertEquals(campaignState.getMobilePrivacyStatus(), MobilePrivacyStatus.UNKNOWN);
		assertEquals(campaignState.getCampaignRegistrationDelay(), 7);
		assertEquals(campaignState.getCampaignRegistrationPaused(), false);
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
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_PrivacyUnknown() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_EmptyCampaignServer() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_NullCampaignServer() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, null);

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_EmptyCampaignMcias() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_MCIAS_KEY, "");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_NullCampaignMcias() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_MCIAS_KEY, null);

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_EmptyPropertyId() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.PROPERTY_ID, "");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_NullPropertyId() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.PROPERTY_ID, null);

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_EmptyExperienceCloudId() {
		// setup
		EventData identityData = getConfigurationEventData();
		identityData.putString(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "");

		campaignState.setState(getConfigurationEventData(), identityData);

		// test
		boolean canDownloadRules = campaignState.canDownloadRulesWithCurrentState();

		// verify
		assertFalse(canDownloadRules);
	}

	@Test
	public void testCanDownloadRulesWithCurrentState_ReturnsFalse_When_NullExperienceCloudId() {
		// setup
		EventData identityData = getIdentityEventData();
		identityData.putString(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, null);

		campaignState.setState(getConfigurationEventData(), identityData);

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
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_NullCampaignServer() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, null);

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_EmptyCampaignPkey() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_PKEY_KEY, "");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_NullCampaignPkey() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_PKEY_KEY, null);

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_EmptyExperienceCloudId() {
		// setup
		EventData identityData = getConfigurationEventData();
		identityData.putString(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "");

		campaignState.setState(getConfigurationEventData(), identityData);

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_NullExperienceCloudId() {
		// setup
		EventData identityData = getIdentityEventData();
		identityData.putString(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, null);

		campaignState.setState(getConfigurationEventData(), identityData);

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_PrivacyOptOut() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canRegister = campaignState.canRegisterWithCurrentState();

		// verify
		assertFalse(canRegister);
	}

	@Test
	public void testCanRegisterWithCurrentState_ReturnsFalse_When_PrivacyUnknown() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");

		campaignState.setState(configData, getIdentityEventData());

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
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_NullCampaignServer() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, null);

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_EmptyExperienceCloudId() {
		// setup
		EventData identityData = getConfigurationEventData();
		identityData.putString(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, "");

		campaignState.setState(getConfigurationEventData(), identityData);

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_NullExperienceCloudId() {
		// setup
		EventData identityData = getIdentityEventData();
		identityData.putString(CampaignTestConstants.EventDataKeys.Identity.VISITOR_ID_MID, null);

		campaignState.setState(getConfigurationEventData(), identityData);

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_PrivacyOptOut() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optedout");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

	@Test
	public void testCanSendTrackInfoWithCurrentState_ReturnsFalse_When_PrivacyUnknown() {
		// setup
		EventData configData = getConfigurationEventData();
		configData.putString(CampaignTestConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, "optunknown");

		campaignState.setState(configData, getIdentityEventData());

		// test
		boolean canSendTrackInfo = campaignState.canSendTrackInfoWithCurrentState();

		// verify
		assertFalse(canSendTrackInfo);
	}

}
