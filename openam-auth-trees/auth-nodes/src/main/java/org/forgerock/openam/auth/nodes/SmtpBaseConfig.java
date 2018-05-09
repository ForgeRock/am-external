/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singleton;

import java.util.Map;
import java.util.Set;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.sm.annotations.adapters.Password;

import com.google.common.collect.ImmutableMap;
import com.sun.identity.authentication.modules.hotp.DefaultSMSGatewayImpl;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Base configuration for the authentication tree nodes using SMTP.
 */
public interface SmtpBaseConfig {

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
     * The username to use when the mail server is using SMTP authentication.
     * @return Mail server authentication user name.
     */
    @Attribute(order = 400, validators = {RequiredValueValidator.class})
    String username();

    /**
     * The password to use when the mail server is using SMTP authentication.
     * @return Mail server authentication password.
     */
    @Attribute(order = 500, validators = {RequiredValueValidator.class})
    @Password
    char[] password();

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
     * Returns the SMTPBaseConfig values as a config map.
     * @return the values in this config.
     */
    default Map<String, Set<String>> asConfigMap() {
        return new ImmutableMap.Builder<String, Set<String>>()
                .put(DefaultSMSGatewayImpl.SMTPHOSTNAME, singleton(hostName()))
                .put(DefaultSMSGatewayImpl.SMTPHOSTPORT, singleton(String.valueOf(hostPort())))
                .put(DefaultSMSGatewayImpl.SMTPUSERNAME, singleton(username()))
                .put(DefaultSMSGatewayImpl.SMTPUSERPASSWORD, singleton(String.valueOf(password())))
                .put(DefaultSMSGatewayImpl.SMTPSSLENABLED, singleton(sslOption().option))
                .build();
    }

    /**
     * Represents the connection types available for SMTP.
     */
    enum SslOption {

        NON_SSL("Non SSL"),
        SSL("SSL"),
        START_TLS("Start TLS");

        String option;

        SslOption(String option) {
            this.option = option;
        }
    }
}
