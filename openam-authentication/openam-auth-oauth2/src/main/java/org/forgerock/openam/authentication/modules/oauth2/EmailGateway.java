/*
 * Copyright 2011-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oauth2;

import java.util.Map;

public interface EmailGateway {
    /**
     * Sends an email  message to the mail with the code
     * <p>
     *
     * @param from The address that sends the E-mail message
     * @param to The address that the E-mail message is sent
     * @param subject The E-mail subject
     * @param message The content contained in the E-mail message
     * @param options The SMS gateway options defined in the HOTP authentication
     * module
     */
    public void sendEmail(String from, String to, String subject,
        String message, Map<String, String> options)
    throws NoEmailSentException;
}
