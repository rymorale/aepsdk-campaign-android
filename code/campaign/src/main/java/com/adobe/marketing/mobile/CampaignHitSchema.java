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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.adobe.marketing.mobile.DatabaseService.Database.ColumnConstraint;
import com.adobe.marketing.mobile.DatabaseService.Database.ColumnDataType;

/**
 * Extends {@code AbstractHitSchema} and defines the structure for the Campaign database table.
 */
class CampaignHitSchema extends AbstractHitSchema<CampaignHit> {

	private static final String HIT_ID_COL_NAME = "ID";
	private static final int HIT_ID_COL_INDEX = 0;
	private static final String HIT_URL_COL_NAME = "URL";
	private static final int HIT_URL_COL_INDEX = 1;
	private static final String HIT_TIMESTAMP_COL_NAME = "TIMESTAMP";
	private static final int HIT_TIMESTAMP_COL_INDEX = 2;
	private static final String HIT_BODY_COL_NAME = "BODY";
	private static final int HIT_BODY_COL_INDEX = 3;
	private static final String HIT_TIMEOUT_COL_NAME = "TIMEOUT";
	private static final int HIT_TIMEOUT_COL_INDEX = 4;

	/**
	 * Constructor
	 * <ul>
	 *     <li>Initializes and populates {@code #columnConstraints}</li>
	 *     <li>Initializes and populates {@code #columnNames}</li>
	 *     <li>Initializes and populates {@code #columnDataTypes}</li>
	 * </ul>
	 */
	CampaignHitSchema() {
		this.columnConstraints =
			new ArrayList<List<ColumnConstraint>>();
		List<ColumnConstraint> idColumnConstraints =
			new ArrayList<ColumnConstraint>();
		idColumnConstraints.add(ColumnConstraint.PRIMARY_KEY);
		idColumnConstraints.add(ColumnConstraint.AUTOINCREMENT);
		this.columnConstraints.add(idColumnConstraints);
		this.columnConstraints.add(new ArrayList<ColumnConstraint>());
		this.columnConstraints.add(new ArrayList<ColumnConstraint>());
		this.columnConstraints.add(new ArrayList<ColumnConstraint>());
		this.columnConstraints.add(new ArrayList<ColumnConstraint>());

		this.columnNames = new String[] {HIT_ID_COL_NAME, HIT_URL_COL_NAME, HIT_TIMESTAMP_COL_NAME
										 , HIT_BODY_COL_NAME, HIT_TIMEOUT_COL_NAME
										};

		this.columnDataTypes = new ColumnDataType[] {
			ColumnDataType.INTEGER,
			ColumnDataType.TEXT,
			ColumnDataType.INTEGER,
			ColumnDataType.TEXT,
			ColumnDataType.INTEGER,
		};
	}

	/**
	 * Converts the provided database query result into a {@code CampaignHit} instance.
	 *
	 * @param queryResult {@link DatabaseService.QueryResult} instance representing a record in the {@link CampaignExtension} database
	 * @return {@link CampaignHit} represented by the provided query result
	 */
	@Override
	CampaignHit generateHit(final DatabaseService.QueryResult queryResult) {
		try {
			CampaignHit campaignHit = new CampaignHit();
			campaignHit.identifier = queryResult.getString(HIT_ID_COL_INDEX);
			campaignHit.url = queryResult.getString(HIT_URL_COL_INDEX);
			campaignHit.timestamp = queryResult.getLong(HIT_TIMESTAMP_COL_INDEX);
			campaignHit.body = queryResult.getString(HIT_BODY_COL_INDEX);
			campaignHit.timeout = queryResult.getInt(HIT_TIMEOUT_COL_INDEX);
			return campaignHit;
		} catch (Exception e) {
			Log.error(CampaignConstants.LOG_TAG, "Unable to read from database. Query failed with error %s", e);
			return null;
		} finally {
			if (queryResult != null) {
				queryResult.close();
			}
		}
	}

	/**
	 * Generates a {@code Map} to be used for a database insert operation.
	 *
	 * @param hit {@link CampaignHit} instance containing the data to be inserted into the {@link CampaignExtension} database
	 * @return {@code Map<String, Object>} containing the data from the provided {@code CampaignHit} instance
	 */
	@Override
	Map<String, Object> generateDataMap(final CampaignHit hit) {
		if (hit == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "generateDataMap - Cannot insert hit into the database because the provided hit is null.");
			return null;
		}

		Map<String, Object> dataMap = new HashMap<String, Object>();
		dataMap.put(HIT_URL_COL_NAME, hit.url);
		dataMap.put(HIT_TIMESTAMP_COL_NAME, hit.timestamp);
		dataMap.put(HIT_BODY_COL_NAME, hit.body);
		dataMap.put(HIT_TIMEOUT_COL_NAME, hit.timeout);
		return dataMap;
	}

}