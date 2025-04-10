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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.x509;

import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectionMethod.EITHER;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectionMethod.HEADER;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectorOutcome.COLLECTED;
import static org.forgerock.openam.auth.nodes.x509.CertificateCollectorNode.CertificateCollectorOutcome.NOT_COLLECTED;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Stream;

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
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.security.X509Decoder;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.util.Reject;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;

/**
 * Certificate Collector Node.
 */
@Node.Metadata(outcomeProvider = CertificateCollectorNode.CertificateCollectorProvider.class,
        configClass = CertificateCollectorNode.Config.class,
        configValidator = CertificateCollectorNode.CertificateCollectorNodeValidator.class,
        tags = {"contextual"},
        namespace = Namespace.PRODUCT)
public class CertificateCollectorNode implements Node {

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/x509/CertificateCollectorNode";
    private static final Logger logger = LoggerFactory.getLogger(CertificateCollectorNode.class);
    private final Config config;
    private final X509Decoder certificateDecoder;

    /**
     * Validates the certificate collector node.
     */
    public static class CertificateCollectorNodeValidator implements ServiceConfigValidator {

        private static final Logger logger =
                LoggerFactory.getLogger(CertificateCollectorNode.CertificateCollectorNodeValidator.class);

        @Override
        public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
                throws ServiceConfigException, ServiceErrorException {
            Reject.ifNull(attributes, "Attributes are required for validation");

            String clientCertificateHttpHeaderName = getMapAttr(attributes, "clientCertificateHttpHeaderName", "");
            String certificateCollectionMethod = getMapAttr(attributes, "certificateCollectionMethod", "");

            if (clientCertificateHttpHeaderName.isEmpty() && Stream.of(HEADER, EITHER).map(Enum::name)
                    .anyMatch(method -> method.equals(certificateCollectionMethod))) {
                logger.error("HTTP Header Name for Client Certificate is required for this collection method.");
                throw new ServiceConfigException("HTTP Header Name for Client Certificate "
                        + "is required for this collection method.");
            }
        }
    }

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
     * @param certificateDecoder the X.509 decoder used for decoding the certificate
     */
    @Inject
    public CertificateCollectorNode(@Assisted Config config, X509Decoder certificateDecoder) {
        this.config = config;
        this.certificateDecoder = certificateDecoder;
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
                .getAttribute("jakarta.servlet.request.X509Certificate");
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
        if (clientCertificateHttpHeaderName != null && clientCertificateHttpHeaderName.length() > 0) {
            logger.debug("Checking cert in HTTP header");
            StringTokenizer tok = new StringTokenizer(clientCertificateHttpHeaderName, ",");
            while (tok.hasMoreTokens()) {
                String key = tok.nextToken();
                if (!headers.containsKey(key)) {
                    continue;
                }
                String certHeader = headers.get(key).get(0).trim();

                try {
                    X509Certificate userCert = certificateDecoder.decodeCertificate(certHeader);
                    logger.debug("X509Certificate: principal is: {}\nissuer DN:{}\nserial number:{}\nsubject dn:{}",
                            userCert.getSubjectDN().getName(), userCert.getIssuerDN().getName(),
                            userCert.getSerialNumber(), userCert.getSubjectDN().getName());
                    return new X509Certificate[]{userCert};
                } catch (CertificateException e) {
                    throw new NodeProcessException("CertificateFromParameter(X509Cert)", e);
                }
            }
        }
        return null;
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
