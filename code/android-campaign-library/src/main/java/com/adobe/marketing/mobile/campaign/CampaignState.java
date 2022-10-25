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

import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;

import com.adobe.marketing.mobile.MobilePrivacyStatus;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.Map;

final class CampaignState {
	private final String SELF_TAG = "CampaignState";
	// ----------- Configuration properties -----------
	private boolean configStateSet = false;
	private String campaignServer;
	private String campaignPkey;
	private String campaignMcias;
	private MobilePrivacyStatus privacyStatus;
	private String propertyId;
	private int timeout;
	private int campaignRegistrationDelayDays;
	private boolean campaignRegistrationPaused;


	// ----------- Identity properties -----------
	private boolean identityStateSet = false;
	private String experienceCloudId;

	// ========================================================================
	// package-private methods
	// ========================================================================

	/**
	 * Get this Campaign server.
	 *
	 * @return {@link String} containing the configured Campaign server
	 */
	String getCampaignServer() {
		return this.campaignServer;
	}

	/**
	 * Get this Campaign pkey.
	 *
	 * @return {@link String} containing the configured Campaign pkey
	 */
	String getCampaignPkey() {
		return this.campaignPkey;
	}

	/**
	 * Get this Campaign mcias endpoint.
	 *
	 * @return {@link String} containing the configured Campaign mcias endpoint.
	 */
	String getCampaignMcias() {
		return this.campaignMcias;
	}

	/**
	 * Get this Campaign timeout.
	 *
	 * @return {@link String} containing the configured Campaign timeout.
	 */
	int getCampaignTimeout() {
		return this.timeout;
	}

	/**
	 * Get this Mobile privacy status.
	 *
	 * @return {@link MobilePrivacyStatus} enum representing the configured mobile privacy status.
	 */
	MobilePrivacyStatus getMobilePrivacyStatus() {
		return this.privacyStatus;
	}

	/**
	 * Get this mobile property Id.
	 *
	 * @return {@link String} containing the configured property Id.
	 */
	String getPropertyId() {
		return this.propertyId;
	}

	/**
	 * Get the Campaign registration delay.
	 *
	 * @return {@link int} containing the configured Campaign registration delay.
	 */
	int getCampaignRegistrationDelay() {
		return this.campaignRegistrationDelayDays;
	}

	/**
	 * Get the Campaign registration paused status.
	 *
	 * @return {@link boolean} containing the Campaign registration paused status.
	 */
	boolean getCampaignRegistrationPaused() {
		return this.campaignRegistrationPaused;
	}

	/**
	 * Get this Experience Cloud Id.
	 *
	 * @return {@link String} containing the configured Experience Cloud Id.
	 */
	String getExperienceCloudId() {
		return this.experienceCloudId;
	}

	/**
	 * Sets this {@code CampaignState} with properties from provided {@code configData} and {@code identityData}.
	 * <p>
	 * Invokes internal methods to set the properties for {@code Configuration} and {@code Identity} shared states.
	 *
	 * @param configSharedStateResult {@link SharedStateResult} representing the retrieved {@code Configuration} shared state
	 * @param identitySharedStateResult {@code SharedStateResult} representing the retrieved {@code Identity} shared state
	 * @see #setConfiguration(Map<String, Object>)
	 * @see #setIdentity(Map<String, Object>)
	 */
	void setState(final SharedStateResult configSharedStateResult, final SharedStateResult identitySharedStateResult) {
		if (configSharedStateResult.getValue() != null) {
			setConfiguration(configSharedStateResult.getValue());
		}
		if (identitySharedStateResult.getValue() != null) {
			setIdentity(identitySharedStateResult.getValue());
		}
	}

	/**
	 * Determines if the necessary configuration and identity shared state data has been received.
	 *
	 * @return {@code boolean} indicating whether the configuration and identity shared state are present
	 */
	boolean isStateSet() {
		return configStateSet && identityStateSet;
	}

	/**
	 * Determines if this contains valid {@code CampaignState} for downloading rules from Campaign.
	 *
	 * @return {@code boolean} indicating whether this contains valid {@code CampaignState} for rules download
	 */
	boolean canDownloadRulesWithCurrentState() {
		if (this.privacyStatus != MobilePrivacyStatus.OPT_IN) {
			Log.trace(LOG_TAG, SELF_TAG,
					"canDownloadRulesWithCurrentState -  Cannot download rules, since privacy status is not opted in.");
			return false;
		}

		return !StringUtils.isNullOrEmpty(this.experienceCloudId) && !StringUtils.isNullOrEmpty(this.campaignServer) &&
				!StringUtils.isNullOrEmpty(this.campaignMcias) && !StringUtils.isNullOrEmpty(this.propertyId);
	}

	/**
	 * Determines if this contains valid {@code CampaignState} for sending registration request to Campaign.
	 *
	 * @return {@code boolean} indicating whether this contains valid {@code CampaignState} for registration
	 */
	boolean canRegisterWithCurrentState() {
		if (this.privacyStatus != MobilePrivacyStatus.OPT_IN) {
			Log.trace(LOG_TAG, SELF_TAG,
					"canRegisterWithCurrentState -  Cannot register with Campaign, since privacy status is not opted in.");
			return false;
		}

		return !StringUtils.isNullOrEmpty(this.experienceCloudId) && !StringUtils.isNullOrEmpty(this.campaignServer) &&
				!StringUtils.isNullOrEmpty(this.campaignPkey);
	}

	/**
	 * Determines if this contains valid {@code CampaignState} for sending message track request to Campaign.
	 *
	 * @return {@code boolean} indicating whether this contains valid {@code CampaignState} for message tracking
	 */
	boolean canSendTrackInfoWithCurrentState() {
		if (this.privacyStatus != MobilePrivacyStatus.OPT_IN) {
			Log.trace(LOG_TAG, SELF_TAG,
					"canSendTrackInfoWithCurrentState -  Cannot send message track request to Campaign, since privacy status is not opted in.");
			return false;
		}

		return !StringUtils.isNullOrEmpty(this.experienceCloudId) && !StringUtils.isNullOrEmpty(this.campaignServer);
	}

	// ========================================================================
	// private methods
	// ========================================================================

	/**
	 * Extracts properties from the provided {@code Map<String, Object>} configState and updates this {@code CampaignState}.
	 *
	 * @param configState {@link Map<String, Object>} representing {@code Configuration} shared state
	 */
	private void setConfiguration(final Map<String, Object> configState) {
		if (configState == null || configState.isEmpty()) {
			Log.debug(LOG_TAG, SELF_TAG,
					"setConfiguration -  Cannot set Configuration properties, provided config data is null.");
			return;
		}

		this.campaignServer = DataReader.optString(configState, CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY, "");
		this.campaignPkey = DataReader.optString(configState, CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_PKEY_KEY, "");
		this.campaignMcias = DataReader.optString(configState, CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_MCIAS_KEY, "");
		this.propertyId = DataReader.optString(configState, CampaignConstants.EventDataKeys.Configuration.PROPERTY_ID, "");
		this.privacyStatus = MobilePrivacyStatus.fromString(DataReader.optString(configState, CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, ""));
		this.timeout = DataReader.optInt(configState, CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_TIMEOUT, CampaignConstants.CAMPAIGN_TIMEOUT_DEFAULT);
		this.campaignRegistrationDelayDays = DataReader.optInt(configState, CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_DELAY_KEY, CampaignConstants.DEFAULT_REGISTRATION_DELAY_DAYS);
		this.campaignRegistrationPaused = DataReader.optBoolean(configState, CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_PAUSED_KEY, false);
		configStateSet = true;
	}

	/**
	 * Extracts properties from the provided {@code Map<String, Object>} identityState and updates this {@code CampaignState}.
	 *
	 * @param identityState {@link Map<String, Object>} representing {@code Identity} shared state
	 */
	private void setIdentity(final Map<String, Object> identityState) {
		if (identityState == null || identityState.isEmpty()) {
			Log.debug(LOG_TAG, SELF_TAG, "setIdentity - Cannot set Identity properties, provided identity data is null.");
			return;
		}

		this.experienceCloudId = DataReader.optString(identityState, CampaignConstants.EventDataKeys.Identity.VISITOR_ID_MID, "");
		identityStateSet = true;
	}

}