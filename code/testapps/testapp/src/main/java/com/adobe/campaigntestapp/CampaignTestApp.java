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

package com.adobe.campaigntestapp;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Identity;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Lifecycle;
import com.adobe.marketing.mobile.Campaign;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.Signal;
import com.adobe.marketing.mobile.UserProfile;
import com.adobe.marketing.mobile.InvalidInitException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

public class CampaignTestApp extends Application {

	private static Application application;

	@Override
	public void onCreate() {
		super.onCreate();
		MobileCore.setApplication(this);
		MobileCore.setLogLevel(LoggingMode.DEBUG);

		try {
			Campaign.registerExtension();
			UserProfile.registerExtension();
			Identity.registerExtension();
			Lifecycle.registerExtension();
			Signal.registerExtension();
			MobileCore.start(new AdobeCallback() {
				@Override
				public void call(Object o) {
					// mobile property: ryan-new, org: Marketing Cloud In App-Messaging Service
					MobileCore.configureWithAppID("launch-EN38885fd18c02433daa8c090382d81123-development");
				}
			});
		} catch (InvalidInitException e) {
			e.printStackTrace();

		}

		application = this;
		FirebaseInstanceId.getInstance().getInstanceId()
		.addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
			@Override
			public void onComplete(@NonNull Task<InstanceIdResult> task) {
				if (!task.isSuccessful()) {
					Log.w("CampaignTestApp", "getInstanceId failed", task.getException());
					return;
				}

				// Get new Instance ID token
				String token = task.getResult().getToken();

				// Log and toast
				System.out.println("CampaignTestApp token: " + token);


				MobileCore.setPushIdentifier(token);
			}
		});
		// compare to latest versions at https://bintray.com/eaps/mobileservicesdk
		Log.d("Core version ", MobileCore.extensionVersion());
		Log.d("Campaign version ", Campaign.extensionVersion());
		Log.d("UserProfile version ", UserProfile.extensionVersion());
		Log.d("Identity version ", Identity.extensionVersion());
		Log.d("Lifecycle version ", Lifecycle.extensionVersion());
		Log.d("Signal version ", Signal.extensionVersion());
	}

	public static Application getApplication() {
		return application;
	}

	public static Context getContext() {
		return getApplication().getApplicationContext();
	}
}
