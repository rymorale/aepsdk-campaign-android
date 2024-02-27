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

package com.example.campaign_push_notification_test_app

import android.app.Application
import android.util.Log
import androidx.annotation.NonNull
import com.adobe.marketing.mobile.*
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging

class PushNotificationApplication : Application() {

    override fun onCreate() {
        super.onCreate()


        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

//        try {
//            AndroidGriffonBridge.registerExtension()
//            AndroidGriffonBridge.setup(this)
//            Places.registerExtension()
//            PlacesMonitor.registerExtension()
//            Campaign.registerExtension()
//            Identity.registerExtension()
//            Lifecycle.registerExtension()
//            Signal.registerExtension()
//            UserProfile.registerExtension()
//            MobileCore.setSmallIconResourceID(R.mipmap.ic_launcher)
//            MobileCore.setLargeIconResourceID(R.drawable.push_notification_large)
//            MobileCore.start { MobileCore.configureWithAppID("launch-EN9d31cdedca2249ea86cd78ca1b6edb6d-development") }
//        } catch (e: InvalidInitException) {
//            e.printStackTrace()
//        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("CampaignTestApp", "getInstanceId failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            println("CampaignTestApp token: $token")
            MobileCore.setPushIdentifier(token)
        }

        // compare to latest versions at https://bintray.com/eaps/mobileservicesdk
        Log.d("Core version ", MobileCore.extensionVersion())
        Log.d("Campaign version ", Campaign.extensionVersion())
//        Log.d("UserProfile version ", UserProfile.extensionVersion())
//        Log.d("Identity version ", Identity.extensionVersion())
//        Log.d("Lifecycle version ", Lifecycle.extensionVersion())
//        Log.d("Signal version ", Signal.extensionVersion())
    }

}