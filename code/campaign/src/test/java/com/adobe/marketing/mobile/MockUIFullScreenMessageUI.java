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

public class MockUIFullScreenMessageUI implements UIService.UIFullScreenMessage {

	boolean showCalled = false;
	@Override
	public void show() {
		showCalled = true;
	}

	boolean openUrlCalled = false;
	String openUrlParamterUrl;
	@Override
	public void openUrl(String url) {
		openUrlCalled = true;
		openUrlParamterUrl = url;
	}

	boolean removeCalled = false;
	@Override
	public void remove() {
		removeCalled = true;

	}

	boolean setLocalAssetsMapCalled = false;
	Map<String, String> setLocalAssetsMapParameterAssetMap = null;

	@Override
	public void setLocalAssetsMap(final Map<String, String> assetMap) {
		setLocalAssetsMapCalled = true;

		if (assetMap != null && !assetMap.isEmpty()) {
			this.setLocalAssetsMapParameterAssetMap = new HashMap<String, String>(assetMap);
		}
	}
}
