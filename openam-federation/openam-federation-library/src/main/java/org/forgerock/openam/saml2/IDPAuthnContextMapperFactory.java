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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.saml2;

import static com.sun.identity.saml2.common.SAML2Constants.AUTHN_CONTEXT_CLASS_REF_UNSPECIFIED;
import static com.sun.identity.saml2.common.SAML2Constants.DEFAULT_IDP_AUTHNCONTEXT_MAPPER_CLASS;
import static com.sun.identity.saml2.common.SAML2Constants.IDP_AUTHNCONTEXT_MAPPER_CLASS;
import static org.forgerock.openam.saml2.plugins.PluginRegistry.newKey;

import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.forgerock.openam.saml2.plugins.PluginRegistry;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;
import com.sun.identity.saml2.plugins.IDPAuthnContextMapper;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;

/**
 * Factory for returning {@link IDPAuthnContextMapper} instnaces.
 */
@Singleton
public class IDPAuthnContextMapperFactory {

    private static final Logger logger = LoggerFactory.getLogger(IDPAuthnContextMapperFactory.class);

    /**
     * Return IDPAuthnContextMapper based on configuration of the hosted idp and remote sp.
     *
     * @param realm the realm
     * @param idpEntityId hosted idp entity id
     * @param spEntityId remote sp entity id
     * @return an IDPAuthnContextMapper instance
     * @throws SAML2Exception if relevant IDPAuthnContextMapper cannot be found
     */
    public IDPAuthnContextMapper getAuthnContextMapper(String realm, String idpEntityId, String spEntityId)
            throws SAML2Exception {
        if (!StringUtils.isBlank(spEntityId)) {
            String treeName = SAML2Utils.getConfiguredServiceForSP(realm, spEntityId);
            if (!StringUtils.isEmptyOrEmptySelection(treeName)) {
                return new ApplicationTreeAuthnContextMapper(treeName);
            }
        }
        return getIDPAuthnContextMapper(realm, idpEntityId);
    }

    /**
     * Return IDPAuthnContextMapper based only on configuration of the hosted idp.
     *
     * @param realm the realm
     * @param idpEntityId hosted idp entity id
     * @return an IDPAuthnContextMapper instance
     * @throws SAML2Exception if relevant IDPAuthnContextMapper cannot be found
     */
    public IDPAuthnContextMapper getAuthnContextMapper(String realm, String idpEntityId)
            throws SAML2Exception {
        return getIDPAuthnContextMapper(realm, idpEntityId);
    }

    private  IDPAuthnContextMapper getIDPAuthnContextMapper(String realm, String idpEntityID) throws SAML2Exception {
        String idpAuthnContextMapperName = IDPSSOUtil.getAttributeValueFromIDPSSOConfig(realm, idpEntityID,
                IDP_AUTHNCONTEXT_MAPPER_CLASS);
        if (idpAuthnContextMapperName == null) {
            idpAuthnContextMapperName = DEFAULT_IDP_AUTHNCONTEXT_MAPPER_CLASS;
        }
        logger.debug("IDPSSOUtil.getIDPAuthnContextMapper: uses {}", idpAuthnContextMapperName);
        return (IDPAuthnContextMapper) PluginRegistry.get(newKey(realm, idpEntityID, IDPAuthnContextMapper.class,
                idpAuthnContextMapperName));
    }

    /**
     * Implementation of IDPAuthnContextMapper to use when there is an application journey/tree configured.
     * In this case, we do not need to act on the AuthnContext configuration. We will just redirect to the configured
     * tree and make the config available in that tree.
     */
    private class ApplicationTreeAuthnContextMapper implements IDPAuthnContextMapper {

        private final String treeName;

        ApplicationTreeAuthnContextMapper(String treeName) {
            this.treeName = treeName;
        }

        @Override
        public IDPAuthnContextInfo getIDPAuthnContextInfo(AuthnRequest authnRequest, String idpEntityID, String realm) throws SAML2Exception {
            // deprecated
            throw new UnsupportedOperationException();
        }

        @Override
        public IDPAuthnContextInfo getIDPAuthnContextInfo(AuthnRequest authnRequest, String idpEntityID,
                String realm, String spEntityId) throws SAML2Exception {
            AuthnContext authnContext = AssertionFactory.getInstance().createAuthnContext();

            // https://groups.oasis-open.org/higherlogic/ws/public/download/35711/
            // sstc-saml-core-errata-2.0-wd-06-diff.pdf/latest
            // 2.7.2.2
            // The <AuthnContext> element specifies the context of an authentication event. The element can contain
            // an authentication context class reference, an authentication context declaration or declaration
            // reference, or both
            // AuthnContextClassRef [Optional] A URI reference identifying an authentication context class
            // that describes the authentication context declaration that follows.
            // 3.2.2.2
            // If the element is present in the query, at least one element in the set of returned
            // assertions MUST contain an element that satisfies the element in the query
            // (see Section 3.3.2.2.1). It is OPTIONAL for the complete set of all such matching
            // assertions to be returned in the response

            // if no authn request, assume IDP-init, in which case, do not need to put context in response
            if (authnRequest != null) {
                RequestedAuthnContext requestedAuthnContext = authnRequest.getRequestedAuthnContext();
                // if no context was requested, do not need to put context in response
                if (requestedAuthnContext != null) {
                    String comparison = requestedAuthnContext.getComparison();
                    // https://groups.oasis-open.org/higherlogic/ws/public/download/35711/
                    // sstc-saml-core-errata-2.0-wd-06-diff.pdf/latest
                    //
                    // 3.3.2.2.1
                    //
                    // If Comparison is set to "exact" or omitted, then the resulting authentication
                    // context in the authentication statement MUST be the exact match of at least one
                    // of the authentication contexts specified
                    //
                    // If Comparison is set to "minimum", then the resulting authentication context in the
                    // authentication statement MUST be at least as strong (as deemed by the responder) as
                    // one of the authentication contexts specified.
                    //
                    // If Comparison is set to "better", then the resulting authentication context in the
                    // authentication statement MUST be stronger (as deemed by the responder) than any one
                    // of the authentication contexts specified.
                    //
                    // If Comparison is set to "maximum", then the resulting authentication context in the
                    // authentication statement MUST be as strong as possible (as deemed by the responder)
                    // without exceeding the strength of at least one of the authentication contexts
                    // specified.
                    if (StringUtils.isBlank(comparison) || comparison.equals("exact")){
                        authnContext.setAuthnContextClassRef(requestedAuthnContext
                                .getAuthnContextClassRef().get(0));
                    }
                }
            }

            if (StringUtils.isBlank(authnContext.getAuthnContextClassRef())) {
                // https://docs.oasis-open.org/security/saml/v2.0/saml-authn-context-2.0-os.pdf
                // 3.4.25
                // The Unspecified class indicates that the authentication was performed by
                // unspecified means.
                authnContext.setAuthnContextClassRef(AUTHN_CONTEXT_CLASS_REF_UNSPECIFIED);
            }

            return new IDPAuthnContextInfo(authnContext, Set.of("service=" + treeName), 0, true);
        }

        @Override
        public boolean isAuthnContextMatching(List requestedACClassRefs, String acClassRef,
                String comparison, String realm, String idpEntityID) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AuthnContext getAuthnContextFromAuthLevel(String authLevel, String realm,
                String idpEntityID) throws SAML2Exception {
            AuthnContext authnContext = AssertionFactory.getInstance().createAuthnContext();
            authnContext.setAuthnContextClassRef(AUTHN_CONTEXT_CLASS_REF_UNSPECIFIED);
            return authnContext;
        }
    }
}
