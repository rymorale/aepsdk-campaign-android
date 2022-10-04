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

import com.adobe.marketing.mobile.campaign.BuildConfig;

import java.util.Map;

public class Campaign {
	private final static String TAG = Campaign.class.getSimpleName();
	private static final String NULL_CONTEXT_MESSAGE = "Context must be set before calling SDK methods";
	private static CampaignCore campaignCore;

	private Campaign() {

	}

	public static String extensionVersion() {
		return BuildConfig.LIB_VERSION;
	}


	public static void registerExtension() throws InvalidInitException {
		Core core = MobileCore.getCore();

		if (core == null) {
			Log.error(TAG,
					  "Unable to register Campaign since MobileCore is not initialized properly. For more details refer to https://aep-sdks.gitbook.io/docs/using-mobile-extensions/mobile-core");
			throw  new InvalidInitException();
		}

		try {
			//MobileCore may not be loaded or present (because may be Core extension was not
			//available). In that case, the Campaign extension will not initialize itself
			campaignCore = new CampaignCore(core.eventHub, new CampaignModuleDetails());
		} catch (Exception e) {
			throw new InvalidInitException();
		}
	}

	/**
	 * Sets the Campaign linkage fields (CRM IDs) in the mobile SDK to be used for downloading personalized messages from Campaign.
	 * <p>
	 * The set linkage fields are stored as base64 encoded JSON string in memory and they are sent in a custom HTTP header 'X-InApp-Auth'
	 * in all future Campaign rules download requests until {@link #resetLinkageFields()} is invoked. These in-memory variables are also
	 * lost in the wake of an Application crash event or upon graceful Application termination or when the privacy status is updated to
	 * {@link MobilePrivacyStatus#OPT_OUT}.
	 * <p>
	 * This method clears cached rules from previous download before triggering a rules download request to the configured Campaign server.
	 * If the current SDK privacy status is not {@code MobilePrivacyStatus.OPT_IN}, no rules download happens.
	 *
	 * @param linkageFields a {@code Map<String, String>} containing linkage field key-value pairs
	 */
	public static void setLinkageFields(final Map<String, String> linkageFields) {

		if (campaignCore == null) {
			Log.error(TAG,
					  "Failed to set linkage fields (%s). For more details refer to https://aep-sdks.gitbook.io/docs/using-mobile-extensions/adobe-campaign-standard/adobe-campaign-standard-api-reference#set-linkage-fields",
					  NULL_CONTEXT_MESSAGE);
			return;
		}

		campaignCore.setLinkageFields(linkageFields);
	}

	/**
	 * Clears previously stored linkage fields in the mobile SDK and triggers Campaign rules download request to the configured Campaign server.
	 * <p>
	 * This method unregisters any previously registered rules with the Event Hub and clears cached rules from previous download.
	 * If the current SDK privacy status is not {@link MobilePrivacyStatus#OPT_IN}, no rules download happens.
	 */
	public static void resetLinkageFields() {

		if (campaignCore == null) {
			Log.error(TAG,
					  "Failed to reset linkage fields (%s). For more details refer to https://aep-sdks.gitbook.io/docs/using-mobile-extensions/adobe-campaign-standard/adobe-campaign-standard-api-reference#reset-linkage-fields",
					  NULL_CONTEXT_MESSAGE);
			return;
		}

		campaignCore.resetLinkageFields();
	}
}
