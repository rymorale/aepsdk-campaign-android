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

import java.util.Map;

/**
 * {@code CampaignRuleConsequence} class represents a Campaign rule consequence.
 */
class CampaignRuleConsequence {
	private final String id;
	private final String type;
	private String assetsPath;
	private final Map<String, Variant> detail;

	/**
	 * Constructor
	 *
	 * @param id {@link String} containing unique consequence Id
	 * @param type {@code String} containing message consequence type
	 * @param assetsPath optional {@code String} containing path for the cached html asset for fullscreen messages
	 * @param detail {@code Map<String, Variant>} containing consequence detail
	 */
	CampaignRuleConsequence(final String id, final String type, final String assetsPath,
							final Map<String, Variant> detail) {
		this.id = id;
		this.type = type;
		this.assetsPath = assetsPath;
		this.detail = detail;
	}

	/**
	 * Set this CampaignRuleConsequence {@code assetsPath} with the provided value.
	 *
	 * @param assetsPath {@link String} containing the assets path
	 */
	void setAssetsPath(final String assetsPath) {
		this.assetsPath = assetsPath;
	}

	/**
	 * Get the {@code id} for this {@code CampaignRuleConsequence} instance.
	 *
	 * @return {@link String} containing this {@link CampaignRuleConsequence#id}
	 */
	String getId() {
		return id;
	}

	/**
	 * Get the {@code type} for this {@code CampaignRuleConsequence} instance.
	 *
	 * @return {@link String} containing this {@link CampaignRuleConsequence#type}
	 */
	String getType() {
		return type;
	}

	/**
	 * Get the {@code assetsPath} for this {@code CampaignRuleConsequence} instance.
	 *
	 * @return {@link String} containing this {@link CampaignRuleConsequence#assetsPath}
	 */
	String getAssetsPath() {
		return assetsPath;
	}

	/**
	 * Get the {@code detail} Map for this {@code CampaignRuleConsequence} instance.
	 *
	 * @return {@code Map<String,Variant>} containing this {@link CampaignRuleConsequence#detail}
	 */
	Map<String, Variant> getDetail() {
		return detail;
	}
}
