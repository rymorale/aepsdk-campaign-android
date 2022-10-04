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

public class MockCampaignHitsDatabase extends CampaignHitsDatabase {
	MockCampaignHitsDatabase(final PlatformServices services) throws MissingPlatformServicesException {
		super(services);
	}

	boolean queueWasCalled = false;
	CampaignHit queueParameterCampaignHit;
	long queueParameterTimestamp;
	MobilePrivacyStatus queueParameterMobilePrivacyStatus;
	@Override
	void queue(final CampaignHit campaignHit, final long timestamp, final MobilePrivacyStatus privacyStatus) {
		this.queueParameterCampaignHit = campaignHit;
		this.queueParameterTimestamp = timestamp;
		this.queueParameterMobilePrivacyStatus = privacyStatus;
		this.queueWasCalled = true;
	}

	boolean updatePrivacyStatusWasCalled = false;
	MobilePrivacyStatus updatePrivacyStatusParameterMobilePrivacyStatus;
	@Override
	void updatePrivacyStatus(final MobilePrivacyStatus privacyStatus) {
		this.updatePrivacyStatusWasCalled = true;
		this.updatePrivacyStatusParameterMobilePrivacyStatus = privacyStatus;
	}
}