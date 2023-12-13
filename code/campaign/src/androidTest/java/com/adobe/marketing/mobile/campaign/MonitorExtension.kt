package com.adobe.marketing.mobile.campaign

import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.Extension
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResolution

typealias ConfigurationMonitor = (firstValidConfiguration: Map<String, Any>) -> Unit

