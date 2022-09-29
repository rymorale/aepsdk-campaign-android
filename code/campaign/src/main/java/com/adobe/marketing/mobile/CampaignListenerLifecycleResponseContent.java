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
 * Listens for {@code EventType.LIFECYCLE}, {@code EventSource.RESPONSE_CONTENT} events and queues them for
 * processing by the parent {@code CampaignExtension}.
 *
 * @see EventType#LIFECYCLE
 * @see EventSource#RESPONSE_CONTENT
 * @see CampaignExtension
 */
class CampaignListenerLifecycleResponseContent extends ModuleEventListener<CampaignExtension> {
	/**
	 * Constructor
	 *
	 * @param extension parent {@link CampaignExtension} that owns this listener
	 * @param type   {@link EventType} this listener is registered to handle
	 * @param source {@link EventSource} this listener is registered to handle
	 */
	CampaignListenerLifecycleResponseContent(final CampaignExtension extension, final EventType type,
			final EventSource source) {
		super(extension, type, source);
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
		final EventData eventData = event.getData();

		if (eventData == null || eventData.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, "Ignoring Lifecycle response event with null or empty EventData.");
			return;
		}

		final Map<String, String> lifecycleData = eventData.optStringMap(
					CampaignConstants.EventDataKeys.Lifecycle.LIFECYCLE_CONTEXT_DATA, null);

		if (lifecycleData == null || lifecycleData.isEmpty()
				|| !lifecycleData.containsKey(CampaignConstants.EventDataKeys.Lifecycle.LAUNCH_EVENT)) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "Ignoring Lifecycle response event, lifecycle context data is null or does not contain launch Event.");
			return;
		}

		parentModule.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				parentModule.queueAndProcessEvent(event);
			}
		});
	}

}
