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

import java.util.Map;

class CampaignCore {

	private static final String LOG_TAG = CampaignCore.class.getSimpleName();

	EventHub eventHub;

	/**
	 * Constructor
	 *
	 * @param eventHub {@link EventHub} instance
	 */
	CampaignCore(final EventHub eventHub, final ModuleDetails moduleDetails) {
		this(eventHub, moduleDetails, true);
	}

	/**
	 * Constructor
	 *
	 * @param eventHub {@link EventHub} instance
	 * @param registerExtension {@code boolean} indicating whether the extension needs to be registered
	 */
	CampaignCore(final EventHub eventHub, final ModuleDetails moduleDetails, final boolean registerExtension) {
		if (eventHub == null) {
			Log.error(LOG_TAG, "CampaignCore -  Core initialization was unsuccessful (No EventHub instance found!)");
			return;
		}

		this.eventHub = eventHub;

		if (registerExtension) {
			try {
				if (!Module.class.isAssignableFrom(CampaignExtension.class)) {
					Log.error(LOG_TAG,
							  "CampaignCore -  Failed to register %s module class, which is not a subClass of com.adobe.marketing.mobile.Module",
							  CampaignExtension.class.getSimpleName());
					return;
				}

				eventHub.registerModule(CampaignExtension.class, moduleDetails);
				Log.trace(LOG_TAG, "CampaignCore - Registered %s extension", CampaignExtension.class.getSimpleName());
			} catch (InvalidModuleException e) {
				Log.debug(LOG_TAG, "CampaignCore -  Failed to register %s module \n Exception: (%s)",
						  CampaignExtension.class.getSimpleName(), e);
				return;
			}
		}

		Log.debug(LOG_TAG, "Core initialization was successful");
	}


	/**
	 * Dispatches a {@code EventType.CAMPAIGN}, {@code EventSource.REQUEST_IDENTITY} event to set linkage fields in the SDK.
	 * <p>
	 * If the provided {@code linkageFields} Map is null or empty, no event is dispatched.
	 *
	 * @param linkageFields {@code Map<String, String>} containing the linkage fields key-value pairs
	 */
	public void setLinkageFields(final Map<String, String> linkageFields) {

		if (linkageFields == null || linkageFields.isEmpty()) {
			Log.debug(LOG_TAG,
					  "setLinkageFields -  Cannot set Linkage Fields, provided linkage fields map is empty. \n For more information: https://aep-sdks.gitbook.io/docs/using-mobile-extensions/adobe-campaign-standard/adobe-campaign-standard-api-reference#set-linkage-fields");
			return;
		}

		final EventData eventData = new EventData();
		eventData.putStringMap(CampaignConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, linkageFields);

		final Event event = new Event.Builder("SetLinkageFields Event",
											  EventType.CAMPAIGN, EventSource.REQUEST_IDENTITY).setData(eventData).build();

		// dispatch event
		eventHub.dispatch(event);
	}

	/**
	 * Dispatches a {@code EventType.CAMPAIGN}, {@code EventSource.REQUEST_RESET} event to clear previously set
	 * linkage fields in the SDK.
	 *
	 * @see #setLinkageFields(Map)
	 */
	public void resetLinkageFields() {

		final Event event = new Event.Builder("resetLinkageFields Event",
											  EventType.CAMPAIGN, EventSource.REQUEST_RESET).build();

		// dispatch event
		eventHub.dispatch(event);
	}

}
