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

package com.example.campaign_push_notification_test_app.storage

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.annotation.NonNull

object PreferenceManager {

    private const val KEY_NAME = "user_name"
    private const val KEY_PASSWORD = "password"

    fun saveUserNamePassword(@NonNull context: Context, @NonNull userName: String, @NonNull password: String) {

        PreferenceManager.getDefaultSharedPreferences(context).also { sharedPref ->
            sharedPref.edit().apply {
                putString(KEY_NAME, userName)
                putString(KEY_PASSWORD, password)
            }.commit()
        }
    }

    fun isAccounctCreated(context: Context) = PreferenceManager.getDefaultSharedPreferences(context).let {
        it.getString(KEY_NAME, null) != null && it.getString(KEY_PASSWORD, null) != null
    }

    fun getUserName(context: Context) = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_NAME, "")

    fun getPassword(context: Context) = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_PASSWORD, "")
}