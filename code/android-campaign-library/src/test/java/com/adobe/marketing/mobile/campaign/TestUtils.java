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

import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.util.DataReader;

import java.io.File;
import java.net.URL;
import java.util.Map;

public class TestUtils {

    static RuleConsequence createRuleConsequence(Map<String, Object> consequenceMap) {
        String id = DataReader.optString(consequenceMap, "id", "");
        String type = DataReader.optString(consequenceMap, "type", "");
        Map details = DataReader.optTypedMap(Object.class, consequenceMap, "detail", null);
        return new RuleConsequence(id, type, details);
    }

    /**
     * Get an instance of {@link File} for resource from the resource directory, that matches the name supplied.
     * <p>
     * The resource directory for the unit tests is <b>{projectroot}/unitTests/resource</b>
     * </p>
     *
     * @param resourceName The name of the resource.
     * @return A File instance, if the resource was found in the resource directory. null otherwise.
     */
    static File getResource(String resourceName) {
        File resourceFile = null;
        URL resource = TestUtils.class.getClassLoader().getResource(resourceName);

        if (resource != null) {
            resourceFile = new File(resource.getFile());
        }

        return resourceFile;
    }
}
