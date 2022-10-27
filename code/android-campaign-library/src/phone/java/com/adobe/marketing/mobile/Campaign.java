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

import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.Campaign.LINKAGE_FIELDS;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;

import com.adobe.marketing.mobile.services.Log;

import java.util.HashMap;
import java.util.Map;

public class Campaign {
	private final static String EXTENSION_VERSION = "2.0.0";

	private Campaign() {
	}

	/**
	 * Returns the version of the {@link Campaign} extension
	 *
	 * @return The version as {@code String}
	 */
	public static String extensionVersion() {
		return EXTENSION_VERSION;
	}

	/**
	 * Dispatches a {@code EventType.CAMPAIGN}, {@code EventSource.REQUEST_IDENTITY} event to set linkage fields in the SDK.
	 * <p>
	 * If the provided {@code linkageFields} Map is null or empty, no event is dispatched.
	 *
	 * @param linkageFields {@code Map<String, String>} containing the linkage fields key-value pairs
	 */
	public static void setLinkageFields(final Map<String, String> linkageFields) {
		if (linkageFields == null || linkageFields.isEmpty()) {
			Log.debug(LOG_TAG, "setLinkageFields",
					"setLinkageFields -  Cannot set Linkage Fields, provided linkage fields map is empty. \n For more information: https://aep-sdks.gitbook.io/docs/using-mobile-extensions/adobe-campaign-standard/adobe-campaign-standard-api-reference#set-linkage-fields");
			return;
		}

		final Map<String, Object> eventData = new HashMap<>();
		eventData.put(LINKAGE_FIELDS, linkageFields);

		final Event event = new Event.Builder("SetLinkageFields Event",
				EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY).setEventData(eventData).build();

		// dispatch event
		MobileCore.dispatchEvent(event);
	}

	/**
	 * Dispatches a {@code EventType.CAMPAIGN}, {@code EventSource.REQUEST_RESET} event to clear previously set
	 * linkage fields in the SDK.
	 *
	 * @see #setLinkageFields(Map)
	 */
	public static void resetLinkageFields() {

		final Event event = new Event.Builder("resetLinkageFields Event",
				EventType.CAMPAIGN, EventSource.REQUEST_RESET).build();

		// dispatch event
		MobileCore.dispatchEvent(event);
	}

}