# Setup push messaging

To enable push messaging with Adobe Campaign, call `setPushIdentifer` to send the push identifier that is received Firebase Cloud Messaging Platform (FCM) to the Adobe Identity service. For more information about the `setPushIdentifer` API, see the [setPushIdentifier section of the Adobe Identity API guide](https://github.com/adobe/aepsdk-core-android).

For more information about setting up your Android app to connect to FCM and retrieve a device registration token that will be used as a push identifier, see the tutorial on [setting up a Firebase Cloud Messaging client app on Android](https://firebase.google.com/docs/cloud-messaging/android/client).

To learn more about creating a push notification using Adobe Campaign, see the tutorial on [preparing and sending a push notification](https://experienceleague.adobe.com/docs/campaign-standard/using/communication-channels/push-notifications/preparing-and-sending-a-push-notification.html).

#### Java

**Example**

```java
FirebaseInstanceId.getInstance().getInstanceId()
        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    return;
                }
                // Get new Instance ID token
                String registrationID = task.getResult().getToken();
                // Log and toast
                System.out.println("Received new registration token: " + registrationID);
                // invoke the API to send the push identifier to the Identity Service
                MobileCore.setPushIdentifier(registrationID);
            }
});
```