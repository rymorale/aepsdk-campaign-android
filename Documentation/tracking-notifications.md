# Tracking local and push notification interactions

User interactions with local or push notifications can be tracked by invoking the `collectMessageInfo` API. After the API is invoked, a network request is made to Campaign that contains the message interaction event.

> **Warning**
> The code samples below are provided as examples on how to correctly invoke the `collectMessageInfo` API. For more specific details, please read the tutorials on [implementing local notification tracking](https://experienceleague.adobe.com/docs/campaign-standard/using/administrating/configuring-mobile/local-tracking.html) and [configuring push tracking](https://experienceleague.adobe.com/docs/campaign-standard/using/administrating/configuring-mobile/push-tracking.html) within the Adobe Campaign documentation.

#### Java

**Syntax**

```java
public static void collectMessageInfo(final Map<String, Object> messageInfo)
```

* _messageInfo_ is a map that contains the delivery ID, message ID, and action type for a local or push notification for which there were interactions. The delivery and message IDs are extracted from the notification payload.

**Example**

```java
@Override
protected void onResume() {
    super.onResume();
    handleTracking();
}

// handle notification open and click tracking
private void handleTracking() {
    Intent intent = getIntent();
    Bundle data = intent.getExtras();
    HashMap<String, Object> userInfo = null;

    if (data != null) {
        userInfo = (HashMap)data.get("NOTIFICATION_USER_INFO");
    } else {
        return;
    }

    // Check if we have notification user info.
    // If it is present, this view was opened based on a notification.
    if (userInfo != null) {
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
```