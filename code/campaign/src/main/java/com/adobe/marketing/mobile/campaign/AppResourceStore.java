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

import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.ServiceProvider;

public class AppResourceStore {
    private final static String DATASTORE_NAME = "ADOBE_MOBILE_APP_STATE";
    private final static String SMALL_ICON_RESOURCE_ID_KEY = "SMALL_ICON_RESOURCE_ID";
    private final static String LARGE_ICON_RESOURCE_ID_KEY = "LARGE_ICON_RESOURCE_ID";
    private static volatile int smallIconResourceID = -1;
    private static volatile int largeIconResourceID = -1;

     public static void setSmallIconResourceID(int resourceID) {
        smallIconResourceID = resourceID;
        NamedCollection dataStore = ServiceProvider.getInstance().getDataStoreService().getNamedCollection("ADOBE_MOBILE_APP_STATE");
        if (dataStore != null) {
            dataStore.setInt("SMALL_ICON_RESOURCE_ID", smallIconResourceID);
        }
    }

    public static int getSmallIconResourceID() {
        if (smallIconResourceID == -1) {
            NamedCollection dataStore = ServiceProvider.getInstance().getDataStoreService().getNamedCollection("ADOBE_MOBILE_APP_STATE");
            if (dataStore != null) {
                smallIconResourceID = dataStore.getInt("SMALL_ICON_RESOURCE_ID", -1);
            }
        }
        return smallIconResourceID;
    }

    public static void setLargeIconResourceID(int resourceID) {
        largeIconResourceID = resourceID;
        NamedCollection dataStore = ServiceProvider.getInstance().getDataStoreService().getNamedCollection("ADOBE_MOBILE_APP_STATE");
        if (dataStore != null) {
            dataStore.setInt("LARGE_ICON_RESOURCE_ID", largeIconResourceID);
        }
    }

    public static int getLargeIconResourceID() {
        if (largeIconResourceID == -1) {
             NamedCollection dataStore = ServiceProvider.getInstance().getDataStoreService().getNamedCollection("ADOBE_MOBILE_APP_STATE");
            if (dataStore != null) {
                largeIconResourceID = dataStore.getInt("LARGE_ICON_RESOURCE_ID", -1);
            }
        }
        return largeIconResourceID;
    }
}
