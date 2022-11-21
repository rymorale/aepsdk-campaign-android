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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.adobe.marketing.mobile.services.DataEntity;
import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.HttpURLConnection;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceProvider.class, CampaignHitProcessor.class})
public class CampaignHitsProcessorTests {
	private CampaignHitProcessor campaignHitProcessor = new CampaignHitProcessor();

	@Mock
	Networking mockNetworkService;
	@Mock
	ServiceProvider mockServiceProvider;
	@Mock
	HttpConnecting mockHttpConnection;
	@Mock
	DataStoring mockDataStoreService;
	@Mock
	NamedCollection mockNamedCollection;

	@Before
	public void setup() {
		// setup mocks
		mockStatic(ServiceProvider.class);
		when(ServiceProvider.getInstance()).thenReturn(mockServiceProvider);
		when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);
		when(mockServiceProvider.getDataStoreService()).thenReturn(mockDataStoreService);
		when(mockDataStoreService.getNamedCollection(anyString())).thenReturn(mockNamedCollection);
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
		// test
		campaignHitProcessor.processHit(null, processingComplete -> {
			// verify
			assertEquals(true, processingComplete);
			verify(mockNamedCollection, times(0)).setLong(eq(CampaignTestConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY), anyLong());
		});
	}

	@Test
	public void testProcessHit_NotRetry_EmptyDataInDataEntity() {
		// setup
		CampaignHit campaignHit = new CampaignHit("", "", 0);
		DataEntity dataEntity = new DataEntity(campaignHit.toString());
		// test
		campaignHitProcessor.processHit(dataEntity, processingComplete -> {
			// verify
			assertEquals(true, processingComplete);
			verify(mockNamedCollection, times(0)).setLong(eq(CampaignTestConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY), anyLong());
		});
	}

	@Test
	public void testProcessHit_Retry_WhenNullNetworkService() {
		// setup
		when(mockServiceProvider.getNetworkService()).thenReturn(null);
		CampaignHit campaignHit = new CampaignHit("https://campaignrequest.com", "payload", 5);
		DataEntity dataEntity = new DataEntity(campaignHit.toString());
		// test
		campaignHitProcessor.processHit(dataEntity, processingComplete -> {
			// verify
			assertEquals(false, processingComplete);
			verify(mockNamedCollection, times(0)).setLong(eq(CampaignTestConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY), anyLong());
		});
	}

	@Test
	public void testProcessHit_Retry_When_ConnectionIsNull() {
		// setup
		doAnswer((Answer<Void>) invocation -> {
			NetworkCallback callback = invocation.getArgument(1);
			callback.call(null);
			return null;
		}).when(mockNetworkService)
				.connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
		CampaignHit campaignHit = new CampaignHit("https://campaignrequest.com", "payload", 5);
		DataEntity dataEntity = new DataEntity(campaignHit.toString());
		// test
		campaignHitProcessor.processHit(dataEntity, processingComplete -> {
			// verify
			assertEquals(false, processingComplete);
			verify(mockNamedCollection, times(0)).setLong(eq(CampaignTestConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY), anyLong());
		});
	}


	@Test
	public void testProcessHit_NotRetry_When_ResponseIsValid() {
		// setup
		when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		doAnswer((Answer<Void>) invocation -> {
			NetworkCallback callback = invocation.getArgument(1);
			callback.call(mockHttpConnection);
			return null;
		}).when(mockNetworkService)
				.connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
		CampaignHit campaignHit = new CampaignHit("https://campaignrequest.com", "payload", 5);
		DataEntity dataEntity = new DataEntity(campaignHit.toString());
		// test
		campaignHitProcessor.processHit(dataEntity, processingComplete -> {
			// verify
			assertEquals(true, processingComplete);
			verify(mockNamedCollection, times(1)).setLong(eq(CampaignTestConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY), anyLong());
		});
	}

	@Test
	public void testProcessHit_NoExceptionAndNotRetry_When_ResponseIsValid_WhenNamedCollectionUnavailable() {
		// setup
		when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		when(mockDataStoreService.getNamedCollection(anyString())).thenReturn(null);
		doAnswer((Answer<Void>) invocation -> {
			NetworkCallback callback = invocation.getArgument(1);
			callback.call(mockHttpConnection);
			return null;
		}).when(mockNetworkService)
				.connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
		CampaignHit campaignHit = new CampaignHit("https://campaignrequest.com", "payload", 5);
		DataEntity dataEntity = new DataEntity(campaignHit.toString());
		// test
		campaignHitProcessor.processHit(dataEntity, processingComplete -> {
			// verify
			assertEquals(true, processingComplete);
			verify(mockNamedCollection, times(0)).setLong(eq(CampaignTestConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY), anyLong());
		});
	}

	@Test
	public void testProcessHit_Retry_When_ConnectionTimeout() {
		// setup
		when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_CLIENT_TIMEOUT);
		when(mockDataStoreService.getNamedCollection(anyString())).thenReturn(mockNamedCollection);
		doAnswer((Answer<Void>) invocation -> {
			NetworkCallback callback = invocation.getArgument(1);
			callback.call(mockHttpConnection);
			return null;
		}).when(mockNetworkService)
				.connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
		CampaignHit campaignHit = new CampaignHit("https://campaignrequest.com", "payload", 5);
		DataEntity dataEntity = new DataEntity(campaignHit.toString());
		// test
		campaignHitProcessor.processHit(dataEntity, processingComplete -> {
			// verify
			assertEquals(false, processingComplete);
			verify(mockNamedCollection, times(0)).setLong(eq(CampaignTestConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY), anyLong());
		});
	}

	@Test
	public void testProcess_Retry_When_GateWayTimeout() {
		// setup
		when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
		when(mockDataStoreService.getNamedCollection(anyString())).thenReturn(mockNamedCollection);
		doAnswer((Answer<Void>) invocation -> {
			NetworkCallback callback = invocation.getArgument(1);
			callback.call(mockHttpConnection);
			return null;
		}).when(mockNetworkService)
				.connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
		CampaignHit campaignHit = new CampaignHit("https://campaignrequest.com", "payload", 5);
		DataEntity dataEntity = new DataEntity(campaignHit.toString());
		// test
		campaignHitProcessor.processHit(dataEntity, processingComplete -> {
			// verify
			assertEquals(false, processingComplete);
			verify(mockNamedCollection, times(0)).setLong(eq(CampaignTestConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY), anyLong());
		});
	}


	@Test
	public void testProcess_Retry_When_HttpUnavailable() {
		// setup
		when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAVAILABLE);
		when(mockDataStoreService.getNamedCollection(anyString())).thenReturn(mockNamedCollection);
		doAnswer((Answer<Void>) invocation -> {
			NetworkCallback callback = invocation.getArgument(1);
			callback.call(mockHttpConnection);
			return null;
		}).when(mockNetworkService)
				.connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
		CampaignHit campaignHit = new CampaignHit("https://campaignrequest.com", "payload", 5);
		DataEntity dataEntity = new DataEntity(campaignHit.toString());
		// test
		campaignHitProcessor.processHit(dataEntity, processingComplete -> {
			// verify
			assertEquals(false, processingComplete);
			verify(mockNamedCollection, times(0)).setLong(eq(CampaignTestConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY), anyLong());
		});
	}

	@Test
	public void testProcess_NotRetry_When_OtherResponseCode() {
		// setup
		when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_PROXY_AUTH);
		when(mockDataStoreService.getNamedCollection(anyString())).thenReturn(mockNamedCollection);
		doAnswer((Answer<Void>) invocation -> {
			NetworkCallback callback = invocation.getArgument(1);
			callback.call(mockHttpConnection);
			return null;
		}).when(mockNetworkService)
				.connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
		CampaignHit campaignHit = new CampaignHit("https://campaignrequest.com", "payload", 5);
		DataEntity dataEntity = new DataEntity(campaignHit.toString());
		// test
		campaignHitProcessor.processHit(dataEntity, processingComplete -> {
			// verify
			assertEquals(true, processingComplete);
			verify(mockNamedCollection, times(0)).setLong(eq(CampaignTestConstants.CAMPAIGN_NAMED_COLLECTION_REGISTRATION_TIMESTAMP_KEY), anyLong());
		});
	}
}