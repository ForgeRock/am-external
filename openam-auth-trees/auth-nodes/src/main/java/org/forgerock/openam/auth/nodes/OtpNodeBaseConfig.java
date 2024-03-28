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
 * Copyright 2017-2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import java.util.Optional;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;

import com.sun.identity.sm.RequiredValueValidator;

/**
 * Base configuration for the authentication tree nodes using SMTP.
 */
public interface OtpNodeBaseConfig {

    /**
     * The name of the mail server.
     * @return Mail server host name.
     */
    @Attribute(order = 200, validators = {RequiredValueValidator.class})
    String hostName();

    /**
     * The port of the mail server. The default port for SMTP is 25,
     * if using SSL the default port is 465.
     * @return Mail server host port.
     */
    @Attribute(order = 300, validators = {RequiredValueValidator.class})
    int hostPort();

    /**
     * The (optional) username to use when the mail server is using SMTP authentication.
     * @return Mail server authentication user name.
     */
    @Attribute(order = 400)
    String username();

    /**
     * The (optional) password to use when the mail server is using SMTP authentication.
     * @return Mail server authentication password.
     */
    @Attribute(order = 500)
    @Deprecated
    Optional<char[]> password();


    /**
     * The (optional) password purpose to use when the mail server is using SMTP authentication.
     *
     * @return The password purpose.
     */
    Optional<Purpose<GenericSecret>> passwordPurpose();

    /**
     * Emails from the HOTP Authentication module will come from this address.
     * @return Email from address.
     */
    @Attribute(order = 600, validators = {RequiredValueValidator.class})
    String fromEmailAddress();

    /**
     * This setting controls whether the authentication module
     * communicates with the mail server using SSL/TLS.
     * @return Mail server secure connection.
     */
    @Attribute(order = 2100, validators = {RequiredValueValidator.class})
    default SslOption sslOption() {
        return SslOption.SSL;
    }

    /**
     * The HOTP authentication module uses this class to send SMS messages.
     * The SMS gateway class must implement the following interface
     * com.sun.identity.authentication.modules.hotp.SMSGateway
     * @return SMS gateway implementation class name.
     */
    @Attribute(order = 2200, validators = {RequiredValueValidator.class})
    default String smsGatewayImplementationClass() {
        return "com.sun.identity.authentication.modules.hotp.DefaultSMSGatewayImpl";
    }

    /**
     * Represents the connection types available for SMTP.
     */
    enum SslOption {

        /** Non SSL. */
        NON_SSL("Non SSL"),
        /** SSL. */
        SSL("SSL"),
        /** Start Tls. */
        START_TLS("Start TLS");

        String option;

        SslOption(String option) {
            this.option = option;
        }
    }
}
