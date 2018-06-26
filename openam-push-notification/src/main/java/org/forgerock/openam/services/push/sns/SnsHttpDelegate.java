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
 * Copyright 2016-2017 ForgeRock AS.
 */
package org.forgerock.openam.services.push.sns;

import static org.forgerock.openam.services.push.sns.SnsMessageResourceRouteProvider.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.services.push.AbstractPushNotificationDelegate;
import org.forgerock.openam.services.push.DefaultMessageTypes;
import org.forgerock.openam.services.push.MessageType;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.predicates.Predicate;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * Delegate for communicating with SNS over HTTP.
 */
public class SnsHttpDelegate extends AbstractPushNotificationDelegate {

    private final static String AUTHENTICATE_ACTION = "authenticate";
    private final static String REGISTER_ACTION = "register";

    private final AmazonSNSClient client;
    private final SnsPushMessageConverter pushMessageConverter;
    private final String realm;
    private final MessageDispatcher messageDispatcher;

    private PushNotificationServiceConfig config;

    /**
     * Generates a new SNS HTTP Delegate, used to communicate over the Internet with
     * the SNS service.
     *
     * @param client AmazonSnsClient - used to put messages on the wire.
     * @param config Necessary to configure this delegate.
     * @param pushMessageConverter a message converter, to ensure the message sent is of the correct format.
     * @param realm the realm in which this delegate exists.
     * @param messageDispatcher the MessageDispatcher used to redirect incoming messages to their caller.
     */
    public SnsHttpDelegate(AmazonSNSClient client, PushNotificationServiceConfig config,
                           SnsPushMessageConverter pushMessageConverter, String realm,
                           MessageDispatcher messageDispatcher) {
        super();
        this.client = client;
        this.config = config;
        this.pushMessageConverter = pushMessageConverter;
        this.realm = realm;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void send(PushMessage message) {
        PublishRequest request = convertToSns(message);
        client.publish(request);
    }

    @Override
    public boolean isRequireNewDelegate(PushNotificationServiceConfig newConfig) {
        return !newConfig.equals(config);
    }

    @Override
    public void updateDelegate(PushNotificationServiceConfig newConfig) {
        //This section intentionally left blank.
    }

    @Override
    public String getMessageTypeEndpoint(MessageType messageType) {
        if (messageType == DefaultMessageTypes.REGISTER) {
            return (realm.endsWith("/") ? realm : realm + "/") + ROUTE + "?_action=" + REGISTER_ACTION;
        } else if (messageType == DefaultMessageTypes.AUTHENTICATE) {
            return (realm.endsWith("/") ? realm : realm + "/") + ROUTE + "?_action=" + AUTHENTICATE_ACTION;
        } else {
            return null;
        }
    }

    @Override
    public void startServices() throws PushNotificationException {
        //This section intentionally left blank.
    }

    @Override
    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    @Override
    public Map<MessageType, Set<Predicate>> getMessagePredicates() {
        HashMap<MessageType, Set<Predicate>> map = new HashMap<>();
        map.put(DefaultMessageTypes.REGISTER, Collections.<Predicate>singleton(new SnsRegistrationPredicate(realm)));
        map.put(DefaultMessageTypes.AUTHENTICATE, Collections.<Predicate>emptySet());
        return map;
    }

    @Override
    public void close() throws IOException {
        //This section intentionally left blank.
    }

    private PublishRequest convertToSns(PushMessage message) {

        PublishRequest request = new PublishRequest()
                .withTargetArn(message.getRecipient())
                .withSubject(message.getSubject());

        request.setMessageStructure("json");
        request.setMessage(pushMessageConverter.toTransferFormat(message));

        return request;
    }

}
