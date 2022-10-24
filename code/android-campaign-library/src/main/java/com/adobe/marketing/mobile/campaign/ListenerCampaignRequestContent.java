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
 * Listens for {@code EventType.CAMPAIGN}, {@code EventSource.REQUEST_CONTENT} events and invokes method on the
 * parent {@code CampaignExtension} for displaying in-app messages or for downloading/ caching their remote assets.
 *
 * @see com.adobe.marketing.mobile.EventType#CAMPAIGN
 * @see com.adobe.marketing.mobile.EventSource#REQUEST_CONTENT
 * @see CampaignExtension
 */
class ListenerCampaignRequestContent implements ExtensionEventListener {
	private static final String SELF_TAG = "ListenerCampaignRequestContent";
	private final CampaignExtension parentExtension;

	/**
	 * Constructor.
	 *
	 * @param {@link CampaignExtension} which created this listener
	 */
	ListenerCampaignRequestContent(final CampaignExtension parentExtension) {
		this.parentExtension = parentExtension;
	}

	/**
	 * Listens to {@code EventType#CAMPAIGN}, {@code EventSource#REQUEST_CONTENT} event.
	 * <p>
	 * Invokes method on the parent {@link CampaignExtension} to handle loaded or triggered Campaign rule consequence
	 * passed in the {@code Map<String, Object} event data.
	 * <p>
	 * If there is a triggered consequence, the parent {@code CampaignExtension} will process the consequence.
	 * <p>
	 * If there is a loaded consequence, the parent {@code CampaignExtension} will perform any necessary pre-fetching
	 * and caching tasks for the consequence's remote assets.
	 *
	 * @param event {@link Event} to be processed
	 * @see CampaignExtension#processMessageEvent(Event)
	 */
	@Override
	public void hear(final Event event) {
		if (event == null || event.getEventData() == null) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"hear - Ignoring Campaign request event because a null/empty event or EventData was found.");
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