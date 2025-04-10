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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.forgerock.http.header.SetCookieHeader;
import org.forgerock.http.protocol.Cookie;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.sm.annotations.adapters.TimeUnit;
import org.forgerock.openam.utils.Time;

import com.google.inject.assistedinject.Assisted;

/**
 * A node to instruct the tree to create a custom {@link Cookie} during processing (without using a Tree Hook).
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = SetCustomCookieNode.Config.class,
        tags = {"contextual"})
public class SetCustomCookieNode extends SingleOutcomeNode {

    private static final String SET_COOKIE_HEADER_KEY = "Set-Cookie";

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The name of the custom cookie.
         *
         * @return the name of the custom cookie.
         */
        @Attribute(order = 100, requiredValue = true)
        String name();

        /**
         * The value of the custom cookie.
         *
         * @return the value of the custom cookie.
         */
        @Attribute(order = 200, requiredValue = true)
        String value();

        /**
         * The maximum permitted lifetime of the cookie in seconds. The cookie becomes invalid after this amount of
         * time.
         *
         * @return the max age in seconds.
         */
        @Attribute(order = 300)
        @TimeUnit(SECONDS)
        Optional<Duration> maxAge();

        /**
         * The domain of the custom cookie.
         *
         * @return the domain of the custom cookie.
         */
        @Attribute(order = 400)
        Optional<String> domain();

        /**
         * The path of the custom cookie.
         *
         * @return the path of the custom cookie.
         */
        @Attribute(order = 500)
        Optional<String> path();

        /**
         * If true, instructs the browser to only send the cookie on secure connections.
         *
         * @return true to use secure cookie.
         */
        @Attribute(order = 600)
        default Boolean useSecureCookie() {
            return false;
        }

        /**
         * If true, instructs the browser to prevent access to this cookie and only use it for http.
         *
         * @return true to use http only cookie.
         */
        @Attribute(order = 700)
        default Boolean useHttpOnlyCookie() {
            return false;
        }

        /**
         * Same Site property of the custom cookie.
         *
         * @return the same site flag of the custom cookie.
         */
        @Attribute(order = 800)
        default Cookie.SameSite sameSite() {
            return Cookie.SameSite.LAX;
        }
    }

    private final Config config;

    /**
     * The SetCustomCookieNode constructor.
     *
     * @param config The service config.
     */
    @Inject
    public SetCustomCookieNode(@Assisted Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        context.request.servletResponse.addHeader(SET_COOKIE_HEADER_KEY,
                new SetCookieHeader(Collections.singletonList(makeCookieForResponse())).getFirstValue());

        return goToNext().build();
    }

    private Cookie makeCookieForResponse() {
        Cookie cookie = new Cookie();
        cookie.setName(config.name());
        cookie.setValue(config.value());
        config.path().ifPresent(cookie::setPath);
        config.domain().ifPresent(cookie::setDomain);
        config.maxAge().ifPresent(age -> {
            cookie.setMaxAge((int) age.toSeconds());
            cookie.setExpires(Date.from(Time.instant().plus(age)));
        });
        cookie.setSecure(config.useSecureCookie());
        cookie.setHttpOnly(config.useHttpOnlyCookie());
        cookie.setSameSite(config.sameSite());

        return cookie;
    }

}
