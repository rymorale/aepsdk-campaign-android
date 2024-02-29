/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.campaign;

import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class CampaignHit {

    String url;
    String payload;
    int timeout;

    CampaignHit(final String url, final String payload, final int timeout) {
        this.url = url;
        this.payload = payload;
        this.timeout = timeout;
    }

    HttpMethod getHttpCommand() {
        return !StringUtils.isNullOrEmpty(payload) ? HttpMethod.POST : HttpMethod.GET;
    }

    @Override
    public String toString() {
        final Map<String, Object> dataMap =
                new HashMap<String, Object>() {
                    {
                        put(CampaignConstants.CampaignHit.URL, url);
                        put(CampaignConstants.CampaignHit.PAYLOAD, payload);
                        put(CampaignConstants.CampaignHit.TIMEOUT, timeout);
                    }
                };
        final JSONObject jsonData = new JSONObject(dataMap);
        return jsonData.toString();
    }
}
