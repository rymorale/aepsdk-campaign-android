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

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.adobe.marketing.mobile.MobileCore;
import android.view.View;
import android.widget.EditText;

public class SignUpActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sign_up);
	}

	// Called when the user click on Submit button
	public void onSubmitClicked(View view) {
		EditText firstNameField = findViewById(R.id.editText);
		EditText lastNameField = findViewById(R.id.editText2);
		EditText emailField = findViewById(R.id.editText3);
		EditText userNameField = findViewById(R.id.editText4);
		EditText passwordField = findViewById(R.id.editText6);

		String firstName = firstNameField.getText().toString();
		String lastName = lastNameField.getText().toString();
		String email = emailField.getText().toString();
		String userName = userNameField.getText().toString();
		String password = passwordField.getText().toString();

		if ((firstName != null && !firstName.isEmpty())
				&& (lastName != null && !lastName.isEmpty())
				&& (email != null && !email.isEmpty())
				&& (userName != null && !userName.isEmpty())
				&& (password != null && !password.isEmpty())
		   )  {

			// Get handle to shared pref
			SharedPreferences pref = getApplicationContext().getSharedPreferences("LinkageFields", 0); // 0 - for private mode
			Editor editor = pref.edit();

			// Erase old kv pairs
			editor.clear();
			editor.commit();

			// Store new kv pairs in shared pref
			editor.putString("cusFirstName", firstName);
			editor.putString("cusLastName", lastName);
			editor.putString("cusEmail", email);

			editor.putString("cusUsername", userName);
			editor.putString("cusPassword", password);

			editor.putBoolean("loggedIn", true);

			editor.commit();


		} else {

			AlertHelper.displayErrorAlert("All fields have not been populated!", this);
			return;
		}

		System.out.println(firstName);
		System.out.println(lastName);
		System.out.println(email);
		System.out.println(userName);

		this.finish();

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
