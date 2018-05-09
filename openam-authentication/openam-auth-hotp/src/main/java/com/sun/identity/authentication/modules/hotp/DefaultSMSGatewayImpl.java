/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DefaultSMSGatewayImpl.java,v 1.3 2009/07/30 17:38:00 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2011-2017 ForgeRock AS.
 */
package com.sun.identity.authentication.modules.hotp;

import com.iplanet.am.util.AMSendMail;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

import java.util.Map;

public class DefaultSMSGatewayImpl implements SMSGateway {

    private Debug debug = null;
    public static final String SMTPHOSTNAME = "sunAMAuthHOTPSMTPHostName";
    public static final String SMTPHOSTPORT = "sunAMAuthHOTPSMTPHostPort";
    public static final String SMTPUSERNAME = "sunAMAuthHOTPSMTPUserName";
    public static final String SMTPUSERPASSWORD = "sunAMAuthHOTPSMTPUserPassword";
    public static final String SMTPSSLENABLED = "sunAMAuthHOTPSMTPSSLEnabled";

    private String smtpHostName = null;
    private String smtpHostPort = null;
    private String smtpUserName = null;
    private String smtpUserPassword = null;
    private String smtpSSLEnabled = null;
    private boolean sslEnabled = true;
    private boolean startTls = false;

    public DefaultSMSGatewayImpl() {
        debug = Debug.getInstance("amAuthHOTP");
    }

    /**
     * {@inheritDoc}
     */
    public void sendSMSMessage(String from, String to, String subject,
        String message, String code, Map options) throws AuthLoginException {
        if (to == null) {
            return;
        }
        try {
            setOptions(options);
            String msg = message + code;
            String tos[] = new String[1];
            // If the phone does not contain provider info, append ATT to it
            // Note : need to figure out a way to add the provider information
            // For now assume : the user phone # entered is
            // <phone@provider_address). For exampe : 4080989109@txt.att.net
            if (!to.contains("@")) {
                to = to + "@txt.att.net";
            }
            tos[0] = to;
            AMSendMail sendMail = new AMSendMail();

            if (smtpHostName == null || smtpHostPort == null) {
                sendMail.postMail(tos, subject, msg, from);
            } else {
                sendMail.postMail(tos, subject, msg, from, "UTF-8", smtpHostName,
                        smtpHostPort, smtpUserName, smtpUserPassword,
                        sslEnabled, startTls);
            }
            if (debug.messageEnabled()) {
                debug.message("DefaultSMSGatewayImpl.sendSMSMessage() : " +
                    "HOTP sent to : " + to + ".");
            }
        } catch (Exception e) {
            debug.error("DefaultSMSGatewayImpl.sendSMSMessage() : " +
                "Exception in sending HOTP code : " , e);
            throw new AuthLoginException("Failed to send OTP code to " + to, e);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void sendEmail(String from, String to, String subject, 
        String message, String code, Map options) throws AuthLoginException {
        sendSMSMessage(from, to, subject, message, code, options);
    }

    private void setOptions(Map options) {
        smtpHostName = CollectionHelper.getMapAttr(options, SMTPHOSTNAME);
        smtpHostPort = CollectionHelper.getMapAttr(options, SMTPHOSTPORT);
        smtpUserName = CollectionHelper.getMapAttr(options, SMTPUSERNAME);
        smtpUserPassword = CollectionHelper.getMapAttr(options,
                SMTPUSERPASSWORD);
        smtpSSLEnabled = CollectionHelper.getMapAttr(options, SMTPSSLENABLED);

        if (smtpSSLEnabled != null) {
            if (smtpSSLEnabled.equals("Non SSL")) {
                sslEnabled = false;
            } else if (smtpSSLEnabled.equals("Start TLS")) {
                sslEnabled = false;
                startTls = true;
            }
        }
    }
}

