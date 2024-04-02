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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.DataEntity;
import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import java.net.HttpURLConnection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CampaignHitsProcessorTests {
    private final CampaignHitProcessor campaignHitProcessor = new CampaignHitProcessor();

    @Mock Networking mockNetworkService;
    @Mock ServiceProvider mockServiceProvider;
    @Mock HttpConnecting mockHttpConnection;
    @Mock DataStoring mockDataStoreService;
    @Mock NamedCollection mockNamedCollection;

    private void setupServiceProviderMockAndRunTest(Runnable testRunnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);
            when(mockServiceProvider.getDataStoreService()).thenReturn(mockDataStoreService);
            when(mockDataStoreService.getNamedCollection(anyString()))
                    .thenReturn(mockNamedCollection);
            testRunnable.run();
        }
    }

    @Test
    public void testConstructor() {
        // verify
        assertNotNull(campaignHitProcessor);
    }

    @Test
    public void testGetRetryInterval() {
        // setup
        CampaignHit campaignHit = new CampaignHit("https://campaignrequest.com", "payload", 5);
        DataEntity dataEntity = new DataEntity(campaignHit.toString());
        // verify
        assertEquals(30, campaignHitProcessor.retryInterval(dataEntity));
    }

    @Test
    public void testProcessHit_NotRetry_NullDataEntity() {
        // setup
        setupServiceProviderMockAndRunTest(
                () -> {
                    // test
                    campaignHitProcessor.processHit(
                            null,
                            processingComplete -> {
                                // verify
                                assertEquals(true, processingComplete);
                                verify(mockNamedCollection, times(0))
                                        .setLong(
                                                eq(
                                                        CampaignConstants
                                                                .CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY),
                                                anyLong());
                            });
                });
    }

    @Test
    public void testProcessHit_NotRetry_EmptyDataInDataEntity() {
        // setup
        setupServiceProviderMockAndRunTest(
                () -> {
                    CampaignHit campaignHit = new CampaignHit("", "", 0);
                    DataEntity dataEntity = new DataEntity(campaignHit.toString());
                    // test
                    campaignHitProcessor.processHit(
                            dataEntity,
                            processingComplete -> {
                                // verify
                                assertEquals(true, processingComplete);
                                verify(mockNamedCollection, times(0))
                                        .setLong(
                                                eq(
                                                        CampaignConstants
                                                                .CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY),
                                                anyLong());
                            });
                });
    }

    @Test
    public void testProcessHit_Retry_WhenNullNetworkService() {
        // setup
        setupServiceProviderMockAndRunTest(
                () -> {
                    when(mockServiceProvider.getNetworkService()).thenReturn(null);
                    CampaignHit campaignHit =
                            new CampaignHit("https://campaignrequest.com", "payload", 5);
                    DataEntity dataEntity = new DataEntity(campaignHit.toString());
                    // test
                    campaignHitProcessor.processHit(
                            dataEntity,
                            processingComplete -> {
                                // verify
                                assertEquals(false, processingComplete);
                                verify(mockNamedCollection, times(0))
                                        .setLong(
                                                eq(
                                                        CampaignConstants
                                                                .CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY),
                                                anyLong());
                            });
                });
    }

    @Test
    public void testProcessHit_Retry_When_ConnectionIsNull() {
        // setup
        setupServiceProviderMockAndRunTest(
                () -> {
                    doAnswer(
                                    (Answer<Void>)
                                            invocation -> {
                                                NetworkCallback callback =
                                                        invocation.getArgument(1);
                                                callback.call(null);
                                                return null;
                                            })
                            .when(mockNetworkService)
                            .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
                    CampaignHit campaignHit =
                            new CampaignHit("https://campaignrequest.com", "payload", 5);
                    DataEntity dataEntity = new DataEntity(campaignHit.toString());
                    // test
                    campaignHitProcessor.processHit(
                            dataEntity,
                            processingComplete -> {
                                // verify
                                assertEquals(false, processingComplete);
                                verify(mockNamedCollection, times(0))
                                        .setLong(
                                                eq(
                                                        CampaignConstants
                                                                .CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY),
                                                anyLong());
                            });
                });
    }

    @Test
    public void testProcessHit_NotRetry_When_ResponseIsValid() {
        // setup
        setupServiceProviderMockAndRunTest(
                () -> {
                    when(mockHttpConnection.getResponseCode())
                            .thenReturn(HttpURLConnection.HTTP_OK);
                    doAnswer(
                                    (Answer<Void>)
                                            invocation -> {
                                                NetworkCallback callback =
                                                        invocation.getArgument(1);
                                                callback.call(mockHttpConnection);
                                                return null;
                                            })
                            .when(mockNetworkService)
                            .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
                    CampaignHit campaignHit =
                            new CampaignHit("https://campaignrequest.com", "payload", 5);
                    DataEntity dataEntity = new DataEntity(campaignHit.toString());
                    // test
                    campaignHitProcessor.processHit(
                            dataEntity,
                            processingComplete -> {
                                // verify
                                assertEquals(true, processingComplete);
                                verify(mockNamedCollection, times(1))
                                        .setLong(
                                                eq(
                                                        CampaignConstants
                                                                .CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY),
                                                anyLong());
                            });
                });
    }

    @Test
    public void
            testProcessHit_NoExceptionAndNotRetry_When_ResponseIsValid_WhenNamedCollectionUnavailable() {
        // setup
        setupServiceProviderMockAndRunTest(
                () -> {
                    when(mockHttpConnection.getResponseCode())
                            .thenReturn(HttpURLConnection.HTTP_OK);
                    when(mockDataStoreService.getNamedCollection(anyString())).thenReturn(null);
                    doAnswer(
                                    (Answer<Void>)
                                            invocation -> {
                                                NetworkCallback callback =
                                                        invocation.getArgument(1);
                                                callback.call(mockHttpConnection);
                                                return null;
                                            })
                            .when(mockNetworkService)
                            .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
                    CampaignHit campaignHit =
                            new CampaignHit("https://campaignrequest.com", "payload", 5);
                    DataEntity dataEntity = new DataEntity(campaignHit.toString());
                    // test
                    campaignHitProcessor.processHit(
                            dataEntity,
                            processingComplete -> {
                                // verify
                                assertEquals(true, processingComplete);
                                verify(mockNamedCollection, times(0))
                                        .setLong(
                                                eq(
                                                        CampaignConstants
                                                                .CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY),
                                                anyLong());
                            });
                });
    }

    @Test
    public void testProcessHit_Retry_When_ConnectionTimeout() {
        // setup
        setupServiceProviderMockAndRunTest(
                () -> {
                    when(mockHttpConnection.getResponseCode())
                            .thenReturn(HttpURLConnection.HTTP_CLIENT_TIMEOUT);
                    when(mockDataStoreService.getNamedCollection(anyString()))
                            .thenReturn(mockNamedCollection);
                    doAnswer(
                                    (Answer<Void>)
                                            invocation -> {
                                                NetworkCallback callback =
                                                        invocation.getArgument(1);
                                                callback.call(mockHttpConnection);
                                                return null;
                                            })
                            .when(mockNetworkService)
                            .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
                    CampaignHit campaignHit =
                            new CampaignHit("https://campaignrequest.com", "payload", 5);
                    DataEntity dataEntity = new DataEntity(campaignHit.toString());
                    // test
                    campaignHitProcessor.processHit(
                            dataEntity,
                            processingComplete -> {
                                // verify
                                assertEquals(false, processingComplete);
                                verify(mockNamedCollection, times(0))
                                        .setLong(
                                                eq(
                                                        CampaignConstants
                                                                .CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY),
                                                anyLong());
                            });
                });
    }

    @Test
    public void testProcess_Retry_When_GateWayTimeout() {
        // setup
        setupServiceProviderMockAndRunTest(
                () -> {
                    when(mockHttpConnection.getResponseCode())
                            .thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
                    when(mockDataStoreService.getNamedCollection(anyString()))
                            .thenReturn(mockNamedCollection);
                    doAnswer(
                                    (Answer<Void>)
                                            invocation -> {
                                                NetworkCallback callback =
                                                        invocation.getArgument(1);
                                                callback.call(mockHttpConnection);
                                                return null;
                                            })
                            .when(mockNetworkService)
                            .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
                    CampaignHit campaignHit =
                            new CampaignHit("https://campaignrequest.com", "payload", 5);
                    DataEntity dataEntity = new DataEntity(campaignHit.toString());
                    // test
                    campaignHitProcessor.processHit(
                            dataEntity,
                            processingComplete -> {
                                // verify
                                assertEquals(false, processingComplete);
                                verify(mockNamedCollection, times(0))
                                        .setLong(
                                                eq(
                                                        CampaignConstants
                                                                .CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY),
                                                anyLong());
                            });
                });
    }

    @Test
    public void testProcess_Retry_When_HttpUnavailable() {
        // setup
        setupServiceProviderMockAndRunTest(
                () -> {
                    when(mockHttpConnection.getResponseCode())
                            .thenReturn(HttpURLConnection.HTTP_UNAVAILABLE);
                    when(mockDataStoreService.getNamedCollection(anyString()))
                            .thenReturn(mockNamedCollection);
                    doAnswer(
                                    (Answer<Void>)
                                            invocation -> {
                                                NetworkCallback callback =
                                                        invocation.getArgument(1);
                                                callback.call(mockHttpConnection);
                                                return null;
                                            })
                            .when(mockNetworkService)
                            .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
                    CampaignHit campaignHit =
                            new CampaignHit("https://campaignrequest.com", "payload", 5);
                    DataEntity dataEntity = new DataEntity(campaignHit.toString());
                    // test
                    campaignHitProcessor.processHit(
                            dataEntity,
                            processingComplete -> {
                                // verify
                                assertEquals(false, processingComplete);
                                verify(mockNamedCollection, times(0))
                                        .setLong(
                                                eq(
                                                        CampaignConstants
                                                                .CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY),
                                                anyLong());
                            });
                });
    }

    @Test
    public void testProcess_NotRetry_When_OtherResponseCode() {
        // setup
        setupServiceProviderMockAndRunTest(
                () -> {
                    when(mockHttpConnection.getResponseCode())
                            .thenReturn(HttpURLConnection.HTTP_PROXY_AUTH);
                    when(mockDataStoreService.getNamedCollection(anyString()))
                            .thenReturn(mockNamedCollection);
                    doAnswer(
                                    (Answer<Void>)
                                            invocation -> {
                                                NetworkCallback callback =
                                                        invocation.getArgument(1);
                                                callback.call(mockHttpConnection);
                                                return null;
                                            })
                            .when(mockNetworkService)
                            .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
                    CampaignHit campaignHit =
                            new CampaignHit("https://campaignrequest.com", "payload", 5);
                    DataEntity dataEntity = new DataEntity(campaignHit.toString());
                    // test
                    campaignHitProcessor.processHit(
                            dataEntity,
                            processingComplete -> {
                                // verify
                                assertEquals(true, processingComplete);
                                verify(mockNamedCollection, times(0))
                                        .setLong(
                                                eq(
                                                        CampaignConstants
                                                                .CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY),
                                                anyLong());
                            });
                });
    }
}
