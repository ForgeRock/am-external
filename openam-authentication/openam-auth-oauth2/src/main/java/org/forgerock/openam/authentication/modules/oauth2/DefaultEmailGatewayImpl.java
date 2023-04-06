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
 * Copyright 2011-2022 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oauth2;

import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_HOSTNAME;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_PASSWORD;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_PORT;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_SSL_ENABLED;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_USERNAME;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.am.mail.application.AMSendMail;

public class DefaultEmailGatewayImpl implements EmailGateway {

    private final AMSendMail sendMail;
    private final Logger debug;
    
    String smtpHostName = null;
    String smtpHostPort = null;
    String smtpUserName = null;
    String smtpUserPassword = null;
    String smtpSSLEnabled = null;
    boolean sslEnabled = true;

    @Inject
    public DefaultEmailGatewayImpl(AMSendMail sendMail) {
        debug = LoggerFactory.getLogger(DefaultEmailGatewayImpl.class);
        this.sendMail = sendMail;
    }

   /**
    * Sends an email  message to the mail with the code
    * <p>
    *
    * @param from The address that sends the E-mail message
    * @param to The address that the E-mail message is sent
    * @param subject The E-mail subject
    * @param message The content contained in the E-mail message
    * @param options The outbound SMTP gateway
    * module
    */
    public void sendEmail(String from, String to, String subject,
                          String message, Map<String, String> options)
    throws NoEmailSentException {
        if (to == null) {
            if (debug.isDebugEnabled()) {
                debug.debug("DefaultEmailGatewayImpl::sendEmail to header is empty");
            }

            return;
        }

        try {
            setOptions(options);
            String[] tos = new String[] { to };
            
            if (smtpHostName == null || smtpHostPort == null ||
                    smtpUserName == null || smtpUserPassword == null ||
                    smtpSSLEnabled == null) {        
                sendMail.postMail(tos, subject, message, from);
                OAuthUtil.debugWarning("DefaultEmailGatewayImpl.sendEmail() :" +
                        "sending email using the defaults localhost and port 25");
            } else {
                sendMail.postMail(tos, subject, message, from, "UTF-8", smtpHostName,
                        smtpHostPort, smtpUserName, smtpUserPassword,
                        sslEnabled);
            }
            OAuthUtil.debugMessage("DefaultEmailGatewayImpl.sendEmail() : " +
                    "email sent to : " + to + ".");

        } catch (Exception ex) {
            debug.error("DefaultEmailGatewayImpl.sendEmail() : " +
                "Exception in sending email : " , ex);
            throw new NoEmailSentException(ex);
        }

    }

    private void setOptions(Map<String, String> options) {
        smtpHostName = options.get(KEY_SMTP_HOSTNAME);
        smtpHostPort = options.get(KEY_SMTP_PORT);
        smtpUserName = options.get(KEY_SMTP_USERNAME);
        smtpUserPassword = options.get(KEY_SMTP_PASSWORD);
        smtpSSLEnabled = options.get(KEY_SMTP_SSL_ENABLED);
        if (smtpSSLEnabled != null) {         
                sslEnabled = smtpSSLEnabled.equalsIgnoreCase("true");
        }
    }
}
