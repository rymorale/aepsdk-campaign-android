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

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.mockito.Mock;

public abstract class BaseTest {
    @Mock
    EventHub eventHub;
    @Mock
    PlatformServices platformServices;
    @Mock
    UIService uiService;

    public BaseTest() {
    }

    void beforeEach() {
        when(platformServices.getUIService()).thenReturn(uiService);
    }

    void afterEach() {
    }

    Map<String, String> getURLQueryParameters(String urlString) {
        HashMap parameters = new HashMap();

        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException var13) {
            var13.printStackTrace();
            return parameters;
        }

        String queryString = url.getQuery();
        String[] paramArray = queryString.split("&");
        String[] var6 = paramArray;
        int var7 = paramArray.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            String currentParam = var6[var8];
            if (currentParam != null && currentParam.length() > 0) {
                String[] currentParamArray = currentParam.split("=", 2);
                if (currentParamArray.length != 1 && (currentParamArray.length != 2 || !currentParamArray[1].isEmpty())) {
                    String key = currentParamArray[0];
                    String value = currentParamArray[1];
                    parameters.put(key, value);
                }
            }
        }

        return parameters;
    }

    File getResource(String resourceName) {
        File resourceFile = null;
        URL resource = this.getClass().getClassLoader().getResource(resourceName);
        if (resource != null) {
            resourceFile = new File(resource.getFile());
        }

        return resourceFile;
    }

    File copyResource(String resourceName, File destinationDirectory, String destinationResourceName) throws IOException {
        File resourceFile = null;
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream(resourceName);
        if (resource != null) {
            resourceFile = new File(destinationDirectory, destinationResourceName);
            FileOutputStream fos = null;

            try {
                byte[] buffer = new byte[4096];
                int n = false;
                fos = new FileOutputStream(resourceFile);

                int n;
                while((n = resource.read(buffer)) != -1) {
                    fos.write(buffer, 0, n);
                }
            } finally {
                if (fos != null) {
                    fos.close();
                }

                resource.close();
            }
        }

        return resourceFile;
    }

    void waitForExecutor(ExecutorService executorService) throws Exception {
        this.waitForExecutor(executorService, 5L);
    }

    void waitForExecutor(ExecutorService executorService, long timeoutSec) throws Exception {
        Future future = executorService.submit(new Runnable() {
            public void run() {
            }
        });

        try {
            future.get(timeoutSec, TimeUnit.SECONDS);
        } catch (TimeoutException var6) {
            Assert.fail(String.format("Executor took longer than %s (sec)", timeoutSec));
        }

    }

    void waitForExecutorWithoutFailing(ExecutorService executorService, long timeoutSec) throws Exception {
        Future future = executorService.submit(new Runnable() {
            public void run() {
            }
        });

        try {
            future.get(timeoutSec, TimeUnit.SECONDS);
        } catch (TimeoutException var6) {
        }

    }
}
