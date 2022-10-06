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

final class CampaignState {

	// ----------- Configuration properties -----------

	private String campaignServer;
	private String campaignPkey;
	private String campaignMcias;
	private MobilePrivacyStatus privacyStatus;
	private String propertyId;
	private int timeout;
	private int campaignRegistrationDelayDays;
	private boolean campaignRegistrationPaused;


	// ----------- Identity properties -----------

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
	 * @param configState {@link EventData} representing {@code Configuration} shared state
	 * @param identityState {@code EventData} representing {@code Identity} shared state
	 * @see #setConfiguration(EventData)
	 * @see #setIdentity(EventData)
	 */
	void setState(final EventData configState, final EventData identityState) {
		setConfiguration(configState);
		setIdentity(identityState);
	}

	/**
	 * Determines if this contains valid {@code CampaignState} for downloading rules from Campaign.
	 *
	 * @return {@code boolean} indicating whether this contains valid {@code CampaignState} for rules download
	 */
	boolean canDownloadRulesWithCurrentState() {
		if (this.privacyStatus != MobilePrivacyStatus.OPT_IN) {
			Log.trace(CampaignConstants.LOG_TAG,
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
			Log.trace(CampaignConstants.LOG_TAG,
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
			Log.trace(CampaignConstants.LOG_TAG,
					  "canSendTrackInfoWithCurrentState -  Cannot send message track request to Campaign, since privacy status is not opted in.");
			return false;
		}

		return !StringUtils.isNullOrEmpty(this.experienceCloudId) && !StringUtils.isNullOrEmpty(this.campaignServer);
	}

	// ========================================================================
	// private methods
	// ========================================================================

	/**
	 * Extracts properties from the provided {@code configState} and updates this {@code CampaignState}.
	 *
	 * @param configState {@link EventData} representing {@code Configuration} shared state
	 */
	private void setConfiguration(final EventData configState) {
		if (configState == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "setConfiguration -  Cannot set Configuration properties, provided config data is null.");
			return;
		}

		this.campaignServer = configState.optString(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_SERVER_KEY,
							  "");
		this.campaignPkey = configState.optString(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_PKEY_KEY,
							"");
		this.campaignMcias = configState.optString(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_MCIAS_KEY,
							 "");
		this.timeout = configState.optInteger(CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_TIMEOUT,
											  CampaignConstants.CAMPAIGN_TIMEOUT_DEFAULT);
		this.propertyId = configState.optString(CampaignConstants.EventDataKeys.Configuration.PROPERTY_ID,
												"");
		this.privacyStatus = MobilePrivacyStatus.fromString(configState.optString(
								 CampaignConstants.EventDataKeys.Configuration.GLOBAL_CONFIG_PRIVACY, ""));
		this.campaignRegistrationDelayDays = configState.optInteger(
				CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_DELAY_KEY,
				CampaignConstants.DEFAULT_REGISTRATION_DELAY_DAYS);
		this.campaignRegistrationPaused = configState.optBoolean(
											  CampaignConstants.EventDataKeys.Configuration.CAMPAIGN_REGISTRATION_PAUSED_KEY, false);
	}

	/**
	 * Extracts properties from the provided {@code identityState} and updates this {@code CampaignState}.
	 *
	 * @param identityState {@link EventData} representing {@code Identity} shared state
	 */
	private void setIdentity(final EventData identityState) {
		if (identityState == null) {
			Log.debug(CampaignConstants.LOG_TAG, "setIdentity -  Cannot set Identity properties, provided identity data is null.");
			return;
		}

		this.experienceCloudId = identityState.optString(CampaignConstants.EventDataKeys.Identity.VISITOR_ID_MID,
								 "");
	}


}
