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

public class MockCampaignMessage extends CampaignMessage {

	MockCampaignMessage(CampaignExtension extension, PlatformServices services, CampaignRuleConsequence consequence)
	throws CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		super(extension, services, consequence);
	}

	boolean shouldDownloadAssetsWasCalled = false;
	boolean shouldDownloadAssetsReturnBooleanValue = false;
	@Override
	boolean shouldDownloadAssets() {
		shouldDownloadAssetsWasCalled = true;
		return shouldDownloadAssetsReturnBooleanValue;
	}

	boolean callDispatchMessageInteractionWasCalled = false;
	Map<String, String> callDispatchMessageInteractionParameterData;
	@Override
	protected void callDispatchMessageInteraction(final Map<String, String> data) {
		callDispatchMessageInteractionWasCalled = true;
		callDispatchMessageInteractionParameterData = data;
	}

	boolean openUrlWasCalled = false;
	String openUrlParameterUrl;
	@Override
	protected void openUrl(final String url) {
		openUrlWasCalled = true;
		openUrlParameterUrl = url;
		super.openUrl(url);
	}

	boolean showMessagesWasCalled = false;
	@Override
	void showMessage() {
		showMessagesWasCalled = true;
	}
}
