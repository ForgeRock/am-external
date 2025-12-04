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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.x509;

import static com.sun.identity.shared.datastruct.CollectionHelper.getServerMapAttr;
import static org.forgerock.openam.auth.nodes.x509.CertificateUtils.getX509Certificate;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import javax.inject.Inject;

import org.forgerock.am.identity.application.IdentityStoreLdapParametersProvider;
import org.forgerock.am.identity.application.model.LdapConnectionParameters;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Namespace;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.ldap.LDAPURL;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.sm.annotations.adapters.SecretPurpose;
import org.forgerock.opendj.ldap.LdapUrl;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.security.cert.AMCRLStore;
import com.sun.identity.security.cert.AMCertPath;
import com.sun.identity.security.cert.AMCertStore;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSException;

/**
 * Certificate Validation Node.
 */
@Node.Metadata(outcomeProvider = CertificateValidationNode.CertificateValidationOutcomeProvider.class,
        configClass = CertificateValidationNode.Config.class,
        tags = {"contextual"},
        namespace = Namespace.PRODUCT)
public class CertificateValidationNode extends AbstractDecisionNode {
    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/x509/CertificateValidationNode";
    private final Logger logger = LoggerFactory.getLogger(CertificateValidationNode.class);
    private final Config config;
    private final Realm realm;
    private final Secrets secrets;
    private final IdentityStoreLdapParametersProvider identityStoreLdapParametersProvider;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Sets whether to match Certificate in LDAP.
         *
         * @return whether to match Certificate in LDAP.
         */
        @Attribute(order = 100)
        default boolean matchCertificateInLdap() {
            return false;
        }

        /**
         * Sets whether to check Certificate Expiration.
         *
         * @return whether to check Certificate Expiration.
         */
        @Attribute(order = 200)
        default boolean checkCertificateExpiry() {
            return false;
        }

        /**
         * Sets the subject DN Attribute Used to Search LDAP for Certificates.
         *
         * @return the subject DN Attribute Used to Search LDAP for Certificates.
         */
        @Attribute(order = 300, requiredValue = true)
        default Optional<String> ldapCertificateAttribute() {
            return Optional.of("CN");
        }

        /**
         * Sets whether to match Certificate to CRL.
         *
         * @return whether to match Certificate to CRL.
         */
        @Attribute(order = 400)
        default boolean matchCertificateToCRL() {
            return false;
        }

        /**
         * Sets the issuer DN Attribute(s) Used to Search LDAP for CRLs.
         *
         * @return the issuer DN Attribute(s) Used to Search LDAP for CRLs.
         */
        @Attribute(order = 500)
        default String crlMatchingCertificateAttribute() {
            return "CN";
        }

        /**
         * Sets the HTTP Parameters for CRL Update.
         *
         * @return the HTTP Parameters for CRL Update.
         */
        @Attribute(order = 600)
        Optional<String> crlHttpParameters();

        /**
         * Sets whether to cache CRLs in Memory.
         *
         * @return whether to cache CRLs in Memory.
         */
        @Attribute(order = 700)
        default boolean cacheCRLsInMemory() {
            return true;
        }

        /**
         * Sets whether to update CA CRLs from CRLDistributionPoint.
         *
         * @return whether to update CA CRLs from CRLDistributionPoint.
         */
        @Attribute(order = 800)
        default boolean updateCRLsFromDistributionPoint() {
            return true;
        }

        /**
         * Sets whether OCSP Validation is enabled.
         *
         * @return whether OCSP Validation is enabled.
         */
        @Attribute(order = 900)
        default boolean ocspValidationEnabled() {
            return false;
        }

        /**
         * Sets the name of the pre-configured LDAP certificate store.
         *
         * @return the name of the LDAP certificate store.
         */
        @Attribute(order = 950, choiceValuesClass = IdentityStoreChoiceValues.class)
        default String certificateIdentityStore() {
            return ISAuthConstants.BLANK;
        }

        /**
         * Sets the LDAP servers where certificates are stored.
         *
         * @return the LDAP certificate servers.
         */
        @Attribute(order = 1000)
        default Set<String> certificateLdapServers() {
            return Collections.singleton(getDirectoryServerURL());
        }

        /**
         * Sets the LDAP base search DN.
         *
         * @return the LDAP base search DN.
         */
        @Attribute(order = 1100)
        Set<String> ldapSearchStartDN();

        /**
         * Sets the LDAP bind DN.
         *
         * @return the LDAP bind DN.
         */
        @Attribute(order = 1200)
        default Optional<String> userBindDN() {
            return Optional.of("cn=Directory Manager");
        }

        /**
         * Sets the LDAP bind password.
         *
         * @return the LDAP bind password
         */
        @Attribute(order = 1300)
        @Password
        Optional<char[]> userBindPassword();

        /**
         * Indicates if mTLS is enabled.
         *
         * @return true if mTLS is enabled otherwise false.
         */
        @Attribute(order = 1325, requiredValue = true)
        default boolean mtlsEnabled() {
            return false;
        }

        /**
         * Label used for mapping to the mTLS certificate in the secret store.
         *
         * @return the label
         */
        @Attribute(order = 1350)
        @SecretPurpose("am.authentication.nodes.certificate.validation.mtls.%s.cert")
        Optional<Purpose<SigningKey>> mtlsSecretLabel();

        /**
         * Sets whether ssl is enabled.
         *
         * @return whether ssl is enabled.
         */
        @Attribute(order = 1400)
        default boolean sslEnabled() {
            return false;
        }
    }

    /**
     * The constructor.
     *
     * @param config the node config
     * @param realm the realm configuration
     * @param secrets the secrets API object
     * @param identityStoreLdapParametersProvider the {@link IdentityStoreLdapParametersProvider}
     */
    @Inject
    public CertificateValidationNode(@Assisted Config config, @Assisted Realm realm, Secrets secrets,
            IdentityStoreLdapParametersProvider identityStoreLdapParametersProvider) {
        this.config = config;
        this.realm = realm;
        this.secrets = secrets;
        this.identityStoreLdapParametersProvider = identityStoreLdapParametersProvider;
    }

    private static String getDirectoryServerURL() {
        final String host = SystemProperties.get(Constants.AM_DIRECTORY_HOST);
        final String port = SystemProperties.get(Constants.AM_DIRECTORY_PORT);

        if (host != null && port != null) {
            return host + ":" + port;
        } else {
            return "";
        }
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        LdapConnectionParameters ldapParam = null;
        List<X509Certificate> certs = context.transientState.get("X509Certificate").asList(X509Certificate.class);
        X509Certificate theCert = getX509Certificate(certs, logger);

        if (config.checkCertificateExpiry()) {
            try {
                theCert.checkValidity();
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                logger.debug("Certificate Expired", e);
                return Action.goTo(CertificateValidationOutcome.EXPIRED.name()).build();
            }
        }

        if (config.matchCertificateInLdap() || config.matchCertificateToCRL() || config.ocspValidationEnabled()) {
            ldapParam = setLdapStoreParam();
        }

        if (config.matchCertificateInLdap()) {
            if (config.ldapCertificateAttribute().isEmpty()) {
                throw new NodeProcessException("Ldap Certificate Attribute is empty in node configuration but needed "
                        + "to match certificate in LDAP");
            }
            if (AMCertStore.getRegisteredCertificate(ldapParam, theCert,
                    config.ldapCertificateAttribute().get()) == null) {
                logger.error("Certificate not found in the directory");
                return Action.goTo(CertificateValidationOutcome.NOT_FOUND.name()).build();
            }
        }

        if (config.matchCertificateToCRL() || config.ocspValidationEnabled()) {
            if (!isCertificatePathValid(certs)) {
                logger.error("Certificate path is not valid");
                return Action.goTo(CertificateValidationOutcome.PATH_VALIDATION_FAILED.name()).build();
            }
            if (!isCertificateRevoked(certs, ldapParam)) {
                logger.error("Certificate is revoked");
                return Action.goTo(CertificateValidationOutcome.REVOKED.name()).build();
            }
        }

        return Action.goTo(CertificateValidationOutcome.TRUE.name()).build();
    }

    private String[] trimItems(String[] items) {
        String[] trimmedItems = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            trimmedItems[i] = items[i].trim();
        }
        return trimmedItems;
    }

    private boolean isCertificatePathValid(List<X509Certificate> theCerts) throws NodeProcessException {
        AMCertPath certPath;
        try {
            certPath = new AMCertPath(null);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new NodeProcessException("Unable to create the Certificate Path", e);
        }
        return certPath.verify(theCerts.toArray(new X509Certificate[0]), false, false);

    }

    private boolean isCertificateRevoked(List<X509Certificate> theCerts, LdapConnectionParameters ldapParam)
            throws NodeProcessException {
        Vector<X509CRL> certificateRevocationLists = new Vector<>();
        for (X509Certificate cert : theCerts) {
            X509CRL crl = AMCRLStore.getCRL(ldapParam, cert, config.cacheCRLsInMemory(),
                    config.updateCRLsFromDistributionPoint(), config.crlHttpParameters().orElse(null),
                    trimItems(config.crlMatchingCertificateAttribute().split(",")));
            if (crl != null) {
                certificateRevocationLists.add(crl);
            }
        }
        logger.debug("CertificateRevocationLists size = {}", certificateRevocationLists.size());
        if (certificateRevocationLists.size() > 0) {
            logger.debug("CRL = {}", certificateRevocationLists);
        }
        AMCertPath certPath;
        try {
            certPath = new AMCertPath(certificateRevocationLists);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new NodeProcessException("Unable to create the Certificate Path", e);
        }

        return certPath.verify(theCerts.toArray(new X509Certificate[0]), config.matchCertificateToCRL(),
                               config.ocspValidationEnabled());
    }

    private LdapConnectionParameters setLdapStoreParam() throws NodeProcessException {
        try {
            String certificateIdentityStore = config.certificateIdentityStore();
            if (isNotBlank(certificateIdentityStore) && !certificateIdentityStore.equals(ISAuthConstants.BLANK)) {
                return createParamsFromIdentityStore();
            }
            return createParamsFromNodeConfig();
        } catch (Exception e) {
            throw new NodeProcessException("Unable to set LDAP Server configuration", e);
        }
    }

    private LdapConnectionParameters createParamsFromIdentityStore() throws SMSException {
        return identityStoreLdapParametersProvider.getLdapParams(realm, config.certificateIdentityStore());
    }

    private LdapConnectionParameters createParamsFromNodeConfig() throws NodeProcessException {
        /*
         * Setup the LDAP certificate directory service context for
         * use in verification of the users certificates.
         */
        Map<String, Set<String>> configMap = ImmutableMap.of(
                "certificateLdapServers", config.certificateLdapServers(),
                "ldapSearchStartDN", config.ldapSearchStartDN());

        String serverHost = getServerMapAttr(configMap, "certificateLdapServers");
        if (serverHost == null) {
            throw new NodeProcessException("Unable to set LDAP Server configuration, LDAP Configuration is null");
        }
        LdapUrl ldapUrl = LdapUrl.valueOf("ldap://" + serverHost);
        Set<LDAPURL> ldapUrls = LDAPUtils.getLdapUrls(ldapUrl.getHost(), ldapUrl.getPort(), config.sslEnabled());
        return LdapConnectionParameters.builder()
                .ldapServers(ldapUrls)
                .ldapUser(getLdapUser())
                .ldapPassword(getLdapPassword())
                .startSearchLocation(getServerMapAttr(configMap, "ldapSearchStartDN"))
                .mtlsEnabled(config.mtlsEnabled())
                .mtlsSecretLabel(getSecretLabel())
                .realm(realm)
                .build();
    }

    private String getLdapUser() {
        return config.userBindDN().or(() -> {
            if (!config.mtlsEnabled()) {
                throw new IllegalStateException("Missing User Bind DN attribute value");
            }
            return Optional.empty();
        }).orElse(null);
    }

    private char[] getLdapPassword() {
        return config.userBindPassword().or(() -> {
            if (!config.mtlsEnabled()) {
                throw new IllegalStateException("Missing User Bind Password attribute value");
            }
            return Optional.empty();
        }).orElse(null);
    }

    private String getSecretLabel() {
        return config.mtlsSecretLabel().or(() -> {
            if (config.mtlsEnabled()) {
                throw new IllegalStateException("Missing mTLS Secret Label Identifier");
            }
            return Optional.empty();
        }).map(Purpose::getLabel).orElse(null);
    }

    /**
     * The possible outcomes for the Certificate Validation node.
     */
    public enum CertificateValidationOutcome {
        /**
         * Successful authentication.
         */
        TRUE,
        /**
         * Authentication failed.
         */
        FALSE,
        /**
         * The certificate is expired.
         */
        NOT_FOUND,
        /**
         * The certificate is expired.
         */
        EXPIRED,
        /**
         * The certificate path validation failed.
         */
        PATH_VALIDATION_FAILED,
        /**
         * The certificate is revoked.
         */
        REVOKED,
    }

    /**
     * Defines the possible outcomes from this Certificate Validation node.
     */
    public static class CertificateValidationOutcomeProvider
            implements org.forgerock.openam.auth.node.api.StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(CertificateValidationNode.BUNDLE,
                    CertificateValidationOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(CertificateValidationOutcome.TRUE.name(), bundle.getString("trueOutcome")),
                    new Outcome(CertificateValidationOutcome.FALSE.name(), bundle.getString("falseOutcome")),
                    new Outcome(CertificateValidationOutcome.NOT_FOUND.name(), bundle.getString("notFound")),
                    new Outcome(CertificateValidationOutcome.EXPIRED.name(), bundle.getString("expiredOutcome")),
                    new Outcome(CertificateValidationOutcome.PATH_VALIDATION_FAILED.name(),
                                bundle.getString("pathValidationFailed")),
                    new Outcome(CertificateValidationOutcome.REVOKED.name(), bundle.getString("revoked")));
        }
    }
}
