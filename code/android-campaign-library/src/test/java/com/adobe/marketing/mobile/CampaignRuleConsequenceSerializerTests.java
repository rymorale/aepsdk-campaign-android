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

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import org.junit.rules.ExpectedException;

public class CampaignRuleConsequenceSerializerTests {
	private CampaignRuleConsequence consequence;
	private CampaignRuleConsequenceSerializer serializer;
	private Map<String, Variant> testDetailMap;
	private Variant consequenceAsVariant;
	private Map<String, Variant> consequenceMap;

	@Before
	public void setup() throws Exception {
		testDetailMap = new HashMap<String, Variant>();
		testDetailMap.put("template", Variant.fromString("fullscreen"));
		testDetailMap.put("html", Variant.fromString("test.html"));
		consequence = new CampaignRuleConsequence("test_id", "iam", "test_assets_path", testDetailMap);

		serializer = new CampaignRuleConsequenceSerializer();

		consequenceMap = new HashMap<String, Variant>();
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID,
						   Variant.fromString("test_id"));
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_TYPE, Variant.fromString("iam"));
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ASSETS_PATH,
						   Variant.fromString("test_assets_path"));
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL,
						   Variant.fromVariantMap(testDetailMap));
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);
	}

	@Test
	public void testSerialize_ValidRuleConsequence_Happy() throws Exception {
		// test
		Variant variant = Variant.fromTypedObject(consequence, serializer);

		final Map<String, Variant> consequenceMap = variant.getVariantMap();

		// verify
		assertNotNull(variant);
		assertEquals(Variant.getVariantFromMap(consequenceMap,
											   CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID).getString(), "test_id");
		assertEquals(Variant.getVariantFromMap(consequenceMap,
											   CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_TYPE).getString(), "iam");
		assertEquals(Variant.getVariantFromMap(consequenceMap,
											   CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ASSETS_PATH).optString(""), "test_assets_path");
		assertEquals(Variant.getVariantFromMap(consequenceMap,
											   CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL).getVariantMap(), testDetailMap);
	}

	@Test
	public void testSerialize_NullRuleConsequence_ShouldReturnNullVariant() throws Exception {
		// setup
		consequence = null;

		// test
		Variant variant = Variant.fromTypedObject(consequence, serializer);

		// verify
		assertEquals(variant, Variant.fromNull());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSerialize_NullSerializer_ShouldThrowException() throws Exception {
		// test
		Variant variant = Variant.fromTypedObject(consequence, null);

		// verify
		assertEquals(variant, Variant.fromNull());
	}

	@Test
	public void testDeserialize_ValidConsequenceVariant_happy() throws Exception {
		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);

		// verify
		assertNotNull(campaignConsequence);
		assertEquals(campaignConsequence.getId(), "test_id");
		assertEquals(campaignConsequence.getType(), "iam");
		assertEquals(campaignConsequence.getAssetsPath(), "test_assets_path");
		assertEquals(campaignConsequence.getDetail(), testDetailMap);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeserialize_NullSerializer_ShouldThrowError() throws Exception {
		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(null);

		// verify
		assertNull(campaignConsequence);
	}

	@Test
	public void testDeserialize_VariantMapNull_ShouldNotThrowException() throws Exception {
		// setup
		consequenceAsVariant = Variant.fromVariantMap((Map<String, Variant>)null); // results in Null Variant

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);

		// verify
		assertNull(campaignConsequence);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testDeserialize_VariantMapEmpty_ShouldThrowException() throws Exception {
		// setup
		consequenceAsVariant = Variant.fromVariantMap(new HashMap<String, Variant>());

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);

		// verify
		assertNull(campaignConsequence);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testDeserialize_VariantNotMap_ShouldThrowException() throws Exception {
		// setup
		consequenceAsVariant = Variant.fromString("blah");

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);

		// verify
		assertNull(campaignConsequence);
	}


	@Test
	public void testDeserialize_NullVariant_ShouldNotThrowException() throws Exception {
		// setup
		consequenceAsVariant = Variant.fromNull();

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);

		// verify
		assertNull(campaignConsequence);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testDeserialize_MissingRuleConsequenceIdInVariant_ShouldThrowException() throws Exception {
		// setup
		consequenceMap.remove(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID);
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testDeserialize_NullRuleConsequenceIdInVariant_ShouldThrowException() throws Exception {
		// setup
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID, Variant.fromString(null));
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testDeserialize_EmptyRuleConsequenceIdInVariant_ShouldThrowException() throws Exception {
		// setup
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID, Variant.fromString(""));
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testSerialize_MissingRuleConsequenceTypeInVariant_ShouldThrowException() throws Exception {
		// setup
		consequenceMap.remove(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_TYPE);
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testSerialize_NullRuleConsequenceTypeInVariant_ShouldThrowException() throws Exception {
		// setup
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_TYPE, Variant.fromString(null));
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testSerialize_EmptyRuleConsequenceTypeInVariant_ShouldThrowException() throws Exception {
		// setup
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_TYPE, Variant.fromString(""));
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);
	}

	@Test
	public void testSerialize_MissingRuleConsequenceAssetsPathInVariant_ShouldNotThrowException() throws Exception {
		// setup
		consequenceMap.remove(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ASSETS_PATH);
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);

		// verify
		assertNotNull(campaignConsequence);
	}

	@Test
	public void testSerialize_NullRuleConsequenceAssetsPathInVariant_ShouldNotThrowException() throws Exception {
		// setup
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ASSETS_PATH,
						   Variant.fromString(null));
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);

		// verify
		assertNotNull(campaignConsequence);
	}

	@Test
	public void testSerialize_EmptyRuleConsequenceAssetsPathInVariant_ShouldNotThrowException() throws Exception {
		// setup
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ASSETS_PATH,
						   Variant.fromString(""));
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);

		// verify
		assertNotNull(campaignConsequence);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testSerialize_MissingRuleConsequenceDetailMapInVariant_ShouldThrowException() throws Exception {
		// setup
		consequenceMap.remove(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL);
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testSerialize_NullRuleConsequenceDetailMapInVariant_ShouldThrowException() throws Exception {
		// setup
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL,
						   Variant.fromVariantMap(null));
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);
	}

	@Test(expected = VariantSerializationFailedException.class)
	public void testSerialize_EmptyRuleConsequenceDetailMapInVariant_ShouldThrowxception() throws Exception {
		// setup
		consequenceMap.put(CampaignTestConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL,
						   Variant.fromVariantMap(new HashMap<String, Variant>()));
		consequenceAsVariant = Variant.fromVariantMap(consequenceMap);

		// test
		CampaignRuleConsequence campaignConsequence = consequenceAsVariant.getTypedObject(serializer);
	}
}
