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

import java.util.HashMap;
import java.util.Map;

public class MockCampaignExtension extends CampaignExtension {

	MockCampaignExtension(final EventHub hub, final PlatformServices services) {
		super(hub, services);
	}

	MockCampaignExtension(final EventHub hub, final PlatformServices services, final CampaignHitsDatabase hitsDatabase) {
		super(hub, services, hitsDatabase);
	}

	boolean processSharedStateUpdateWasCalled = false;
	String processSharedStateUpdateParameterStateName;
	@Override
	void processSharedStateUpdate(final String stateOwner) {
		processSharedStateUpdateParameterStateName = stateOwner;
		processSharedStateUpdateWasCalled = true;
		super.processSharedStateUpdate(stateOwner);
	}

	boolean processMessageEventWasCalled = false;
	Event processMessageEventParameterEvent;
	@Override
	void processMessageEvent(final Event event) {
		processMessageEventParameterEvent = event;
		processMessageEventWasCalled = true;
		super.processMessageEvent(event);
	}

	boolean processConfigurationResponseWasCalled = false;
	Event processConfigurationResponseParameterEvent;
	@Override
	void processConfigurationResponse(final Event event) {
		processConfigurationResponseWasCalled = true;
		processConfigurationResponseParameterEvent = event;
		super.processConfigurationResponse(event);
	}

	boolean processLifecycleUpdateWasCalled = false;
	Event processLifecycleUpdateParameterEvent;
	CampaignState processLifecycleUpdateParameterState;
	@Override
	void processLifecycleUpdate(final Event event, final CampaignState state) {
		processLifecycleUpdateWasCalled = true;
		processLifecycleUpdateParameterEvent = event;
		processLifecycleUpdateParameterState = state;
		super.processLifecycleUpdate(event, state);
	}

	boolean processQueuedEventsWasCalled = false;
	@Override
	void processQueuedEvents() {
		processQueuedEventsWasCalled = true;
		super.processQueuedEvents();
	}

	boolean queueAndProcessEventWasCalled = false;
	Event queueAndProcessEventParameterEvent;
	@Override
	void queueAndProcessEvent(final Event event) {
		queueAndProcessEventWasCalled = true;
		queueAndProcessEventParameterEvent = event;
		super.queueAndProcessEvent(event);
	}

	boolean clearRulesCacheDirectoryWasCalled = false;
	@Override
	void clearRulesCacheDirectory() {
		clearRulesCacheDirectoryWasCalled = true;
		super.clearRulesCacheDirectory();
	}

	boolean handleSetLinkageFieldsWasCalled = false;
	Event handleSetLinkageFieldsParameterEvent;
	Map<String, String> handleSetLinkageFieldsParameterLinkageFields;
	@Override
	void handleSetLinkageFields(final Event event, final Map<String, String> linkageFields) {

		handleSetLinkageFieldsWasCalled = true;
		handleSetLinkageFieldsParameterEvent = event;
		handleSetLinkageFieldsParameterLinkageFields = linkageFields;
		super.handleSetLinkageFields(event, linkageFields);
	}

	boolean handleResetLinkageFieldsWasCalled = false;
	Event handleResetLinkageFieldsParameterEvent;
	@Override
	void handleResetLinkageFields(final Event event) {

		handleResetLinkageFieldsWasCalled = true;
		handleResetLinkageFieldsParameterEvent = event;
		super.handleResetLinkageFields(event);
	}

	boolean getCampaignRulesRemoteDownloaderWasCalled = false;
	String getCampaignRulesRemoteDownloaderParameterUrl;
	Map<String, String> getCampaignRulesRemoteDownloaderParameterRequestPeroperties;
	CampaignRulesRemoteDownloader getCampaignRulesRemoteDownloaderReturnValue;
	@Override
	CampaignRulesRemoteDownloader getCampaignRulesRemoteDownloader(final String url,
			final Map<String, String> requestProperties) {
		getCampaignRulesRemoteDownloaderWasCalled = true;
		getCampaignRulesRemoteDownloaderParameterUrl = url;
		getCampaignRulesRemoteDownloaderParameterRequestPeroperties = requestProperties;

		if (getCampaignRulesRemoteDownloaderReturnValue != null) {
			return getCampaignRulesRemoteDownloaderReturnValue;
		}

		return super.getCampaignRulesRemoteDownloader(url, requestProperties);
	}
}
