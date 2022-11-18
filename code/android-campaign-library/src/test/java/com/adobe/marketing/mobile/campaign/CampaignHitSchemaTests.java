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
import java.util.Map;
import static org.junit.Assert.*;

public class CampaignHitSchemaTests {
//	private static final String HIT_ID_COL_NAME = "ID";
//	private static final String HIT_URL_COL_NAME = "URL";
//	private static final String HIT_TIMESTAMP_COL_NAME = "TIMESTAMP";
//	private static final String HIT_BODY_COL_NAME = "BODY";
//	private static final String HIT_TIMEOUT_COL_NAME = "TIMEOUT";
//	private CampaignHitSchema schema;
//
//	@Before
//	public void setup() {
//		schema = new CampaignHitSchema();
//	}
//
//	@Test
//	public void testGenerateDataMap_NullCampaignHit() {
//		// setup
//		CampaignHit newHit = null;
//		// test
//		Map<String, Object> values = schema.generateDataMap(newHit);
//		// verify
//		assertNull(values);
//	}
//
//	@Test
//	public void testGenerateDataMapHappy() {
//		// setup
//		CampaignHit newHit = new CampaignHit();
//		newHit.identifier = "id";
//		newHit.url = "url";
//		newHit.timestamp = 123;
//		newHit.body = "url-body";
//		newHit.timeout = 5;
//		// test
//		Map<String, Object> values = schema.generateDataMap(newHit);
//		// verify
//		assertFalse(values.containsKey(HIT_ID_COL_NAME));
//		assertEquals("url", values.get(HIT_URL_COL_NAME));
//		assertEquals(123L, values.get(HIT_TIMESTAMP_COL_NAME));
//		assertEquals("url-body", values.get(HIT_BODY_COL_NAME));
//		assertEquals(5, values.get(HIT_TIMEOUT_COL_NAME));
//	}
//
//	@Test
//	public void testGenerateHitHappy() {
//		// setup
//		CampaignHit hit = null;
//		DatabaseService.QueryResult queryResult = new MockQueryResult(new Object[][] {
//					new Object[]{"id", "url", 123L, "url-body", 5}
//				});
//		// test
//		hit = schema.generateHit(queryResult);
//		// verify
//		assertEquals("id", hit.identifier);
//		assertEquals("url", hit.url);
//		assertEquals(123L, hit.timestamp);
//		assertEquals("url-body", hit.body);
//		assertEquals(5, hit.timeout);
//	}
//
//	@Test
//	public void testGenerateHit_NullQueryResult() {
//		// setup
//		CampaignHit hit = null;
//		DatabaseService.QueryResult queryResult = null;
//		// test
//		hit = schema.generateHit(queryResult);
//		// verify
//		assertNull(hit);
//	}
}