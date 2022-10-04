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

package com.adobe.marketing.mobile;

/**
 * Contains {@code static} constants used by the classes within the Campaign extension.
 * <p>
 * This class is not instantiable.
 */
class CampaignTestConstants {

	static final String CAMPAIGN_DATA_STORE_NAME = "CampaignDataStore";
	static final String CAMPAIGN_DATA_STORE_REMOTES_URL_KEY = "CampaignRemoteUrl";
	static final String CAMPAIGN_DATA_STORE_EXPERIENCE_CLOUD_ID_KEY = "ExperienceCloudId";
	static final String CAMPAIGN_DATA_STORE_REGISTRATION_TIMESTAMP_KEY = "CampaignRegistrationTimestamp";

	static final long DEFAULT_TIMESTAMP_VALUE = -1;

	static final String CACHE_DIR = "cache";
	static final String MESSAGE_CACHE_DIR = "messages";


	// message consequence "type" value
	static final String MESSAGE_CONSEQUENCE_MESSAGE_TYPE = "iam";

	// message consequence "template" values
	static final String MESSAGE_TEMPLATE_ALERT = "alert";
	static final String MESSAGE_TEMPLATE_FULLSCREEN = "fullscreen";
	static final String MESSAGE_TEMPLATE_LOCAL_NOTIFICATION = "local";

	static final String RULES_CACHE_FOLDER = "campaignRules";
	static final String RULES_JSON_KEY = "rules";
	static final String RULES_JSON_FILE_NAME = "rules.json";
	static final String RULES_JSON_CONDITION_KEY = "condition";
	static final String	RULES_JSON_CONSEQUENCES_KEY = "consequences";

	static final String CAMPAIGN_REGISTRATION_URL = "https://%s/rest/head/mobileAppV5/%s/subscriptions/%s";
	static final String CAMPAIGN_RULES_DOWNLOAD_URL = "https://%s/%s/%s/%s/rules.zip";
	static final String CAMPAIGN_TRACKING_URL = "https://%s/r/?id=%s,%s,%s&mcId=%s";
	static final int CAMPAIGN_TIMEOUT_DEFAULT = 5;

	static final String CAMPAIGN_PUSH_PLATFORM = "pushPlatform";
	static final String EXPERIENCE_CLOUD_ID = "marketingCloudId";

	static final String CAMPAIGN_INTERACTION_URL  = "url";
	static final String CAMPAIGN_INTERACTION_TYPE  = "type";

	static final String MESSAGE_TOKEN_MESSAGE_ID = "messageId";

	/**
	 * Context data keys
	 */
	static final class ContextDataKeys {
		// in-app constants for message tracking
		static final String MESSAGE_TRIGGERED            	= "a.message.triggered";
		static final String MESSAGE_CLICKED		        	= "a.message.clicked";
		static final String MESSAGE_VIEWED               	= "a.message.viewed";
		static final String MESSAGE_ID		            	= "a.message.id";

		private ContextDataKeys() {}
	}

	/*
		EventDataKeys
	 */
	static final class EventDataKeys {
		static final String STATE_OWNER = "stateowner";

		private EventDataKeys() {}

		static final class Campaign {
			static final String EXTENSION_NAME       = "com.adobe.module.campaign";
			static final String LINKAGE_FIELDS 		 = "linkagefields";
			static final String TRACK_INFO_KEY_BROADLOG_ID  = "broadlogId";
			static final String TRACK_INFO_KEY_DELIVERY_ID  = "deliveryId";
			static final String TRACK_INFO_KEY_ACTION = "action";

			private Campaign() {}
		}

		static final class Configuration {
			static final String EXTENSION_NAME       = "com.adobe.module.configuration";

			// config response keys
			static final String GLOBAL_CONFIG_PRIVACY            = "global.privacy";
			static final String PROPERTY_ID    = "property.id";
			static final String CAMPAIGN_SERVER_KEY     = "campaign.server";
			static final String CAMPAIGN_PKEY_KEY       = "campaign.pkey";
			static final String CAMPAIGN_MCIAS_KEY = "campaign.mcias";
			static final String CAMPAIGN_TIMEOUT = "campaign.timeout";
			static final String CAMPAIGN_REGISTRATION_DELAY_KEY = "campaign.registrationDelay";
			static final String CAMPAIGN_REGISTRATION_PAUSED_KEY = "campaign.registrationPaused";

			private Configuration() {}
		}

		static final class RuleEngine {
			static final String CONSEQUENCE_TRIGGERED = "triggeredconsequence";

			// root message consequence keys
			static final String MESSAGE_CONSEQUENCE_ID = "id";
			static final String MESSAGE_CONSEQUENCE_TYPE = "type";
			static final String MESSAGE_CONSEQUENCE_DETAIL = "detail";

			// all message types
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE = "template";

			// fullscreen messages only
			static final String MESSAGE_CONSEQUENCE_ASSETS_PATH = "assetsPath";
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML = "html";
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS = "remoteAssets";

			// alert and local notifications messages only
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT = "content";

			// alert messages only
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_TITLE = "title";
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_CONFIRM = "confirm";
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_CANCEL = "cancel";
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_URL = "url";

			// local notification messages only
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_WAIT = "wait";
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_FIRE_DATE = "date";
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_DEEPLINK_URL = "adb_deeplink";
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_USER_DATA = "userData";
			static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_SOUND = "sound";

			private RuleEngine() {}
		}

		static final class Identity {
			static final String EXTENSION_NAME = "com.adobe.module.identity";
			static final String VISITOR_ID_MID = "mid";

			private Identity() {}
		}

		static final class Lifecycle {
			static final String EXTENSION_NAME = "com.adobe.module.lifecycle";
			static final String LAUNCH_EVENT           = "launchevent";
			static final String LIFECYCLE_CONTEXT_DATA  = "lifecyclecontextdata";

			private Lifecycle() {}
		}

	}

	private CampaignTestConstants() {}
}
