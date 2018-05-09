/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.framework;

import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Types;

/**
 * Creates Session hooks and provides any needed dependencies.
 */
public class SessionHookFactory {

    private final LoadingCache<Class<TreeHook>, SessionHookFactory.SessionHookProvider> sessionHookProviderCache;

    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * Constructs a SessionHookFactory.
     *
     * @param injector the Guice injector.
     */
    @Inject
    public SessionHookFactory(Injector injector) {
        this.sessionHookProviderCache = CacheBuilder.newBuilder()
                .build(CacheLoader.from(sessionHookClass -> {
                    Class<?> configClass = sessionHookClass.getAnnotation(TreeHook.Metadata.class).configClass();
                    TypeLiteral<SessionHookProvider<?>> typeLiteral = getSessionHookProviderTypeLiteral(configClass);
                    Injector childInjector = injector.createChildInjector(new FactoryModuleBuilder()
                            .implement(TreeHook.class, sessionHookClass)
                            .build(typeLiteral));
                    return childInjector.getInstance(Key.get(typeLiteral));
                }));
    }

    /**
     * Creates a TreeHook instance with the needed dependencies.
     *
     * @param sessionHookClass the class of session hook.
     * @param session the auth session.
     * @param request the request.
     * @param response the response.
     * @param config the config for the session hook class.
     * @return the TreeHook instance.
     */
    public TreeHook createSessionHook(String sessionHookClass, Session session, Request request, Response response,
                                      Object config) {
        TreeHook treeHook = null;
        try {
            Class<TreeHook> clazz = (Class<TreeHook>) Class.forName(sessionHookClass);
            SessionHookFactory.SessionHookProvider provider = sessionHookProviderCache.get(clazz);
            logger.info("creating session hook");
            treeHook = provider.create(session, request, response, config);
            logger.info("session hook created successfully");
        } catch (ClassNotFoundException | ExecutionException e) {
            logger.error("Failed to instantiate a session hook.", e);
            e.printStackTrace();
        }

        return treeHook;
    }

    @SuppressWarnings("unchecked")
    private TypeLiteral<SessionHookProvider<?>> getSessionHookProviderTypeLiteral(Class<?> configClass) {
        Type type = Types.newParameterizedTypeWithOwner(SessionHookFactory.class, SessionHookProvider.class,
                configClass);
        return (TypeLiteral<SessionHookProvider<?>>) TypeLiteral.get(type);
    }

    private interface SessionHookProvider<T> {
        TreeHook create(Session session, Request request, Response response, T config);
    }

}
