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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper.getLocalisedMessage;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.annotations.adapters.SecretPurpose;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.modules.hotp.SMSGateway;
import com.sun.identity.authentication.modules.hotp.SMSGatewayLookup;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node responsible for sending a generated One Time Password via SMS.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = OneTimePasswordSmsSenderNode.Config.class,
        tags = {"otp", "mfa", "multi-factor authentication"})
public class OneTimePasswordSmsSenderNode extends SingleOutcomeNode {

    private final Logger logger = LoggerFactory.getLogger(OneTimePasswordSmsSenderNode.class);
    private final LocaleSelector localeSelector;
    private final SMSGatewayLookup smsGatewayLookup;
    private final Realm realm;
    private final NodeUserIdentityProvider identityProvider;

    /**
     * Configuration for the node.
     */
    public interface Config extends OtpNodeBaseConfig {
        /**
         * The attribute name for storing the mobile phone number in the data store.
         *
         * @return mobile phone attribute name.
         */
        @Attribute(order = 1100, validators = {RequiredValueValidator.class})
        default String mobilePhoneAttributeName() {
            return "telephoneNumber";
        }

        /**
         * This is the attribute name used for a mobile carrier domain for sending SMS messages.
         *
         * @return mobile carrier attribute name.
         */
        @Attribute(order = 1200)
        Optional<String> mobileCarrierAttributeName();

        /**
         * The subject of the message.
         * @return the message title
         */
        @Attribute(order = 1300)
        Map<Locale, String> smsSubject();

        /**
         * The content of the message.
         *
         * <p>{@literal {{OTP}}} will be replaced with the value of the OTP.</p>
         * @return the email content
         */
        @Attribute(order = 1400)
        Map<Locale, String> smsContent();

        /**
         * The (optional) password purpose to use when the mail server is using SMTP authentication.
         * @return The password purpose.
         */
        @Override
        @Attribute(order = 550, resourceName = "secretLabelIdentifier")
        @SecretPurpose("am.authentication.nodes.otp.sms.%s.password")
        Optional<Purpose<GenericSecret>> passwordPurpose();
    }

    private final Config config;

    private final OtpNodeConnectionConfigMapper connectionConfigMapper;

    private static final String BUNDLE = OneTimePasswordSmsSenderNode.class.getName();

    /**
     * Constructs a new OneTimePasswordSmsSenderNode instance.
     *
     * @param config                  Node configuration.
     * @param realm                   The current realm.
     * @param identityProvider        The NodeUserIdentityProvider.
     * @param localeSelector          An instance of LocaleSelector.
     * @param smsGatewayLookup        lookup for {@link SMSGateway}.
     * @param connectionConfigMapper  the config mapper.
     */
    @Inject
    public OneTimePasswordSmsSenderNode(@Assisted Config config, @Assisted Realm realm,
            NodeUserIdentityProvider identityProvider, LocaleSelector localeSelector,
            SMSGatewayLookup smsGatewayLookup, OtpNodeConnectionConfigMapper connectionConfigMapper) {
        this.config = config;
        this.realm = realm;
        this.localeSelector = localeSelector;
        this.smsGatewayLookup = smsGatewayLookup;
        this.connectionConfigMapper = connectionConfigMapper;
        this.identityProvider = identityProvider;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OneTimePasswordSmsSenderNode started");
        String phone;
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());

        Optional<AMIdentity> identity = identityProvider.getAMIdentity(context.universalId,
                context.getStateFor(this));
        if (identity.isEmpty()) {
            logger.warn("identity not found");
            throw new NodeProcessException(bundle.getString("identity.failure"));
        }

        try {
            phone = getTelephoneNumber(identity.get());
            if (phone == null) {
                logger.warn("phoneNotFound");
                throw new NodeProcessException(bundle.getString("phone.not.found"));
            }
            phone = getTelephoneNumberWithCarrier(identity.get(), phone);
        } catch (IdRepoException | SSOException e) {
            logger.warn("phoneNotFound");
            throw new NodeProcessException(bundle.getString("phone.not.found"), e);
        }

        SMSGateway gateway = getSmsGateway(bundle);

        JsonValue oneTimePassword = context.getState(ONE_TIME_PASSWORD);
        if (oneTimePassword == null) {
            logger.warn("oneTimePasswordNotFound");
            throw new NodeProcessException(bundle.getString("oneTimePassword.not.found"));
        }

        try {
            logger.debug("sending SMS message from {}, to {}", config.fromEmailAddress(), phone);

            String subject = getLocalisedMessage(context, localeSelector, this.getClass(), config.smsSubject(),
                    "messageSubject");
            String content = getLocalisedMessage(context, localeSelector, this.getClass(), config.smsContent(),
                    "messageContent");
            gateway.sendSMSMessage(config.fromEmailAddress(), phone, subject, content, oneTimePassword.asString(),
                    connectionConfigMapper.asConfigMap(config, realm));
        } catch (AuthLoginException e) {
            logger.warn("SMSGateway sending error", e);
            throw new NodeProcessException(bundle.getString("sms.send.failure"), e);
        }
        return goToNext().build();
    }

    private SMSGateway getSmsGateway(ResourceBundle bundle) throws NodeProcessException {
        try {
            return smsGatewayLookup.getSmsGateway(config.smsGatewayImplementationClass());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logger.warn("SMSGateway error", e);
            throw new NodeProcessException(bundle.getString("gateway.failure"), e);
        }
    }

    private String getTelephoneNumberWithCarrier(AMIdentity identity, String phone) throws IdRepoException,
            SSOException {
        String carrierAttribute = config.mobileCarrierAttributeName().orElse("");
        return (StringUtils.isNotBlank(carrierAttribute))
                ? concatCarrierToPhoneNumber(phone, identity.getAttribute(carrierAttribute))
                : phone;
    }

    private String concatCarrierToPhoneNumber(String phone, Set<String> carriers) {
        if (carriers != null && !carriers.isEmpty()) {
            String carrier = carriers.iterator().next();
            if (carrier.startsWith("@")) {
                phone = phone.concat(carrier);
            } else {
                phone = phone.concat("@" + carrier);
            }
        }
        return phone;
    }

    /**
     * Gets the Telephone number of the user.
     *
     * @param identity The user's identity.
     * @return The user's telephone number.
     * @throws IdRepoException If there is a problem getting the user's telephone number.
     * @throws SSOException If there is a problem getting the user's telephone number.
     */
    private String getTelephoneNumber(AMIdentity identity) throws IdRepoException, SSOException {
        String telephoneAttribute = config.mobilePhoneAttributeName();
        if (StringUtils.isBlank(telephoneAttribute)) {
            telephoneAttribute = "telephoneNumber";
        }
        Set<String> telephoneNumbers = identity.getAttribute(telephoneAttribute);

        String phone = null;
        if (telephoneNumbers != null && !telephoneNumbers.isEmpty()) {
            phone = telephoneNumbers.iterator().next();
        }
        return phone;
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(USERNAME),
            new InputState(REALM),
            new InputState(ONE_TIME_PASSWORD)
        };
    }
}
