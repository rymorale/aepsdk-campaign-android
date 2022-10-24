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

import static com.adobe.marketing.mobile.campaign.CampaignConstants.CAMPAIGN_INTERACTION_TYPE;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ASSETS_PATH;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_ID;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.LOG_TAG;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_CACHE_DIR;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_DATA_ID_TOKENS_LEN;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_DATA_TAG_ID;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_DATA_TAG_ID_BUTTON_1;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_DATA_TAG_ID_BUTTON_2;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_DATA_TAG_ID_BUTTON_X;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_DATA_TAG_ID_DELIMITER;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_SCHEME;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_SCHEME_PATH_CANCEL;
import static com.adobe.marketing.mobile.campaign.CampaignConstants.MESSAGE_SCHEME_PATH_CONFIRM;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.services.ui.FullscreenMessage;
import com.adobe.marketing.mobile.services.ui.FullscreenMessageDelegate;
import com.adobe.marketing.mobile.services.ui.MessageSettings;
import com.adobe.marketing.mobile.services.ui.UIService;
import com.adobe.marketing.mobile.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides implementation logic for handling events related to full screen in-app messages.
 * <p>
 * The actual UI implementation happens in the platform services.
 */
class FullScreenMessage extends CampaignMessage {
	private final String SELF_TAG = "FullScreenMessage";

	private String html;
	private String htmlContent;
	private String assetsPath;

	List<List<String>> assets;

	class FullScreenMessageUiListener implements FullscreenMessageDelegate {
		/**
		 * Invoked when a {@code UIFullScreenMessage} is displayed.
		 * <p>
		 * Triggers a call to parent method {@link CampaignMessage#triggered()}.
		 *
		 * @param message the {@link FullscreenMessage} being displayed
		 */
		@Override
		public void onShow(final FullscreenMessage message) {
			Log.debug(LOG_TAG, SELF_TAG, "Fullscreen on show callback received.");
			triggered();
		}

		/**
		 * Invoked when a {@code UIFullScreenMessage} is dismissed.
		 * <p>
		 * Triggers a call to parent method {@link CampaignMessage#viewed()}.
		 *
		 * @param message the {@link FullscreenMessage} being dismissed
		 */
		@Override
		public void onDismiss(final FullscreenMessage message) {
			Log.debug(LOG_TAG, SELF_TAG, "Fullscreen on dismiss callback received.");
			viewed();
		}

		/**
		 * Invoked when a {@code FullscreenMessage} is attempting to load a URL.
		 * <p>
		 * The provided {@code urlString} can be in one of the following forms:
		 * <ul>
		 *     <li>{@literal adbinapp://confirm?id={broadlogId},{deliveryId},3&url={clickThroughUrl}}</li>
		 *     <li>{@literal adbinapp://confirm?id={broadlogId},{deliveryId},4}</li>
		 *     <li>{@literal adbinapp://cancel?id={broadlogId},{deliveryId},5}</li>
		 * </ul>
		 * Returns false if the scheme of the given {@code urlString} is not equal to {@value CampaignConstants#MESSAGE_SCHEME},
		 * or if the host is not one of {@value CampaignConstants#MESSAGE_SCHEME_PATH_CONFIRM} or
		 * {@value CampaignConstants#MESSAGE_SCHEME_PATH_CANCEL}.
		 * <p>
		 * Extracts the host and query information from the provided {@code urlString} in a {@code Map<String, String>} and
		 * passes it to the {@link CampaignMessage} class to dispatch a message click-through or viewed event.
		 *
		 * @param message the {@link FullscreenMessage} instance
		 * @param urlString {@link String} containing the URL being loaded by the {@code CampaignMessage}
		 *
		 * @return true if the SDK wants to handle the URL
		 * @see #processMessageInteraction(Map)
		 */
		@Override
		public boolean overrideUrlLoad(final FullscreenMessage message, final String urlString) {

			Log.trace(LOG_TAG, "Fullscreen overrideUrlLoad callback received with url (%s)", urlString);

			if (StringUtils.isNullOrEmpty(urlString)) {
				Log.debug(LOG_TAG, SELF_TAG, "Cannot process provided URL string, it is null or empty.");
				return true;
			}

			URI uri;

			try {
				uri = new URI(urlString);

			} catch (URISyntaxException ex) {
				Log.debug(LOG_TAG, "overrideUrlLoad -  Invalid message URI found (%s).", urlString);
				return true;
			}

			// check adbinapp scheme
			final String messageScheme =  uri.getScheme();

			if (!messageScheme.equals(MESSAGE_SCHEME)) {
				Log.debug(LOG_TAG, "overrideUrlLoad -  Invalid message scheme found in URI. (%s)", urlString);
				return false;
			}

			// cancel or confirm
			final String host = uri.getHost();

			if (!host.equals(MESSAGE_SCHEME_PATH_CONFIRM) &&
					!host.equals(MESSAGE_SCHEME_PATH_CANCEL)) {
				Log.debug(LOG_TAG,
						"overrideUrlLoad -  Unsupported URI host found, neither \"confirm\" nor \"cancel\". (%s)", urlString);
				return false;
			}

			// extract query, eg: id=h11901a,86f10d,3&url=https://www.adobe.com
			final String query = uri.getQuery();

			// Populate message data
			final Map<String, String> messageData = extractQueryParameters(query);

			if (messageData != null && !messageData.isEmpty()) {
				messageData.put(CAMPAIGN_INTERACTION_TYPE, host);

				// handle message interaction
				processMessageInteraction(messageData);
			}

			if (message != null) {
				message.dismiss();
			}

			return true;
		}

		@Override
		public boolean shouldShowMessage(final FullscreenMessage fullscreenMessage) {
			return true;
		}

		@Override
		public void onShowFailure() {
			Log.debug(LOG_TAG, SELF_TAG, "onShowFailure -  Fullscreen message failed to show.");
		}
	}

	/**
	 * Constructor.
	 *
	 * @param extension {@link CampaignExtension} that is this parent
	 * @param consequence {@code Map<String, Object>} instance containing a message-defining payload
	 * @throws CampaignMessageRequiredFieldMissingException if {@code consequence} is null or if any required field for a
	 * {@link FullScreenMessage} is null or empty
	 */
	FullScreenMessage(final CampaignExtension extension, final Map<String, Object> consequence) throws CampaignMessageRequiredFieldMissingException {
		super(extension, consequence);
		parseFullScreenMessagePayload(consequence);
	}

	/**
	 * Parses a {@code CampaignRuleConsequence} instance defining message payload for a {@code FullScreenMessage} object.
	 *
	 * Required fields:
	 * <ul>
	 *     <li>{@code html} - {@link String} containing html for this message</li>
	 * </ul>
	 * Optional fields:
	 * <ul>
	 *     <li>{@code assets} - {@code Array} of {@code String[]}s containing remote assets to prefetch and cache</li>
	 * </ul>
	 *
	 * @param consequence {@code Map<String, Object>} instance containing the message payload to be parsed
	 * @throws CampaignMessageRequiredFieldMissingException if any of the required fields are missing from {@code consequence}
	 */
	@SuppressWarnings("unchecked")
	private void parseFullScreenMessagePayload(final Map<String, Object> consequence) throws CampaignMessageRequiredFieldMissingException {

		if (consequence == null) {
			throw new CampaignMessageRequiredFieldMissingException("Message consequence is null.");
		}

		assetsPath = (String) consequence.get(MESSAGE_CONSEQUENCE_ASSETS_PATH);

		if (StringUtils.isNullOrEmpty(assetsPath)) {
			Log.debug(LOG_TAG, SELF_TAG,
					"parseFullScreenMessagePayload -  Unable to create fullscreen message, provided assets path is missing/empty.");
			throw new CampaignMessageRequiredFieldMissingException("Messages - Unable to create fullscreen message, assetPath is missing/empty.");
		}

		final Map<String, Object> detailDictionary = (Map<String, Object>) consequence.get(MESSAGE_CONSEQUENCE_DETAIL);

		if (detailDictionary == null || detailDictionary.isEmpty()) {
			throw new CampaignMessageRequiredFieldMissingException("Unable to create fullscreen message, message detail is missing or not an object.");
		}

		// html is required
		html = (String) detailDictionary.get(MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML);

		if (StringUtils.isNullOrEmpty(html)) {
			Log.debug(LOG_TAG, SELF_TAG,
					"parseFullScreenMessagePayload -  Unable to create fullscreen message, html is missing/empty.");
			throw new CampaignMessageRequiredFieldMissingException("Messages - Unable to create fullscreen message, html is missing/empty.");
		}

		// remote assets are optional
		final List<List<String>> assetsArray = (List<List<String>>) detailDictionary.get(MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS);

		if (assetsArray != null && !assetsArray.isEmpty()) {
			assets = new ArrayList<>();

			for (final List<String> currentAssetArray : assetsArray) {
				extractAsset(currentAssetArray);
			}
		} else {
			Log.trace(LOG_TAG, SELF_TAG,
					"parseFullScreenMessagePayload -  Tried to read \"assets\" for fullscreen message but none found.  This is not a required field.");
		}
	}

	/**
	 * Loops through a {@code List<String>} array and adds any assets it finds there to this.
	 * <p>
	 * If {@code currentAssets} param is null or empty, calling this method has no effect.
	 *
	 * @param currentAssets {@code List<String>} containing assets specific for this {@link FullScreenMessage}
	 */
	private void extractAsset(final List<String> currentAssets) {
		if (currentAssets == null || currentAssets.isEmpty()) {
			Log.trace(LOG_TAG, SELF_TAG, "There are no assets to extract.");
			return;
		}

		final ArrayList<String> currentAsset = new ArrayList<String>();

		for (final String assetString : currentAssets) {
			if (!StringUtils.isNullOrEmpty(assetString)) {
				currentAsset.add(assetString);
			}
		}

		assets.add(currentAsset);
	}

	/**
	 * Creates and shows a new {@link FullscreenMessage} object and registers a {@link FullScreenMessageUiListener}
	 * instance with the {@code UIService} to receive message interaction events.
	 * <p>
	 * This method reads the {@link #htmlContent} from the cached html at {@link #assetsPath} and generates the expanded html by
	 * replacing assets URLs with cached references, before calling the method on the {@code UIService} to display the message.
	 *
	 * @see #getCachedResourcesMapAndUpdateHtml()
	 * @see UIService#createFullscreenMessage(String, FullscreenMessageDelegate, boolean, MessageSettings)
	 */
	@Override
	void showMessage() {
		Log.debug(LOG_TAG, SELF_TAG, "showMessage -  Attempting to show fullscreen message with ID %s", messageId);

		final UIService uiService = ServiceProvider.getInstance().getUIService();

		if (uiService == null) {
			Log.warning(LOG_TAG, SELF_TAG,
					"UI Service is unavailable.  Unable to show fullscreen message with ID (%s)",
					messageId);
			return;
		}

		htmlContent = readHtmlFromFile(new File(assetsPath + File.separator + html));

		if (StringUtils.isNullOrEmpty(htmlContent)) {
			Log.debug(LOG_TAG, SELF_TAG,
					"showMessage -  No html content in file (%s). File is missing or invalid!", html);
			return;
		}

		final Map<String, String> cachedResourcesMap = getCachedResourcesMapAndUpdateHtml();

		final FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessageUiListener();
		final MessageSettings messageSettings = new MessageSettings();
		// display ACS fullscreen messages are displayed at 100% scale
		messageSettings.setHeight(100);
		messageSettings.setWidth(100);
		messageSettings.setParent(this);
		final FullscreenMessage fullscreenMessage = uiService.createFullscreenMessage(htmlContent,
				fullScreenMessageUiListener, !cachedResourcesMap.isEmpty(), messageSettings);


		if (fullscreenMessage != null) {
			fullscreenMessage.setLocalAssetsMap(cachedResourcesMap);
			fullscreenMessage.show();
		}
	}

	/**
	 * Reads a html file from disk and returns its contents as {@code String}.
	 *
	 * @param htmlFile the {@link File} object to read
	 * @return the file contents as {@code String}
	 */
	private String readHtmlFromFile(final File htmlFile) {
		String htmlString = null;

		if (htmlFile != null) {
			FileInputStream htmlInputStream = null;

			try {
				htmlInputStream = new FileInputStream(htmlFile);
				htmlString = Utils.inputStreamToString(htmlInputStream);
			} catch (final IOException ex) {
				Log.debug(LOG_TAG, SELF_TAG, "readHtmlFromFile -  Could not read the html file! (%s)", ex);
			} finally {
				try {
					if (htmlInputStream != null) {
						htmlInputStream.close();
					}
				} catch (final Exception e) {
					Log.trace(LOG_TAG, SELF_TAG,"readHtmlFromFile -  Failed to close stream for %s", htmlFile);
				}
			}
		}

		return htmlString;
	}

	/**
	 * Determines whether this class has downloadable assets.
	 *
	 * @return true as this class has downloadable assets
	 */
	@Override
	boolean shouldDownloadAssets() {
		return true;
	}


	/**
	 * Returns a {@code Map<String,String>} containing remote resource URL as key and cached resource path as value for all remote resource that are cached.
	 * <p>
	 * This function uses {@link CacheService} to find cached remote file. if a cached file is present then add it to the {@code Map<String, String>} that will be returned.
	 * Replace the remote URL's in {@link #htmlContent} that are not cached with fallback image (if present).
	 * </p>
	 * This functions returns empty map in following cases.
	 * <ul>
	 * <li>Asset List is empty.</li>
	 * <li>{@link CacheService} in null.</li>
	 * </ul>
	 *
	 * @return {@code Map<String,String>}
	 */
	private Map<String, String> getCachedResourcesMapAndUpdateHtml() {
		// early bail if we don't have assets
		if (assets == null || assets.isEmpty()) {
			Log.debug(LOG_TAG, SELF_TAG, "generateExpandedHtml -  No cached assets found, cannot expand URLs in the HTML.");
			return Collections.emptyMap();
		}

		CacheService cacheService = ServiceProvider.getInstance().getCacheService();

		if (cacheService == null) {
			Log.debug(LOG_TAG, SELF_TAG,
					"getLocalResourcesMapping -  No cache service found, cannot generate local resource mapping.");
			return Collections.emptyMap();
		}

		final HashMap<String, String> cachedImagesMap = new HashMap<String, String>();
		final Map<String, String> fallbackImagesMap = new HashMap<String, String>();

		for (final List<String> currentAssetArray : assets) {
			if (currentAssetArray.isEmpty()) {
				continue;
			}

			final String assetUrl = currentAssetArray.get(0);
			final int currentAssetArrayCount = currentAssetArray.size();
			String assetValue = null;
			int currentAssetNumber = 0;

			// loop through our assets to see if we have any of them in cache
			while (currentAssetNumber < currentAssetArrayCount) {
				final String currentAsset = currentAssetArray.get(currentAssetNumber);
				final CacheResult assetValueFile = cacheService.get(currentAsset, MESSAGE_CACHE_DIR + File.separator + messageId);

				if (assetValueFile != null) {
					assetValue = assetValueFile.getMetadata().get("path");
					break;
				}

				currentAssetNumber++;
			}

			// if assetValue is still null, none of our urls have been cached, so we check for a bundled asset
			if (assetValue == null) {
				assetValue = currentAssetArray.get(currentAssetArrayCount - 1);
				boolean isLocalImage = !Utils.stringIsUrl(assetValue);

				if (isLocalImage) {
					fallbackImagesMap.put(assetUrl, assetValue);
				}
			} else {
				cachedImagesMap.put(assetUrl, assetValue);
			}
		}

		htmlContent = expandTokens(htmlContent, fallbackImagesMap);
		return cachedImagesMap;
	}

	/**
	 * Attempts to handle {@code Fullscreen} message interaction by inspecting the {@code id} field on the clicked message.
	 * <p>
	 * The method looks for {@code id} field in the provided {@code query} Map and invokes method on the parent {@link CampaignMessage}
	 * class to dispatch message click-through or viewed event. The {@code id} field is a {@code String} in the form
	 * {@literal {broadlogId},{deliveryId},{tagId}}, where {@code tagId} can assume values 3,4 or 5.
	 * <p>
	 * If the {@code id} field is missing in the provided {@code query} or if it cannot be parsed to extract a valid {@code tagId}
	 * then no message interaction event shall be dispatched.
	 *
	 * @param query {@code Map<String, String>} query containing message interaction details
	 * @see #clickedWithData(Map)
	 */
	private void processMessageInteraction(final Map<String, String> query) {
		if (query == null || query.isEmpty()) {
			Log.debug(LOG_TAG, SELF_TAG,
					"processMessageInteraction -  Cannot process message interaction, input query is null or empty.");
			return;
		}

		if (query.containsKey(MESSAGE_DATA_TAG_ID)) {
			final String id = query.get(MESSAGE_DATA_TAG_ID);
			final String[] strTokens = id.split(MESSAGE_DATA_TAG_ID_DELIMITER);

			if (strTokens.length != MESSAGE_DATA_ID_TOKENS_LEN) {
				Log.debug(LOG_TAG, MESSAGE_CONSEQUENCE_ID,
						"processMessageInteraction -  Cannot process message interaction, input query contains insufficient id tokens.");
				return;
			}

			int tagId = 0;

			try {
				tagId = Integer.parseInt(strTokens[2]);
			} catch (NumberFormatException e) {
				Log.debug(LOG_TAG, SELF_TAG,
						"processMessageInteraction -  Cannot parse tag Id from the id field in given query (%s).", e);
			}

			switch (tagId) {
				case MESSAGE_DATA_TAG_ID_BUTTON_1: // adbinapp://confirm/?id=h11901a,86f10d,3
				case MESSAGE_DATA_TAG_ID_BUTTON_2: // adbinapp://confirm/?id=h11901a,86f10d,4
				case MESSAGE_DATA_TAG_ID_BUTTON_X: // adbinapp://cancel?id=h11901a,86f10d,5
					clickedWithData(query);
					viewed(); // Temporary fix for AMSDK-7633. No viewed event should be dispatched on confirm.
					break;

				default:
					Log.debug(LOG_TAG, SELF_TAG,
							"processMessageInteraction -  Unsupported tag Id found in the id field in the given query (%s).", tagId);
					break;

			}
		}
	}

	/**
	 * Extracts query parameters from a given {@code String} into a {@code Map<String, String>}.
	 *
	 * @param queryString {@link String} containing query parameters
	 * @return the extracted {@code Map<String, String>} query parameters
	 */
	private Map<String, String> extractQueryParameters(final String queryString) {
		if (StringUtils.isNullOrEmpty(queryString)) {
			return null;
		}

		final Map<String, String> parameters = new HashMap<>();
		final String[] paramArray = queryString.split("&");

		for (String currentParam : paramArray) {
			// quick out in case this entry is null or empty string
			if (StringUtils.isNullOrEmpty(currentParam)) {
				continue;
			}

			final String[] currentParamArray = currentParam.split("=", 2);

			if (currentParamArray.length != 2 ||
					(currentParamArray[0].isEmpty() || currentParamArray[1].isEmpty())) {
				continue;
			}

			final String key = currentParamArray[0];
			final String value = currentParamArray[1];
			parameters.put(key, value);
		}

		return parameters;
	}
}