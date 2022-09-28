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
import java.util.HashMap;

/**
 * Provides implementation logic for handling events related to native Alert messages.
 * <p>
 * The actual UI implementation happens in the platform services.
 */
class AlertMessage extends CampaignMessage {

	// ================================================================================
	// protected class members
	// ================================================================================
	String title;
	String content;
	String url;
	String confirmButtonText;
	String cancelButtonText;

	/**
	 * Serves as the delegate for handling {@code AlertMessage} events triggered by the {@code UIService}.
	 */
	class UIAlertMessageUIListener implements UIService.UIAlertListener {
		/**
		* Invoked on positive button clicks.
		* <p>
		* Checks to see if the alert message's url is populated and if so adds the url to the context data map
		* then calls {@link #clickedWithData(Map)}.
		* If a url is not present then call the {@link #clickedThrough()} method implemented in the parent class {@link CampaignMessage}.
		*/
		@Override
		public void onPositiveResponse() {

			// Temporary fix for AMSDK-7710. No viewed event should be dispatched on confirm.
			viewed();

			if (url != null && !url.isEmpty()) {
				Map<String, String> contextData = new HashMap<String, String>();
				contextData.put(CampaignConstants.CAMPAIGN_INTERACTION_URL, url);
				clickedWithData(contextData);
			} else {
				clickedThrough();
			}
		}

		/**
		 * Invoked on negative button clicks.
		 * <p>
		 * Calls the {@link #viewed()} method implemented in the parent class {@link CampaignMessage}.
		 */
		@Override
		public void onNegativeResponse() {
			viewed();
		}

		/**
		 * Invoked when the alert is displayed.
		 * <p>
		 * Calls the {@link #triggered()} method implemented in the parent class {@link CampaignMessage}.
		 */
		@Override
		public void onShow() {
			triggered();
		}

		/**
		 * Invoked when the alert is dismissed.
		 * <p>
		 * Calls the {@link #viewed()} method implemented in the parent class {@link CampaignMessage}.
		 */
		@Override
		public void onDismiss() {
			viewed();
		}
	}

	/**
	 * Constructor.
	 *
	 * @param extension parent {@link CampaignExtension} instance
	 * @param platformServices {@link PlatformServices} instance used to access JSON implementation
	 * @param consequence {@code CampaignRuleConsequence} instance containing {@code CampaignMessage} defining payload
	 * @throws CampaignMessageRequiredFieldMissingException if {@code consequence} is null or if any required field for an
	 * {@link AlertMessage} is null or empty
	 * @throws MissingPlatformServicesException if {@code platformServices} is null
	 */
	AlertMessage(final CampaignExtension extension, final PlatformServices platformServices,
				 final CampaignRuleConsequence consequence)
	throws CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		super(extension, platformServices, consequence);
		parseAlertMessagePayload(consequence);
	}

	/**
	 * Parses a {@code CampaignRuleConsequence} instance defining message payload for an {@code AlertMessage} object.
	 * <p>
	 * Required fields:
	 * <ul>
	 *     <li>{@value CampaignConstants.EventDataKeys.RuleEngine#MESSAGE_CONSEQUENCE_DETAIL_KEY_TITLE} - {@link String} containing the title for this message</li>
	 *     <li>{@value CampaignConstants.EventDataKeys.RuleEngine#MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT} - {@code String} containing the content of the message</li>
	 *     <li>{@value CampaignConstants.EventDataKeys.RuleEngine#MESSAGE_CONSEQUENCE_DETAIL_KEY_CANCEL} - {@code String} containing the text of the cancel or negative action button on this message</li>
	 * </ul>
	 * Optional fields:
	 * <ul>
	 *     <li>{@value CampaignConstants.EventDataKeys.RuleEngine#MESSAGE_CONSEQUENCE_DETAIL_KEY_CONFIRM} - {@code String} containing the text of the confirm or positive action button on this message</li>
	 *     <li>{@value CampaignConstants.EventDataKeys.RuleEngine#MESSAGE_CONSEQUENCE_DETAIL_KEY_URL} - {@code String} containing a URL destination to be shown on positive click-through</li>
	 * </ul>
	 *
	 * @param consequence {@code CampaignRuleConsequence} instance containing the message payload to be parsed
	 * @throws CampaignMessageRequiredFieldMissingException if any of the required fields are missing from {@code consequence}
	 */
	@SuppressWarnings("unchecked")
	private void parseAlertMessagePayload(final CampaignRuleConsequence consequence) throws
		CampaignMessageRequiredFieldMissingException {
		Log.trace(CampaignConstants.LOG_TAG,
				  "parseAlertMessagePayload - Parsing rule consequence to show alert message with messageid %s", messageId);

		if (consequence == null) {
			throw new CampaignMessageRequiredFieldMissingException("Message consequence is null.");
		}

		final Map<String, Variant> detailDictionary = consequence.getDetail();

		if (detailDictionary == null || detailDictionary.isEmpty()) {
			throw new CampaignMessageRequiredFieldMissingException("Message detail is missing.");
		}

		// title is required
		title = Variant.optVariantFromMap(detailDictionary,
										  CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TITLE).optString(null);

		if (StringUtils.isNullOrEmpty(title)) {
			throw new CampaignMessageRequiredFieldMissingException("Alert Message title is empty.");
		}

		// content is required
		content = Variant.optVariantFromMap(detailDictionary,
											CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT).optString(null);

		if (StringUtils.isNullOrEmpty(content)) {
			throw new CampaignMessageRequiredFieldMissingException("Alert Message content is empty.");
		}

		// cancel button text is required
		cancelButtonText = Variant.optVariantFromMap(detailDictionary,
						   CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_CANCEL).optString(null);

		if (StringUtils.isNullOrEmpty(cancelButtonText)) {
			throw new CampaignMessageRequiredFieldMissingException("Alert Message cancel button text is empty.");
		}

		// confirm button text is optional
		confirmButtonText = Variant.optVariantFromMap(detailDictionary,
							CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_CONFIRM).optString(null);

		if (StringUtils.isNullOrEmpty(confirmButtonText)) {
			Log.trace(CampaignConstants.LOG_TAG,
					  "Tried to read \"confirm\" for Alert message but found none. This is not a required field.");
		}

		// url is optional
		url = Variant.optVariantFromMap(detailDictionary,
										CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_URL).optString(null);

		if (StringUtils.isNullOrEmpty(url)) {
			Log.trace(CampaignConstants.LOG_TAG,
					  "Tried to read url for Alert message but found none. This is not a required field.");
		}
	}

	/**
	 * This method requests {@code UIService} to show this {@code AlertMessage}.
	 * <p>
	 * This method registers a {@link UIAlertMessageUIListener} instance with the {@code UIService} to handle message
	 * interaction events.
	 *
	 * @see CampaignMessage#showMessage()
	 * @see UIService#showAlert(String, String, String, String, UIService.UIAlertListener)
	 */
	@Override
	void showMessage() {
		Log.debug(CampaignConstants.LOG_TAG, "Attempting to show Alert message with ID %s ", messageId);

		if (parentModulePlatformServices != null) {
			final UIService uiService = parentModulePlatformServices.getUIService();

			if (uiService != null) {
				final UIAlertMessageUIListener uiListener = new UIAlertMessageUIListener();
				uiService.showAlert(title, content, confirmButtonText, cancelButtonText, uiListener);
			}
		}
	}

	/**
	 * Determines whether this class has downloadable assets.
	 * <p>
	 * This method returns false as no assets need to be downloaded for {@code AlertMessage} class.
	 *
	 * @return true if this class has downloadable assets
	 */
	@Override
	boolean shouldDownloadAssets() {
		return false;
	}

	/**
	 * Invokes method in parent {@code CampaignMessage} class so that the {@code url} can be shown by the {@code UIService}.
	 *
	 * @see #openUrl(String)
	 */
	void showUrl() {
		super.openUrl(url);
	}
}
