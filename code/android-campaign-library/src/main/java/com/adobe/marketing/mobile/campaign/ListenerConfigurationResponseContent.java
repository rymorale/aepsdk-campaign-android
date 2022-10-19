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
 * Listens for {@code EventType.CONFIGURATION}, {@code EventSource.RESPONSE_CONTENT} events and invokes method on
 * the parent {@code CampaignExtension} to process mobile privacy status change and trigger Campaign rules download.
 */
class ListenerConfigurationResponseContent implements ExtensionEventListener {
	private static final String SELF_TAG = "ListenerConfigurationResponseContent";
	private final CampaignExtension parentExtension;

	/**
	 * Constructor.
	 *
	 * @param {@link CampaignExtension} which created this listener
	 */
	ListenerConfigurationResponseContent(final CampaignExtension parentExtension) {
		this.parentExtension = parentExtension;
	}

	/**
	 * Listens to {@code EventType#CONFIGURATION}, {@code EventSource#RESPONSE_CONTENT} event.
	 * <p>
	 * Invokes method on the parent {@link CampaignExtension} to handle change in {@link com.adobe.marketing.mobile.MobilePrivacyStatus} and to
	 * queue event for downloading Campaign rules.
	 *
	 * @param event {@link Event} to be processed
	 * @see CampaignExtension#processConfigurationResponse(Event)
	 */
	@Override
	public void hear(final Event event) {
		if (event == null || event.getEventData() == null) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"hear - Ignoring Configuration response event with null EventData.");
			return;
		}

		if (parentExtension == null) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"hear - The parent extension, associated with this listener is null, ignoring the event.");
			return;
		}
		parentExtension.processConfigurationResponse(event);
	}
}