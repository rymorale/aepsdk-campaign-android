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

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobilePrivacyStatus;


public class SettingsActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
	}

	public void onOptIn(View view) {
		MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_IN);
	}

	public void onOptOut(View view) {
		MobileCore.setPrivacyStatus(MobilePrivacyStatus.OPT_OUT);

		// Get handle to shared pref
		SharedPreferences pref = getApplicationContext().getSharedPreferences("LinkageFields", 0); // 0 - for private mode
		SharedPreferences.Editor editor = pref.edit();

		// Erase old kv pairs
		editor.clear();
		editor.commit();
	}

	public void onOptUnknown(View view) {
		MobileCore.setPrivacyStatus(MobilePrivacyStatus.UNKNOWN);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}
}
