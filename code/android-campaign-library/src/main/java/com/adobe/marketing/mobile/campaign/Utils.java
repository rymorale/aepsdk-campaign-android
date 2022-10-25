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

package com.adobe.marketing.mobile.campaign;

import static com.adobe.marketing.mobile.campaign.CampaignConstants.CampaignHit.PAYLOAD;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.CampaignHit.TIMEOUT;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.CampaignHit.URL;
import com.adobe.marketing.mobile.services.DataEntity;

import org.json.JSONException;
import org.json.JSONObject;

class Utils {
    private Utils() {}

    static CampaignHit campaignHitFromDataEntity(final DataEntity dataEntity) throws JSONException {
        final JSONObject jsonData = new JSONObject(dataEntity.getData());
        return new CampaignHit(jsonData.getString(URL), jsonData.getString(PAYLOAD), jsonData.getInt(TIMEOUT));
    }
}