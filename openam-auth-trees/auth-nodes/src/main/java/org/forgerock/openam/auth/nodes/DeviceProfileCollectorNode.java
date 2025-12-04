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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.send;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.DecimalValidator;
import org.forgerock.openam.authentication.callbacks.DeviceProfileCallback;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node which collects device profile from the device via a {@link DeviceProfileCallback} callback.
 *
 * <p>Places the result in the shared state as {@link DeviceProfile#DEVICE_PROFILE_CONTEXT_NAME}
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = DeviceProfileCollectorNode.Config.class,
        tags = {"contextual"})
public class DeviceProfileCollectorNode extends SingleOutcomeNode implements DeviceProfile {

    private final Logger logger = LoggerFactory.getLogger(DeviceProfileCollectorNode.class);
    private final Config config;
    private final LocaleSelector localeSelector;

    //audit attribute
    private JsonValue profile;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Specify the maximum accepted size for a device profile in kilobytes.
         *
         * @return Maximum Profile Size (KB)
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class, DecimalValidator.class})
        default String maximumSize() {
            return "3";
        }

        /**
         * Indicator to instruct client to collect device metadata.
         *
         * @return True to request for device metadata, otherwise false
         */
        @Attribute(order = 200)
        default boolean deviceMetadata() {
            return true;
        }

        /**
         * Indicator to instruct client to collect device location.
         *
         * @return True to request for device location, otherwise false
         */
        @Attribute(order = 300)
        default boolean deviceLocation() {
            return false;
        }

        /**
         * The message display to the user while capturing device information.
         *
         * @return the message
         */
        @Attribute(order = 400)
        default Map<Locale, String> message() {
            return Collections.emptyMap();
        }

    }

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of
     * other classes from the plugin.
     *
     * @param config         the service config
     * @param localeSelector a LocaleSelector for choosing the correct message to display
     */
    @Inject
    public DeviceProfileCollectorNode(@Assisted Config config, LocaleSelector localeSelector) {
        this.config = config;
        this.localeSelector = localeSelector;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        Optional<DeviceProfileCallback> opt = context.getCallback(DeviceProfileCallback.class);
        if (opt.isPresent() && !StringUtils.isEmpty(opt.get().getValue())) {
            profile = JsonValueBuilder.toJsonValue(opt.get().getValue());
            logger.debug("DeviceProfile collected, saving to TreeContext");
            validate(opt.get().getValue(), profile);
            return save(context, opt.get().getValue());
        } else {
            logger.debug("Send DeviceProfileCallback to client.");
            return getCallback(context);
        }
    }

    /**
     * Validate the collected data.
     *
     * @param raw       A json string which contains device profiles
     * @param jsonValue A JsonValue Object which contains device profiles
     * @throws NodeProcessException Any Validation Error
     */
    private void validate(String raw, JsonValue jsonValue) throws NodeProcessException {
        if (raw.getBytes().length > (new BigDecimal(config.maximumSize()).doubleValue() * 1024)) {
            throw new NodeProcessException("Captured data exceed maximum accepted size");
        }
        if (!jsonValue.isDefined(IDENTIFIER_ATTRIBUTE_NAME)) {
            throw new NodeProcessException("Device Identifier is not captured");
        }
    }


    /**
     * Persist the device profile to {@link TreeContext#sharedState}.
     *
     * @param context The TreeContext
     * @param value   A json string which contains device profiles
     * @return Action which updated with {@link TreeContext#sharedState}
     */
    private Action save(TreeContext context, String value) {

        profile = JsonValueBuilder.toJsonValue(value);

        JsonValue newSharedState = context.sharedState.copy();

        newSharedState.put(DEVICE_PROFILE_CONTEXT_NAME, profile);

        return goToNext().replaceSharedState(newSharedState).build();
    }

    private Action getCallback(TreeContext context) {
        return send(
                new DeviceProfileCallback(config.deviceMetadata(),
                        config.deviceLocation(),
                        getLocalisedMessage(context, config.message()))).build();
    }

    private String getLocalisedMessage(TreeContext context, Map<Locale, String> localisations) {
        PreferredLocales preferredLocales = context.request.locales;
        Locale bestLocale = localeSelector.getBestLocale(preferredLocales, localisations.keySet());

        if (bestLocale != null) {
            return localisations.get(bestLocale);
        } else if (localisations.size() > 0) {
            return localisations.get(localisations.keySet().iterator().next());
        }
        return null;
    }

    @Override
    public JsonValue getAuditEntryDetail() {
        return json(object(
                field(DEVICE_PROFILE_CONTEXT_NAME, profile)));
    }


}
