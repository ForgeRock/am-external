/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2020-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.Action.send;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

/**
 * Node which displays links to download the 2FA authenticator apps from Apple and Google stores.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = GetAuthenticatorAppNode.Config.class,
        tags = {"mfa", "multi-factor authentication"})
public class GetAuthenticatorAppNode extends SingleOutcomeNode {

    /** Default message key for get authenticator on app stores. */
    static final String MESSAGE_KEY = "default.message";
    /** The default label for the Continue button. */
    static final String CONTINUE_LABEL_KEY = "default.continueLabel";
    /** The link for the PingID Authenticator app on Apple App Store. */
    static final String APPLE_APP_LINK = "https://apps.apple.com/app/pingid/id891247102";
    /** The link for the PingID Authenticator app on Google Play Store. */
    static final String GOOGLE_APP_LINK = "https://play.google.com/store/apps/details?id=prod.com.pingidentity.pingid";
    /** The Apple app store name. */
    private static final String APPLE_APP_STORE = "appleStore";
    /** The Google app store name. */
    private static final String GOOGLE_PLAY_STORE = "googlePlay";
    /** The callback ID which will render the JavaScript with App Links. */
    private static final String CALLBACK_ELEMENT_ID = "callback_0";
    /** The key to replace the Apple Store URL. */
    static final String APPLE_APP_LINK_REGEX = "\\{\\{appleLink\\}\\}";
    /** The key to replace the Google Play Store URL. */
    static final String GOOGLE_APP_LINK_REGEX = "\\{\\{googleLink\\}\\}";
    /** The key to replace the Apple Store URL. */
    static final String APPLE_APP_LABEL_REGEX = "\\{\\{appleLabel\\}\\}";
    /** The key to replace the Google Play Store URL. */
    static final String GOOGLE_APP_LABEL_REGEX = "\\{\\{googleLabel\\}\\}";

    private static final String BUNDLE = GetAuthenticatorAppNode.class.getName();
    private static final Logger LOGGER = LoggerFactory.getLogger(GetAuthenticatorAppNode.class);

    private final GetAuthenticatorAppNode.Config config;
    private final LocaleSelector localeSelector;

    /**
     * Create the node.
     *
     * @param config The service config.
     * @param localeSelector The locale selector support class.
     */
    @Inject
    public GetAuthenticatorAppNode(@Assisted GetAuthenticatorAppNode.Config config,
            LocaleSelector localeSelector) {
        this.config = config;
        this.localeSelector = localeSelector;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        LOGGER.debug("GetAuthenticatorAppNode started");
        if (context.getCallback(ConfirmationCallback.class).isPresent()) {
            LOGGER.debug("Get Authenticator app message has been displayed");
            return goToNext().build();
        }

        String script = getAppLinksJavascript(context);
        String[] continueLabel = {getLocalisedMessage(context, config.continueLabel(), CONTINUE_LABEL_KEY)};
        List<Callback> callbacks = ImmutableList.of(
                new ScriptTextOutputCallback(script),
                new ConfirmationCallback(ConfirmationCallback.INFORMATION, continueLabel, 0)
        );

        LOGGER.debug("Displaying Get Authenticator app message");
        return send(callbacks).build();
    }

    /**
     * Create a message replacing the tags with web links for the Authenticator apps.
     *
     * @param context The context of the tree authentication.
     * @return The script message with web links to the apps.
     */
    private String getAppLinksJavascript(TreeContext context) {
        String message = getLocalisedMessage(context, config.message(), MESSAGE_KEY);

        String appleLink = StringUtils.isBlank(config.appleLink()) ? APPLE_APP_LINK : config.appleLink();
        String googleLink = StringUtils.isBlank(config.appleLink()) ? GOOGLE_APP_LINK : config.googleLink();

        if (message.contains("{appleLabel}") || message.contains("{googleLabel}")) {
            message = message.replaceAll(APPLE_APP_LABEL_REGEX, getLocalizedResource(context, APPLE_APP_STORE));
            message = message.replaceAll(GOOGLE_APP_LABEL_REGEX, getLocalizedResource(context, GOOGLE_PLAY_STORE));
            message = message.replaceAll(APPLE_APP_LINK_REGEX, appleLink);
            message = message.replaceAll(GOOGLE_APP_LINK_REGEX, googleLink);
        } else {
            message = message.replaceAll(APPLE_APP_LINK_REGEX,
                    "<a target='_blank' href='" + appleLink + "'>"
                            + getLocalizedResource(context, APPLE_APP_STORE) + "</a>");
            message = message.replaceAll(GOOGLE_APP_LINK_REGEX,
                    "<a target='_blank' href='" + googleLink + "'>"
                            + getLocalizedResource(context, GOOGLE_PLAY_STORE) + "</a>");
        }

        String script = "document.getElementById(\"" + GetAuthenticatorAppNode.CALLBACK_ELEMENT_ID
                + "\").innerHTML=\"<center>" + message + "</center>\"";

        return script;
    }

    /**
     * Get the localized message.
     *
     * @param context The context of the tree authentication.
     * @param localisations The localization map
     * @param defaultMessageKey The key for the default message in the resource bundle
     * @return The localized string
     */
    private String getLocalisedMessage(TreeContext context, Map<Locale, String> localisations,
            String defaultMessageKey) {
        PreferredLocales preferredLocales = context.request.locales;
        Locale bestLocale = localeSelector.getBestLocale(preferredLocales, localisations.keySet());

        if (bestLocale != null) {
            return localisations.get(bestLocale);
        } else if (localisations.size() > 0) {
            return localisations.get(localisations.keySet().iterator().next());
        }

        ResourceBundle bundle = preferredLocales.getBundleInPreferredLocale(GetAuthenticatorAppNode.BUNDLE,
                GetAuthenticatorAppNode.class.getClassLoader());
        return bundle.getString(defaultMessageKey);
    }

    /**
     * Get the localized resource.
     *
     * @param context The context of the tree authentication.
     * @param defaultKey The key for the default message in the resource bundle
     * @return The localized string
     */
    private String getLocalizedResource(TreeContext context, String defaultKey) {
        PreferredLocales preferredLocales = context.request.locales;
        ResourceBundle bundle = preferredLocales.getBundleInPreferredLocale(GetAuthenticatorAppNode.BUNDLE,
                GetAuthenticatorAppNode.class.getClassLoader());
        return bundle.getString(defaultKey);
    }

    /**
     * The node configuration.
     */
    public interface Config {

        /**
         * The message to displayed to user.
         *
         * @return the message
         */
        @Attribute(order = 10)
        default Map<Locale, String> message() {
            return Collections.emptyMap();
        }

        /**
         * Configurable label for CONTINUE button.
         *
         * @return collection of localized CONTINUE labels
         */
        @Attribute(order = 20)
        default Map<Locale, String> continueLabel() {
            return Collections.emptyMap();
        }

        /**
         * Specifies the link for the authenticator app on Apple App Store.
         *
         * @return link for the app on Apple Store.
         */
        @Attribute(order = 30)
        default String appleLink() {
            return APPLE_APP_LINK;
        }

        /**
         * Specifies the link for the authenticator app on Google Play Store.
         *
         * @return link for the app on Google Play Store.
         */
        @Attribute(order = 40)
        default String googleLink() {
            return GOOGLE_APP_LINK;
        }
    }
}
