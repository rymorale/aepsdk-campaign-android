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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionEventListener;
import com.adobe.marketing.mobile.services.Log;

import java.util.Map;

/**
 * Listens for {@code EventType.CAMPAIGN}, {@code EventSource.REQUEST_IDENTITY} events and queues them for
 * processing by the parent {@code CampaignExtension} in order to set linkage fields in the SDK.
 *
 * @see com.adobe.marketing.mobile.EventType#CAMPAIGN
 * @see com.adobe.marketing.mobile.EventSource#REQUEST_IDENTITY
 * @see CampaignExtension
 */
class ListenerCampaignRequestIdentity implements ExtensionEventListener {
	private static final String SELF_TAG = "ListenerCampaignRequestIdentity";
	private final CampaignExtension parentExtension;

	/**
	 * Constructor.
	 *
	 * @param {@link CampaignExtension} which created this listener
	 */
	ListenerCampaignRequestIdentity(final CampaignExtension parentExtension) {
		this.parentExtension = parentExtension;
	}

	/**
	 * Listens to {@code EventType#CAMPAIGN}, {@code EventSource#REQUEST_IDENTITY} event.
	 * <p>
	 *  Invokes method on the parent {@link CampaignExtension} to queue then
	 *  process the event to set linkage fields
	 *  in the SDK and to download personalized Campaign rules.
	 *
	 * @param event {@link Event} to be processed
	 */
	@Override
	public void hear(final Event event) {
		if (event == null || event.getEventData() == null) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"hear - Ignoring Campaign request identity event with null or empty EventData.");
			return;
		}

		if (parentExtension == null) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"hear - The parent extension, associated with this listener is null, ignoring the event.");
			return;
		}

		final Map<String, Object> eventData = event.getEventData();
		final Map<String, String> linkageFields = (Map<String, String>) eventData.get(CampaignConstants.EventDataKeys.Campaign.LINKAGE_FIELDS);

		if (linkageFields == null || linkageFields.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "hear - Ignoring Campaign request identity event, linkage fields are null or does not contain any key-value pairs.");
			return;
		}
		parentExtension.handleSetLinkageFields(event, linkageFields);
	}

}