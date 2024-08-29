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

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateAttributeProfileMappingExtension.NONE;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateAttributeProfileMappingExtension.RFC822_NAME;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateAttributeProfileMappingExtension.UPN;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateAttributeToProfileMapping.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateAttributeToProfileMapping.OTHER;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateAttributeToProfileMapping.SUBJECT_CN;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateAttributeToProfileMapping.SUBJECT_DN;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateAttributeToProfileMapping.SUBJECT_UID;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateUserExtractorOutcome.EXTRACTED;
import static org.forgerock.openam.auth.nodes.x509.CertificateUserExtractorNode.CertificateUserExtractorOutcome.NOT_EXTRACTED;
import static org.forgerock.openam.auth.nodes.x509.CertificateUtils.getX509Certificate;
import static org.forgerock.opendj.io.Asn1Tag.contextSpecific;

import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.lang.StringUtils;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Namespace;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.opendj.io.Asn1;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.security.x509.CertUtils;
import com.sun.identity.idm.IdType;

/**
 * Certificate User Extractor Node.
 */
@Node.Metadata(outcomeProvider = CertificateUserExtractorNode.CertificateUserExtractorOutcomeProvider.class,
        configClass = CertificateUserExtractorNode.Config.class,
        tags = {"contextual"},
        namespace = Namespace.PRODUCT)
public class CertificateUserExtractorNode extends AbstractDecisionNode {

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/x509/CertificateUserExtractorNode";
    private final Logger logger = LoggerFactory.getLogger(CertificateUserExtractorNode.class);
    private Config config;
    private final LegacyIdentityService identityService;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * Sets the certificate profile mapping attribute.
         *
         * @return the certificate profile mapping attribute.
         */
        @Attribute(order = 100)
        default CertificateAttributeToProfileMapping certificateAttributeToProfileMapping() {
            return SUBJECT_CN;
        }

        /**
         * Sets the optional "other" certificate profile mapping attribute.
         *
         * @return the "other" certificate profile mapping attribute.
         */
        @Attribute(order = 200)
        Optional<String> otherCertificateAttributeToProfileMapping();

        /**
         * Sets the certificate profile mapping extension.
         *
         * @return the certificate profile mapping extension.
         */
        @Attribute(order = 300)
        default CertificateAttributeProfileMappingExtension certificateAttributeProfileMappingExtension() {
            return NONE;
        }
    }

    /**
     * The constructor.
     *
     * @param config node config.
     * @param identityService An {@link LegacyIdentityService} instance.
     */
    @Inject
    public CertificateUserExtractorNode(@Assisted Config config, LegacyIdentityService identityService) {
        this.config = config;
        this.identityService = identityService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        JsonValue sharedState = context.sharedState;
        List<X509Certificate> certs = context.transientState.get("X509Certificate").asList(X509Certificate.class);
        X509Certificate theCert = getX509Certificate(certs, logger);

        if (logger.isDebugEnabled()) {
            logger.debug("Client Cert= \n" + theCert.toString());
        }

        String userTokenId = getTokenFromCert(theCert, config.certificateAttributeToProfileMapping(),
                config.otherCertificateAttributeToProfileMapping(),
                config.certificateAttributeProfileMappingExtension());

        if (StringUtils.isEmpty(userTokenId)) {
            logger.error("Unable to parse user token ID from Certificate");
            return Action.goTo(NOT_EXTRACTED.name()).build();
        }

        sharedState.put(SharedStateConstants.USERNAME, userTokenId);
        logger.debug("UserTokenId=" + userTokenId);
        String realm = context.sharedState.get(REALM).asString();
        return Action
                .goTo(EXTRACTED.name())
                .withIdentifiedIdentity(userTokenId, IdType.USER)
                .withUniversalId(identityService.getUniversalId(userTokenId, realm, IdType.USER))
                .build();
    }

    private String getTokenFromCert(X509Certificate cert, CertificateAttributeToProfileMapping userProfileMapper,
            Optional<String> altUserProfileMapper, CertificateAttributeProfileMappingExtension subjectAltExtMapper)
            throws NodeProcessException {
        String userTokenId = null;
        if (subjectAltExtMapper != NONE) {
            userTokenId = getTokenFromSubjectAltExt(cert, subjectAltExtMapper);
        }
        if (userProfileMapper != CertificateAttributeToProfileMapping.NONE && userTokenId == null) {
            userTokenId = getTokenFromSubjectDN(cert, userProfileMapper, altUserProfileMapper);
        }
        return userTokenId;
    }

    private String getTokenFromSubjectDN(X509Certificate cert,
            CertificateAttributeToProfileMapping userProfileMapper, Optional<String> altUserProfileMapper) {
        /*
         * The certificate has passed the authentication steps
         * so return the part of the certificate as specified
         * in the profile server.
         */
        String userTokenId = null;
        X500Principal subjectPrincipal = cert.getSubjectX500Principal();
        /*
         * Get the Attribute value of the input certificate
         */
        logger.debug("getTokenFromCert: Subject DN : {}", CertUtils.getSubjectName(cert));

        if (userProfileMapper == SUBJECT_DN) {
            userTokenId = CertUtils.getSubjectName(cert);
        } else if (userProfileMapper == SUBJECT_UID) {
            userTokenId = CertUtils.getAttributeValue(subjectPrincipal, CertUtils.UID);
        } else if (userProfileMapper == SUBJECT_CN) {
            userTokenId = CertUtils.getAttributeValue(subjectPrincipal, CertUtils.COMMON_NAME);
        } else if (userProfileMapper == EMAIL_ADDRESS) {
            userTokenId = CertUtils.getAttributeValue(subjectPrincipal, CertUtils.EMAIL_ADDRESS);
            if (userTokenId == null) {
                userTokenId = CertUtils.getAttributeValue(subjectPrincipal, CertUtils.MAIL);
            }
        } else if (userProfileMapper == OTHER) {
            //  "other" has been selected, so use attribute specified in the
            //  iplanet-am-auth-cert-user-profile-mapper-other attribute,
            //  which is in amAuthCert_altUserProfileMapper.
            String atlProfileMapper = altUserProfileMapper.orElseThrow(() ->
                    new IllegalStateException("Missing Other Certificate Field to map user profile attribute value"));
            userTokenId = CertUtils.getAttributeValue(subjectPrincipal, atlProfileMapper);
        }
        logger.debug("getTokenFromCert: {} {}", userProfileMapper, userTokenId);
        return userTokenId;
    }

    private String getTokenFromSubjectAltExt(X509Certificate cert,
            CertificateAttributeProfileMappingExtension subjectAltExtMapper) throws NodeProcessException {
        try {
            Collection<List<?>> subjectAlternativeNames = cert.getSubjectAlternativeNames();
            if (subjectAlternativeNames == null) {
                return null;
            }
            for (List<?> subjectAlternativeName : subjectAlternativeNames) {
                switch ((int) subjectAlternativeName.get(0)) {
                case 0: // otherName
                    // OtherName ::= SEQUENCE {
                    //         type-id    OBJECT IDENTIFIER,
                    //         value      [0] EXPLICIT ANY DEFINED BY type-id }
                    if (UPN.equals(subjectAltExtMapper)) {
                        byte[] otherName = (byte[]) subjectAlternativeName.get(1);
                        try (var reader = Asn1.getReader(otherName).readSequence()) {
                            String oid = reader.readObjectIdentifier();
                            String upnOid = "1.3.6.1.4.1.311.20.2.3";
                            if (oid.equals(upnOid)) {
                                try (var upn = reader.readExplicitTag()) {
                                    // Java getSubjectAlternativeNames for otherName may be doubly tagged
                                    // Despite the cert ASN.1 shows otherwise
                                    if (upn.hasNextElement() && upn.peekType() == contextSpecific().constructed()
                                            .number(0)) {
                                        upn.readExplicitTag();
                                    }
                                    return upn.readOctetStringAsString();
                                }
                            }
                        }
                    }
                    break;
                case 1: // rfc822Name
                    if (RFC822_NAME.equals(subjectAltExtMapper)) {
                        return subjectAlternativeName.get(1).toString();
                    }
                    break;
                default:
                    // Skip
                }
            }
        } catch (CertificateParsingException | IOException e) {
            throw new NodeProcessException("Unable to parse SubjectAlternativeNameExtension", e);
        }
        return null;
    }

    /**
     * Possible Certificate attribute profile mappings.
     */
    public enum CertificateAttributeToProfileMapping {
        /** Subject DN. */
        SUBJECT_DN {
            @Override
            public String toString() {
                return "subject DN";
            }
        },
        /** Subject CN. */
        SUBJECT_CN {
            @Override
            public String toString() {
                return "subject CN";
            }
        },
        /** Subject UID. */
        SUBJECT_UID {
            @Override
            public String toString() {
                return "subject UID";
            }
        },
        /** Email Address. */
        EMAIL_ADDRESS {
            @Override
            public String toString() {
                return "email address";
            }
        },
        /** Other. */
        OTHER {
            @Override
            public String toString() {
                return "other";
            }
        },
        /** None. */
        NONE {
            @Override
            public String toString() {
                return "none";
            }
        }
    }

    /**
     * Possible Certificate attribute profile mapping extensions.
     */
    public enum CertificateAttributeProfileMappingExtension {
        /** None. */
        NONE {
            @Override
            public String toString() {
                return "none";
            }
        },
        /** RFC822 Name. */
        RFC822_NAME {
            @Override
            public String toString() {
                return "RFC822Name";
            }
        },
        /** UPN. */
        UPN {
            @Override
            public String toString() {
                return "UPN";
            }
        }
    }

    /**
     * The possible outcomes for the Certificate User Extractor NodeNode.
     */
    public enum CertificateUserExtractorOutcome {
        /**
         * Successfully  extracted username.
         */
        EXTRACTED,
        /**
         * Failed to extract username.
         */
        NOT_EXTRACTED
    }

    /**
     * Defines the possible outcomes from this Certificate User Extractor node.
     */
    public static class CertificateUserExtractorOutcomeProvider
            implements org.forgerock.openam.auth.node.api.StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    CertificateUserExtractorOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(EXTRACTED.name(), bundle.getString("extractedOutcome")),
                    new Outcome(NOT_EXTRACTED.name(),
                                bundle.getString("notExtractedOutcome")));
        }
    }
}
