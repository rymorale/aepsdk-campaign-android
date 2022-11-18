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

import org.junit.Before;
import org.junit.Test;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;


public class CampaignHitsDatabaseTests  {

//	private CampaignHitsDatabase campaignHitsDatabase;
//	private MockNetworkService networkService;
//	private MockHitQueue<CampaignHit, CampaignHitSchema> hitQueue;
//	private FakeLocalStorageService fakeLocalStorageService;
//	private LocalStorageService.DataStore campaignDatastore;
//
//	@Before
//	public void setup() {
//		super.beforeEach();
//		hitQueue = new MockHitQueue<CampaignHit, CampaignHitSchema>(platformServices);
//		networkService = platformServices.getMockNetworkService();
//		fakeLocalStorageService = (FakeLocalStorageService) platformServices.getLocalStorageService();
//		campaignDatastore = (FakeDataStore) fakeLocalStorageService.getDataStore(
//								CampaignTestConstants.CAMPAIGN_DATA_STORE_NAME);
//		campaignHitsDatabase = getCampaignHitsDatabase(platformServices, campaignDatastore, hitQueue);
//	}
//
//	@Test
//	public void testConstructor() throws Exception {
//		// test
//		campaignHitsDatabase = new CampaignHitsDatabase(platformServices);
//		// verify
//		assertNotNull(campaignHitsDatabase);
//	}
//
//	@Test
//	public void testProcess_Retry_When_ConnectionIsNull() throws Exception {
//		// setup
//		CampaignHit campaignHit = createHit("id", 3000, "url", "url-body", 5);
//		networkService.connectUrlReturnValue = null;
//		// test
//		HitQueue.RetryType retryType = campaignHitsDatabase.process(campaignHit);
//		// verify
//		assertEquals(HitQueue.RetryType.YES, retryType);
//	}
//
//
//	@Test
//	public void testProcess_NotRetry_When_ResponseIsValid() throws Exception {
//		// setup
//		CampaignHit campaignHit = createHit("id", 3000, "url", "url-body", 5);
//		MockConnection mockConnection = new MockConnection("", 200, null,
//				null);
//		networkService.connectUrlReturnValue = mockConnection;
//		//test
//		HitQueue.RetryType retryType = campaignHitsDatabase.process(campaignHit);
//		// verify
//		assertEquals(HitQueue.RetryType.NO, retryType);
//	}
//
//	@Test
//	public void testProcess_Retry_When_ConnectionTimeout() throws Exception {
//		// setup
//		CampaignHit campaignHit = createHit("id", 3000, "url", "url-body", 5);
//		MockConnection mockConnection = new MockConnection("", HttpURLConnection.HTTP_CLIENT_TIMEOUT, null,
//				null);
//		networkService.connectUrlReturnValue = mockConnection;
//		// test
//		HitQueue.RetryType retryType = campaignHitsDatabase.process(campaignHit);
//		// verify
//		assertEquals(HitQueue.RetryType.YES, retryType);
//	}
//
//	@Test
//	public void testProcess_Retry_When_GateWayTimeout() throws Exception {
//		// setup
//		CampaignHit campaignHit = createHit("id", 3000, "url", "url-body", 5);
//		MockConnection mockConnection = new MockConnection("", HttpURLConnection.HTTP_GATEWAY_TIMEOUT, null,
//				null);
//		networkService.connectUrlReturnValue = mockConnection;
//		// test
//		HitQueue.RetryType retryType = campaignHitsDatabase.process(campaignHit);
//		// verify
//		assertEquals(HitQueue.RetryType.YES, retryType);
//	}
//
//
//	@Test
//	public void testProcess_Retry_When_HttpUnavailable() throws Exception {
//		// setup
//		CampaignHit campaignHit = createHit("id", 3000, "url", "url-body", 5);
//		MockConnection mockConnection = new MockConnection("", HttpURLConnection.HTTP_UNAVAILABLE, null,
//				null);
//		networkService.connectUrlReturnValue = mockConnection;
//		// test
//		HitQueue.RetryType retryType = campaignHitsDatabase.process(campaignHit);
//		// verify
//		assertEquals(HitQueue.RetryType.YES, retryType);
//	}
//
//	@Test
//	public void testProcess_NotRetry_When_OtherResponseCode() throws Exception {
//		// setup
//		CampaignHit campaignHit = createHit("id", 3000, "url", "url-body", 5);
//		MockConnection mockConnection = new MockConnection("", 301, null,
//				null);
//		networkService.connectUrlReturnValue = mockConnection;
//		// test
//		HitQueue.RetryType retryType = campaignHitsDatabase.process(campaignHit);
//		// verify
//		assertEquals(HitQueue.RetryType.NO, retryType);
//	}
//
//	@Test
//	public void testQueue_Happy_PrivacyStatusIsOptIn() throws Exception {
//		// setup
//		CampaignHit campaignHit = createHit("id", 3000, "url", "url-body", 5);
//		long currentTimestamp = System.currentTimeMillis();
//		// test
//		campaignHitsDatabase.queue(campaignHit, currentTimestamp, MobilePrivacyStatus.OPT_IN);
//		// verify
//		assertTrue(hitQueue.queueWasCalled);
//		assertEquals("id", hitQueue.queueParametersHit.identifier);
//		assertEquals(TimeUnit.MILLISECONDS.toSeconds(currentTimestamp), hitQueue.queueParametersHit.timestamp);
//		assertEquals("url-body", hitQueue.queueParametersHit.body);
//		assertEquals("url", hitQueue.queueParametersHit.url);
//		assertEquals(5, hitQueue.queueParametersHit.timeout);
//		assertTrue(hitQueue.bringOnlineWasCalled);
//	}
//
//	@Test
//	public void testQueue_NullHit() throws Exception {
//		// setup
//		long currentTimestamp = System.currentTimeMillis();
//		// test
//		campaignHitsDatabase.queue(null, currentTimestamp, MobilePrivacyStatus.OPT_IN);
//		// verify
//		assertFalse(hitQueue.queueWasCalled);
//		assertNull(hitQueue.queueParametersHit);
//		assertFalse(hitQueue.bringOnlineWasCalled);
//	}
//
//	@Test
//	public void testQueue_When_PrivacyStatusUnknown() throws Exception {
//		// setup
//		CampaignHit campaignHit = createHit("id", 3000, "url", "url-body", 5);
//		long currentTimestamp = System.currentTimeMillis();
//		// test
//		campaignHitsDatabase.queue(campaignHit, currentTimestamp, MobilePrivacyStatus.UNKNOWN);
//		// verify
//		assertTrue(hitQueue.queueWasCalled);
//		assertFalse(hitQueue.bringOnlineWasCalled);
//	}
//
//	@Test
//	public void testQueue_When_PrivacyStatusOptOut() throws Exception {
//		// setup
//		CampaignHit campaignHit = createHit("id", 3000, "url", "url-body", 5);
//		long currentTimestamp = System.currentTimeMillis();
//		// test
//		campaignHitsDatabase.queue(campaignHit, currentTimestamp, MobilePrivacyStatus.OPT_OUT);
//		// verify
//		assertTrue(hitQueue.queueWasCalled);
//		assertFalse(hitQueue.bringOnlineWasCalled);
//	}
//
//	@Test
//	public void testUpdatePrivacyStatus_When_PrivacyStatusOptIn() throws Exception {
//		//test
//		campaignHitsDatabase.updatePrivacyStatus(MobilePrivacyStatus.OPT_IN);
//		//verify
//		assertTrue(hitQueue.bringOnlineWasCalled);
//	}
//
//	@Test
//	public void testUpdatePrivacyStatus_When_PrivacyStatusUnknown() throws Exception {
//		//test
//		campaignHitsDatabase.updatePrivacyStatus(MobilePrivacyStatus.UNKNOWN);
//		//verify
//		assertTrue(hitQueue.suspendWasCalled);
//	}
//
//	@Test
//	public void testUpdatePrivacyStatus_When_PrivacyStatusOptOut() throws Exception {
//		//test
//		campaignHitsDatabase.updatePrivacyStatus(MobilePrivacyStatus.OPT_OUT);
//		//verify
//		assertTrue(hitQueue.suspendWasCalled);
//		assertTrue(hitQueue.deleteAllHitsWasCalled);
//	}
//
//	// helper methods
//	private CampaignHitsDatabase getCampaignHitsDatabase(final PlatformServices services,
//			final LocalStorageService.DataStore dataStore,
//			final HitQueue<CampaignHit, CampaignHitSchema> hitQueue) {
//		try {
//			return new CampaignHitsDatabase(services, hitQueue);
//		} catch (MissingPlatformServicesException e) {
//			fail("Could not create CampaignHitsDatabase instance (%s)." + e);
//		}
//
//		return  null;
//	}
//
//	private CampaignHit createHit(final String identifier, final long timeStamp, final String url, final String body,
//								  final int timeout) {
//		CampaignHit newHit = new CampaignHit();
//		newHit.identifier = identifier;
//		newHit.url = url;
//		newHit.body = body;
//		newHit.timestamp = timeStamp;
//		newHit.timeout = timeout;
//		return newHit;
//	}
//
//	@Test
//	public void testUpdateTimestampInDatastore() {
//		// setup
//		final long timestamp = 1234567890123l;
//
//		// test
//		campaignHitsDatabase.updateTimestampInDataStore(timestamp);
//
//		// verify
//		assertEquals(timestamp, campaignDatastore.getLong(CampaignTestConstants.CAMPAIGN_DATA_STORE_REGISTRATION_TIMESTAMP_KEY,
//					 CampaignTestConstants.DEFAULT_TIMESTAMP_VALUE));
//	}
}