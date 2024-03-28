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
 * Copyright 2019-2024 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.x509;

import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectionMethod.EITHER;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectionMethod.HEADER;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectorOutcome.COLLECTED;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectorOutcome.NOT_COLLECTED;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import javax.inject.Inject;

import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Namespace;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.encode.Base64;

/**
 * Certificate Collector Node.
 */
@Node.Metadata(outcomeProvider = CertificateCollectorNode.CertificateCollectorProvider.class,
        configClass = CertificateCollectorNode.Config.class,
        tags = {"contextual"},
        namespace = Namespace.PRODUCT)
public class CertificateCollectorNode implements Node {

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/x509/CertificateCollectorNode";
    private static final Logger logger = LoggerFactory.getLogger(CertificateCollectorNode.class);
    private final Config config;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Sets the certificate collection method.
         *
         * @return the certificate collection method.
         */
        @Attribute(order = 100)
        default CertificateCollectionMethod certificateCollectionMethod() {
            return CertificateCollectionMethod.EITHER;
        }

        /**
         * Sets the HTTP header name which will contain the certificate.
         *
         * @return the HTTP header name.
         */
        @Attribute(order = 200)
        Optional<String> clientCertificateHttpHeaderName();

        /**
         * Sets the trusted remote host IP addresses.
         *
         * @return the trusted remote host IP addresses.
         */
        @Attribute(order = 300)
        default Set<String> trustedRemoteHosts() {
            return Collections.emptySet();
        }
    }

    /**
     * The constructor.
     *
     * @param config node config.
     */
    @Inject
    public CertificateCollectorNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        Set<String> trustedRemoteHosts = config.trustedRemoteHosts();
        CertificateCollectionMethod collectionMethod = config.certificateCollectionMethod();

        X509Certificate[] allCerts = new X509Certificate[0];
        if (collectionMethod.equals(CertificateCollectionMethod.REQUEST)) {
            allCerts = getCertificatesFromRequest(context);
        } else if (collectionMethod.equals(HEADER) && isHostTrusted(trustedRemoteHosts, context.request.clientIp)) {
            String httpHeaderName = config.clientCertificateHttpHeaderName()
                    .orElseThrow(() ->
                            new IllegalStateException("Missing Client Certificate Http Header Name attribute value"));
            allCerts = getPortalStyleCert(context.request.headers, httpHeaderName);
        } else if (collectionMethod.equals(EITHER)) {
            allCerts = getCertificatesFromRequest(context);
            X509Certificate certificate = allCerts != null ? allCerts[0] : null;
            if (certificate == null) {
                String httpHeaderName = config.clientCertificateHttpHeaderName()
                        .orElseThrow(() -> new IllegalStateException(
                                        "Missing Client Certificate Http Header Name attribute value"));
                allCerts = getPortalStyleCert(context.request.headers, httpHeaderName);
            }
        }

        X509Certificate userCert = allCerts != null && allCerts.length != 0 ? allCerts[0] : null;
        if (userCert != null) {
            List<X509Certificate> certs = new ArrayList<>(Arrays.asList(allCerts));
            return Action.goTo(COLLECTED.name()).replaceTransientState(
                    context.transientState.put("X509Certificate", JsonValue.json(certs))).build();
        }
        logger.debug("Certificate was not successfully collected based on node configuration and client request");
        return Action.goTo(NOT_COLLECTED.name()).build();
    }

    private boolean isHostTrusted(Set<String> trustedRemoteHosts, String clientIp) {
        if (trustedRemoteHosts.size() == 0) {
            logger.debug("No trusted hosts specified, return false");
            return false;
        }
        if (trustedRemoteHosts.size() == 1 && trustedRemoteHosts.contains("any")) {
            logger.debug("All hosts are trusted, return true");
            return true;
        }
        return trustedRemoteHosts.contains(clientIp);
    }

    private X509Certificate[] getCertificatesFromRequest(TreeContext context) {
        X509Certificate[] allCerts = (X509Certificate[]) context.request.servletRequest
                .getAttribute("javax.servlet.request.X509Certificate");
        if (allCerts != null && allCerts.length != 0) {
            X509Certificate userCert = allCerts[0];
            logger.debug("X509Certificate: principal is: {}\nissuer DN:{}\nserial number:{}\nsubject dn:{}",
                    userCert.getSubjectDN().getName(), userCert.getIssuerDN().getName(), userCert.getSerialNumber(),
                    userCert.getSubjectDN().getName());
            return allCerts;
        }
        return null;
    }

    private X509Certificate[] getPortalStyleCert(ListMultimap<String, String> headers,
            String clientCertificateHttpHeaderName) throws NodeProcessException {
        String cert = null;
        if (clientCertificateHttpHeaderName != null && clientCertificateHttpHeaderName.length() > 0) {
            logger.debug("Checking cert in HTTP header");
            StringTokenizer tok = new StringTokenizer(clientCertificateHttpHeaderName, ",");
            while (tok.hasMoreTokens()) {
                String key = tok.nextToken();
                if (!headers.containsKey(key)) {
                    continue;
                }
                cert = headers.get(key).get(0);
                cert = cert.trim();
                String beginCert = "-----BEGIN CERTIFICATE-----";
                String endCert = "-----END CERTIFICATE-----";
                int idx = cert.indexOf(endCert);
                if (idx != -1) {
                    cert = cert.substring(beginCert.length(), idx);
                    cert = cert.trim();
                }
            }
        }
        logger.debug("Validate cert: {}", cert);
        if (cert == null || cert.isEmpty()) {
            return null;
        }

        byte[] decoded = Base64.decode(cert);
        if (decoded == null) {
            throw new NodeProcessException("CertificateFromParameter decode failed, possibly invalid Base64 input");
        }

        logger.debug("CertificateFactory.getInstance.");
        CertificateFactory cf;
        X509Certificate userCert;
        try {
            cf = CertificateFactory.getInstance("X.509");
            userCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(decoded));
        } catch (Exception e) {
            throw new NodeProcessException("CertificateFromParameter(X509Cert)", e);
        }

        if (userCert == null) {
            throw new NodeProcessException("Certificate is null");
        }

        logger.debug("X509Certificate: principal is: {}\nissuer DN:{}\nserial number:{}\nsubject dn:{}",
                userCert.getSubjectDN().getName(), userCert.getIssuerDN().getName(), userCert.getSerialNumber(),
                userCert.getSubjectDN().getName());
        return new X509Certificate[]{userCert};
    }

    /**
     * Possible Certificate collection methods.
     */
    public enum CertificateCollectionMethod {
        /**
         * Collect from request attribute.
         */
        REQUEST,
        /**
         * Collect from request header.
         */
        HEADER,
        /**
         * Collect from either attribute and header.
         */
        EITHER
    }

    /**
     * The possible outcomes for the CertificateCollectorNode.
     */
    public enum CertificateCollectorOutcome {
        /**
         * Successful authentication.
         */
        COLLECTED,
        /**
         * Authentication failed.
         */
        NOT_COLLECTED
    }

    /**
     * Defines the possible outcomes from this Certificate Collector node.
     */
    public static class CertificateCollectorProvider implements StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(CertificateCollectorNode.BUNDLE,
                    CertificateCollectorProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(COLLECTED.name(), bundle.getString("collectedOutcome")),
                    new Outcome(NOT_COLLECTED.name(),
                            bundle.getString("notCollectedOutcome")));
        }
    }
}
