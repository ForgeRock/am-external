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
 * Copyright 2019 ForgeRock AS.
 */
package org.forgerock.openam.federation.rest.guice;

import java.lang.reflect.Type;
import java.security.cert.X509Certificate;
import java.util.function.Function;

import javax.inject.Named;
import javax.xml.bind.JAXBIntrospector;

import org.forgerock.openam.objectenricher.ObjectEnricher;
import org.forgerock.openam.objectenricher.service.AnnotationObjectEnricher;
import org.forgerock.util.Pair;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.sun.identity.saml2.key.KeyUtil;

/**
 * Guice module for the federation REST endpoints.
 */
public class RestFederationGuiceModule extends AbstractModule {

    /**
     * The name of the object mapper for used converting saml2 entity provider POJOs.
     */
    public static final String SAML2_ENTITY_PROVIDER_MAPPER = "Saml2EntityProviderMapper";

    @Override
    protected void configure() {
    }

    /**
     * Returns the default annotation based object enricher implementation.
     *
     * @param injector used to look up object instances
     * @return The default annotation based object enricher
     */
    @Provides
    @Singleton
    public ObjectEnricher getJaxbAnnotationObjectEnricher(Injector injector) {
        return new AnnotationObjectEnricher((object, type) -> {
            Object wrappedValue = JAXBIntrospector.getValue(object);
            Type wrappedType = (wrappedValue == object) ? type : wrappedValue.getClass();
            return Pair.of(wrappedValue, wrappedType);
        }, injector::getInstance);
    }

    /**
     * Returns the default object mapper for converting the POJOs to json.
     *
     * @return The default object mapper.
     */
    @Provides
    @Singleton
    @Named(SAML2_ENTITY_PROVIDER_MAPPER)
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    /**
     * Retrieves the function to map from an alias key to its corresponding certificate.
     *
     * @return alias key to certificate function
     */
    @Provides
    @Named("certificateFunction")
    public Function<String, X509Certificate> getKeyAliasToCertificateFunction() {
        return KeyUtil.getKeyProviderInstance()::getX509Certificate;
    }

}
