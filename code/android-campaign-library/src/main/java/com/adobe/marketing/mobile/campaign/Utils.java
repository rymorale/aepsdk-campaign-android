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

import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class Utils {

    private static final long MILLISECONDS_PER_SECOND = 1000L;

    private Utils() {}

    static boolean isNullOrEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    static boolean isNullOrEmpty(final Map<String, Object> map) {
        return map == null || map.isEmpty();
    }

    static boolean isNullOrEmpty(final List<?> list) {
        return list == null || list.isEmpty();
    }

    /* JSON - Map conversion helpers */

    /**
     * Converts provided {@link JSONObject} into {@link Map} for any number of levels, which can be used as event data
     * This method is recursive.
     * The elements for which the conversion fails will be skipped.
     *
     * @param jsonObject to be converted
     * @return {@link Map} containing the elements from the provided json, null if {@code jsonObject} is null
     */
    static Map<String, Object> toMap(final JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        Map<String, Object> jsonAsMap = new HashMap<>();
        Iterator<String> keysIterator = jsonObject.keys();

        while (keysIterator.hasNext()) {
            String nextKey = keysIterator.next();
            Object value = null;
            Object returnValue;

            try {
                value = jsonObject.get(nextKey);
            } catch (JSONException e) {
                MobileCore.log(
                        LoggingMode.DEBUG,
                        LOG_TAG,
                        "Utils(toMap) - Unable to convert jsonObject to Map for key " + nextKey + ", skipping."
                );
            }

            if (value == null) {
                continue;
            }

            if (value instanceof JSONObject) {
                returnValue = toMap((JSONObject) value);
            } else if (value instanceof JSONArray) {
                returnValue = toList((JSONArray) value);
            } else {
                returnValue = value;
            }

            jsonAsMap.put(nextKey, returnValue);
        }

        return jsonAsMap;
    }

    /**
     * Converts provided {@link JSONArray} into {@link List} for any number of levels which can be used as event data
     * This method is recursive.
     * The elements for which the conversion fails will be skipped.
     *
     * @param jsonArray to be converted
     * @return {@link List} containing the elements from the provided json, null if {@code jsonArray} is null
     */
    static List<Object> toList(final JSONArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }

        List<Object> jsonArrayAsList = new ArrayList<>();
        int size = jsonArray.length();

        for (int i = 0; i < size; i++) {
            Object value = null;
            Object returnValue = null;

            try {
                value = jsonArray.get(i);
            } catch (JSONException e) {
                MobileCore.log(
                        LoggingMode.DEBUG,
                        LOG_TAG,
                        "Utils(toList) - Unable to convert jsonObject to List for index " + i + ", skipping."
                );
            }

            if (value == null) {
                continue;
            }

            if (value instanceof JSONObject) {
                returnValue = toMap((JSONObject) value);
            } else if (value instanceof JSONArray) {
                returnValue = toList((JSONArray) value);
            } else {
                returnValue = value;
            }

            jsonArrayAsList.add(returnValue);
        }

        return jsonArrayAsList;
    }

    /**
     * Get the String representation of an {@code InputStream}
     *
     * @param inputStream {@link InputStream} to read
     * @return {@link String} representation of the input stream
     */
    static String inputStreamToString(final InputStream inputStream) throws IOException {
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int result = bufferedInputStream.read(); result != -1; result = bufferedInputStream.read()) {
            byteArrayOutputStream.write((byte) result);
        }
        return byteArrayOutputStream.toString("UTF-8");
    }

    /**
     * Check if the given {@code String} is a valid URL.
     * <p>
     * It uses {@link URL} class to identify that.
     *
     * @param stringUrl URL that needs to be tested
     * @return return a {@code boolean} indicating if the given parameter is a valid URL
     */
    static boolean stringIsUrl(final String stringUrl) {
        if (isNullOrEmpty(stringUrl)) {
            return false;
        }

        try {
            new URL(stringUrl);
            return true;
        } catch (final MalformedURLException ex) {
            return false;
        }
    }
}