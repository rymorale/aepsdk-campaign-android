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
 * Listens for {@code EventType.CAMPAIGN}, {@code EventSource.REQUEST_IDENTITY} events and queues them for
 * processing by the parent {@code CampaignExtension} in order to set linkage fields in the SDK.
 *
 * @see EventType#CAMPAIGN
 * @see EventSource#REQUEST_IDENTITY
 * @see CampaignExtension
 */
class CampaignListenerCampaignRequestIdentity extends ModuleEventListener<CampaignExtension> {


	/**
	 * Constructor
	 *
	 * @param extension parent {@link CampaignExtension} that owns this listener
	 * @param type   {@link EventType} this listener is registered to handle
	 * @param source {@link EventSource} this listener is registered to handle
	 */
	CampaignListenerCampaignRequestIdentity(final CampaignExtension extension, final EventType type,
											final EventSource source) {
		super(extension, type, source);
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
		final EventData eventData = event.getData();

		if (eventData == null || eventData.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, "hear -  Ignoring Campaign request identity event with null or empty EventData.");
			return;
		}

		final Map<String, String> linkageFields = eventData.optStringMap(
					CampaignConstants.EventDataKeys.Campaign.LINKAGE_FIELDS, null);

		if (linkageFields == null || linkageFields.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "Ignoring Campaign request identity event, linkage fields are null or does not contain any key-value pairs.");
			return;
		}

		parentModule.handleSetLinkageFields(event, linkageFields);
	}

}
