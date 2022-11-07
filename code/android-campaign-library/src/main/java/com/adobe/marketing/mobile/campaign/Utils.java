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
import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;

import com.adobe.marketing.mobile.services.DataEntity;
import com.adobe.marketing.mobile.services.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class Utils {
    private Utils() {
    }

    /**
     * Creates a {@code CampaignHit} object from the given {@code DataEntity}.
     *
     * @param dataEntity {@link DataEntity} containing a Campaign network request
     * @return {@link CampaignHit} created from the {@code DataEntity}
     */
    static CampaignHit campaignHitFromDataEntity(final DataEntity dataEntity) throws JSONException {
        final JSONObject jsonData = new JSONObject(dataEntity.getData());
        return new CampaignHit(jsonData.getString(URL), jsonData.getString(PAYLOAD), jsonData.getInt(TIMEOUT));
    }

    /**
     * Recursively checks and deletes files within the cached assets directory which aren't within the {@code assetsToRetain} list.
     *
     * @param cacheAsset     {@link File} containing the cached assets directory
     * @param assetsToRetain {@code List<String>} containing assets which should be retained
     */
    static void clearCachedAssetsNotInList(final File cacheAsset, final List<String> assetsToRetain) {
        if (cacheAsset.isDirectory()) {
            for (final File child : cacheAsset.listFiles()) {
                clearCachedAssetsNotInList(child, assetsToRetain);
            }
        } else {
            if (!assetsToRetain.contains(cacheAsset.getName())) {
                if (cacheAsset.exists()) {
                    cacheAsset.delete();
                }
            }
        }
    }
}