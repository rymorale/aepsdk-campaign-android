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

/**
 * Listens for {@code EventType.CAMPAIGN}, {@code EventSource.REQUEST_RESET} events and queues them for
 * processing by the parent {@code CampaignExtension} in order to clear previously set
 * linkage fields in the SDK.
 *
 * @see com.adobe.marketing.mobile.EventType#CAMPAIGN
 * @see com.adobe.marketing.mobile.EventSource#REQUEST_RESET
 * @see CampaignExtension
 */
class ListenerCampaignRequestReset implements ExtensionEventListener {
	private static final String SELF_TAG = "ListenerCampaignRequestReset";
	private final CampaignExtension parentExtension;

	/**
	 * Constructor.
	 *
	 * @param {@link CampaignExtension} which created this listener
	 */
	ListenerCampaignRequestReset(final CampaignExtension parentExtension) {
		this.parentExtension = parentExtension;
	}

	/**
	 * Listens to {@code EventType#CAMPAIGN}, {@code EventSource#REQUEST_RESET} event.
	 * <p>
	 * Invokes method on the parent {@link CampaignExtension} to queue and
	 * process event to clear previously set linkage fields and download Campaign rules.
	 *
	 * @param event {@link Event} to be processed
	 * @see CampaignExtension#queueEvent(Event)
	 */
	@Override
	public void hear(final Event event) {
		if (event == null || event.getEventData() == null) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"hear -  Ignoring Campaign request identity event with null or empty EventData.");
			return;
		}

		if (parentExtension == null) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"hear - The parent extension, associated with this listener is null, ignoring the event.");
			return;
		}
		parentExtension.handleResetLinkageFields(event);
	}

}