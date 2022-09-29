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
 * Listens for {@code EventType.CAMPAIGN}, {@code EventSource.REQUEST_RESET} events and queues them for
 * processing by the parent {@code CampaignExtension} in order to clear previously set
 * linkage fields in the SDK.
 *
 * @see EventType#CAMPAIGN
 * @see EventSource#REQUEST_RESET
 * @see CampaignExtension
 */
class CampaignListenerCampaignRequestReset extends ModuleEventListener<CampaignExtension> {


	/**
	 * Constructor
	 *
	 * @param extension parent {@link CampaignExtension} that owns this listener
	 * @param type   {@link EventType} this listener is registered to handle
	 * @param source {@link EventSource} this listener is registered to handle
	 */
	CampaignListenerCampaignRequestReset(final CampaignExtension extension, final EventType type,
										 final EventSource source) {
		super(extension, type, source);
	}

	/**
	 * Listens to {@code EventType#CAMPAIGN}, {@code EventSource#REQUEST_RESET} event.
	 * <p>
	 * Invokes method on the parent {@link CampaignExtension} to queue and
	 * process event to clear previously set linkage fields and download Campaign rules.
	 *
	 * @param event {@link Event} to be processed
	 * @see CampaignExtension#queueAndProcessEvent(Event)
	 */
	@Override
	public void hear(final Event event) {
		parentModule.handleResetLinkageFields(event);

	}

}