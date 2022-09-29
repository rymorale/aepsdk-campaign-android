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
 * Listens for {@code EventType.GENERIC_DATA}, {@code EventSource.OS} events and invokes method on
 * the parent {@code CampaignExtension} to send notification tracking information to Campaign.
 */
class CampaignListenerGenericDataOS extends ModuleEventListener<CampaignExtension> {

	/**
	 * Constructor
	 *
	 * @param extension {@link CampaignExtension} that owns this listener
	 * @param type {@link EventType} this listener is registered to handle
	 * @param source {@link EventSource} this listener is registered to handle
	 */
	CampaignListenerGenericDataOS(final CampaignExtension extension, final EventType type,
								  final EventSource source) {
		super(extension, type, source);
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

		if (event == null || event.getData() == null || event.getData().isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, "Ignoring Generic data OS event with null or empty EventData.");
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