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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import static java.util.Collections.singletonMap;

import org.forgerock.openam.auth.nodes.OneTimePasswordSmtpSenderNode;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A OneTimeSMTPSenderNode builder.
 */
public class OtpEmailSenderBuilder extends AbstractNodeBuilder implements OneTimePasswordSmtpSenderNode.Config {

    private String hostName = "mail.example.com";
    private int hostPort = 25;

    /**
     * A OtpEmailSenderBuilder constructor.
     */
    public OtpEmailSenderBuilder() {
        super("OTP Email Sender", OneTimePasswordSmtpSenderNode.class);
    }

    /**
     * Sets the host name.
     *
     * @param hostName the host name.
     * @return this builder.
     */
    public OtpEmailSenderBuilder hostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    /**
     * Sets the host port.
     *
     * @param hostPort the host port.
     * @return this builder.
     */
    public OtpEmailSenderBuilder hostPort(int hostPort) {
        this.hostPort = hostPort;
        return this;
    }

    @Override
    public String hostName() {
        return this.hostName;
    }

    @Override
    public int hostPort() {
        return this.hostPort;
    }

    @Override
    public String username() {
        return "admin@example.com";
    }

    @Override
    public Optional<char[]> password() {
        return Optional.of("password".toCharArray());
    }

    @Override
    public String fromEmailAddress() {
        return "admin@example.com";
    }

    @Override
    public Map<Locale, String> emailSubject() {
        return singletonMap(new Locale("en"), "Your One Time Password");
    }

    @Override
    public Map<Locale, String> emailContent() {
        return singletonMap(new Locale("en"),
                "Here is your One Time Password: '{{OTP}}'.</p><p>If you did not request this,"
                        + " please contact support.");
    }

    @Override
    public Optional<Purpose<GenericSecret>> passwordPurpose() {
        return Optional.empty();
    }
}
