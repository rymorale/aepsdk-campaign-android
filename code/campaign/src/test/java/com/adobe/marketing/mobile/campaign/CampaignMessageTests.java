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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CampaignMessageTests {
    private HashMap<String, Object> happyMessageMap;
    private HashMap<String, Object> happyDetailMap;

    @Mock CampaignExtension mockCampaignExtension;

    @Before
    public void setup() {
        happyDetailMap = new HashMap<>();
        happyDetailMap.put("template", "alert");
        happyDetailMap.put("title", "Title");
        happyDetailMap.put("content", "content");
        happyDetailMap.put("confirm", "Y");
        happyDetailMap.put("cancel", "N");
        happyDetailMap.put("url", "http://www.adobe.com");

        happyMessageMap = new HashMap<>();
        happyMessageMap.put("id", "123");
        happyMessageMap.put("type", "iam");
        happyMessageMap.put("detail", happyDetailMap);
    }

    @Test(expected = CampaignMessageRequiredFieldMissingException.class)
    public void init_ExceptionThrown_When_ConsequenceIsNull() throws Exception {
        // test
        CampaignMessage.createMessageObject(mockCampaignExtension, null);
    }

    @Test(expected = CampaignMessageRequiredFieldMissingException.class)
    public void init_ExceptionThrown_When_ConsequenceMapIsEmpty() throws Exception {
        // test
        CampaignMessage.createMessageObject(
                mockCampaignExtension, TestUtils.createRuleConsequence(new HashMap<>()));
    }

    @Test(expected = CampaignMessageRequiredFieldMissingException.class)
    public void init_ExceptionThrown_When_NoTemplateType() throws Exception {
        // setup
        happyDetailMap.remove("template");
        happyMessageMap.put("detail", happyDetailMap);

        // test
        CampaignMessage.createMessageObject(
                mockCampaignExtension, TestUtils.createRuleConsequence(happyMessageMap));
    }

    @Test
    public void nullReturned_When_InvalidTemplateType() throws Exception {
        // setup
        happyDetailMap.put("template", "invalid");
        happyMessageMap.put("detail", happyDetailMap);

        // test
        CampaignMessage message =
                CampaignMessage.createMessageObject(
                        mockCampaignExtension, TestUtils.createRuleConsequence(happyMessageMap));

        // verify
        assertNull(message);
    }

    @Test
    public void init_Success_When_MessagePayloadIsValid() throws Exception {
        // test
        final CampaignMessage message =
                CampaignMessage.createMessageObject(
                        mockCampaignExtension, TestUtils.createRuleConsequence(happyMessageMap));

        // verify
        assertNotNull(message);
        assertEquals(AlertMessage.class, message.getClass());
        assertEquals("123", message.messageId);
        AlertMessage alertMessage = (AlertMessage) message;
        assertEquals("Title", alertMessage.title);
        assertEquals("content", alertMessage.content);
        assertEquals("Y", alertMessage.confirmButtonText);
        assertEquals("N", alertMessage.cancelButtonText);
    }
}
