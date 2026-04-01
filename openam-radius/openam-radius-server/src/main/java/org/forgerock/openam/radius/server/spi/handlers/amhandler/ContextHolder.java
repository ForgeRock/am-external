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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 *
 * Portions Copyright 2015-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server.spi.handlers.amhandler;

import javax.security.auth.callback.Callback;

import org.forgerock.am.trees.model.TreeResult;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.PagePropertiesCallback;

/**
 * Holds server side info for an authentication conversation in progress for a user via a radius client.
 * <p/>
 */
public class ContextHolder {

    /**
     * Debug logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ContextHolder.class);

    /**
     * The page properties callback class that encapsulates properties related to the callback set including some
     * attributes of the Callbacks element such as timeout, header, template, image, and error.
     */
    private PagePropertiesCallback callbackSetProps = null;

    /**
     * Indicates what phase of authentication we are in. The flow is the same as the order of declaration.
     */
    public enum AuthPhase {
        /**
         * The auth phase is starting.
         */
        STARTING,

        /**
         * The auth phase is that in which the user must add input.
         */
        GATHERING_INPUT,

        /**
         * The auth phase is in its final stage.
         */
        FINALIZING,

        /**
         * The auth phase has terminated.
         */
        TERMINATED
    }

    /**
     * The name of the module instance.
     */
    private String moduleName = null;

    /**
     * The zero based index of the current module in the chain for whom we are gather values from the user. Initialized
     * to -1 so that we can centralize updating this info set without regard to whether we are handling the first set or
     * following sets.
     */
    private int chainModuleIndex = -1;

    /**
     * The current set of callbacks being fulfilled by a user through radius.
     */
    private Callback[] callbacks = null;

    /**
     * The zero based index of the current callback (field) whose requirement is being sought through a RADIUS
     * accessChallenge response excluding the undeclared PagePropertiesCallback object that is always first in the array
     * of callbacks and contains the header for the html page in which the fields for this set of callbacks are
     * presented for html clients.
     */
    private int idxOfCurrentCallback = 0;

    /**
     * The zero based index of the set of callbacks within a given module. Modules can have more than one set of
     * callback with a single set translating to a single web page when openam is used for web authentication.
     */
    private int idxOfCallbackSetInModule = 0;

    /**
     * The context object being held in cache.
     */
    private AuthContext authContext;

    /**
     * The key for this object in the server-side cache.
     */
    private final String cacheKey;

    /**
     * Indicates in which phase of authentication we are at any point in time.
     */
    private AuthPhase authPhase = AuthPhase.STARTING;

    private TreeResult treeResult;

    private ExternalRequestContext requestContext;

    /**
     * Constructs a ContextHolder with its unique key.
     *
     * @param key the unique cache key for the context holder
     */
    public ContextHolder(String key) {
        this.cacheKey = key;
    }

    /**
     * get the callback set properties.
     *
     * @return the callback properties for the current module.
     */
    public PagePropertiesCallback getCallbackSetProps() {
        return this.callbackSetProps;
    }

    /**
     * Sets the callback properties for the module currently being processed.
     *
     * @param callbackSetProps - a <code>PagePropertiesCallback</code> reference.
     */
    public void setCallbackSetProps(PagePropertiesCallback callbackSetProps) {
        this.callbackSetProps = callbackSetProps;
    }

    /**
     * Gets the name of the module.
     *
     * @return the name of the module currently being processed, or null if no module is being processed.
     */
    public String getModuleName() {
        return this.moduleName;
    }

    /**
     * Sets the name of the module currently being processed.
     *
     * @param moduleName - a non null string containing the name of the module.
     */
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Increments the chain module index to indicate that the next module in the chain is currently being processed.
     */
    public void incrementChainModuleIndex() {
        ++chainModuleIndex;
    }

    /**
     * Get the chain module index.
     *
     * @return The zero based index of the current module in the chain for whom we are gathering values from the user.
     * -1 indicates that there is no module set. This is so that we can centralize updating this info set without regard
     * to whether we are handling the first set or following sets.
     */
    public int getChainModuleIndex() {
        return this.chainModuleIndex;
    }

    /**
     * Gets the array of callback objects for the chain.
     *
     * @return an array of <code>Callback</code> objects.
     */
    public Callback[] getCallbacks() {
        return this.callbacks;
    }

    /**
     * Sets the array of <code>Callback</code> objects for the chain.
     *
     * @param callbacks an array of <code>Callback</code> objects.
     */
    public void setCallbacks(Callback[] callbacks) {
        this.callbacks = callbacks;
    }

    /**
     * Get the index of the current callback.
     *
     * @return the index of the current callback.
     */
    public int getIdxOfCurrentCallback() {
        return this.idxOfCurrentCallback;
    }

    /**
     * set the current callback index.
     *
     * @param idxOfCurrentCallback The index of the current callback.
     */
    public void setIdxOfCurrentCallback(int idxOfCurrentCallback) {
        this.idxOfCurrentCallback = idxOfCurrentCallback;
    }

    /**
     * Increment the current callback index.
     */
    public void incrementIdxOfCurrentCallback() {
        ++idxOfCurrentCallback;
    }

    /**
     * gets the index of the callback set in the module.
     *
     * @return the index of the callback set in the module.
     */
    public int getIdxOfCallbackSetInModule() {
        return this.idxOfCallbackSetInModule;
    }

    /**
     * Set the index of the callback set in the module.
     *
     * @param idxOfCallbackSetInModule the index of the callback set in the module.
     */
    public void setIdxOfCallbackSetInModule(int idxOfCallbackSetInModule) {
        this.idxOfCallbackSetInModule = idxOfCallbackSetInModule;
    }

    /**
     * Increments the index of the callback set in the module.
     */
    public void incrementIdxOfCallbackSetInModule() {
        ++this.idxOfCallbackSetInModule;
    }

    /**
     * get the auth context.
     *
     * @return a <code>AuthContext</code> object.
     */
    public AuthContext getAuthContext() {
        return this.authContext;
    }

    /**
     * set the auth context.
     *
     * @param authContext a <code>AuthContext</code> object
     */
    public void setAuthContext(AuthContext authContext) {
        this.authContext = authContext;
    }

    /**
     * get the cache key of this context holder.
     *
     * @return a <code>String</code> object containing the key for the context holder.
     */
    public String getCacheKey() {
        return this.cacheKey;
    }

    /**
     * get the auth phase of the context holder.
     *
     * @return an <code>AuthPhase</code> object.
     */
    public AuthPhase getAuthPhase() {
        return this.authPhase;
    }

    /**
     * Set the auth phase of the context holder.
     *
     * @param authPhase an <code>AuthPhase</code> object.
     */
    public void setAuthPhase(AuthPhase authPhase) {
        this.authPhase = authPhase;
    }

    /**
     * Get the current {@link TreeResult}.
     *
     * @return the current {@link TreeResult}
     */
    public TreeResult getTreeResult() {
        return treeResult;
    }

    /**
     * Set the current {@link TreeResult}.
     *
     * @param treeResult the {@link TreeResult} to set
     */
    public void setTreeResult(TreeResult treeResult) {
        this.treeResult = treeResult;
    }

    /**
     * Get the current {@link ExternalRequestContext}.
     *
     * @return the current {@link ExternalRequestContext}
     */
    public ExternalRequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * Set the current {@link ExternalRequestContext}.
     *
     * @param requestContext the {@link ExternalRequestContext} to set
     */
    public void setRequestContext(ExternalRequestContext requestContext) {
        this.requestContext = requestContext;
    }
}
