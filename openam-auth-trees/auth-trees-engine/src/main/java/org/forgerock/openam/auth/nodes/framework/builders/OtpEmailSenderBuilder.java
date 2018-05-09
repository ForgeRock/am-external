/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework.builders;

import org.forgerock.openam.auth.nodes.OneTimePasswordSmtpSenderNode;

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
    public char[] password() {
        return "password".toCharArray();
    }

    @Override
    public String fromEmailAddress() {
        return "admin@example.com";
    }
}
