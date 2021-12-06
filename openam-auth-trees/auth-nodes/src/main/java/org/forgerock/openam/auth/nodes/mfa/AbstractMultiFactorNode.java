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
 * Copyright 2020-2021 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.mfa;

import static org.forgerock.openam.auth.nodes.helpers.AuthNodeUserIdentityHelper.getAMIdentity;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.RECOVERY_CODE_DEVICE_NAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.RECOVERY_CODE_KEY;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.authentication.callbacks.helpers.QRCallbackBuilder;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.UrlEscapers;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

/**
 * Abstract class for multi-factor authentication nodes, shares common features and components
 * across the multiple mfa-related nodes.
 */
public abstract class AbstractMultiFactorNode implements Node {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMultiFactorNode.class);

    /** How often to poll AM for a response in milliseconds. */
    public static final int REGISTER_DEVICE_POLL_INTERVAL = 5000;

    /**  Success outcome Id. */
    public static final String SUCCESS_OUTCOME_ID = "successOutcome";
    /**  Failure outcome Id. */
    public static final String FAILURE_OUTCOME_ID = "failureOutcome";
    /**  Timeout outcome Id. */
    public static final String TIMEOUT_OUTCOME_ID = "timeoutOutcome";
    /**  Recovery code outcome Id. */
    public static final String RECOVERY_CODE_OUTCOME_ID = "recoveryCodeOutcome";
    /**  Not registered outcome Id. */
    public static final String NOT_REGISTERED_OUTCOME_ID = "notRegisteredOutcome";

    private final Realm realm;
    private final CoreWrapper coreWrapper;
    private final MultiFactorNodeDelegate<?> multiFactorNodeDelegate;
    private final IdentityUtils identityUtils;

    /**
     * The constructor.
     *
     * @param realm the realm.
     * @param coreWrapper the {@code CoreWrapper} instance.
     * @param multiFactorNodeDelegate shared utilities common to second factor implementations.
     * @param identityUtils an instance of the IdentityUtils.
     */
    public AbstractMultiFactorNode(Realm realm,
            CoreWrapper coreWrapper,
            MultiFactorNodeDelegate<?> multiFactorNodeDelegate,
            IdentityUtils identityUtils) {
        this.realm = realm;
        this.coreWrapper = coreWrapper;
        this.multiFactorNodeDelegate = multiFactorNodeDelegate;
        this.identityUtils = identityUtils;
    }

    /**
     * Builds an Action for the passed Outcome and cleanup the SharedState.
     *
     * @param outcome the target outcome.
     * @param context the context of the tree authentication.
     * @return the next action to perform.
     */
    protected Action buildAction(String outcome, TreeContext context) {
        Action.ActionBuilder builder = Action.goTo(outcome);
        return cleanupSharedState(context, builder).build();
    }

    /**
     * After a successful registration, the Recovery Code Display node displays the recovery codes.
     *
     * @param context the context of the tree authentication.
     * @param deviceName the device name.
     * @param recoveryCodes the list of recovery codes.
     * @return the next action to perform.
     */
    protected Action buildActionWithRecoveryCodes(TreeContext context, String deviceName,
            List<String> recoveryCodes) {
        Action.ActionBuilder builder = Action.goTo(SUCCESS_OUTCOME_ID);
        JsonValue transientState = context.transientState.copy();
        transientState
                .put(RECOVERY_CODE_KEY, recoveryCodes)
                .put(RECOVERY_CODE_DEVICE_NAME, deviceName);

        return cleanupSharedState(context, builder).replaceTransientState(transientState).build();
    }

    /**
     * Creates the QRCode callback.
     *
     * @param scheme the URI scheme.
     * @param host the URI host name.
     * @param path the URI path.
     * @param port the port.
     * @param callbackIndex the callback index.
     * @param params the query parameters.
     * @return the QR Code callback.
     */
    protected Callback createQRCodeCallback(String scheme, String host, String path,
            String port, int callbackIndex, Map<String, String> params) {

        QRCallbackBuilder builder = new QRCallbackBuilder().withUriScheme(scheme)
                .withUriHost(host)
                .withUriPath(path)
                .withUriPort(port)
                .withCallbackIndex(callbackIndex);

        for (Map.Entry<String, String> entries : params.entrySet()) {
            builder.addUriQueryComponent(entries.getKey(), entries.getValue());
        }

        return builder.build();
    }

    /**
     * Creates a HiddenValueCallback to store the registration URI, for easy consumptions in mobile SDKs.
     *
     * @param id the HiddenValueCallback Id
     * @param scheme the URI scheme.
     * @param host the URI host name.
     * @param path the URI path.
     * @param port the port.
     * @param queryParams the query parameters.
     * @return the QR Code callback.
     */
    protected Callback createHiddenValueCallback(String id, String scheme, String host, String path,
            String port, Map<String, String> queryParams) {
        String uri = createUri(scheme, host, path, port, queryParams);

        return new HiddenValueCallback(id, uri);
    }

    /**
     * Creates the URI.
     *
     * @param scheme the URI scheme.
     * @param host the URI host name.
     * @param path the URI path.
     * @param port the port.
     * @param queryParams the query parameters.
     * @return the QR Code callback.
     */
    protected String createUri(String scheme, String host, String path,
            String port, Map<String, String> queryParams) {
        StringBuilder sb = new StringBuilder();
        sb.append(scheme)
                .append("://").append(host)
                .append("/").append(path)
                .append(":").append(port)
                .append("?").append(getQueryString(queryParams));

        return sb.toString();
    }

    private String getQueryString(Map<String, String> queryContents) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entries : queryContents.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }
            sb.append(entries.getKey()).append("=").append(entries.getValue());
        }
        return sb.toString();
    }

    /**
     * Cleanup the SharedState.
     *
     * @param context the context of the tree authentication.
     * @param builder the action builder.
     * @return the action builder.
     */
    protected abstract Action.ActionBuilder cleanupSharedState(TreeContext context, Action.ActionBuilder builder);

    /**
     * Creates a PollingWaitCallback g.
     *
     * @return the polling wait callback.
     */
    protected PollingWaitCallback getPollingWaitCallback() {
        return PollingWaitCallback.makeCallback()
                .withWaitTime(String.valueOf(REGISTER_DEVICE_POLL_INTERVAL))
                .build();
    }

    /**
     * Retrieve the user identity using the username from the context.
     *
     * @param context the context of the tree authentication.
     * @return the AM identity object.
     * @throws NodeProcessException if failed to get the identity object.
     */
    public AMIdentity getIdentity(TreeContext context) throws NodeProcessException {
        Optional<AMIdentity> userIdentity = getAMIdentity(context, identityUtils, coreWrapper);
        if (userIdentity.isEmpty()) {
            throw new NodeProcessException("Failed to get the identity object");
        }
        return userIdentity.get();
    }

    /**
     * Set the current device should be skipped.
     *
     * @param amIdentity the identity of the user.
     * @param realm the realm.
     * @param skippable the skippable attribute.
     */
    @VisibleForTesting
    protected void setUserSkip(AMIdentity amIdentity, String realm, SkipSetting skippable) {
        try {
            multiFactorNodeDelegate.setUserSkip(amIdentity, realm, skippable);
        } catch (NodeProcessException e) {
            logger.debug("Unable to set user attribute as skippable.", e);
        }
    }

    /**
     * Get the user attribute to be used as Account Name.
     *
     * @param identity the AM identity object.
     * @param userAttribute the user attribute from node configuration.
     * @return the user attribute value URL encoded.
     */
    protected String getUserAttributeForAccountName(AMIdentity identity, String userAttribute) {
        String accountName = identity.getName();

        if (StringUtils.isBlank(userAttribute)) {
            // default to username
            userAttribute = UserAttributeToAccountNameMapping.USERNAME.toString();
        }
        logger.debug("Using user attribute of '{}'", userAttribute);
        try {
            Set<String> attributeValue = identity.getAttribute(userAttribute);
            if (CollectionUtils.isNotEmpty(attributeValue)) {
                accountName = attributeValue.iterator().next();
            }
        } catch (IdRepoException | SSOException e) {
            logger.debug("Unable to get user attribute of '{}', returning username for Account Name",
                    userAttribute, e);
        }


        return UrlEscapers.urlFragmentEscaper().escape(accountName);
    }

    /**
     * Possible User attribute to Account Name mappings.
     */
    public enum UserAttributeToAccountNameMapping {
        /**
         * Common Name.
         */
        COMMON_NAME {
            @Override
            public String toString() {
                return "cn";
            }
        },
        /**
         * Email Address.
         */
        EMAIL_ADDRESS {
            @Override
            public String toString() {
                return "mail";
            }
        },
        /**
         * Employee Number.
         */
        EMPLOYEE_NUMBER {
            @Override
            public String toString() {
                return "employeeNumber";
            }
        },
        /**
         * Given Name.
         */
        GIVEN_NAME {
            @Override
            public String toString() {
                return "givenName";
            }
        },
        /**
         * Last Name.
         */
        LAST_NAME {
            @Override
            public String toString() {
                return "sn";
            }
        },
        /**
         * Username.
         */
        USERNAME {
            @Override
            public String toString() {
                return "_username";
            }
        }
    }
}
