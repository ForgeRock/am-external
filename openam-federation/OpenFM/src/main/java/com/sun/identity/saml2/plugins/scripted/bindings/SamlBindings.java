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
 * Copyright 2023 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins.scripted.bindings;

import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpPreAuthenticationBindings.SamlIdpPreAuthenticationBindingsStep1;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpPreSendFailureResponseBindings.SamlIdpPreSendFailureResponseBindingsStep1;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpPreSendResponseBindings.SamlIdpPreSendResponseBindingsStep1;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlIdpPreSignResponseBindings.SamlIdpPreSignResponseBindingsStep1;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpPostSingleSignOnFailureBindings.SamlSpPostSingleSignOnFailureBindingsStep1;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpPostSingleSignOnSuccessBindings.SamlSpPostSingleSignOnSuccessBindingsStep1;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpPreSingleSignOnProcessBindings.SamlSpPreSingleSignOnProcessBindingsStep1;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpPreSingleSignOnRequestBindings.SamlSpPreSingleSignOnRequestBindingsStep1;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpUserIdLoginLogoutBindings.SamlSpUserIdLoginLogoutBindingsStep1;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpUserIdRequestResponseBindings.SamlSpUserIdRequestResponseBindingsStep1;

/**
 * A facade to access SAML Bindings.
 */
public class SamlBindings {

    /**
     * A facade to access SAML SP Bindings.
     */
    public static class SpBindings {

        /**
         * Get a new instance of a {@link SamlSpUserIdLoginLogoutBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static SamlSpUserIdLoginLogoutBindingsStep1 loginLogoutBindings() {
            return SamlSpUserIdLoginLogoutBindings.builder();
        }

        /**
         * Get a new instance of a {@link SamlSpPostSingleSignOnFailureBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static SamlSpPostSingleSignOnFailureBindingsStep1 postSingleSignOnFailure() {
            return SamlSpPostSingleSignOnFailureBindings.builder();
        }

        /**
         * Get a new instance of a {@link SamlSpPostSingleSignOnSuccessBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static SamlSpPostSingleSignOnSuccessBindingsStep1 postSingleSignOnSuccess() {
            return SamlSpPostSingleSignOnSuccessBindings.builder();
        }

        /**
         * Get a new instance of a {@link SamlSpPreSingleSignOnProcessBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static SamlSpPreSingleSignOnProcessBindingsStep1 preSingleSignOnProcess() {
            return SamlSpPreSingleSignOnProcessBindings.builder();
        }

        /**
         * Get a new instance of a {@link SamlSpPreSingleSignOnRequestBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static SamlSpPreSingleSignOnRequestBindingsStep1 preSingleSignOnRequest() {
            return SamlSpPreSingleSignOnRequestBindings.builder();
        }

        /**
         * Get a new instance of a {@link SamlSpUserIdRequestResponseBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static SamlSpUserIdRequestResponseBindingsStep1 userIdRequestBindings() {
            return SamlSpUserIdRequestResponseBindings.builder();
        }

    }

    /**
     * A facade to access SAML IDP Bindings.
     */
    public static class IdpBindings {

        /**
         * Get a new instance of a {@link SamlIdpPreAuthenticationBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static SamlIdpPreAuthenticationBindingsStep1 preAuthentication() {
            return SamlIdpPreAuthenticationBindings.builder();
        }

        /**
         * Get a new instance of a {@link SamlIdpPreSendFailureResponseBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static SamlIdpPreSendFailureResponseBindingsStep1 preSendFailureResponse() {
            return SamlIdpPreSendFailureResponseBindings.builder();
        }

        /**
         * Get a new instance of a {@link SamlIdpPreSendResponseBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static SamlIdpPreSendResponseBindingsStep1 preSendResponse() {
            return SamlIdpPreSendResponseBindings.builder();
        }

        /**
         * Get a new instance of a {@link SamlIdpPreSignResponseBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static SamlIdpPreSignResponseBindingsStep1 preSignResponse() {
            return SamlIdpPreSignResponseBindings.builder();
        }

        /**
         * Get a new instance of a {@link SamlIdpPreSingleSignOnBindings} builder.
         *
         * @return The first step of the builder.
         */
        public static BaseSamlIdpBindings.SamlIdpAdapterRequestBindingsStep1 preSingleSignOn() {
            return SamlIdpPreSingleSignOnBindings.builder();
        }

    }
}
