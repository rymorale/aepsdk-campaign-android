package com.adobe.marketing.mobile.campaign;

import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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
        final Map<String, Object> dataMap = new HashMap<String, Object>() {
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
