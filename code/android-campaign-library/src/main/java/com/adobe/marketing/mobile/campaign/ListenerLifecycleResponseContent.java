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
 * Listens for {@code EventType.LIFECYCLE}, {@code EventSource.RESPONSE_CONTENT} events and queues them for
 * processing by the parent {@code CampaignExtension}.
 *
 * @see com.adobe.marketing.mobile.EventType#LIFECYCLE
 * @see com.adobe.marketing.mobile.EventSource#RESPONSE_CONTENT
 * @see CampaignExtension
 */
class ListenerLifecycleResponseContent implements ExtensionEventListener {
	private static final String SELF_TAG = "ListenerLifecycleResponseContent";
	private final CampaignExtension parentExtension;

	/**
	 * Constructor.
	 *
	 * @param {@link CampaignExtension} which created this listener
	 */
	ListenerLifecycleResponseContent(final CampaignExtension parentExtension) {
		this.parentExtension = parentExtension;
	}

	/**
	 * Listens to {@code EventType#LIFECYCLE}, {@code EventSource#RESPONSE_CONTENT} event.
	 * <p>
	 * If Lifecycle context data contains Launch Event, the parent module queues then processes the event and sends a
	 * registration request to the configured Campaign server.
	 *
	 * @param event {@link Event} to be processed
	 */
	@Override
	public void hear(final Event event) {
		final Map<String, Object> eventData = event.getEventData();

		if (eventData == null || eventData.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "Ignoring Lifecycle response event with null or empty EventData.");
			return;
		}

		final Map<String, String> lifecycleData = (Map<String, String>) eventData.get(
				CampaignConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA);

		if (lifecycleData == null || lifecycleData.isEmpty()
				|| !lifecycleData.containsKey(CampaignConstants.EventDataKeys.Lifecycle.LAUNCH_EVENT)) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"Ignoring Lifecycle response event, lifecycle context data is null or does not contain launch Event.");
			return;
		}

		if (parentExtension == null) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"hear - The parent extension, associated with this listener is null, ignoring the event.");
			return;
		}

		parentExtension.getExecutor().execute(() -> {
			parentExtension.queueEvent(event);
			parentExtension.processEvents();
		});
	}

}