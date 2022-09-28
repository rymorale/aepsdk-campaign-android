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

/**
 * {@code CampaignDispatcherGenericDataOS} class dispatches {@code EventType.GENERIC_DATA},
 * {@code EventSource.OS} events.
 */

class CampaignDispatcherGenericDataOS extends ModuleEventDispatcher<CampaignExtension> {

	private static final String INTERNAL_GENERIC_DATA_EVENT_NAME = "InternalGenericDataEvent";

	/**
	 * Constructor.
	 *
	 * @param hub {@link EventHub} instance used by this dispatcher
	 * @param extension parent {@link CampaignExtension} class that owns this dispatcher
	 */
	CampaignDispatcherGenericDataOS(final EventHub hub, final CampaignExtension extension) {
		super(hub, extension);
	}

	/**
	 * Dispatches a {@code EventType.GENERIC_DATA}, {@code EventSource.OS} event with the provided
	 * message information {@code broadlogId}, {@code deliveryId} and {@code action}.
	 * <p>
	 * This method is called internally by the {@code Campaign} extension.
	 *
	 * @param broadlogId {@link String} containing message broadlogId
	 * @param deliveryId {@code String} containing message deliveryId
	 * @param action {@code String} containing message action value
	 */
	void dispatch(final String broadlogId, final String deliveryId, final String action) {
		final EventData eventData = new EventData();

		eventData.putString(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_BROADLOG_ID, broadlogId);
		eventData.putString(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_DELIVERY_ID, deliveryId);
		eventData.putString(CampaignConstants.EventDataKeys.Campaign.TRACK_INFO_KEY_ACTION, action);

		final Event messageEvent = new Event.Builder(INTERNAL_GENERIC_DATA_EVENT_NAME,
				EventType.GENERIC_DATA, EventSource.OS).setData(eventData).build();

		dispatch(messageEvent);
	}
}
