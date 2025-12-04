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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD_TIMESTAMP;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.HMACKeyLengthValidator;
import org.forgerock.openam.utils.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.modules.hotp.HOTPAlgorithm;
import com.sun.identity.authentication.modules.hotp.OTPGenerator;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node which generates a One Time Password.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = OneTimePasswordGeneratorNode.Config.class,
        tags = {"otp", "mfa", "multi-factor authentication"})
public class OneTimePasswordGeneratorNode extends SingleOutcomeNode {

    private static final Logger logger = LoggerFactory.getLogger(OneTimePasswordGeneratorNode.class);

    private static final String STORE_OTP_IN_SHARED_STATE_KEY = "org.forgerock.am.auth.node.otp.inSharedState";
    //The moving factor or counter that is provided to the HTOP algorithm along with the shared secret
    private static final int MOVING_FACTOR = 0;
    //A flag that indicates if a checksum digit should be appended to the OTP.
    private static final boolean ADD_CHECKSUM = false;
    //The offset into the MAC result to begin truncation.
    private static final int TRUNCATION_OFFSET = 16;
    private final SecureRandom secureRandom;
    private final Config config;
    private final OTPGenerator otpGenerator;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * Specifies the length of the One Time Password to generate.
         *
         * @return the configured One Time Password length.
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class, HMACKeyLengthValidator.class})
        default int length() {
            return 8;
        }
    }

    /**
     * Constructs a new OneTimePasswordGeneratorNode instance.
     *
     * @param config Node configuration.
     * @param secureRandom SecureRandom instance.
     * @param hotpGenerator A class to generate One Time Passwords.
     */
    @Inject
    public OneTimePasswordGeneratorNode(@Assisted Config config, SecureRandom secureRandom,
                                        HOTPAlgorithm hotpGenerator) {
        this.secureRandom = secureRandom;
        this.config = config;
        this.otpGenerator = hotpGenerator;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OneTimePasswordGeneratorNode started");
        String otp;
        try {
            logger.debug("generating one time password");
            // The shared secret is a generated long from a secure random, so no point in a moving factor here
            otp = otpGenerator.generateOTP(getSharedSecret(), MOVING_FACTOR, config.length(), ADD_CHECKSUM,
                    TRUNCATION_OFFSET);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.warn("Error generating HOTP password.");
            throw new NodeProcessException(e);
        }
        Action.ActionBuilder builder = goToNext();
        long otpTimestamp = Time.getClock().instant().getEpochSecond();
        if (SystemProperties.getAsBoolean(STORE_OTP_IN_SHARED_STATE_KEY, false)) {
            JsonValue newSharedState = context.sharedState.copy();
            newSharedState.put(ONE_TIME_PASSWORD, otp);
            newSharedState.put(ONE_TIME_PASSWORD_TIMESTAMP, otpTimestamp);
            builder.replaceSharedState(newSharedState);
        }
        JsonValue newTransientState = context.transientState.copy();
        newTransientState.put(ONE_TIME_PASSWORD, otp);
        newTransientState.put(ONE_TIME_PASSWORD_TIMESTAMP, otpTimestamp);
        builder.replaceTransientState(newTransientState);
        logger.debug("one time password has been generated successfully");
        return builder.build();
    }

    private byte[] getSharedSecret() {
        return Long.toHexString(secureRandom.nextLong()).getBytes();
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(ONE_TIME_PASSWORD),
            new OutputState(ONE_TIME_PASSWORD_TIMESTAMP)
        };
    }
}
