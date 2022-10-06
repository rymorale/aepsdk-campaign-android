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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code CampaignMessage} class serves as the base class for any implementation of an in-app message type.
 */
abstract class CampaignMessage {

	private static final Map<String, Class> messageTypeDictionary = new HashMap<String, Class>();
	static {
		messageTypeDictionary.put(CampaignConstants.MESSAGE_TEMPLATE_FULLSCREEN, FullScreenMessage.class);
		messageTypeDictionary.put(CampaignConstants.MESSAGE_TEMPLATE_ALERT, AlertMessage.class);
		messageTypeDictionary.put(CampaignConstants.MESSAGE_TEMPLATE_LOCAL_NOTIFICATION, LocalNotificationMessage.class);
	}

	// package-private members
	final CampaignExtension parentModule;
	final PlatformServices parentModulePlatformServices;
	protected final String messageId;

	/**
	 * Constructor.
	 * <p>
	 * Every {@link CampaignMessage} requires a {@link #messageId}, and must be of type {@value CampaignConstants#MESSAGE_CONSEQUENCE_MESSAGE_TYPE}.
	 * <p>
	 * The {@code consequence} parameter for a {@code CampaignMessage} is required to have valid values for the following fields:
	 * <ul>
	 *     <li>{@value CampaignConstants.EventDataKeys.RuleEngine#MESSAGE_CONSEQUENCE_ID} - {@link String} containing the message ID</li>
	 *     <li>{@value CampaignConstants.EventDataKeys.RuleEngine#MESSAGE_CONSEQUENCE_TYPE} - {@code String} containing the consequence type</li>
	 *     <li>{@value CampaignConstants.EventDataKeys.RuleEngine#MESSAGE_CONSEQUENCE_DETAIL} - {@code Map<String, Variant>} containing details of the Message</li>
	 * </ul>
	 *
	 * @param extension {@link CampaignExtension} instance that is the parent of this {@code CampaignMessage}
	 * @param platformServices {@link PlatformServices} reference
	 * @param consequence {@link CampaignRuleConsequence} containing a {@code CampaignMessage}-defining payload
	 * @throws CampaignMessageRequiredFieldMissingException if {@code consequence} is null or if it does not contain a valid
	 * {@code id}, {@code type}, or {@code detail}
	 * @throws MissingPlatformServicesException if {@code platformServices} is null
	 */
	protected CampaignMessage(final CampaignExtension extension, final PlatformServices platformServices,
							  final CampaignRuleConsequence consequence)
	throws CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		parentModule = extension;
		parentModulePlatformServices = platformServices;

		if (parentModulePlatformServices == null) {
			throw new MissingPlatformServicesException("Platform services cannot be null!");
		}

		if (consequence == null) {
			throw new CampaignMessageRequiredFieldMissingException("Consequence cannot be null!");
		}

		messageId = consequence.getId();

		if (StringUtils.isNullOrEmpty(messageId)) {
			Log.debug(CampaignConstants.LOG_TAG, "Invalid consequence. Required field \"id\" is null or empty.");
			throw new CampaignMessageRequiredFieldMissingException("Required field: Message \"id\" is null or empty.");
		}

		final String consequenceType = consequence.getType();

		if (!CampaignConstants.MESSAGE_CONSEQUENCE_MESSAGE_TYPE.equals(consequenceType)) {
			Log.debug(CampaignConstants.LOG_TAG, "Invalid consequence. Required field \"type\" is (%s) should be of type (iam).",
					  consequenceType);
			throw new CampaignMessageRequiredFieldMissingException("Required field: \"type\" is not equal to \"iam\".");
		}

		final Map<String, Variant> details = consequence.getDetail();

		if (details == null || details.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "Invalid consequence. Required field \"detail\" is null or empty.");
			throw new CampaignMessageRequiredFieldMissingException("Required field: \"detail\" is null or empty.");
		}
	}

	/**
	 * Static method that should be used to create an instance of any {@code CampaignMessage} subclass.
	 * <p>
	 * Verifies that the {@code consequence} parameter contains a valid {@code CampaignMessage} definition payload. If it does,
	 * this method will detect and call the proper constructor for the corresponding {@link CampaignMessage} subclass.
	 * <p>
	 * At this stage in {@code CampaignMessage} initialization, the only required JSON field is {@value CampaignConstants.EventDataKeys.RuleEngine#MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE}.
	 * If this value is missing, null, or empty, a {@link CampaignMessageRequiredFieldMissingException} will be thrown.
	 *
	 * @param extension {@link CampaignExtension} instance that is the parent of this {@code CampaignMessage}
	 * @param platformServices {@link PlatformServices} reference
	 * @param consequence {@link CampaignRuleConsequence} instance containing a {@code CampaignMessage}-defining payload
	 *
	 * @return {@code CampaignMessage} that has been initialized using its proper constructor
	 * @throws CampaignMessageRequiredFieldMissingException if {@code consequence} is null or if any required field for a
	 * {@code CampaignMessage} is null or empty
	 * @throws MissingPlatformServicesException if {@code platformServices} is null
	 */
	@SuppressWarnings("unchecked") // reflective access to the proper message constructor requires this access
	static CampaignMessage createMessageObject(final CampaignExtension extension, final PlatformServices platformServices,
			final CampaignRuleConsequence consequence)
	throws CampaignMessageRequiredFieldMissingException, MissingPlatformServicesException {
		// fast fail
		if (consequence == null) {
			Log.debug(CampaignConstants.LOG_TAG, "createMessageObject -  No message consequence found. Unable to proceed.");
			throw new CampaignMessageRequiredFieldMissingException("Message consequence is null.");
		}

		if (platformServices == null) {
			Log.debug(CampaignConstants.LOG_TAG, "createMessageObject -  Platform services is null. Unable to proceed.");
			throw new MissingPlatformServicesException("Messages. Platform services is null.");
		}

		// detail is required
		final Map<String, Variant> detailObject = consequence.getDetail();

		if (detailObject == null || detailObject.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, "createMessageObject -  No detail dictionary found. Unable to proceed.");
			throw new CampaignMessageRequiredFieldMissingException("Message detail dictionary is null or empty.");
		}

		// template is required
		final String template = Variant.optVariantFromMap(detailObject,
								CampaignConstants.EventDataKeys.RuleEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE).optString(null);

		if (StringUtils.isNullOrEmpty(template)) {
			Log.debug(CampaignConstants.LOG_TAG, "createMessageObject -  No message template found. Unable to proceed.");
			throw new CampaignMessageRequiredFieldMissingException("Required message field: Message template is null or empty.");
		}

		final Class messageClass = messageTypeDictionary.get(template);

		if (messageClass == null) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "createMessageObject -  Provided message type is not supported. Unable to proceed.");
			return null;
		}

		CampaignMessage msgObject = null;

		try {
			msgObject = (CampaignMessage)messageClass.getDeclaredConstructor(CampaignExtension.class, PlatformServices.class,
						CampaignRuleConsequence.class).newInstance(extension, platformServices, consequence);

			if (msgObject.shouldDownloadAssets()) {
				msgObject.downloadAssets();
			}

		} catch (final IllegalAccessException e) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "createMessageObject -  Caught IllegalAccessException exception while trying to instantiate Message object. \n (%s)",
					  e);
		} catch (final InstantiationException e) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "createMessageObject -  Caught InstantiationException exception while trying to instantiate Message object.\n (%s)", e);
		} catch (final NoSuchMethodException e) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "createMessageObject -  Caught NoSuchMethodException exception while trying to instantiate Message object. \n (%s)", e);
		} catch (final InvocationTargetException e) {
			Log.debug(CampaignConstants.LOG_TAG,
					  "createMessageObject -  Caught InvocationTargetException exception while trying to instantiate Message object. \n (%s)",
					  e);
		}

		return msgObject;
	}

	/**
	 * Creates an instance of the {@code CampaignMessage} subclass and invokes method on the class to handle asset downloading,
	 * if it supports remote assets.
	 *
	 * @param extension parent {@link CampaignExtension} instance for this {@code CampaignMessage}
	 * @param platformServices {@link PlatformServices} instance
	 * @param consequence {@link CampaignRuleConsequence} instance containing a {@code CampaignMessage}-defining payload
	 *
	 * @see #shouldDownloadAssets()
	 * @see #downloadAssets()
	 */
	static void downloadRemoteAssets(final CampaignExtension extension, final PlatformServices platformServices,
									 final CampaignRuleConsequence consequence) {
		try {
			CampaignMessage.createMessageObject(extension, platformServices,
												consequence); // Assets download call is there inside createMessageObject.

		} catch (final MissingPlatformServicesException ex) {
			Log.warning(CampaignConstants.LOG_TAG, "Error reading message definition: %s", ex);
		} catch (final CampaignMessageRequiredFieldMissingException ex) {
			Log.warning(CampaignConstants.LOG_TAG, "Error reading message definition: %s", ex);
		}
	}

	/**
	 * Abstract method to be overridden by child {@code CampaignMessage} class to display the message.
	 */
	abstract void showMessage();

	/**
	 * Optional abstract method invoked to let the child class handle asset downloading.
	 */
	protected void downloadAssets() { }

	/**
	 * Generates a {@code Map} with message data for a "message triggered" event and passes it to the parent
	 * {@code CampaignExtension} for dispatch.
	 *
	 * @see CampaignExtension#dispatchMessageInteraction(Map)
	 */
	protected void triggered() {
		HashMap<String, String> msgData = new HashMap<String, String>();
		msgData.put(CampaignConstants.ContextDataKeys.MESSAGE_ID, messageId);
		msgData.put(CampaignConstants.ContextDataKeys.MESSAGE_TRIGGERED, String.valueOf(1));
		callDispatchMessageInteraction(msgData);
	}

	/**
	 * Generates a {@code Map} with message data for a "message viewed" event and passes it to the parent
	 * {@code CampaignExtension} for dispatch.
	 *
	 * @see CampaignExtension#dispatchMessageInteraction(Map)
	 */
	protected void viewed() {
		HashMap<String, String> contextData = new HashMap<String, String>();
		contextData.put(CampaignConstants.ContextDataKeys.MESSAGE_ID, messageId);
		contextData.put(CampaignConstants.ContextDataKeys.MESSAGE_VIEWED, String.valueOf(1));
		callDispatchMessageInteraction(contextData);
	}

	/**
	 * Generates a {@code Map} with message data for a "message clicked" event and passes it to the parent
	 * {@code CampaignExtension} for dispatch.
	 *
	 * @see CampaignExtension#dispatchMessageInteraction(Map)
	 */
	protected void clickedThrough() {
		HashMap<String, String> contextData = new HashMap<String, String>();
		contextData.put(CampaignConstants.ContextDataKeys.MESSAGE_ID, messageId);
		contextData.put(CampaignConstants.ContextDataKeys.MESSAGE_CLICKED, String.valueOf(1));
		callDispatchMessageInteraction(contextData);
	}

	/**
	 * Generates a {@code Map} with message data for a "message clicked" event and passes it to the parent
	 * {@code CampaignExtension} for dispatch.
	 * <p>
	 * This method also adds click through URL to the data and attempts to open the URL, after decoding and
	 * expanding tokens in the URL.
	 *
	 * @param data {@code Map<String, String>} containing message interaction data
	 * @see #openUrl(String)
	 * @see CampaignExtension#dispatchMessageInteraction(Map)
	 */
	protected void clickedWithData(final Map<String, String> data)  {
		Map<String, String> messageData = new HashMap<String, String>();

		// Need to decode and token expand the click through URL before putting it on the event
		for (Map.Entry<String, String> entry : data.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue();

			if (key.equals(CampaignConstants.CAMPAIGN_INTERACTION_URL)) {
				// TODO: in V4 we were also expanding AID (from analytics), user identifier (from visitor id) and
				// lifetime value (it doesn't have any module yet); we need to check if we need to expand those as well

				String url = null;

				try {
					url = URLDecoder.decode(value, StringUtils.CHARSET_UTF_8);
				} catch (UnsupportedEncodingException e) {
					Log.warning(CampaignConstants.LOG_TAG, "Failed to decode message interaction url (%s)", e);
				}

				Map<String, String> urlTokens = new HashMap<String, String>();
				urlTokens.put(CampaignConstants.MESSAGE_TOKEN_MESSAGE_ID, messageId);

				url = expandTokens(url, urlTokens);
				openUrl(url);
				messageData.put(key, url);
			} else {
				messageData.put(key, value);
			}
		}

		messageData.put(CampaignConstants.ContextDataKeys.MESSAGE_ID, messageId);
		messageData.put(CampaignConstants.ContextDataKeys.MESSAGE_CLICKED, String.valueOf(1));

		callDispatchMessageInteraction(messageData);
	}

	/**
	 * Requests that the {@code UIService} show this {@code url}.
	 *
	 * @param url {@link String} containing url to be shown
	 */
	protected void openUrl(final String url) {
		if (StringUtils.isNullOrEmpty(url)) {
			Log.debug(CampaignConstants.LOG_TAG, "Cannot open a null or empty URL.");
			return;
		}

		if (parentModulePlatformServices != null) {
			final UIService uiService = parentModulePlatformServices.getUIService();

			if (uiService == null || !uiService.showUrl(url)) {
				Log.debug(CampaignConstants.LOG_TAG, "Could not open URL (%s)", url);
			}
		}
	}

	/**
	 * Expands provided {@code tokens} in the given {@code input} String.
	 * <p>
	 * It returns the same input {@link String} if,
	 * <ul>
	 *     <li>Input {@code String} is null or empty.</li>
	 *     <li>Provided tokens Map is null or empty.</li>
	 *     <li>No key from the tokens Map is present in the input {@code String}.</li>
	 * </ul>
	 *
	 * @param input {@code String} in which we should replace the given tokens
	 * @param tokens {@code Map<String, String>} with the tokens that need to be replaced and their values
	 * @return the updated {@code String} with the expanded tokens
	 */
	String expandTokens(final String input, final Map<String, String> tokens) {
		if (StringUtils.isNullOrEmpty(input)) {
			Log.debug(CampaignConstants.LOG_TAG, "expandTokens -  Unable to expand tokens, input string is null or empty");
			return input;
		}

		if (tokens == null || tokens.isEmpty()) {
			Log.debug(CampaignConstants.LOG_TAG, "expandTokens -  Unable to expand tokens, provided tokens Map is null or empty");
			return input;
		}

		String returnString = input;

		for (Map.Entry<String, String> entry : tokens.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue();

			if (key == null || value == null) {
				continue;
			}

			returnString = returnString.replace(entry.getKey(), entry.getValue());
		}

		return returnString;
	}

	/**
	 * Invokes method on the {@code parentModule} to dispatch the provided message interaction {@code data}.
	 *
	 * @param data {@code Map<String, String>} containing message interaction data
	 */
	protected void callDispatchMessageInteraction(final Map<String, String> data) {
		parentModule.dispatchMessageInteraction(data);
	}

	/**
	 * Invokes method on the {@code parentModule} to dispatch the provided message info.
	 *
	 * @param broadlogId {@link String} containing message broadlogId
	 * @param deliveryId {@code String} containing message deliveryId
	 * @param action {@code String} containing message action
	 */
	protected void callDispatchMessageInfo(final String broadlogId, final String deliveryId, final String action) {
		parentModule.dispatchMessageInfo(broadlogId, deliveryId, action);
	}

	/**
	 * Determines whether a {@code CampaignMessage} should attempt to download assets for caching.
	 *
	 * @return {@code boolean} indicating whether this should download assets
	 */
	abstract boolean shouldDownloadAssets();
}
