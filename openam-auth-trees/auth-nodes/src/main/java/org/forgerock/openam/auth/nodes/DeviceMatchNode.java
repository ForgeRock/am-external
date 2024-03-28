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
 * Copyright 2020-2023 ForgeRock AS.
 */


package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE_NAME;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.script.Bindings;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.audit.validation.PositiveIntegerValidator;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript;
import org.forgerock.openam.auth.nodes.script.DeviceMatchNodeBindings;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptBindings;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.config.consumer.ScriptContext;
import org.forgerock.openam.scripting.persistence.config.defaults.GlobalScript;
import org.forgerock.openam.utils.Time;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.idm.AMIdentity;

/**
 * The device match node provides device fingerprinting functionality for risk-based authentication.
 * The {@link DeviceProfileCollectorNode} collects the unique characteristics of a remote user's device.
 * {@link DeviceMatchNode} compares them to characteristics on a saved device profile,
 * and computes any variances between the collected characteristics to those stored on the saved device profile.
 */
@Node.Metadata(outcomeProvider = DeviceMatchNode.OutcomeProvider.class,
        configClass = DeviceMatchNode.Config.class,
        tags = {"contextual"})
public class DeviceMatchNode extends AbstractDecisionNode implements DeviceProfile {

    private static final String BUNDLE = DeviceMatchNode.class.getName();
    private static final String UNKNOWN_DEVICE_OUTCOME_ID = "unknownDevice";
    private static final String OUTCOME_IDENTIFIER = "outcome";
    private final Logger logger = LoggerFactory.getLogger(DeviceMatchNode.class);
    private final CoreWrapper coreWrapper;
    private final LegacyIdentityService identityService;
    private final Config config;
    private final Realm realm;
    private final DeviceProfilesDao deviceProfilesDao;
    private final ScriptEvaluator scriptEvaluator;

    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of
     * other classes from the plugin.
     *
     * @param deviceProfilesDao      An DeviceProfilesDao Instance
     * @param scriptEvaluatorFactory A ScriptEvaluatorFactory Instance
     * @param coreWrapper            A CoreWrapper Instance
     * @param identityService        An IdentityService Instance
     * @param config                 Node Configuration
     * @param realm                  The Realm
     */
    @Inject
    public DeviceMatchNode(DeviceProfilesDao deviceProfilesDao,
            ScriptEvaluatorFactory scriptEvaluatorFactory,
            CoreWrapper coreWrapper, LegacyIdentityService identityService, @Assisted Config config,
            @Assisted Realm realm) {
        this.deviceProfilesDao = deviceProfilesDao;
        this.scriptEvaluator = scriptEvaluatorFactory.create(AUTHENTICATION_TREE_DECISION_NODE);
        this.coreWrapper = coreWrapper;
        this.identityService = identityService;
        this.config = config;
        this.realm = realm;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        JsonValue metadata = getAttribute(context, METADATA_ATTRIBUTE_NAME);
        String identifier = getAttribute(context, IDENTIFIER_ATTRIBUTE_NAME).asString();

        if (config.useScript()) {
            return execute(context);
        }

        try {
            AMIdentity identity = getUserIdentity(context.universalId, context.getStateFor(this), coreWrapper,
                    identityService);
            List<JsonValue> devices = deviceProfilesDao
                    .getDeviceProfiles(identity.getName(), realm.asPath());
            Optional<JsonValue> result = devices.stream()
                    //Find matching device with same identifier and has profile
                    .filter(s -> {
                        if (identifier
                                .equals(s.get(IDENTIFIER_ATTRIBUTE_NAME).asString())) {
                            return s.isDefined(METADATA_ATTRIBUTE_NAME);
                        }
                        return false;
                    })
                    .findFirst();

            if (result.isPresent()) {
                logger.debug("Found device with stored metadata");
                JsonValue device = result.get();
                if (isExpired(device)) {
                    logger.debug("Stored device profile is expired");
                    return goTo(false).build();
                }
                JsonValue storedMetadata = device.get(METADATA_ATTRIBUTE_NAME);
                device.put(LAST_SELECTED_DATE, Time.currentTimeMillis());
                deviceProfilesDao.saveDeviceProfiles(identity.getName(), realm.asPath(), devices);
                if (storedMetadata.diff(metadata).size() <= config.acceptableVariance()) {
                    logger.debug("Device matches with last stored metadata");
                    return goTo(true).build();
                } else {
                    logger.debug("Device not match with last stored metadata");
                    return goTo(false).build();
                }
            } else {
                logger.debug("Device metadata not found");
                return Action.goTo(UNKNOWN_DEVICE_OUTCOME_ID).build();
            }
        } catch (Exception e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Check device expiration.
     *
     * @param device The stored device.
     * @return true is device is expired.
     */
    private boolean isExpired(JsonValue device) {
        return (new Date(device.get(LAST_SELECTED_DATE).asLong())).toInstant()
                .plus(config.expiration(), ChronoUnit.DAYS)
                .isBefore(new Date(Time.currentTimeMillis()).toInstant());
    }

    private Action execute(TreeContext context) throws NodeProcessException {
        try {
            Script script = config.script();
            ScriptBindings scriptBindings = DeviceMatchNodeBindings.builder()
                    .withNodeState(context.getStateFor(this))
                    .withCallbacks(context.getAllCallbacks())
                    .withDeviceProfilesDao(deviceProfilesDao)
                    .withSharedState(context.sharedState)
                    .withTransientState(context.transientState)
                    .build();
            Bindings binding = scriptEvaluator.evaluateScript(script, scriptBindings, realm).getBindings();
            logger.debug("script {} \n binding {}", script, binding);

            Object rawResult = binding.get(OUTCOME_IDENTIFIER);
            if (!(rawResult instanceof String)) {
                logger.warn("script outcome error");
                throw new NodeProcessException("Script must set '" + OUTCOME_IDENTIFIER + "' to a string.");
            }
            String outcome = (String) rawResult;
            return Action.goTo(outcome).build();

        } catch (javax.script.ScriptException e) {
            logger.warn("error evaluating the script", e);
            throw new NodeProcessException(e);
        }
    }

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Acceptable number of different device attribute.
         *
         * @return acceptable variance
         */
        @Attribute(order = 100, requiredValue = true)
        default int acceptableVariance() {
            return 0;
        }

        /**
         * Device Profile expiration time (in days).
         *
         * @return device expiration time
         */
        @Attribute(order = 200, requiredValue = true, validators = {PositiveIntegerValidator.class})
        default int expiration() {
            return 30;
        }

        /**
         * Use Authentication Tree Decision Node Script to do the match, {@link #acceptableVariance()} and
         * {@link #expiration()} will be ignored if selected.
         *
         * @return true to use Decision Node script
         */
        @Attribute(order = 300, requiredValue = true)
        default boolean useScript() {
            return false;
        }

        /**
         * The script configuration.
         *
         * @return The script configuration.
         */
        @Attribute(order = 400)
        @ScriptContext(AUTHENTICATION_TREE_DECISION_NODE_NAME)
        default Script script() {
            GlobalScript scriptDetails = AuthNodesGlobalScript.DECISION_NODE_SCRIPT;
            try {
                return Script.builder()
                        .setId(scriptDetails.getId())
                        .setName(scriptDetails.getDisplayName())
                        .setScript("")
                        .setLanguage(ScriptingLanguage.JAVASCRIPT)
                        .setContext(scriptDetails.getContext())
                        .build();
            } catch (ScriptException e) {
                return null;
            }
        }

    }

    /**
     * Provides the authentication node's set of outcomes.
     */
    public static class OutcomeProvider implements
            org.forgerock.openam.auth.node.api.StaticOutcomeProvider {

        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    DeviceLocationMatchNode.OutcomeProvider.class.getClassLoader());

            return ImmutableList.of(
                    new Outcome(TRUE_OUTCOME_ID, bundle.getString(TRUE_OUTCOME_ID)),
                    new Outcome(FALSE_OUTCOME_ID, bundle.getString(FALSE_OUTCOME_ID)),
                    new Outcome(UNKNOWN_DEVICE_OUTCOME_ID, bundle.getString(UNKNOWN_DEVICE_OUTCOME_ID)));
        }
    }
}
