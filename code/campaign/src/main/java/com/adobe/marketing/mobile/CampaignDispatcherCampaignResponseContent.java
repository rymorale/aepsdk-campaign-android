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

import java.util.Map;

/**
 * {@code CampaignDispatcherCampaignResponseContent} class dispatches {@code EventType.CAMPAIGN},
 * {@code EventSource.RESPONSE_CONTENT} events.
 */

class CampaignDispatcherCampaignResponseContent extends ModuleEventDispatcher<CampaignExtension> {

	private static final String DATA_FOR_MESSAGE_REQUEST_EVENT_NAME = "DataForMessageRequest";

	/**
	 * Constructor.
	 *
	 * @param hub {@link EventHub} instance used by this dispatcher
	 * @param extension parent {@link CampaignExtension} class that owns this dispatcher
	 */
	CampaignDispatcherCampaignResponseContent(final EventHub hub, final CampaignExtension extension) {
		super(hub, extension);
	}

	/**
	 * Dispatches a {@code EventType#CAMPAIGN}, {@code EventSource#RESPONSE_CONTENT} event with the provided
	 * {@code messageData} for tracking purposes.
	 * <p>
	 * If {@code messageData} is null or empty or does not contain a non null key-value pair, no event is dispatched.
	 *
	 * @param messageData {@link EventData} object containing message tracking information
	 */
	void dispatch(final Map<String, String> messageData) {
		if (messageData == null || messageData.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "dispatch -  Cannot dispatch Campaign response event, message interaction data is null or empty.");
			return;
		}

		final EventData eventData = new EventData();

		for (Map.Entry<String, String> entry : messageData.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue();

			if (key != null && value != null) {
				eventData.putString(key, value);
			}
		}

		if (eventData.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "Cannot dispatch Campaign response event, message interaction data does not have a valid key-value pair.");
			return;
		}

		final Event messageEvent = new Event.Builder(DATA_FOR_MESSAGE_REQUEST_EVENT_NAME,
				EventType.CAMPAIGN, EventSource.RESPONSE_CONTENT).setData(eventData).build();

		dispatch(messageEvent);
	}
}
