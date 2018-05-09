/*
 * Copyright 2011-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oauth2;

import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import com.iplanet.am.util.AMSendMail;

public class DefaultEmailGatewayImpl implements EmailGateway {

    protected Debug debug = null;
    
    static final String KEY_EMAIL_GWY_IMPL = "org-forgerock-auth-oauth-email-gwy-impl";
    static final String KEY_SMTP_HOSTNAME = "org-forgerock-auth-oauth-smtp-hostname";
    static final String KEY_SMTP_PORT = "org-forgerock-auth-oauth-smtp-port";
    static final String KEY_SMTP_USERNAME = "org-forgerock-auth-oauth-smtp-username";
    static final String KEY_SMTP_PASSWORD = "org-forgerock-auth-oauth-smtp-password";
    static final String KEY_SMTP_SSL_ENABLED = "org-forgerock-auth-oauth-smtp-ssl_enabled";
    
    String smtpHostName = null;
    String smtpHostPort = null;
    String smtpUserName = null;
    String smtpUserPassword = null;
    String smtpSSLEnabled = null;
    boolean sslEnabled = true;

    public DefaultEmailGatewayImpl() {
        debug = Debug.getInstance("amAuth");
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
            if (debug.messageEnabled()) {
                debug.message("DefaultEmailGatewayImpl::sendEmail to header is empty");
            }

            return;
        }

        try {
            setOptions(options);
            String tos[] = new String[] { to };
            AMSendMail sendMail = new AMSendMail();
            
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
