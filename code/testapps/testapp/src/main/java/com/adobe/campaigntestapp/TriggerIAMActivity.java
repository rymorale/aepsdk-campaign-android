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

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.adobe.marketing.mobile.Campaign;
import com.adobe.marketing.mobile.MobileCore;

import java.util.HashMap;


public class TriggerIAMActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trigger_iam);
		// Get handle to shared pref
		SharedPreferences pref = getApplicationContext().getSharedPreferences("LinkageFields", 0); // 0 - for private mode

		// Retrieve login status from shared pref
		Boolean loggedIn = pref.getBoolean("loggedIn", false);
		Button logoutButton = (Button) findViewById(R.id.button10);

		if (loggedIn) {
			logoutButton.setVisibility(View.VISIBLE);
		} else {
			logoutButton.setVisibility(View.GONE);
		}
	}

	// Called when the user click on Trigger IAM button
	public void onTriggerIAMClicked(View view) {

		EditText editTextAnalyticsAction = (EditText) findViewById(R.id.editText7);
		String analyticsActionName = editTextAnalyticsAction.getText().toString();

		if (analyticsActionName != null && !analyticsActionName.isEmpty())  {
			MobileCore.trackAction(analyticsActionName, null);
		} else {
			EditText editTextAnalyticsState = (EditText) findViewById(R.id.editText10);
			String analyticsStateName = editTextAnalyticsState.getText().toString();

			if (analyticsStateName != null && !analyticsStateName.isEmpty()) {
				MobileCore.trackState(analyticsStateName, null);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		handleTracking();
	}
	// handle a push or local notification click
	private void handleTracking() {
		// Check to see if this view was opened based on a notification
		Intent intent = getIntent();
		Bundle data = intent.getExtras();

		if (data != null) {
			HashMap<String, Object> userInfo = (HashMap)data.get("NOTIFICATION_USER_INFO");
			String deliveryId = (String)userInfo.get("deliveryId");
			String broadlogId = (String)userInfo.get("broadlogId");

			HashMap<String, Object> contextData = new HashMap<>();

			if (deliveryId != null && broadlogId != null) {
				contextData.put("deliveryId", deliveryId);
				contextData.put("broadlogId", broadlogId);

				// Send Click Tracking since the user did click on the notification
				contextData.put("action", "2");
				MobileCore.collectMessageInfo(contextData);

				// Send Open Tracking since the user opened the app
				contextData.put("action", "1");
				MobileCore.collectMessageInfo(contextData);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	// Called when the user click on Logout button
	public void onLogoutClicked(View view) {
		Campaign.resetLinkageFields();
		// Get handle to shared pref
		SharedPreferences pref = getApplicationContext().getSharedPreferences("LinkageFields", 0); // 0 - for private mode
		SharedPreferences.Editor editor = pref.edit();

		// Set login status to false
		editor.putBoolean("loggedIn", false);
		editor.commit();

		this.finish();
	}

	// Called when the user click on Settings button
	public void onSettingsClicked(View view) {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

}
