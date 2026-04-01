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
 * Copyright 2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import com.google.inject.Injector;
import com.sun.identity.multiprotocol.MultiProtocolRelayServlet;
import com.sun.identity.saml2.soapbinding.QueryHandlerServlet;
import com.sun.identity.wsfederation.servlet.WSFederationServlet;

public class FedLibraryServletInitializer implements ServletContextListener {

    private ServletContext context;
    private Injector injector;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        context = sce.getServletContext();
        injector = (Injector) context.getAttribute(Injector.class.getName());

        addServlet(QueryHandlerServlet.class, "/saml2query/*");
        addServlet(IDPArtifactResolutionServiceSOAP.class, "/ArtifactResolver/*");
        addServlet(IDPSingleLogoutInitServlet.class, "/IDPSloInit/*",
                "/saml2/jsp/idpSingleLogoutInit.jsp");
        addServlet(IDPSingleLogoutRedirectServlet.class, "/IDPSloRedirect/*",
                "/saml2/jsp/idpSingleLogoutRedirect.jsp");
        addServlet(IDPSingleLogoutPostServlet.class, "/IDPSloPOST/*", "/saml2/jsp/idpSingleLogoutPOST.jsp");
        addServlet(IDPSingleLogoutServiceSOAP.class, "/IDPSloSoap/*");
        addServlet(IDPMNIRedirectServlet.class, "/IDPMniRedirect/*", "/saml2/jsp/idpMNIRedirect.jsp");
        addServlet(IDPMniPostServlet.class, "/IDPMniPOST/*", "/saml2/jsp/idpMNIPOST.jsp");
        addServlet(IDPManageNameIDServiceSOAP.class, "/IDPMniSoap/*");
        addServlet(IDPMniRequestInitServlet.class, "/IDPMniInit/*", "/saml2/jsp/idpMNIRequestInit.jsp");
        addServlet(IDPSSOFederateServlet.class, "/SSORedirect/*", "/SSOPOST/*", "/idpSSOFederate/*",
                "/saml2/jsp/idpSSOFederate.jsp");
        addServlet(IDPSingleSignOnServiceSOAP.class, "/SSOSoap/*");
        addServlet(AttributeServiceSOAP.class, "/AttributeServiceSoap/*");
        addServlet(AuthnQueryServiceSOAP.class, "/AuthnQueryServiceSoap/*");
        addServlet(AssertionIDRequestServiceSOAP.class, "/AIDReqSoap/*");
        addServlet(AssertionIDRequestServiceURI.class, "/AIDReqUri/*");
        addServlet(NameIDMappingServiceSOAP.class, "/NIMSoap/*");
        addServlet(SPAssertionConsumerServlet.class, "/Consumer/*", "/saml2/jsp/spAssertionConsumer.jsp");
        addServlet(SPSingleLogoutInitServlet.class, "/SPSloInit/*", "/saml2/jsp/spSingleLogoutInit.jsp");
        addServlet(SPSingleLogoutRedirectServlet.class, "/SPSloRedirect/*",
                "/saml2/jsp/spSingleLogoutRedirect.jsp");
        addServlet(SPSingleLogoutPostServlet.class, "/SPSloPOST/*", "/saml2/jsp/spSingleLogoutPOST.jsp");
        addServlet(SPSingleLogoutServiceSOAP.class, "/SPSloSoap/*");
        addServlet(SPECPService.class, "/SPECP/*");
        addServlet(SPMniRedirectServlet.class, "/SPMniRedirect/*", "/saml2/jsp/spMNIRedirect.jsp");
        addServlet(SPMniPostServlet.class, "/SPMniPOST/*", "/saml2/jsp/spMNIPOST.jsp");
        addServlet(SPManageNameIDServiceSOAP.class, "/SPMniSoap/*");
        addServlet(SPMniRequestInitServlet.class, "/SPMniInit/*", "/saml2/jsp/spMNIRequestInit.jsp");
        addServlet(SPSSOInitServlet.class, "/spssoinit", "/saml2/jsp/spSSOInit.jsp");
        addServlet(IDPSSOInitServlet.class, "/idpssoinit", "/saml2/jsp/idpSSOInit.jsp");
        addServlet(IDPSaeHandlerServlet.class, "/idpsaehandler/*", "/saml2/jsp/SA_IDP.jsp");
        addServlet(SPSaeHandlerServlet.class, "/spsaehandler/*", "/saml2/jsp/SA_SP.jsp");
        addServlet(ExportSamlMetadataServlet.class, "/ExportSamlMetadata", "/saml2/jsp/exportmetadata.jsp");
        addServlet(MultiProtocolRelayServlet.class, "/multiprotocolrelay/*");
        addServlet(WSFederationServlet.class, "/WSFederationServlet/*", "/FederationMetadata/*");
    }

    private void addServlet(Class<? extends jakarta.servlet.Servlet> servletClass, String... urlPatterns) {
        var servlet = injector.getInstance(servletClass);
        var registration = context.addServlet(servletClass.getSimpleName(), servlet);
        if (urlPatterns != null && urlPatterns.length > 0) {
            registration.addMapping(urlPatterns);
        }
    }
}
