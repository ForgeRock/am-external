/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: NameIDMapping.java,v 1.6 2009/11/20 21:41:16 exu Exp $
 *
 * Portions Copyrighted 2013-2020 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import static org.forgerock.openam.utils.Time.newDate;

import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.annotations.Supported;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.crypto.signing.Saml2SigningCredentials;
import org.forgerock.openam.saml2.crypto.signing.SigningConfigFactory;
import org.forgerock.openam.saml2.plugins.Saml2CredentialResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.AccountUtils;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.key.EncryptionConfig;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.plugins.IDPAccountMapper;
import com.sun.identity.saml2.protocol.NameIDMappingRequest;
import com.sun.identity.saml2.protocol.NameIDMappingResponse;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.Status;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class provides methods to send or process
 * <code>NameIDMappingRequest</code>.
 *
 */
@Supported

public class NameIDMapping {
    private static final Logger logger = LoggerFactory.getLogger(NameIDMapping.class);
    static ProtocolFactory pf = ProtocolFactory.getInstance();
    static SAML2MetaManager metaManager = null;

    static SessionProvider sessionProvider = null;
    
    static {
        try {
            metaManager= new SAML2MetaManager();
            sessionProvider = SessionManager.getProvider();
        } catch (SAML2MetaException se) {
            logger.error(SAML2Utils.bundle.getString(
                "errorMetaManager"), se);
        } catch (SessionException sessE) {
            logger.error("Error retrieving session provider.", sessE);
        }
    }
    
    /**
     * Parses the request parameters and builds the NameIDMappingRequest to
     * sent to remote identity provider.
     *
     * @param session user session.
     * @param realm the realm of hosted entity
     * @param spEntityID entity ID of hosted service provider
     * @param idpEntityID entity ID of remote idendity provider
     * @param targetSPEntityID entity ID of target entity ID of service
     *     provider
     * @param targetNameIDFormat format of target Name ID
     * @param paramsMap Map of all other parameters
     *
     * @return the <code>NameIDMappingResponse</code>
     * @throws SAML2Exception if error initiating request to remote entity.
     *
     */
    @Supported
    public static NameIDMappingResponse initiateNameIDMappingRequest(
        Object session, String realm, String spEntityID, String idpEntityID,
        String targetSPEntityID, String targetNameIDFormat,
        Map paramsMap) throws SAML2Exception {
            
        if (spEntityID == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("nullSPEntityID"));
        }
                
        if (idpEntityID == null)  {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("nullIDPEntityID"));
        }

        String userID = null;

        try {
            userID = sessionProvider.getPrincipalName(session);
        } catch (SessionException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "NameIDMapping.createNameIDMappingRequest: ", e);
            }
        }

        if (userID == null) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("invalidSSOToken"));
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug(
                "NameIDMapping.initiateNameMappingRequest:" +
                " IDP EntityID is : " + idpEntityID);
            logger.debug(
                "NameIDMapping.initiateNameMappingRequest:" +
                " SP HOST EntityID is : " + spEntityID); 
            logger.debug(
                "NameIDMapping.initiateNameMappingRequest:" +
                " target SP EntityID is : " + targetSPEntityID); 
        }
        
        try {
            // nameIDMappingService
            String binding = 
                SAML2Utils.getParameter(paramsMap, SAML2Constants.BINDING); 
            if (binding == null) {
                binding = SAML2Constants.SOAP;
            } else if (!binding.equals(SAML2Constants.SOAP)) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nimServiceBindingUnsupport"));
            }

            String nimURL = SAML2Utils.getParameter(paramsMap,
                "nimURL");
            if (nimURL == null) {
                EndpointType nameIDMappingService =
                    getNameIDMappingService(realm, idpEntityID, binding);

                if (nameIDMappingService != null) {
                    nimURL = nameIDMappingService.getLocation();
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "NameIDMapping.initiateNameMappingRequest:" +
                    " nimURL" + nimURL);
            }

            if (nimURL == null) {
                throw new SAML2Exception(
                    SAML2Utils.bundle.getString("nimServiceNotFound"));
            }

            NameIDMappingRequest nimRequest = createNameIDMappingRequest(
                userID, realm, spEntityID, idpEntityID, nimURL,
                targetSPEntityID, targetNameIDFormat);

            signNIMRequest(nimRequest, realm, idpEntityID, spEntityID, false);

            IDPSSOConfigElement config = metaManager.getIDPSSOConfig(realm,
                idpEntityID);

            nimURL = SAML2SDKUtils.fillInBasicAuthInfo(config, nimURL);

            return doNIMBySOAP(nimRequest.toXMLString(true,true), nimURL, 
                realm, spEntityID);

        } catch (SAML2MetaException sme) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("metaDataError"));            
        }
    }
    
    public static NameIDMappingResponse processNameIDMappingRequest(
        NameIDMappingRequest nimRequest, String realm, String idpEntityID)
        throws SAML2Exception {

        NameIDMappingResponse nimResponse = null;
        String spEntityID = nimRequest.getIssuer().getValue();
        if (spEntityID == null)  {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("nullSPEntityID"));
        }

        String responseID = SAML2Utils.generateID();
        if (responseID == null) {
            logger.error(
                SAML2Utils.bundle.getString("failedToGenResponseID"));
        }
        nimResponse = pf.createNameIDMappingResponse();
        nimResponse.setID(responseID);
        nimResponse.setInResponseTo(nimRequest.getID());
        nimResponse.setVersion(SAML2Constants.VERSION_2_0);
        nimResponse.setIssueInstant(newDate());
        nimResponse.setIssuer(SAML2Utils.createIssuer(idpEntityID)); 

        SAML2Utils.verifyRequestIssuer(realm, idpEntityID,
            nimRequest.getIssuer(), nimRequest.getID());


        NameIDPolicy nameIDPolicy = nimRequest.getNameIDPolicy();
        String targetSPEntityID = nameIDPolicy.getSPNameQualifier();
        String format = nameIDPolicy.getFormat();

        Status status = null;

        if ((format != null) && (format.length() != 0) &&
            (!format.equals(SAML2Constants.PERSISTENT)) &&
            (!format.equals(SAML2Constants.UNSPECIFIED))) {

            nimResponse.setNameID(nimRequest.getNameID());
            nimResponse.setEncryptedID(nimRequest.getEncryptedID());
            status = SAML2Utils.generateStatus(
            SAML2Constants.INVALID_NAME_ID_POLICY,
                 SAML2Utils.bundle.getString("targetNameIDFormatUnsupported"));
        } else if ((targetSPEntityID == null) ||
            (targetSPEntityID.length() == 0) ||
            targetSPEntityID.equals(spEntityID)) {

            nimResponse.setNameID(nimRequest.getNameID());
            nimResponse.setEncryptedID(nimRequest.getEncryptedID());
            status = SAML2Utils.generateStatus(
                SAML2Constants.INVALID_NAME_ID_POLICY,
                SAML2Utils.bundle.getString("targetNameIDNoChange"));
        } else {
            // check if source SP has account fed
            // if yes then get nameid of targetSP
            IDPAccountMapper idpAcctMapper = SAML2Utils.getIDPAccountMapper(
                realm, idpEntityID);

            NameID nameID = getNameID(nimRequest, realm, idpEntityID);
            String userID = idpAcctMapper.getIdentity(nameID, idpEntityID,
                spEntityID, realm);
            NameIDInfo targetNameIDInfo = null;
            if (userID != null) {
                targetNameIDInfo = AccountUtils.getAccountFederation(userID,
                    idpEntityID, targetSPEntityID);
            }
            if (targetNameIDInfo == null) {
                nimResponse.setNameID(nimRequest.getNameID());
                nimResponse.setEncryptedID(nimRequest.getEncryptedID());
                status = SAML2Utils.generateStatus(
                    SAML2Constants.INVALID_NAME_ID_POLICY,
                    SAML2Utils.bundle.getString("targetNameIDNotFound"));
            } else {
                NameID targetSPNameID = targetNameIDInfo.getNameID();
                if (logger.isDebugEnabled()) {
                    logger.debug(
                        "NameIDMapping.processNameIDMappingRequest: " +
                        "User ID = " + userID + ", name ID = " +
                        targetSPNameID.toXMLString(true,true));
                }

                nimResponse.setEncryptedID(getEncryptedID(targetSPNameID,
                    realm, spEntityID, SAML2Constants.SP_ROLE));
                status = SAML2Utils.generateStatus(
                    SAML2Constants.SUCCESS, null);
	    }
        }

        nimResponse.setStatus(status);
        signNIMResponse(nimResponse, realm, idpEntityID, spEntityID, false);

        return nimResponse;
    }
    
    static private NameIDMappingRequest createNameIDMappingRequest(
        String userID, String realm, String spEntityID, String idpEntityID,
        String destination, String targetSPEntityID, String targetNameIDFormat)
        throws SAML2Exception {

        if (logger.isDebugEnabled()) {
            logger.debug(
                "NameIDMapping.createNameIDMappingRequest: User ID : " +
                userID);
        }
        
        NameIDMappingRequest nimRequest = pf.createNameIDMappingRequest();
        
        nimRequest.setID(SAML2Utils.generateID());
        nimRequest.setVersion(SAML2Constants.VERSION_2_0);
        nimRequest.setDestination(XMLUtils.escapeSpecialCharacters(
            destination));
        nimRequest.setIssuer(SAML2Utils.createIssuer(spEntityID));
        nimRequest.setIssueInstant(newDate());

        setNameIDForNIMRequest(nimRequest, realm, spEntityID, idpEntityID,
            targetSPEntityID, targetNameIDFormat, userID);
        return nimRequest;
    }

    static private NameIDMappingResponse doNIMBySOAP(
        String nimRequestXMLString, String nimURL, String realm,
        String spEntityID) throws SAML2Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("NameIDMapping.doNIMBySOAP: " +
                "NIMRequestXMLString : " + nimRequestXMLString);
            logger.debug("NameIDMapping.doNIMBySOAP: " +
                "NIMRedirectURL : " + nimURL);
        }
        
        SOAPMessage resMsg = null;
        try {
            resMsg = SOAPCommunicator.getInstance().sendSOAPMessage(nimRequestXMLString, nimURL,
                    true);
        } catch (SOAPException se) {
            logger.error("NameIDMapping.doNIMBySOAP: ", se);
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "invalidSOAPMessge"));
        }

        Element nimRespElem = SOAPCommunicator.getInstance().getSamlpElement(resMsg,
                SAML2Constants.NAME_ID_MAPPING_RESPONSE);
        NameIDMappingResponse nimResponse = 
             pf.createNameIDMappingResponse(nimRespElem);
        
        if (logger.isDebugEnabled()) {
            logger.debug("NameIDMapping.doNIMBySOAP: " +
                "NameIDMappingResponse without SOAP envelope:\n" +
                nimResponse.toXMLString(true,true));
        }


        String idpEntityID = nimResponse.getIssuer().getValue();
        Issuer resIssuer = nimResponse.getIssuer();
        String requestId = nimResponse.getInResponseTo();
        SAML2Utils.verifyResponseIssuer(realm, spEntityID, resIssuer,
            requestId);
                    
        if (!verifyNIMResponse(nimResponse, realm, idpEntityID)) {
            throw new SAML2Exception(
                SAML2Utils.bundle.getString("invalidSignInResponse"));
        }

        return nimResponse;
    }

    static private void setNameIDForNIMRequest(NameIDMappingRequest nimRequest,
        String realm, String spEntityID, String idpEntityID,
        String targetSPEntityID, String targetNameIDFormat, String userID)
        throws SAML2Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("NameIDMapping.setNameIDForNIMRequest: " +
                "user ID = " + userID);
        }

        NameID nameID = AssertionFactory.getInstance().createNameID();
        NameIDInfo info = AccountUtils.getAccountFederation(userID, spEntityID,
            idpEntityID);
        nameID.setValue(info.getNameIDValue());
        nameID.setFormat(info.getFormat());
        nameID.setNameQualifier(idpEntityID);
        nameID.setSPNameQualifier(spEntityID);

        NameIDPolicy nameIDPolicy =
            ProtocolFactory.getInstance().createNameIDPolicy();
        nameIDPolicy.setSPNameQualifier(targetSPEntityID);
        nameIDPolicy.setFormat(targetNameIDFormat);
        nimRequest.setNameIDPolicy(nameIDPolicy);

        boolean needEncryptIt = SAML2Utils.getWantNameIDEncrypted(realm,
            idpEntityID, SAML2Constants.IDP_ROLE);
        if (!needEncryptIt) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "NameIDMapping.setNameIDForNIMRequest: "  +
                    "NamID doesn't need to be encrypted.");
            }
            nimRequest.setNameID(nameID);
            return;
        }
        
        EncryptedID encryptedID = getEncryptedID(nameID, realm, idpEntityID,
                SAML2Constants.IDP_ROLE);

        nimRequest.setEncryptedID(encryptedID);
    }    

    /**
     * Returns first NameIDMappingService matching specified binding in an
     * entity under the realm.
     *
     * @param realm The realm under which the entity resides.
     * @param entityId ID of the entity to be retrieved.
     * @param binding bind type need to has to be matched.
     * @return <code>ManageNameIDServiceElement</code> for the entity or null
     * @throws SAML2MetaException if unable to retrieve the first identity provider's SSO configuration.
     */
    static public EndpointType getNameIDMappingService(
        String realm, String entityId, String binding)
        throws SAML2MetaException {


        IDPSSODescriptorType idpSSODesc = metaManager.getIDPSSODescriptor(
            realm, entityId);
        if (idpSSODesc == null) {
            logger.error(SAML2Utils.bundle.getString("noIDPEntry"));
            return null;
        }

        List<EndpointType> list = idpSSODesc.getNameIDMappingService();

        EndpointType nimService = null;
        if ((list != null) && !list.isEmpty()) {
            if (binding == null) {
                return list.get(0);
            }
            Iterator<EndpointType> it = list.iterator();
            while (it.hasNext()) {
                nimService = it.next();
                if (binding.equalsIgnoreCase(nimService.getBinding())) {
                    return nimService;
                }
            }
        }
        return null;
    }
        
    static EncryptedID getEncryptedID(NameID nameID, String realm,
        String entityID, String role) throws SAML2Exception {

        RoleDescriptorType roled = null;

        if (role.equals(SAML2Constants.SP_ROLE)) {
            roled = metaManager.getSPSSODescriptor(realm, entityID);
        } else {
            roled = metaManager.getIDPSSODescriptor(realm, entityID);
        }

        EncryptionConfig encryptionConfig = KeyUtil.getEncryptionConfig(roled, entityID, role, realm);
        
        if (encryptionConfig == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString("UnableToFindEncryptKeyInfo"));
        }

        return nameID.encrypt(encryptionConfig, entityID);
    }    

    private static void signNIMRequest(NameIDMappingRequest nimRequest, 
        String realm, String idpEntityId, String spEntityID, boolean includeCert)
        throws SAML2Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("NameIDMapping.signNIMRequest: " +
                "NIMRequest before sign : " +
                nimRequest.toXMLString(true, true));
        }

        Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveActiveSigningCredential(realm, spEntityID, Saml2EntityRole.SP);
        Key signingKey = credentials.getSigningKey();

        if (signingKey == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }

        X509Certificate signingCert = includeCert ? credentials.getSigningCertificate() : null;
        nimRequest.sign(SigningConfigFactory.getInstance()
                .createXmlSigningConfig(signingKey,
                        signingCert,
                        metaManager.getEntityDescriptor(realm, idpEntityId),
                        Saml2EntityRole.IDP));

        if (logger.isDebugEnabled()) {
            logger.debug("NameIDMapping.signNIMRequest: " +
                "NIMRequest after sign : " +
                nimRequest.toXMLString(true, true));
        }
    }

    static void signNIMResponse(NameIDMappingResponse nimResponse, String realm, String idpEntityID, String spEntityId,
            boolean includeCert) throws SAML2Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("NameIDMapping.signNIMResponse: " + realm);
            logger.debug("NameIDMapping.signNIMResponse: " + idpEntityID);
        }

        Saml2SigningCredentials credentials = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                .resolveActiveSigningCredential(realm, spEntityId, Saml2EntityRole.IDP);
        Key signingKey = credentials.getSigningKey();

        if (signingKey == null) {
            logger.error("NameIDMapping.signNIMResponse: Incorrect configuration for Signing Certificate.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("metaDataError"));
        }

        X509Certificate signingCert = includeCert ? credentials.getSigningCertificate() : null;
        nimResponse.sign(SigningConfigFactory.getInstance()
                .createXmlSigningConfig(signingKey,
                        signingCert,
                        metaManager.getEntityDescriptor(realm, spEntityId),
                        Saml2EntityRole.SP));
    }

    private static boolean verifyNIMResponse(NameIDMappingResponse nimResponse,
        String realm, String idpEntityID) throws SAML2Exception {

        IDPSSODescriptorType idpSSODesc = metaManager.getIDPSSODescriptor(
            realm, idpEntityID);
        Set<X509Certificate> signingCerts = KeyUtil.getVerificationCerts(idpSSODesc, idpEntityID,
                SAML2Constants.IDP_ROLE, realm);
        
        if (!signingCerts.isEmpty()) {
            boolean valid = nimResponse.isSignatureValid(signingCerts);
            if (logger.isDebugEnabled()) {
                logger.debug("NameIDMapping.verifyNIMResponse: " +
                    "Signature is : " + valid);
            }
            return valid;
        } else {
            throw new SAML2Exception(SAML2Utils.bundle.getString("missingSigningCertAlias"));
        }
    }

    private static NameID getNameID(NameIDMappingRequest nimRequest, String realm, String idpEntityID) {
        NameID nameID = nimRequest.getNameID();
        if (nameID == null) {
            EncryptedID encryptedID = nimRequest.getEncryptedID();
            try {
                final Set<PrivateKey> decryptionKeys = InjectorHolder.getInstance(Saml2CredentialResolver.class)
                        .resolveValidDecryptionCredentials(realm, idpEntityID, Saml2EntityRole.IDP);
                nameID = encryptedID.decrypt(decryptionKeys);
            } catch (SAML2Exception ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("NameIDMapping.getNameID:", ex);
                }
                return null;
            }
        }

        if (!SAML2Utils.isPersistentNameID(nameID)) {
            return null;
        }

        return nameID;
    }
}
