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

import androidx.annotation.NonNull;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionEventListener;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.Map;

/**
 * Listens for {@code EventType.HUB}, {@code EventSource.SHARED_STATE} events and passes them to
 * the parent {@code CampaignExtension} to kick-off queue processing.
 *
 * @see com.adobe.marketing.mobile.EventType#HUB
 * @see com.adobe.marketing.mobile.EventSource#SHARED_STATE
 * @see CampaignExtension
 */
class ListenerHubSharedState implements ExtensionEventListener {
	private static final String SELF_TAG = "ListenerHubSharedState";
	private final CampaignExtension parentExtension;

	/**
	 * Constructor.
	 *
	 * @param {@link CampaignExtension} which created this listener
	 */
	ListenerHubSharedState(final CampaignExtension parentExtension) {
		this.parentExtension = parentExtension;
	}

	/**
	 * Listens to {@code EventType#HUB}, {@code EventSource#SHARED_STATE} event.
	 * <p>
	 * If there is an event owner, passes the owner's name to the parent {@link CampaignExtension} to
	 * potentially kick-off queue processing.
	 *
	 * @param event {@link Event} to be processed
	 * @see CampaignExtension#processSharedStateUpdate(String)
	 */
	@Override
	public void hear(@NonNull Event event) {
		final Map<String, Object> eventData = event.getEventData();

		if (eventData == null || eventData.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "Ignoring Hub shared state event with null or empty EventData.");
			return;
		}

		final String stateName = (String) eventData.get(CampaignConstants.EventDataKeys.STATE_OWNER);

		if (StringUtils.isNullOrEmpty(stateName)) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG, "Ignoring Hub shared state event, state owner is null or empty.");
			return;
		}

		if (parentExtension == null) {
			Log.debug(CampaignConstants.LOG_TAG, SELF_TAG,
					"hear - The parent extension, associated with this listener is null, ignoring the event.");
			return;
		}

		parentExtension.processSharedStateUpdate(stateName);
	}
}