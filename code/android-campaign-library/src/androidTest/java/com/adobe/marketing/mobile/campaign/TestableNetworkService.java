/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.campaign;

import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestableNetworkService implements Networking {
    static final String ETAG_HEADER = "ETag";
    static final String ETAG = "\"SOME-ETAG-12345\"";
    static final String WEAK_ETAG = "W/\"SOME-WEAK-ETAG-12345\"";
    static final String LAST_MODIFIED_HEADER_KEY = "Last-Modified";
    static String lastModified = null;
    private CountUpLatch countUpLatch = new CountUpLatch();

    private final Map<String, NetworkRequest> capturedRequests = new HashMap<>();
    private final Map<String, HttpConnecting> networkResponseMap = new HashMap<>();

    @Override
    public void connectAsync(NetworkRequest networkRequest, NetworkCallback networkCallback) {
        capturedRequests.put(networkRequest.getUrl(), networkRequest);
        if (networkCallback != null) {
            if (networkResponseMap.containsKey(networkRequest.getUrl())) {
                networkCallback.call(networkResponseMap.get(networkRequest.getUrl()));
            }
        }
        countUpLatch.countUp();
        return;
    }

    /**
     * Waits up to 10 seconds for the request to be sent to the specified url.
     */
    public boolean waitForRequest(String expectedUrl) {
        CountDownLatch latch = new CountDownLatch(1);
        int elapsedTime = 0;

        while(elapsedTime <= 10000) {
            if (capturedRequests.containsKey(expectedUrl)) {
                latch.countDown();
                return true;
            }
            TestHelper.sleep(100);
            elapsedTime += 100;
        }

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {

        }
        return false;
    }

    public int waitAndGetCount(int expectedCount) {
        return waitAndGetCount(expectedCount, 2000);
    }

    public int waitAndGetCount(int expectedCount, int timeoutInMilli) {
        countUpLatch.await(expectedCount, timeoutInMilli);
        return capturedRequests.size();
    }

    private void setResponseForUrl(String url, HttpConnecting response) {
        networkResponseMap.put(url, response);
    }

    public void reset() {
        clearCapturedRequests();
        clearResponses();
    }

    public void clearCapturedRequests() {
        countUpLatch = new CountUpLatch();
        capturedRequests.clear();
    }

    public void clearResponses() {
        networkResponseMap.clear();
    }

    public NetworkRequest getRequest(String url) {
        NetworkRequest foundRequest = capturedRequests.get(url);
        if (foundRequest == null) {
            Log.debug("testableNetworkService","getRequest", "Network request (%s) not found.", url);
        }
        return foundRequest;
    }

    public void setNetworkResponse(final String urlToMatch, final String message, final int responseCode) {
        setResponseFromFileWithETag(null, urlToMatch, null, message, responseCode);
    }

    public void setResponseFromFile(final String path,
                                    final String urlToMatch) {
        setResponseFromFileWithETag(path, urlToMatch, null, null, 0);
    }

    public void setResponseFromFileWithResponseCode(final String path,
                                                    final String urlToMatch, final String message, final int responseCode) {
        setResponseFromFileWithETag(path, urlToMatch, null, message, responseCode);
    }

    public void setResponseFromFileWithETag(final String path,
                                            final String urlToMatch, final String etag, final String message, final int responseCode) {
        InputStream zipFile = null;

        if (!StringUtils.isNullOrEmpty(path)) {
            try {
                zipFile = ServiceProvider.getInstance().getAppContextService().getApplicationContext().getAssets().open(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // we want to use the same last modified date for requests which share an ETag
        if (lastModified == null || etag == null) {
            SimpleDateFormat simpleDateFormat = TestHelper.createRFC2822Formatter();
            lastModified = simpleDateFormat.format(new Date());
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(LAST_MODIFIED_HEADER_KEY, lastModified);

        if (etag != null) {
            headers.put(ETAG_HEADER, etag);
        }

        InputStream finalZipFile = zipFile;
        HttpConnecting connection = new HttpConnecting() {
            @Override
            public InputStream getInputStream() {
                return finalZipFile;
            }

            @Override
            public InputStream getErrorStream() {
                // no-op
                return null;
            }

            @Override
            public int getResponseCode() {
                return responseCode <= 0 ? HttpURLConnection.HTTP_OK : responseCode;
            }

            @Override
            public String getResponseMessage() {
                return message == null ? "OK" : message;
            }

            @Override
            public String getResponsePropertyValue(String s) {
                return headers.toString();
            }

            @Override
            public void close() {

            }
        };
        setResponseForUrl(urlToMatch, connection);
    }
}
