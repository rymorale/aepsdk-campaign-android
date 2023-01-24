///*
//  Copyright 2023 Adobe. All rights reserved.
//  This file is licensed to you under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License. You may obtain a copy
//  of the License at http://www.apache.org/licenses/LICENSE-2.0
//  Unless required by applicable law or agreed to in writing, software distributed under
//  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
//  OF ANY KIND, either express or implied. See the License for the specific language
//  governing permissions and limitations under the License.
//*/
//
//package com.adobe.marketing.mobile.campaign;
//
//import android.content.Context;
//
//import com.adobe.marketing.mobile.services.DeviceInforming;
//import com.adobe.marketing.mobile.services.ServiceProvider;
//
//import java.io.File;
//import java.util.UUID;
//
//// need to extend E2EAndroidSystemInfoService because the E2EAndroidSystemInfoService returns a random cache directory each time
//class CampaignAndroidSystemInfoService extends DeviceInforming {
//	static String campaignCacheDirectory = String.valueOf(UUID.randomUUID()).replaceAll("-", "");
//	public CampaignAndroidSystemInfoService() {
//	}
//
//	@Override
//	public File getApplicationCacheDir() {
//		Context context = ServiceProvider.getInstance().getAppContextService().getApplicationContext();
//		File systemCacheDir = (context == null ? null : context.getCacheDir());
//		File tempDir = new File(systemCacheDir, campaignCacheDirectory);
//		tempDir.mkdir();
//		return tempDir;
//	}
//}
//
//public class CampaignTestingPlatform extends TestingPlatform {
//	E2EAndroidNetworkService e2EAndroidNetworkService = new E2EAndroidNetworkService(this.getSystemInfoService());
//	static E2EAndroidSystemInfoService e2EAndroidSystemInfoService = new CampaignAndroidSystemInfoService();
//
//	CampaignTestingPlatform() {
//	}
//
//	public AndroidNetworkService getNetworkService() {
//		return this.e2EAndroidNetworkService;
//	}
//
//	public SystemInfoService getSystemInfoService() {
//		return this.e2EAndroidSystemInfoService;
//	}
//}
