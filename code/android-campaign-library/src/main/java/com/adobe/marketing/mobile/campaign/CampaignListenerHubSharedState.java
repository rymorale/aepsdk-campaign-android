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

/**
 * Listens for {@code EventType.HUB}, {@code EventSource.SHARED_STATE} events and passes them to
 * the parent {@code CampaignExtension} to kick-off queue processing.
 *
 * @see EventType#HUB
 * @see EventSource#SHARED_STATE
 * @see CampaignExtension
 */
class CampaignListenerHubSharedState extends ModuleEventListener<CampaignExtension> {

	/**
	 * Constructor
	 *
	 * @param extension parent {@link CampaignExtension} that owns this listener
	 * @param type  {@link EventType} this listener is registered to handle
	 * @param source {@link EventSource} this listener is registered to handle
	 */
	CampaignListenerHubSharedState(final CampaignExtension extension, final EventType type, final EventSource source) {
		super(extension, type, source);
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
	public void hear(final Event event) {
		final EventData eventData = event.getData();

		if (eventData == null || eventData.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, "Ignoring Hub shared state event with null or empty EventData.");
			return;
		}

		final String stateName = eventData.optString(CampaignConstants.EventDataKeys.STATE_OWNER, null);

		if (StringUtils.isNullOrEmpty(stateName)) {
			Log.debug(CampaignConstants.LOG_TAG, "Ignoring Hub shared state event, state owner is null or empty.");
			return;
		}

		parentModule.processSharedStateUpdate(stateName);
	}
}