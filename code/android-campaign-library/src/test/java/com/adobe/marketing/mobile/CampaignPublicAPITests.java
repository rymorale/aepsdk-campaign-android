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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import com.adobe.marketing.mobile.campaign.CampaignExtension;
import com.adobe.marketing.mobile.services.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class, Log.class})
public class CampaignPublicAPITests {

    @Before
    public void setup() {
        mockStatic(MobileCore.class);
        mockStatic(Log.class);
    }

    @Test
    public void test_extensionVersion() {
        assertEquals("2.0.0", Campaign.extensionVersion());
    }

    @Test
    public void test_publicExtensionConstants() {
        assertEquals(CampaignExtension.class, Campaign.EXTENSION);
        List<Class<? extends Extension>> extensions = new ArrayList<>();
        extensions.add(Campaign.EXTENSION);
        // should not throw exceptions
        MobileCore.registerExtensions(extensions, null);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_registerExtension() {
        // setup
        ArgumentCaptor<Class> extensionClassCaptor = ArgumentCaptor.forClass(Class.class);
        ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(
                ExtensionErrorCallback.class
        );
        // test
        Campaign.registerExtension();
        // verify registerExtension called
        verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.registerExtension(extensionClassCaptor.capture(), callbackCaptor.capture());
        // verify error callback was not called when extension error is null
        callbackCaptor.getValue().error(null);
        assertNotNull(callbackCaptor.getValue());
        verifyStatic(Log.class, Mockito.times(0));
        Log.error(anyString(), anyString(), anyString(), any());
        // verify campaign extension registered
        assertEquals(CampaignExtension.class, extensionClassCaptor.getValue());

    }

    @SuppressWarnings("rawtypes")
    @Test
    public void test_registerExtension_withoutError() {
        // setup
        ArgumentCaptor<Class> extensionClassCaptor = ArgumentCaptor.forClass(Class.class);
        ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(
                ExtensionErrorCallback.class
        );
        // test
        Campaign.registerExtension();
        // verify registerExtension called
        verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.registerExtension(extensionClassCaptor.capture(), callbackCaptor.capture());
        // verify no exception when error callback is called and that log.error was called
        callbackCaptor.getValue().error(ExtensionError.UNEXPECTED_ERROR);
        assertNotNull(callbackCaptor.getValue());
        verifyStatic(Log.class, Mockito.times(1));
        Log.error(anyString(), anyString(), anyString(), any());
        // verify campaign extension registered
        assertEquals(CampaignExtension.class, extensionClassCaptor.getValue());
    }

    @Test
    public void test_setLinkageFields() {
        // setup
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Map<String, String> linkageFields = new HashMap<>();
        linkageFields.put("cusFirstName", "firstName");
        linkageFields.put("cusLastName", "lastName");
        linkageFields.put("cusEmail", "firstNameLastName@email.com");
        // test
        Campaign.setLinkageFields(linkageFields);
        // verify campaign request identity event dispatched
        verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture());
        Event capturedEvent = eventCaptor.getValue();
        assertEquals(EventType.CAMPAIGN, capturedEvent.getType());
        assertEquals(EventSource.REQUEST_IDENTITY, capturedEvent.getSource());
        assertEquals("setLinkageFields Event", capturedEvent.getName());
        Map<String, Object> capturedEventData = capturedEvent.getEventData();
        Map<String, String> capturedLinkageFields = (Map<String, String>) capturedEventData.get("linkagefields");
        assertEquals("firstName", capturedLinkageFields.get("cusFirstName"));
        assertEquals("lastName", capturedLinkageFields.get("cusLastName"));
        assertEquals("firstNameLastName@email.com", capturedLinkageFields.get("cusEmail"));
    }

    @Test
    public void test_setLinkageFields_NullMap() {
        // test
        Campaign.setLinkageFields(null);
        // verify campaign request identity event not dispatched
        verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class));
    }

    @Test
    public void test_setLinkageFields_EmptyMap() {
        // test
        Campaign.setLinkageFields(new HashMap<>());
        // verify campaign request identity event not dispatched
        verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class));
    }

    @Test
    public void test_resetLinkageFields() {
        // setup
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // test
        Campaign.resetLinkageFields();
        // verify campaign request reset event dispatched
        verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture());
        Event capturedEvent = eventCaptor.getValue();
        assertEquals(EventType.CAMPAIGN, capturedEvent.getType());
        assertEquals(EventSource.REQUEST_RESET, capturedEvent.getSource());
        assertEquals("resetLinkageFields Event", capturedEvent.getName());
    }
}
