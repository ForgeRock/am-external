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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.mfa;

import static org.forgerock.openam.auth.nodes.mfa.AbstractMultiFactorNode.REGISTER_DEVICE_POLL_INTERVAL;

import javax.security.auth.callback.Callback;
import java.util.Map;
import java.util.Set;

import com.google.common.net.UrlEscapers;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.authentication.callbacks.helpers.QRCallbackBuilder;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class with shared code for the MFA registration.
 */
public class MultiFactorRegistrationUtilities {

    private static final Logger logger = LoggerFactory.getLogger(MultiFactorRegistrationUtilities.class);

    /**
     * Get the user attribute to be used as Account Name.
     *
     * @param identity the AM identity object.
     * @param userAttribute the user attribute from node configuration.
     * @return the user attribute value URL encoded.
     */
    public String getUserAttributeForAccountName(AMIdentity identity, String userAttribute) {
        String accountName = identity.getName();

        if (StringUtils.isBlank(userAttribute)) {
            // default to username
            userAttribute = AbstractMultiFactorNode.UserAttributeToAccountNameMapping.USERNAME.toString();
        }
        logger.debug("Using user attribute of '{}'", userAttribute);
        try {
            Set<String> attributeValue = getAttributeFromIdentity(identity, userAttribute);
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
    public Callback createQRCodeCallback(String scheme, String host, String path,
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
     * @param id the HiddenValueCallback Id.
     * @param scheme the URI scheme.
     * @param host the URI host name.
     * @param path the URI path.
     * @param port the port.
     * @param queryParams the query parameters.
     * @return the QR Code callback.
     */
    public Callback createHiddenValueCallback(String id, String scheme, String host, String path,
                                              String port, Map<String, String> queryParams) {
        String uri = createUri(scheme, host, path, port, queryParams);

        return new HiddenValueCallback(id, uri);
    }

    /**
     * Creates a PollingWaitCallback with default wait time.
     *
     * @return the polling wait callback.
     */
    public PollingWaitCallback getPollingWaitCallback() {
        return PollingWaitCallback.builder()
                .withWaitTime(String.valueOf(REGISTER_DEVICE_POLL_INTERVAL))
                .build();
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
    public String createUri(String scheme, String host, String path,
                               String port, Map<String, String> queryParams) {
        StringBuilder sb = new StringBuilder();
        sb.append(scheme)
                .append("://").append(host)
                .append("/").append(path)
                .append(":").append(port)
                .append("?").append(getQueryString(queryParams));

        return sb.toString();
    }

    private Set<String> getAttributeFromIdentity(AMIdentity identity, String userAttribute)
            throws IdRepoException, SSOException {
        Set<String> attributeValue = identity.getAttribute(userAttribute);
        if (attributeValue.isEmpty()) {
            identity.getAttributes(); // Refresh the cache
            attributeValue = identity.getAttribute(userAttribute);
        }
        return attributeValue;
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

}
