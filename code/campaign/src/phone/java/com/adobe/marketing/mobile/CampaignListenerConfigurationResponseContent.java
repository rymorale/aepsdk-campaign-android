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
 * Listens for {@code EventType.CONFIGURATION}, {@code EventSource.RESPONSE_CONTENT} events and invokes method on
 * the parent {@code CampaignExtension} to process mobile privacy status change and trigger Campaign rules download.
 */
class CampaignListenerConfigurationResponseContent extends ModuleEventListener<CampaignExtension> {

	/**
	 * Constructor
	 *
	 * @param extension {@link CampaignExtension} that owns this listener
	 * @param type {@link EventType} this listener is registered to handle
	 * @param source {@link EventSource} this listener is registered to handle
	 */
	CampaignListenerConfigurationResponseContent(final CampaignExtension extension, final EventType type,
			final EventSource source) {
		super(extension, type, source);
	}

	/**
	 * Listens to {@code EventType#CONFIGURATION}, {@code EventSource#RESPONSE_CONTENT} event.
	 * <p>
	 * Invokes method on the parent {@link CampaignExtension} to handle change in {@link MobilePrivacyStatus} and to
	 * queue event for downloading Campaign rules.
	 *
	 * @param event {@link Event} to be processed
	 * @see CampaignExtension#processConfigurationResponse(Event)
	 */
	@Override
	public void hear(final Event event) {
		EventData eventData = event.getData();

		if (eventData == null) {
			Log.debug(CampaignConstants.LOG_TAG, "Ignoring Configuration response event with null EventData.");
			return;
		}

		parentModule.processConfigurationResponse(event);
	}
}