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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Extends {@code HitQueue.IHitProcessor} and aids in the necessary tasks to
 * create and maintain a database to hold requests for the Campaign Extension.
 */
class CampaignHitsDatabase implements HitQueue.IHitProcessor<CampaignHit> {

	private final NetworkService networkService;
	private final SystemInfoService systemInfoService;
	private final HitQueue<CampaignHit, CampaignHitSchema> hitQueue;
	private LocalStorageService.DataStore dataStore;
	/**
	 * Default Constructor
	 *
	 * @param services {@link PlatformServices} instance
	 * @throws MissingPlatformServicesException if {@code services} is null
	 */
	CampaignHitsDatabase(final PlatformServices services) throws MissingPlatformServicesException {
		this(services, null);
		this.dataStore = services.getLocalStorageService().getDataStore(CampaignConstants.CAMPAIGN_DATA_STORE_NAME);
	}

	/**
	 * Constructor for test
	 *
	 * @param services {@link PlatformServices} instance
	 * @param hitQueue {@link HitQueue} for unit testing purposes
	 * @throws MissingPlatformServicesException if {@code services} is null
	 */
	CampaignHitsDatabase(final PlatformServices services,
						 final HitQueue<CampaignHit, CampaignHitSchema> hitQueue)
	throws MissingPlatformServicesException {

		if (services == null) {
			throw new MissingPlatformServicesException("Platform services are not available.");
		}

		this.networkService = services.getNetworkService();
		this.systemInfoService = services.getSystemInfoService();
		this.dataStore = services.getLocalStorageService().getDataStore(CampaignConstants.CAMPAIGN_DATA_STORE_NAME);

		if (hitQueue != null) { // for unit test
			this.hitQueue = hitQueue;
		} else {
			final File directory = systemInfoService != null ? systemInfoService.getApplicationCacheDir() : null;
			final File dbFilePath = new File(directory, CampaignConstants.CAMPAIGN_DATABASE_FILENAME);
			this.hitQueue = new HitQueue<CampaignHit, CampaignHitSchema>(services, dbFilePath,
					CampaignConstants.CAMPAIGN_TABLE_NAME, new CampaignHitSchema(), this);
		}
	}

	/**
	 * Processes and attempts to send the provided {@code hit}.
	 *<p>
	 *
	 * This method will call {@link NetworkService#connectUrl(String, NetworkService.HttpCommand, byte[], Map, int, int)}
	 * to send the {@link CampaignExtension} network request represented by the {@link CampaignHit}.
	 * A {@link NetworkService.HttpConnection} instance is returned from the {@code NetworkService#connectUrl()} method call
	 * which is used to determine if the {@code CampaignExtension} network request should be retried.
	 *
	 * If the {@code NetworkService.HttpConnection} is null (which occurs when the android device has no network connection),
	 * this method will return {@link HitQueue.RetryType#YES} and the {@code hit} will be retried later.
	 *
	 * If the {@code NetworkService.HttpConnection} contains a response code that is recoverable ({@link HttpURLConnection#HTTP_CLIENT_TIMEOUT},
	 * {@link HttpURLConnection#HTTP_UNAVAILABLE}, or {@link HttpURLConnection#HTTP_GATEWAY_TIMEOUT}) this method will return
	 * {@code HitQueue.RetryType.YES} and the {@code hit} will be retried later.
	 *
	 * If the {@code NetworkService.HttpConnection} contains a {@link HttpURLConnection#HTTP_OK} response code, or any additional response code
	 * not considered to be a recoverable network error, then this method will return {@code HitQueue.RetryType.NO}.
	 * The {@code hit} will be considered processed and will not be retried.
	 *
	 * @param hit {@code CampaignHit} instance to be processed
	 * @return {@code HitQueue.RetryType} indicating whether the hit should be processed again
	 */
	@Override
	public HitQueue.RetryType process(final CampaignHit hit) {
		try {
			byte[] outputBytes = null;

			if (hit.body != null) {
				outputBytes = hit.body.getBytes(StringUtils.CHARSET_UTF_8);
			}

			Map<String, String> headers = NetworkConnectionUtil.getHeaders(true,
										  NetworkConnectionUtil.HTTP_HEADER_CONTENT_TYPE_JSON_APPLICATION);
			headers.put(NetworkConnectionUtil.HTTP_HEADER_KEY_ACCEPT, "*/*");
			NetworkService.HttpConnection connection = networkService.connectUrl(hit.url, hit.getHttpCommand(), outputBytes,
					headers, hit.timeout, hit.timeout);

			if (connection == null || (connection.getResponseCode() == CampaignConstants.INVALID_CONNECTION_RESPONSE_CODE)) {
				Log.debug(CampaignConstants.LOG_TAG,
						  "network process - Could not process a Campaign network request because the connection was null or response code was invalid. Retrying the request.");
				return HitQueue.RetryType.YES;
			} else if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				Log.debug(CampaignConstants.LOG_TAG, "network process -  Request (%s) was sent", hit.url);
				updateTimestampInDataStore(TimeUnit.SECONDS.toMillis(hit.timestamp));
				return HitQueue.RetryType.NO;
			} else if (!NetworkConnectionUtil.recoverableNetworkErrorCodes.contains(connection.getResponseCode())) {
				Log.debug(CampaignConstants.LOG_TAG,
						  "network process -  Un-recoverable network error while processing requests. Discarding request.");
				return HitQueue.RetryType.NO;
			} else {
				Log.debug(CampaignConstants.LOG_TAG,
						  "network process -  Recoverable network error while processing requests, will retry.");
				return HitQueue.RetryType.YES;
			}
		} catch (UnsupportedEncodingException e) {
			Log.warning(CampaignConstants.LOG_TAG,
						"Unable to encode the post body (%s) for the Campaign request, %s", hit.body, e);
			return HitQueue.RetryType.NO;
		}
	}

	/**
	 * Updates the mobile privacy status and does one of the following based on the privacy setting.
	 * <ul>
	 * <li>If {@link MobilePrivacyStatus#OPT_IN}, resumes processing the queued hits.</li>
	 * <li>If {@code MobilePrivacyStatus.OPT_OUT}, suspends the queue and deletes all hits.</li>
	 * <li>If {@code MobilePrivacyStatus.UNKNOWN}, suspends the queue.</li>
	 * </ul>
	 *
	 * @param privacyStatus the new {@code MobilePrivacyStatus}
	 */
	void updatePrivacyStatus(final MobilePrivacyStatus privacyStatus) {
		switch (privacyStatus) {
			case OPT_IN:
				this.hitQueue.bringOnline();
				break;

			case OPT_OUT:
				this.hitQueue.suspend();
				this.hitQueue.deleteAllHits();
				break;

			case UNKNOWN:
				this.hitQueue.suspend();
				break;
		}
	}

	/**
	 * Creates a new record in the {@code CampaignHitsDatabase} table from the information in the provided {@code CampaignHit} instance.
	 * <p>
	 * If the current {@link MobilePrivacyStatus} is {@link MobilePrivacyStatus#OPT_IN} then the {@link HitQueue} is brought online.
	 * The {@code HitQueue} object then queries the Campaign database table and starts processing any queued hits.
	 *
	 * @param campaignHit the {@code CampaignHit} instance
	 * @param timestampMillis {@code long} value containing the event timestamp to be associated with the campaign hit
	 * @param privacyStatus {@code MobilePrivacyStatus} the current privacy status
	 */
	void queue(final CampaignHit campaignHit, final long timestampMillis, final MobilePrivacyStatus privacyStatus) {
		if (campaignHit == null) {
			Log.debug(CampaignConstants.LOG_TAG, "Unable to queue the provided Campaign hit, the hit is null");
			return;
		}

		campaignHit.timestamp = TimeUnit.MILLISECONDS.toSeconds(timestampMillis);

		this.hitQueue.queue(campaignHit);

		if (privacyStatus == MobilePrivacyStatus.OPT_IN) {
			this.hitQueue.bringOnline();
		}
	}

	/**
	 * Updates {@value CampaignConstants#CAMPAIGN_DATA_STORE_REGISTRATION_TIMESTAMP_KEY} in {@code CampaignExtension}'s {@code DataStore}.
	 * <p>
	 *
	 * @param timestamp {@code long} containing the time of the last successful registration.
	 */
	protected void updateTimestampInDataStore(final long timestamp) {
		if (dataStore == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "updateTimestampInDataStore -  Campaign Data store is not available to update.");
			return;
		}

		Log.trace(CampaignConstants.LOG_TAG,
				  "updateTimestampInDataStore -  Persisting timestamp (%d) in Campaign Data Store.", timestamp);
		dataStore.setLong(CampaignConstants.CAMPAIGN_DATA_STORE_REGISTRATION_TIMESTAMP_KEY, timestamp);
	}

}