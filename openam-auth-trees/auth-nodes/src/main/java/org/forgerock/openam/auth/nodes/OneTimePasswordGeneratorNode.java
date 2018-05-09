/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.Time;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.modules.hotp.HOTPAlgorithm;
import com.sun.identity.authentication.modules.hotp.OTPGenerator;
import com.sun.identity.sm.RequiredValueValidator;
import org.forgerock.openam.auth.nodes.validators.HMACKeyLengthValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node which generates a One Time Password.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = OneTimePasswordGeneratorNode.Config.class)
public class OneTimePasswordGeneratorNode extends SingleOutcomeNode {

    private static final Logger logger = LoggerFactory.getLogger("amAuth");
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
        JsonValue newSharedState = context.sharedState.copy();
        newSharedState.put(ONE_TIME_PASSWORD, otp);
        newSharedState.put(ONE_TIME_PASSWORD_TIMESTAMP, Time.getClock().instant().getEpochSecond());
        logger.debug("one time password has been generated successfully");
        return goToNext().replaceSharedState(newSharedState).build();
    }

    private byte[] getSharedSecret() {
        return Long.toHexString(secureRandom.nextLong()).getBytes();
    }
}