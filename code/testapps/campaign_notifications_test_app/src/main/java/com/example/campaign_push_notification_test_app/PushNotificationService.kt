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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.adobe.marketing.mobile.MobileCore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushNotificationService : FirebaseMessagingService() {

    companion object {
        @JvmField
        val NOTIFICATION_ID = 0x12E45
        const val channelId = "campaign_notification_channel"
    }

    override fun onNewToken(token: String?) {
        MobileCore.setPushIdentifier(token)
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)
        print("Push notification received")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            val channelName = "Campaign Notifications Channel"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Settings for push notification for Campaign app"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId).apply {
            setSmallIcon(R.drawable.ic_launcher_background)
            setContentTitle(message?.data?.get("title") ?: "")
            setContentText(message?.data?.get("body") ?: "")
            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(PendingIntent.getActivity(this@PushNotificationService, 0, Intent(this@PushNotificationService, MainActivity::class.java), 0))
            setAutoCancel(true)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}