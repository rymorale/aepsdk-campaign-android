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
 * Listens for {@code EventType.GENERIC_DATA}, {@code EventSource.OS} events and invokes method on
 * the parent {@code CampaignExtension} to send notification tracking information to Campaign.
 */
class ListenerGenericDataOS implements ExtensionEventListener {
	private static final String SELF_TAG = "ListenerGenericDataOS";
	private final CampaignExtension parentExtension;

	/**
	 * Constructor.
	 *
	 * @param {@link CampaignExtension} which created this listener
	 */
	ListenerGenericDataOS(final CampaignExtension parentExtension) {
		this.parentExtension = parentExtension;
	}

	/**
	 * Listens to {@code EventType.GENERIC_DATA}, {@code EventSource.OS} event.
	 * <p>
	 * Invokes method on the parent {@link CampaignExtension} to queue then process the event in order to
	 * send a notification tracking request to the configured Campaign server.
	 *
	 * @param event {@link Event} to be processed
	 */
	@Override
	public void hear(final Event event) {
		if (event == null || event.getEventData() == null || event.getEventData().isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "Ignoring Generic data OS event with null or empty EventData.");
			return;
		}

		if (parentExtension == null) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"hear - The parent extension, associated with this listener is null, ignoring the event.");
			return;
		}

		parentExtension.queueEvent(event);
		parentExtension.processEvents();
	}
}