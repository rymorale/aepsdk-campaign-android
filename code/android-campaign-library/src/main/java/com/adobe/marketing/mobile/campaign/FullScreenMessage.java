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

import com.adobe.marketing.mobile.UIService.UIFullScreenMessage;

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
	private String html;
	private String htmlContent;
	private String assetsPath;

	List<List<String>> assets;

	class FullScreenMessageUiListener implements UIService.UIFullScreenListener {
		/**
		 * Invoked when a {@code UIFullScreenMessage} is displayed.
		 * <p>
		 * Triggers a call to parent method {@link CampaignMessage#triggered()}.
		 *
		 * @param message the {@link UIFullScreenMessage} being displayed
		 */
		@Override
		public void onShow(final UIFullScreenMessage message) {
			Log.debug(CampaignConstants.LOG_TAG, "Fullscreen on show callback received.");
			triggered();
		}

		/**
		 * Invoked when a {@code UIFullScreenMessage} is dismissed.
		 * <p>
		 * Triggers a call to parent method {@link CampaignMessage#viewed()}.
		 *
		 * @param message the {@link UIFullScreenMessage} being dismissed
		 */
		@Override
		public void onDismiss(final UIFullScreenMessage message) {
			Log.debug(CampaignConstants.LOG_TAG, "Fullscreen on dismiss callback received.");
			viewed();
		}

		/**
		 * Invoked when a {@code UIFullScreenMessage} is attempting to load a URL.
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
		 * passes it to the {@link CampaignMessage} class to dispatch message click-through or viewed event.
		 *
		 * @param message the {@link UIFullScreenMessage} instance
		 * @param urlString {@link String} containing the URL being loaded by the {@code CampaignMessage}
		 *
		 * @return true if the SDK wants to handle the URL
		 * @see #processMessageInteraction(Map)
		 */
		@Override
		public boolean overrideUrlLoad(final UIFullScreenMessage message, final String urlString) {

			Log.trace(CampaignConstants.LOG_TAG, "Fullscreen overrideUrlLoad callback received with url (%s)", urlString);

			if (StringUtils.isNullOrEmpty(urlString)) {
				Log.debug(CampaignConstants.LOG_TAG, "Cannot process provided URL string, it is null or empty.");
				return true;
			}

			URI uri = null;

			try {
				uri = new URI(urlString);

			} catch (URISyntaxException ex) {
				Log.debug(CampaignConstants.LOG_TAG, "overrideUrlLoad -  Invalid message URI found (%s).", urlString);
				return true;
			}

			// check adbinapp scheme
			final String messageScheme =  uri.getScheme();

			if (!messageScheme.equals(CampaignConstants.MESSAGE_SCHEME)) {
				Log.debug(CampaignConstants.LOG_TAG, "overrideUrlLoad -  Invalid message scheme found in URI. (%s)", urlString);
				return false;
			}

			// cancel or confirm
			final String host = uri.getHost();

			if (!host.equals(CampaignConstants.MESSAGE_SCHEME_PATH_CONFIRM) &&
					!host.equals(CampaignConstants.MESSAGE_SCHEME_PATH_CANCEL)) {
				Log.debug(CampaignConstants.LOG_TAG,
						  "overrideUrlLoad -  Unsupported URI host found, neither \"confirm\" nor \"cancel\". (%s)", urlString);
				return false;
			}

			// extract query, eg: id=h11901a,86f10d,3&url=https://www.adobe.com
			final String query = uri.getQuery();

			// Populate message data
			final Map<String, String> messageData = UrlUtilities.extractQueryParameters(query);

			if (messageData != null && !messageData.isEmpty()) {
				messageData.put(CampaignConstants.CAMPAIGN_INTERACTION_TYPE, host);

				// handle message interaction
				processMessageInteraction(messageData);
			}

			if (message != null) {
				message.remove();
			}

			return true;
		}
	}

	/**
	 * Constructor.
	 *
	 * @param extension {@link CampaignExtension} that is this parent
	 * @param platformServices {@link PlatformServices} instance used to access JSON implementation
	 * @param consequence {@code CampaignRuleConsequence} instance containing a message-defining payload
	 * @throws CampaignMessageRequiredFieldMissingException if {@code consequence} is null or if any required field for a
	 * {@link FullScreenMessage} is null or empty
	 * @throws MissingPlatformServicesException if {@code platformServices} is null
	 */
	FullScreenMessage(final CampaignExtension extension, final PlatformServices platformServices,
					  final CampaignRuleConsequence consequence)
	throws CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		super(extension, platformServices, consequence);
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
	 * @param consequence {@code CampaignRuleConsequence} instance containing the message payload to be parsed
	 * @throws CampaignMessageRequiredFieldMissingException if any of the required fields are missing from {@code consequence}
	 */
	@SuppressWarnings("unchecked")
	private void parseFullScreenMessagePayload(final CampaignRuleConsequence consequence)
	throws CampaignMessageRequiredFieldMissingException {

		if (consequence == null) {
			throw new CampaignMessageRequiredFieldMissingException("Message consequence is null.");
		}

		assetsPath = consequence.getAssetsPath();

		if (StringUtils.isNullOrEmpty(assetsPath)) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "parseFullScreenMessagePayload -  Unable to create fullscreen message, provided assets path is missing/empty.");
			throw new CampaignMessageRequiredFieldMissingException("Messages - Unable to create fullscreen message, assetPath is missing/empty.");
		}

		final Map<String, Variant> detailDictionary = consequence.getDetail();

		if (detailDictionary == null || detailDictionary.isEmpty()) {
			throw new CampaignMessageRequiredFieldMissingException("Unable to create fullscreen message, message detail is missing or not an object.");
		}

		// html is required
		html = Variant.optVariantFromMap(detailDictionary,
										 CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML).optString(null);

		if (StringUtils.isNullOrEmpty(html)) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "parseFullScreenMessagePayload -  Unable to create fullscreen message, html is missing/empty.");
			throw new CampaignMessageRequiredFieldMissingException("Messages - Unable to create fullscreen message, html is missing/empty.");
		}

		// remote assets are optional
		final VariantSerializer<List<String>> assetsSerializer = new TypedListVariantSerializer<String>
		(new StringVariantSerializer());
		final List<List<String>> assetsArray = Variant.optVariantFromMap(detailDictionary,
											   CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS).optTypedList(null,
													   assetsSerializer);

		if (assetsArray != null && !assetsArray.isEmpty()) {
			assets = new ArrayList<List<String>>();

			for (final List<String> currentAssetArray : assetsArray) {
				extractAsset(currentAssetArray);
			}
		} else {
			Log.trace(CampaignConstants.LOG_TAG,
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
			Log.warning(CampaignConstants.LOG_TAG, "There are no assets to extract.");
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
	 * Creates and shows a new {@code UIService.UIFullScreenMessage} object and registers a {@code FullScreenMessageUiListener}
	 * instance with the {@code UIService} to receive message interaction events.
	 * <p>
	 * This method reads the {@link #htmlContent} from the cached html at {@link #assetsPath} and generates the expanded html by
	 * replacing assets URLs with cached references, before calling the method on the {@code UIService} to display the message.
	 *
	 * @see #getCachedResourcesMapAndUpdateHtml()
	 * @see UIService#createFullscreenMessage(String, UIService.UIFullScreenListener)
	 */
	@Override
	void showMessage() {
		Log.debug(CampaignConstants.LOG_TAG, "showMessage -  Attempting to show fullscreen message with ID %s", messageId);

		if (parentModulePlatformServices == null) {
			Log.warning(CampaignConstants.LOG_TAG,
						"Platform Service is unavailable.  Unable to show fullscreen message with ID (%s)", messageId);
			return;
		}

		final UIService uiService = parentModulePlatformServices.getUIService();

		if (uiService == null) {
			Log.warning(CampaignConstants.LOG_TAG,
						"UI Service is unavailable.  Unable to show fullscreen message with ID (%s)",
						messageId);
			return;
		}

		htmlContent = readHtmlFromFile(new File(assetsPath + File.separator + html));

		if (StringUtils.isNullOrEmpty(htmlContent)) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "showMessage -  No html content in file (%s). File is missing or invalid!", html);
			return;
		}

		final Map<String, String> cachedResourcesMap = getCachedResourcesMapAndUpdateHtml();

		final FullScreenMessageUiListener fullScreenMessageUiListener = new FullScreenMessageUiListener();
		final UIFullScreenMessage uiFullScreenMessage = uiService.createFullscreenMessage(htmlContent,
				fullScreenMessageUiListener);


		if (uiFullScreenMessage != null) {

			uiFullScreenMessage.setLocalAssetsMap(cachedResourcesMap);
			uiFullScreenMessage.show();
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
				htmlString = StringUtils.streamToString(htmlInputStream);
			} catch (IOException ex) {
				Log.debug(CampaignConstants.LOG_TAG, "readHtmlFromFile -  Could not read the html file! (%s)", ex);
			} finally {
				try {
					if (htmlInputStream != null) {
						htmlInputStream.close();
					}
				} catch (Exception e) {
					Log.trace(CampaignConstants.LOG_TAG, "readHtmlFromFile -  Failed to close stream for %s", htmlFile);
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
	 * This function uses {@link CacheManager} to find cached remote file. if a cached file is present then add it to the {@code Map<String, String>} that will be returned.
	 * Replace the remote URL's in {@link #htmlContent} that are not cached with fallback image (if present).
	 * </p>
	 * This functions returns empty map in following cases.
	 * <ul>
	 * <li>Asset List is empty.</li>
	 * <li>{@link CacheManager} in null.</li>
	 * </ul>
	 *
	 * @return {@code Map<String,String>}
	 */
	private Map<String, String> getCachedResourcesMapAndUpdateHtml() {
		// early bail if we don't have assets
		if (assets == null || assets.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, "generateExpandedHtml -  No cached assets found, cannot expand URLs in the HTML.");
			return Collections.emptyMap();
		}

		CacheManager cacheManager = getCacheManager();

		if (cacheManager == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getLocalResourcesMapping -  No cache manager found, cannot generate local resource mapping.");
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
				final File assetValueFile = cacheManager.getFileForCachedURL(currentAsset,
											CampaignConstants.MESSAGE_CACHE_DIR + File.separator + messageId, false);

				if (assetValueFile != null) {
					assetValue = assetValueFile.getPath();
					break;
				}

				currentAssetNumber++;
			}

			// if assetValue is still null, none of our urls have been cached, so we check for a bundled asset
			if (assetValue == null) {
				assetValue = currentAssetArray.get(currentAssetArrayCount - 1);
				boolean isLocalImage = !StringUtils.stringIsUrl(assetValue);

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
	 * Returns an instance of {@link CacheManager}.
	 * <p>
	 *  Return null if {@link MissingPlatformServicesException} is thrown by the {@code CacheManager} constructor.
	 * </p>
	 *
	 * @return instance of {@code CacheManager}
	 */
	CacheManager getCacheManager() {
		try {
			return new CacheManager(parentModulePlatformServices.getSystemInfoService());
		} catch (MissingPlatformServicesException e) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "getCacheManager -  No cache manager found.", e);
		}

		return null;
	}

	/**
	 * Attempts to prefetch and cache all {@code assets} for this {@code FullScreenMessage}.
	 * <p>
	 * If there are no {@link #assets} for this instance, method does nothing..
	 */
	@Override
	protected void downloadAssets() {
		if (assets == null || assets.isEmpty()) {
			Log.trace(CampaignConstants.LOG_TAG, "downloadAssets -  No assets to download for message %s", messageId);
			return;
		}

		for (final List<String> currentAssetArray : assets) {
			if (currentAssetArray.isEmpty()) {
				continue;
			}

			final CampaignMessageAssetsDownloader assetsDownloader = new CampaignMessageAssetsDownloader(
				parentModulePlatformServices,
				currentAssetArray, messageId);
			assetsDownloader.downloadAssetCollection();
		}
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
			Log.debug(CampaignConstants.LOG_TAG,
					  "processMessageInteraction -  Cannot process message interaction, input query is null or empty.");
			return;
		}

		if (query.containsKey(CampaignConstants.MESSAGE_DATA_TAG_ID)) {
			final String id = query.get(CampaignConstants.MESSAGE_DATA_TAG_ID);
			final String[] strTokens = id.split(CampaignConstants.MESSAGE_DATA_TAG_ID_DELIMITER);

			if (strTokens.length != CampaignConstants.MESSAGE_DATA_ID_TOKENS_LEN) {
				Log.debug(CampaignConstants.LOG_TAG,
						  "processMessageInteraction -  Cannot process message interaction, input query contains insufficient id tokens.");
				return;
			}

			int tagId = 0;

			try {
				tagId = Integer.parseInt(strTokens[2]);
			} catch (NumberFormatException e) {
				Log.debug(CampaignConstants.LOG_TAG,
						  "processMessageInteraction -  Cannot parse tag Id from the id field in given query (%s).", e);
			}

			switch (tagId) {
				case CampaignConstants.MESSAGE_DATA_TAG_ID_BUTTON_1: // adbinapp://confirm/?id=h11901a,86f10d,3
				case CampaignConstants.MESSAGE_DATA_TAG_ID_BUTTON_2: // adbinapp://confirm/?id=h11901a,86f10d,4
				case CampaignConstants.MESSAGE_DATA_TAG_ID_BUTTON_X: // adbinapp://cancel?id=h11901a,86f10d,5
					clickedWithData(query);
					viewed(); // Temporary fix for AMSDK-7633. No viewed event should be dispatched on confirm.
					break;

				default:
					Log.debug(CampaignConstants.LOG_TAG,
							  "processMessageInteraction -  Unsupported tag Id found in the id field in the given query (%s).", tagId);
					break;

			}
		}
	}
}
