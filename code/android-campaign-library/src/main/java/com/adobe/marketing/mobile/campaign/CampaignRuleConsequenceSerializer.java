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

import java.util.HashMap;
import java.util.Map;

/**
 * {@code CampaignConsequenceSerializer} can be used to serialize {@code CampaignRuleConsequence} instance to a {@code Variant}
 * and to deserialize a {@code Variant} to {@code CampaignRuleConsequence} instance.
 */
final class CampaignRuleConsequenceSerializer implements VariantSerializer<CampaignRuleConsequence> {

	/**
	 * Serializes the given {@code CampaignConsequence} instance to a {@code Variant}.
	 *
	 * @param consequence {@link CampaignRuleConsequence} instance to serialize
	 * @return {@link Variant} representing {@code CampaignRuleConsequence}, or the null variant if {@code consequence} is null
	 */
	@Override
	public Variant serialize(final CampaignRuleConsequence consequence) {
		if (consequence == null) {
			Log.debug(CampaignConstants.LOG_TAG, "serialize - CampaignRuleConsequence is null, so returning null Variant.");
			return Variant.fromNull();
		}

		Map<String, Variant> map = new HashMap<String, Variant>();

		String id = consequence.getId();
		map.put(CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID, (id == null) ? Variant.fromNull() :
				Variant.fromString(consequence.getId()));

		String type = consequence.getType();
		map.put(CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_TYPE, (type == null) ? Variant.fromNull() :
				Variant.fromString(consequence.getType()));

		String assetsPath = consequence.getAssetsPath();
		map.put(CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ASSETS_PATH,
				(assetsPath == null) ? Variant.fromNull() :
				Variant.fromString(consequence.getAssetsPath()));

		Map<String, Variant> detailMap = consequence.getDetail();
		map.put(CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL,
				(detailMap == null) ? Variant.fromNull() :
				Variant.fromVariantMap(consequence.getDetail()));

		return Variant.fromVariantMap(map);
	}

	/**
	 * Deserializes the given {@code Variant} to a {@code CampaignRuleConsequence} instance.
	 *
	 * @param variant {@link Variant} to deserialize
	 * @return a {@link CampaignRuleConsequence} instance that was deserialized from the variant. Can be null.
	 *
	 * @throws IllegalArgumentException if variant is null
	 * @throws VariantSerializationFailedException if variant serialization failed
	 */
	@Override
	public CampaignRuleConsequence deserialize(final Variant variant) throws VariantSerializationFailedException {
		if (variant == null) {
			throw new IllegalArgumentException("Variant for deserialization is null.");
		}

		if (variant.getKind() == VariantKind.NULL) {
			Log.trace(CampaignConstants.LOG_TAG,
					  "deserialize -  Variant kind is null, null Consequence is returned.");
			return null;
		}

		final Map<String, Variant> consequenceMap = variant.optVariantMap(null);

		if (consequenceMap == null || consequenceMap.isEmpty()) {
			throw new VariantSerializationFailedException("deserialize -  Consequence Map is null or empty.");
		}

		// id - required field
		final String id = Variant.optVariantFromMap(consequenceMap,
						  CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID).optString(null);

		if (StringUtils.isNullOrEmpty(id)) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "deserialize -  Unable to find field \"id\" in Campaign rules consequence. This a required field.");
			throw new VariantSerializationFailedException("Consequence \"id\" is null or empty.");
		}

		// type - required field
		final String type = Variant.optVariantFromMap(consequenceMap,
							CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_TYPE).optString(null);

		if (StringUtils.isNullOrEmpty(type)) {
			Log.warning(CampaignConstants.LOG_TAG,
						"No valid field \"type\" in Campaign rules consequence. This is a required field.");
			throw new VariantSerializationFailedException("Consequence \"type\" is null or empty.");
		}

		// assetsPath - optional field
		final String assetsPath = Variant.optVariantFromMap(consequenceMap,
								  CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ASSETS_PATH).optString("");

		if (StringUtils.isNullOrEmpty(assetsPath)) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "No valid field \"assetsPath\" in Campaign rules consequence. This is not a required field.");
		}

		// detail - required field
		final Map<String, Variant> detail = Variant.optVariantFromMap(consequenceMap,
											CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL).optVariantMap(null);

		if (detail == null || detail.isEmpty()) {
			Log.warning(CampaignConstants.LOG_TAG,
						"No valid field \"detail\" in Campaign rules consequence. This a required field.");
			throw new VariantSerializationFailedException("Consequence \"detail\" is null or empty.");
		}

		return new CampaignRuleConsequence(id, type, assetsPath, detail);
	}
}