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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.services.push.sns;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.services.push.PushNotificationDelegateFactory;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.sns.utils.SnsClientFactory;

import com.amazonaws.services.sns.AmazonSNS;

/**
 * Produces SnsHttpDelegates matching the PushNotificationServiceFactory interface.
 */
public class SnsHttpDelegateFactory implements PushNotificationDelegateFactory {

    private final SnsPushMessageConverter pushMessageConverter;
    private final Secrets secrets;
    private final RealmLookup realmLookup;

    /**
     * Default constructor sets the debug for passing into produced delegates.
     */
    public SnsHttpDelegateFactory() {
        this.pushMessageConverter  = InjectorHolder.getInstance(SnsPushMessageConverter.class);
        this.secrets = InjectorHolder.getInstance(Secrets.class);
        this.realmLookup = InjectorHolder.getInstance(RealmLookup.class);
    }

    @Override
    public SnsHttpDelegate produceDelegateFor(PushNotificationServiceConfig.Realm config, String realm,
                                              MessageDispatcher messageDispatcher) {
        AmazonSNS service = new SnsClientFactory(secrets, realmLookup).produce(config, realm);
        return new SnsHttpDelegate(service, config, pushMessageConverter, realm, messageDispatcher);
    }

}
