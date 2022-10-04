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

public class MockFullscreenMessage extends FullScreenMessage {

	MockFullscreenMessage(CampaignExtension extension, PlatformServices platformServices,
						  final CampaignRuleConsequence consequence) throws CampaignMessageRequiredFieldMissingException,
		MissingPlatformServicesException {
		super(extension, platformServices, consequence);
	}

	boolean showMessageWasCalled = false;

	@Override
	protected void showMessage() {
		showMessageWasCalled = true;
		super.showMessage();
	}

	@Override
	protected void downloadAssets() {
		super.downloadAssets();
	}

	boolean triggeredWasCalled = false;
	@Override
	protected void triggered() {
		triggeredWasCalled = true;
		super.triggered();
	}

	boolean viewedWasCalled = false;
	@Override
	protected void viewed() {
		viewedWasCalled = true;
		super.viewed();
	}

	boolean clickedThroughWasCalled = false;
	@Override
	protected void clickedThrough() {
		clickedThroughWasCalled = true;
		super.clickedThrough();
	}

	boolean clickedWithDataWasCalled = false;
	Map<String, String> clickedWithDataParameterData;
	@Override
	protected void clickedWithData(final Map<String, String> data) {
		clickedWithDataWasCalled = true;
		clickedWithDataParameterData = data;
		super.clickedWithData(data);
	}
}
