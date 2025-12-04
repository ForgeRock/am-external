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
package org.forgerock.openam.auth.nodes.example;

import static com.sun.identity.shared.Constants.AUTHENTICATION_CHAINS_ENABLED_PROPERTY;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Locale.ENGLISH;
import static org.forgerock.am.trees.api.Outcome.TREE_NODE_FAILURE_ID;
import static org.forgerock.am.trees.api.Outcome.TREE_NODE_SUCCESS_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.FALSE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.TRUE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE;
import static org.forgerock.openam.auth.nodes.NodesPlugin.NODES_PLUGIN_VERSION;
import static org.forgerock.openam.auth.nodes.PatchObjectNode.PatchObjectOutcome.FAILURE;
import static org.forgerock.openam.auth.nodes.PatchObjectNode.PatchObjectOutcome.PATCHED;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNode.SocialAuthOutcome.ACCOUNT_EXISTS;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNode.SocialAuthOutcome.NO_ACCOUNT;
import static org.forgerock.openam.auth.nodes.builders.RetryLimitDecisionBuilder.REJECT_OUTCOME;
import static org.forgerock.openam.auth.nodes.builders.RetryLimitDecisionBuilder.RETRY_OUTCOME;
import static org.forgerock.openam.auth.nodes.framework.FrameworkNodesPlugin.FRAMEWORK_NODES_VERSION;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_PASSWORD_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.nodes.plugin.IdmIntegrationNodesPlugin.IDM_NODES_PLUGIN_VERSION;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.KeyGenerator;
import javax.inject.Inject;

import org.forgerock.am.trees.api.NodeBuilder;
import org.forgerock.am.trees.api.TreeBuilderFactory;
import org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode;
import org.forgerock.openam.auth.nodes.AgentDataStoreDecisionNode;
import org.forgerock.openam.auth.nodes.AttributeCollectorNode;
import org.forgerock.openam.auth.nodes.CreateObjectNode;
import org.forgerock.openam.auth.nodes.DataStoreDecisionNode;
import org.forgerock.openam.auth.nodes.KbaCreateNode;
import org.forgerock.openam.auth.nodes.LoginCountDecisionNode;
import org.forgerock.openam.auth.nodes.NodesPlugin;
import org.forgerock.openam.auth.nodes.PasswordCollectorNode;
import org.forgerock.openam.auth.nodes.UsernameCollectorNode;
import org.forgerock.openam.auth.nodes.ValidatedPasswordNode;
import org.forgerock.openam.auth.nodes.ValidatedUsernameNode;
import org.forgerock.openam.auth.nodes.ZeroPageLoginNode;
import org.forgerock.openam.auth.nodes.amster.AmsterJwtDecisionNode;
import org.forgerock.openam.auth.nodes.builders.AcceptTermsAndConditionsBuilder;
import org.forgerock.openam.auth.nodes.builders.AccountLockoutBuilder;
import org.forgerock.openam.auth.nodes.builders.AgentDataStoreDecisionBuilder;
import org.forgerock.openam.auth.nodes.builders.AmsterJwtDecisionNodeBuilder;
import org.forgerock.openam.auth.nodes.builders.AnonymousUserBuilder;
import org.forgerock.openam.auth.nodes.builders.AttributeCollectorBuilder;
import org.forgerock.openam.auth.nodes.builders.AttributePresentDecisionBuilder;
import org.forgerock.openam.auth.nodes.builders.CreateObjectBuilder;
import org.forgerock.openam.auth.nodes.builders.CreatePasswordBuilder;
import org.forgerock.openam.auth.nodes.builders.DataStoreBuilder;
import org.forgerock.openam.auth.nodes.builders.EmailSuspendBuilder;
import org.forgerock.openam.auth.nodes.builders.HotpGeneratorBuilder;
import org.forgerock.openam.auth.nodes.builders.IdentifyExistingUserBuilder;
import org.forgerock.openam.auth.nodes.builders.IncrementLoginCountBuilder;
import org.forgerock.openam.auth.nodes.builders.InnerTreeEvaluatorBuilder;
import org.forgerock.openam.auth.nodes.builders.KbaCreateBuilder;
import org.forgerock.openam.auth.nodes.builders.LoginCountDecisionBuilder;
import org.forgerock.openam.auth.nodes.builders.OtpDecisionBuilder;
import org.forgerock.openam.auth.nodes.builders.OtpEmailSenderBuilder;
import org.forgerock.openam.auth.nodes.builders.PageNodeBuilder;
import org.forgerock.openam.auth.nodes.builders.PasswordCollectorBuilder;
import org.forgerock.openam.auth.nodes.builders.PatchObjectBuilder;
import org.forgerock.openam.auth.nodes.builders.PersistentCookieDecisionBuilder;
import org.forgerock.openam.auth.nodes.builders.ProvisionDynamicAccountBuilder;
import org.forgerock.openam.auth.nodes.builders.ProvisionIdmAccountBuilder;
import org.forgerock.openam.auth.nodes.builders.QueryFilterDecisionBuilder;
import org.forgerock.openam.auth.nodes.builders.RetryLimitDecisionBuilder;
import org.forgerock.openam.auth.nodes.builders.SessionDataBuilder;
import org.forgerock.openam.auth.nodes.builders.SetPersistentCookieBuilder;
import org.forgerock.openam.auth.nodes.builders.SetSuccessUrlBuilder;
import org.forgerock.openam.auth.nodes.builders.SocialFacebookBuilder;
import org.forgerock.openam.auth.nodes.builders.SocialGoogleBuilder;
import org.forgerock.openam.auth.nodes.builders.UsernameCollectorBuilder;
import org.forgerock.openam.auth.nodes.builders.ValidatedPasswordBuilder;
import org.forgerock.openam.auth.nodes.builders.ValidatedUsernameBuilder;
import org.forgerock.openam.auth.nodes.builders.ZeroPageLoginBuilder;
import org.forgerock.openam.auth.nodes.framework.FrameworkNodesPlugin;
import org.forgerock.openam.auth.nodes.framework.PageNode;
import org.forgerock.openam.auth.trees.engine.AuthenticationTreesServiceConfig;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.homedirectory.AMHomeDirectoryService;
import org.forgerock.openam.integration.idm.nodes.plugin.IdmIntegrationNodesPlugin;
import org.forgerock.openam.plugins.AmPlugin;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.PluginTools;
import org.forgerock.openam.plugins.StartupType;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.am.util.SystemPropertiesWrapper;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.SMSException;

/** Plugin for nodes that are tightly coupled to the tree engine. */
public class ExampleTreesPlugin implements AmPlugin {

    private static final String ENCODING_TYPE = "AES";
    private static final int NUMBER_OF_SIGNED_BITS = 256;
    private static final String CLIENT_SECRET_VALUE = "anExampleClientSecret";
    private static final int NODE_VERSION = 1;

    private final Logger logger = LoggerFactory.getLogger(ExampleTreesPlugin.class);
    private final TreeBuilderFactory treeBuilderFactory;
    private final Realm realm;
    private final SystemPropertiesWrapper systemProperties;
    private final PluginTools pluginTools;
    private final AnnotatedServiceRegistry serviceRegistry;

    /**
     * DI-enabled constructor.
     * @param treeBuilderFactory the tree builder factory
     * @param systemProperties the SystemPropertiesWrapper object
     * @param pluginTools The PluginTools.
     * @param serviceRegistry The AnnotatedServiceRegistry.
     */
    @Inject
    public ExampleTreesPlugin(TreeBuilderFactory treeBuilderFactory, SystemPropertiesWrapper systemProperties,
            PluginTools pluginTools, AnnotatedServiceRegistry serviceRegistry) {
        this.treeBuilderFactory = treeBuilderFactory;
        this.systemProperties = systemProperties;
        this.pluginTools = pluginTools;
        this.serviceRegistry = serviceRegistry;
        this.realm = Realms.root();
    }

    @Override
    public String getPluginVersion() {
        return "1.0.0";
    }

    @Override
    public void onInstall() throws PluginException {
        if (!systemProperties.getAsBoolean(AUTHENTICATION_CHAINS_ENABLED_PROPERTY, false)) {
            createDefaultTrees();
        }
    }

    private void createDefaultTrees() throws PluginException {
        try {
            ensureServicesStarted(
                    ZeroPageLoginNode.class,
                    UsernameCollectorNode.class,
                    PasswordCollectorNode.class,
                    DataStoreDecisionNode.class,
                    PageNode.class,
                    AmsterJwtDecisionNode.class,
                    AgentDataStoreDecisionNode.class);

            installLdapServiceTree();
            installAgentTree();
            installAmsterServiceTree();
        } catch (SMSException | SSOException e) {
            logger.error("failed to create ldapService tree", e);
            throw new PluginException("Could not create an ldapService tree", e);
        }
    }

    @Override
    public Map<Class<? extends AmPlugin>, String> getDependencies() {
        return Map.of(
                NodesPlugin.class, "[31.0.0,%s]".formatted(NODES_PLUGIN_VERSION),
                FrameworkNodesPlugin.class, "[1.0.0,%s]".formatted(FRAMEWORK_NODES_VERSION),
                IdmIntegrationNodesPlugin.class, "[1.0.0,%s]".formatted(IDM_NODES_PLUGIN_VERSION)
        );
    }

    private void ensureServicesStarted(Class<?>... nodeClasses) throws PluginException {
        for (Class<?> nodeClass : nodeClasses) {
            pluginTools.startAuthNode(nodeClass);
        }
    }

    private boolean doesTreeExist(String treeName) throws SMSException, SSOException {
        if (serviceRegistry.getRealmInstance(AuthenticationTreesServiceConfig.Realm.class, realm, treeName)
                .isPresent()) {
            logger.info("'{}' tree already exists, skipping creation", treeName);
            return true;
        }
        return false;
    }

    @Override
    public void onStartup(StartupType startupType) throws PluginException {
        if (startupType == StartupType.FIRST_TIME_DEMO_INSTALL) {
            try {
                installExampleTree();
                installRetryLimitTree();
                installPersistentCookieTree();
                installHmacOneTimePasswordTree();
                installSocialAuthFacebookIDMTree();
                installSocialAuthGoogleAnonymousUserTree();
                installSocialAuthGoogleDynamicProfileWithPasswordTree();
                installPlatformRegistrationTree();
                installPlatformProgressiveProfileTree();
                installPlatformLoginTree();
                installPlatformForgottenUsernameTree();
                installPlatformResetPasswordTree();
                installPlatformUpdatePassword();
            } catch (SMSException | SSOException | IOException e) {
                logger.error("failed to create example trees", e);
                throw new PluginException("Could not create an example tree", e);
            }
        }

        if ((startupType == StartupType.FBC_FIRST_STARTUP
                || startupType == StartupType.FIRST_TIME_DEMO_INSTALL)
                && !systemProperties.getAsBoolean(AUTHENTICATION_CHAINS_ENABLED_PROPERTY, false)) {
            createDefaultTrees();
        }
    }

    private void installLdapServiceTree() throws SSOException, SMSException {
        String treeName = "ldapService";

        if (doesTreeExist(treeName)) {
            return;
        }

        NodeBuilder usernameNode = new UsernameCollectorBuilder().uuid("cfcd2084-95d5-35ef-a6e7-dff9f98764db");
        NodeBuilder passwordNode = new PasswordCollectorBuilder().uuid("c4ca4238-a0b9-3382-8dcc-509a6f75849c");
        NodeBuilder dataStoreDecisionNode = new DataStoreBuilder().uuid("c81e728d-9d4c-3f63-af06-7f89cc14862d");
        NodeBuilder zeroPageLoginNode = new ZeroPageLoginBuilder().uuid("eccbc87e-4b5c-32fe-a830-8fd9f2a7baf5");
        NodeBuilder pageNode = new PageNodeBuilder()
                .pageHeader(Map.of(ENGLISH, "Sign In"))
                .pageDescription(Map.of(ENGLISH, ""))
                .addNode(usernameNode, UsernameCollectorNode.class.getSimpleName(), NODE_VERSION,
                        UsernameCollectorBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .addNode(passwordNode, PasswordCollectorNode.class.getSimpleName(), NODE_VERSION,
                        PasswordCollectorBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .uuid("6c8349cc-7260-3e62-a3b1-396831a8398a");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .add(zeroPageLoginNode, pageNode, dataStoreDecisionNode)
                .entryNode(zeroPageLoginNode)
                .connect(zeroPageLoginNode, TRUE_OUTCOME_ID, dataStoreDecisionNode)
                .connect(zeroPageLoginNode, FALSE_OUTCOME_ID, pageNode)
                .connect(pageNode, dataStoreDecisionNode)
                .connect(dataStoreDecisionNode, TRUE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(dataStoreDecisionNode, FALSE_OUTCOME_ID, TREE_NODE_FAILURE_ID)
                .build();

        logger.debug("created the ldapService tree {}", treeName);
    }

    private void installAmsterServiceTree() throws SSOException, SMSException {
        String treeName = "amsterService";

        if (doesTreeExist(treeName)) {
            return;
        }

        NodeBuilder amsterJwtNode = new AmsterJwtDecisionNodeBuilder()
                .authorizedKeys(Path.of(AMHomeDirectoryService.getHomeDirectory().getAmsterKeysDir(),
                        "authorized_keys").toString())
                .uuid("cfcd2084-95d5-35ef-a6e7-d7f9f98764db");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .add(amsterJwtNode)
                .entryNode(amsterJwtNode)
                .connect(amsterJwtNode, TRUE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(amsterJwtNode, FALSE_OUTCOME_ID, TREE_NODE_FAILURE_ID)
                .build();

        logger.debug("created the amsterService tree {}", treeName);
    }

    private void installExampleTree() throws SSOException, SMSException, IOException {
        String treeName = "Example";

        NodeBuilder usernameNode = new UsernameCollectorBuilder().uuid("cfcd2084-95d5-35ef-a6e7-dff9f98764da");
        NodeBuilder passwordNode = new PasswordCollectorBuilder().uuid("c4ca4238-a0b9-3382-8dcc-509a6f75849b");
        NodeBuilder dataStoreDecisionNode = new DataStoreBuilder().uuid("c81e728d-9d4c-3f63-af06-7f89cc14862c");
        NodeBuilder zeroPageLoginNode = new ZeroPageLoginBuilder().uuid("eccbc87e-4b5c-32fe-a830-8fd9f2a7baf3");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .add(usernameNode, passwordNode, dataStoreDecisionNode, zeroPageLoginNode)
                .entryNode(zeroPageLoginNode)
                .connect(zeroPageLoginNode, TRUE_OUTCOME_ID, dataStoreDecisionNode)
                .connect(zeroPageLoginNode, FALSE_OUTCOME_ID, usernameNode)
                .connect(usernameNode, passwordNode)
                .connect(passwordNode, dataStoreDecisionNode)
                .connect(dataStoreDecisionNode, TRUE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(dataStoreDecisionNode, FALSE_OUTCOME_ID, TREE_NODE_FAILURE_ID)
                .build();

        logger.debug("created the example tree {}", treeName);
    }

    private void installAgentTree() throws SSOException, SMSException {
        String treeName = "Agent";

        if (doesTreeExist(treeName)) {
            return;
        }

        NodeBuilder usernameNode = new UsernameCollectorBuilder().uuid("6c40132d-c3a5-492e-86b6-23f7978c8d47");
        NodeBuilder passwordNode = new PasswordCollectorBuilder().uuid("5494a939-cb47-44c2-b667-d6db0a9f2d55");
        NodeBuilder agentDataStoreDecisionNode = new AgentDataStoreDecisionBuilder()
                .uuid("a87ff679-a2f3-371d-9181-a67b7542122c");
        NodeBuilder zeroPageLoginNode = new ZeroPageLoginBuilder().uuid("e4da3b7f-bbce-3345-9777-2b0674a318d5");
        NodeBuilder pageNode = new PageNodeBuilder()
                .pageHeader(Map.of())
                .pageDescription(Map.of())
                .addNode(usernameNode, UsernameCollectorNode.class.getSimpleName(), NODE_VERSION,
                        UsernameCollectorBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .addNode(passwordNode, PasswordCollectorNode.class.getSimpleName(), NODE_VERSION,
                        PasswordCollectorBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .uuid("56e0ecc5-16d7-4cbd-abdf-8e5bf11a4b4e");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .add(zeroPageLoginNode, pageNode, agentDataStoreDecisionNode)
                .entryNode(zeroPageLoginNode)
                .connect(zeroPageLoginNode, TRUE_OUTCOME_ID, agentDataStoreDecisionNode)
                .connect(zeroPageLoginNode, FALSE_OUTCOME_ID, pageNode)
                .connect(pageNode, agentDataStoreDecisionNode)
                .connect(agentDataStoreDecisionNode, TRUE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(agentDataStoreDecisionNode, FALSE_OUTCOME_ID, TREE_NODE_FAILURE_ID)
                .build();

        logger.debug("created the agent tree {}", treeName);
    }

    private void installHmacOneTimePasswordTree() throws SSOException, SMSException, IOException {
        String treeName = "HmacOneTimePassword";

        NodeBuilder usernameNode = new UsernameCollectorBuilder().uuid("c74d97b0-1eae-357e-84aa-9d5bade97baf");
        NodeBuilder passwordNode = new PasswordCollectorBuilder().uuid("70efdf2e-c9b0-3607-9795-c442636b55fb");
        NodeBuilder dataStoreDecisionNode = new DataStoreBuilder().uuid("6f4922f4-5568-361a-8cdf-4ad2299f6d23");
        NodeBuilder otpGeneratorNode = new HotpGeneratorBuilder().uuid("1f0e3dad-9990-3345-b743-9f8ffabdffc4");
        NodeBuilder otpSenderNode = new OtpEmailSenderBuilder()
                .hostName("mail.example.com")
                .uuid("98f13708-2101-34c4-b568-7be6106a3b84");
        NodeBuilder otpDecisionNode = new OtpDecisionBuilder().uuid("3c59dc04-8e88-3024-bbe8-079a5c74d079");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .add(usernameNode, passwordNode, dataStoreDecisionNode, otpDecisionNode, otpGeneratorNode,
                        otpSenderNode)
                .entryNode(usernameNode)
                .connect(usernameNode, passwordNode)
                .connect(passwordNode, dataStoreDecisionNode)
                .connect(dataStoreDecisionNode, TRUE_OUTCOME_ID, otpGeneratorNode)
                .connect(dataStoreDecisionNode, FALSE_OUTCOME_ID, TREE_NODE_FAILURE_ID)
                .connect(otpGeneratorNode, otpSenderNode)
                .connect(otpSenderNode, otpDecisionNode)
                .connect(otpDecisionNode, TRUE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(otpDecisionNode, FALSE_OUTCOME_ID, TREE_NODE_FAILURE_ID)
                .build();

        logger.debug("created the example tree {}", treeName);
    }

    private void installRetryLimitTree() throws SSOException, SMSException, IOException {
        String treeName = "RetryLimit";

        NodeBuilder usernameNode = new UsernameCollectorBuilder().uuid("1679091c-5a88-3faf-afb5-e6087eb1b2dc");
        NodeBuilder passwordNode = new PasswordCollectorBuilder().uuid("8f14e45f-ceea-367a-9a36-dedd4bea2543");
        NodeBuilder dataStoreDecisionNode = new DataStoreBuilder().uuid("c9f0f895-fb98-3b91-99f5-1fd0297e236d");
        NodeBuilder retryLimitNode = new RetryLimitDecisionBuilder().uuid("45c48cce-2e2d-3fbd-aa1a-fc51c7c6ad26");
        NodeBuilder accountLockoutNode = new AccountLockoutBuilder().uuid("d3d94468-02a4-3259-b55d-38e6d163e820");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .add(usernameNode, passwordNode, dataStoreDecisionNode, retryLimitNode, accountLockoutNode)
                .entryNode(usernameNode)
                .connect(usernameNode, passwordNode)
                .connect(passwordNode, dataStoreDecisionNode)
                .connect(dataStoreDecisionNode, TRUE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(dataStoreDecisionNode, FALSE_OUTCOME_ID, retryLimitNode)
                .connect(retryLimitNode, RETRY_OUTCOME, usernameNode)
                .connect(retryLimitNode, REJECT_OUTCOME, accountLockoutNode)
                .connect(accountLockoutNode, TREE_NODE_FAILURE_ID)
                .build();

        logger.debug("created the example tree {}", treeName);
    }

    private void installPersistentCookieTree() throws PluginException, SSOException, SMSException, IOException {
        String treeName = "PersistentCookie";

        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(ENCODING_TYPE);
        } catch (NoSuchAlgorithmException e) {
            throw new PluginException("Failed to generate HMAC key for persistent cookie tree plugin.");
        }
        keyGenerator.init(NUMBER_OF_SIGNED_BITS);
        char[] hmacKey = Base64.encode(new String(keyGenerator.generateKey().getEncoded()).getBytes()).toCharArray();

        NodeBuilder usernameNode = new UsernameCollectorBuilder().uuid("6512bd43-d9ca-36e0-ac99-0b0a82652dca");
        NodeBuilder passwordNode = new PasswordCollectorBuilder().uuid("c20ad4d7-6fe9-3759-aa27-a0c99bff6710");
        NodeBuilder dataStoreDecisionNode = new DataStoreBuilder().uuid("c51ce410-c124-310e-8db5-e4b97fc2af39");
        NodeBuilder persistentCookieDecision = new PersistentCookieDecisionBuilder()
                .hmacKey(Optional.of(hmacKey))
                .useSecureCookie(false)
                .uuid("aab32389-22bc-325a-af60-6eb525ffdc56");
        NodeBuilder setPersistentCookieNode = new SetPersistentCookieBuilder()
                .hmacKey(hmacKey)
                .useSecureCookie(false)
                .uuid("9bf31c7f-f062-336a-96d3-c8bd1f8f2ff3");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .add(usernameNode, passwordNode, dataStoreDecisionNode, persistentCookieDecision)
                .add(setPersistentCookieNode)
                .entryNode(persistentCookieDecision)
                .connect(persistentCookieDecision, TRUE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(persistentCookieDecision, FALSE_OUTCOME_ID, usernameNode)
                .connect(usernameNode, passwordNode)
                .connect(passwordNode, dataStoreDecisionNode)
                .connect(dataStoreDecisionNode, TRUE_OUTCOME_ID, setPersistentCookieNode)
                .connect(dataStoreDecisionNode, FALSE_OUTCOME_ID, usernameNode)
                .connect(setPersistentCookieNode, TREE_NODE_SUCCESS_ID)
                .build();

        logger.debug("created the example tree {}", treeName);
    }

    private void installSocialAuthGoogleAnonymousUserTree() throws SSOException, SMSException, IOException {
        String treeName = "Google-AnonymousUser";
        NodeBuilder setSuccessUrlNode = new SetSuccessUrlBuilder().uuid("1ff1de77-4005-38da-93f4-2943881c655f");
        NodeBuilder anonymousUserNode = new AnonymousUserBuilder().uuid("8e296a06-7a37-3633-b0de-d05f5a3bf3ec");
        NodeBuilder socialGoogleNode = new SocialGoogleBuilder()
                .clientId("aClientId")
                .clientSecret(CLIENT_SECRET_VALUE)
                .uuid("4e732ced-3463-306d-a0ca-9a15b6153677");
        treeBuilderFactory.builder(realm)
                .name(treeName)
                .add(anonymousUserNode, setSuccessUrlNode, socialGoogleNode)
                .entryNode(socialGoogleNode)
                .connect(socialGoogleNode, ACCOUNT_EXISTS.name(), TREE_NODE_SUCCESS_ID)
                .connect(socialGoogleNode,  NO_ACCOUNT.name(), anonymousUserNode)
                .connect(anonymousUserNode, setSuccessUrlNode)
                .connect(setSuccessUrlNode, TREE_NODE_SUCCESS_ID)
                .build();

        logger.debug("created the example tree {}", treeName);
    }

    private void installSocialAuthFacebookIDMTree() throws SSOException, SMSException, IOException {
        String treeName = "Facebook-ProvisionIDMAccount";

        NodeBuilder provisionIDMAccountNode = new ProvisionIdmAccountBuilder()
                .uuid("b6d767d2-f8ed-3d21-a44b-0e5886680cb9");
        NodeBuilder socialFacebookNode = new SocialFacebookBuilder()
                .clientId("aClientId")
                .clientSecret(CLIENT_SECRET_VALUE)
                .uuid("37693cfc-7480-39e4-9d87-b8c7d8b9aacd");
        treeBuilderFactory.builder(realm)
                .name(treeName)
                .add(socialFacebookNode, provisionIDMAccountNode)
                .entryNode(socialFacebookNode)
                .connect(socialFacebookNode, ACCOUNT_EXISTS.name(), TREE_NODE_SUCCESS_ID)
                .connect(socialFacebookNode, NO_ACCOUNT.name(), provisionIDMAccountNode)
                .connect(provisionIDMAccountNode, TREE_NODE_SUCCESS_ID)
                .build();

        logger.debug("created the example tree {}", treeName);
    }

    private void installSocialAuthGoogleDynamicProfileWithPasswordTree() throws SSOException, SMSException,
            IOException {
        String treeName = "Google-DynamicAccountCreation";

        NodeBuilder provisionDynamicAccountNode = new ProvisionDynamicAccountBuilder()
                .uuid("02e74f10-e032-3ad8-a8d1-38f2b4fdd6f0");
        NodeBuilder socialGoogleNode = new SocialGoogleBuilder()
                .clientId("aClientId")
                .clientSecret(CLIENT_SECRET_VALUE)
                .uuid("33e75ff0-9dd6-31bb-a69f-351039152189");
        NodeBuilder otpGeneratorNode = new HotpGeneratorBuilder().uuid("6ea9ab1b-aa0e-3b9e-9909-4440c317e21b");
        NodeBuilder otpSenderNode = new OtpEmailSenderBuilder()
                .hostName("mail.example.com")
                .uuid("34173cb3-8f07-389d-9beb-c2ac9128303f");
        NodeBuilder otpDecisionNode = new OtpDecisionBuilder().uuid("c16a5320-fa47-3530-9958-3c34fd356ef5");
        NodeBuilder otpCollectorRetryLimitDecisionNode = new RetryLimitDecisionBuilder()
                .uuid("6364d3f0-f495-36ab-9dcf-8d3b5c6e0b01");
        NodeBuilder createPasswordNode = new CreatePasswordBuilder().uuid("182be0c5-cdcd-3072-bb18-64cdee4d3d6e");
        treeBuilderFactory.builder(realm)
                .name(treeName)
                .add(socialGoogleNode, otpGeneratorNode, otpSenderNode, otpDecisionNode, provisionDynamicAccountNode,
                        createPasswordNode, otpCollectorRetryLimitDecisionNode)
                .entryNode(socialGoogleNode)
                .connect(socialGoogleNode, ACCOUNT_EXISTS.name(), TREE_NODE_SUCCESS_ID)
                .connect(socialGoogleNode, NO_ACCOUNT.name(), otpGeneratorNode)
                .connect(otpGeneratorNode, otpSenderNode)
                .connect(otpSenderNode, otpDecisionNode)
                .connect(otpDecisionNode, TRUE_OUTCOME_ID, createPasswordNode)
                .connect(otpDecisionNode, FALSE_OUTCOME_ID, otpCollectorRetryLimitDecisionNode)
                .connect(otpCollectorRetryLimitDecisionNode, RETRY_OUTCOME, otpDecisionNode)
                .connect(otpCollectorRetryLimitDecisionNode, REJECT_OUTCOME, TREE_NODE_FAILURE_ID)
                .connect(createPasswordNode, provisionDynamicAccountNode)
                .connect(provisionDynamicAccountNode, TREE_NODE_SUCCESS_ID)
                .build();

        logger.debug("created the example tree {}", treeName);
    }

    private void installPlatformRegistrationTree() throws SSOException, SMSException, IOException {
        String treeName = "PlatformRegistration";

        NodeBuilder validatedUsernameNode = new ValidatedUsernameBuilder().validateInput(TRUE)
                .usernameAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .uuid("e369853d-f766-3a44-a1ed-0ff613f563bd");

        NodeBuilder validatedPasswordNode = new ValidatedPasswordBuilder().validateInput(TRUE)
                .passwordAttribute(DEFAULT_IDM_PASSWORD_ATTRIBUTE)
                .uuid("1c383cd3-0b7c-398a-b502-93adfecb7b18");

        NodeBuilder attributeCollectorNode = new AttributeCollectorBuilder()
                .attributesToCollect(asList("givenName", "sn", "mail", "preferences/marketing", "preferences/updates"))
                .validateInputs(true).required(true)
                .identityAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .uuid("19ca14e7-ea63-38a4-ae0e-b13d585e4c22");

        NodeBuilder kbaCreateNode = new KbaCreateBuilder()
                .message(singletonMap(ENGLISH, "Select a security question"))
                .uuid("a5bfc9e0-7964-38dd-9eb9-5fc584cd965d");

        NodeBuilder acceptTermsAndConditionsNode = new AcceptTermsAndConditionsBuilder()
                .uuid("a5771bce-93e2-30c3-af7c-d9dfd0e5deaa");

        NodeBuilder pageNode = new PageNodeBuilder().pageHeader(singletonMap(ENGLISH, "Sign Up"))
                .pageDescription(singletonMap(ENGLISH, "Signing up is fast and easy.<br>Already have an account?"
                        + "<a href='#/service/PlatformLogin'>Sign In</a>"
                ))
                .addNode(validatedUsernameNode, ValidatedUsernameNode.class.getSimpleName(),
                        NODE_VERSION, ValidatedUsernameBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .addNode(attributeCollectorNode, AttributeCollectorNode.class.getSimpleName(),
                        NODE_VERSION, AttributeCollectorBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .addNode(validatedPasswordNode, ValidatedPasswordNode.class.getSimpleName(),
                        NODE_VERSION, ValidatedPasswordBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .addNode(kbaCreateNode, KbaCreateNode.class.getSimpleName(),
                        NODE_VERSION, KbaCreateBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .addNode(acceptTermsAndConditionsNode, AcceptTermsAndConditionsNode.class.getSimpleName(),
                        NODE_VERSION, AcceptTermsAndConditionsBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .uuid("d67d8ab4-f4c1-3bf2-aaa3-53e27879133c");

        NodeBuilder createObjectNode = new CreateObjectBuilder()
                .identityResource(DEFAULT_IDM_IDENTITY_RESOURCE)
                .uuid("d645920e-395f-3dad-bbbb-ed0eca3fe2e0");

        NodeBuilder incrementLoginCountNode = new IncrementLoginCountBuilder()
                .identityAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .uuid("3416a75f-4cea-3109-907c-acd8e2f2aefc");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .description("Platform Registration Tree")
                .add(pageNode, createObjectNode, incrementLoginCountNode)
                .entryNode(pageNode)
                .connect(pageNode, createObjectNode)
                .connect(createObjectNode, CreateObjectNode.CreateObjectOutcome.FAILURE.toString(),
                        TREE_NODE_FAILURE_ID)
                .connect(createObjectNode, CreateObjectNode.CreateObjectOutcome.CREATED.toString(),
                        incrementLoginCountNode)
                .connect(incrementLoginCountNode, TREE_NODE_SUCCESS_ID)
                .build();

        logger.debug("created the platform registration tree {}", treeName);
    }

    private void installPlatformLoginTree() throws SSOException, SMSException, IOException {
        String treeName = "PlatformLogin";

        NodeBuilder validatedUsernameNode = new ValidatedUsernameBuilder().validateInput(FALSE)
                .usernameAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .uuid("67c6a1e7-ce56-33d6-ba74-8ab6d9af3fd7");
        NodeBuilder validatedPasswordNode = new ValidatedPasswordBuilder().validateInput(FALSE)
                .passwordAttribute(DEFAULT_IDM_PASSWORD_ATTRIBUTE)
                .uuid("642e92ef-b794-3173-8881-b53e1e1b18b6");

        NodeBuilder pageNode = new PageNodeBuilder().pageHeader(singletonMap(ENGLISH, "Sign In"))
                .pageDescription(singletonMap(ENGLISH,
                        "New here? <a href=\"#/service/PlatformRegistration\">Create an account</a>"
                                + "<br><a href=\"#/service/PlatformForgottenUsername\">Forgot username?</a>"
                                + "<a href=\"#/service/PlatformResetPassword\"> Forgot password?</a>"))
                .addNode(validatedUsernameNode, ValidatedUsernameNode.class.getSimpleName(),
                        NODE_VERSION, ValidatedUsernameBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .addNode(validatedPasswordNode, ValidatedPasswordNode.class.getSimpleName(),
                        NODE_VERSION, ValidatedPasswordBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .uuid("f457c545-a9de-388f-98ec-ee47145a72c0");

        NodeBuilder dataStoreNode = new DataStoreBuilder().uuid("c0c7c76d-30bd-3dca-afc9-6f40275bdc0a");

        NodeBuilder incrementLoginCountNode = new IncrementLoginCountBuilder()
                .identityAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .uuid("2838023a-778d-3aec-9c21-2708f721b788");

        NodeBuilder innerTreeEvaluatorNode = new InnerTreeEvaluatorBuilder()
                .tree("PlatformProgressiveProfile")
                .uuid("9a115815-4dfa-32ca-9dbd-0694a4e9bdc8");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .description("Platform Login Tree")
                .add(pageNode, dataStoreNode, incrementLoginCountNode, innerTreeEvaluatorNode)
                .entryNode(pageNode)
                .connect(pageNode, dataStoreNode)
                .connect(dataStoreNode, TRUE_OUTCOME_ID, incrementLoginCountNode)
                .connect(dataStoreNode, FALSE_OUTCOME_ID, TREE_NODE_FAILURE_ID)
                .connect(incrementLoginCountNode, innerTreeEvaluatorNode)
                .connect(innerTreeEvaluatorNode, TRUE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(innerTreeEvaluatorNode, FALSE_OUTCOME_ID, TREE_NODE_FAILURE_ID)
                .build();

        logger.debug("created the platform login tree {}", treeName);
    }

    private void installPlatformProgressiveProfileTree() throws SSOException, SMSException, IOException {
        String treeName = "PlatformProgressiveProfile";

        NodeBuilder loginCountDecisionNode = new LoginCountDecisionBuilder().amount(3)
                .interval(LoginCountDecisionNode.LoginCountIntervalType.AT)
                .identityAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .uuid("a1d0c6e8-3f02-3327-9846-1063f4ac58a6");

        NodeBuilder queryFilterDecisionNode = new QueryFilterDecisionBuilder()
                .queryFilter("!(/preferences pr) or /preferences/marketing eq false or /preferences/updates eq false")
                .identityAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .uuid("17e62166-fc85-36df-a4d1-bc0e1742c08b");

        NodeBuilder attributeCollectorNode = new AttributeCollectorBuilder().validateInputs(false).required(false)
                .identityAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .attributesToCollect(asList("preferences/updates", "preferences/marketing"))
                .uuid("f7177163-c833-3ff4-b38f-c8d2872f1ec6");

        NodeBuilder pageNode = new PageNodeBuilder().pageHeader(singletonMap(ENGLISH, "Please select your preferences"))
                .pageDescription(emptyMap())
                .addNode(attributeCollectorNode, AttributeCollectorNode.class.getSimpleName(),
                        NODE_VERSION, AttributeCollectorBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .uuid("6c8349cc-7260-3e62-a3b1-396831a8398f");

        NodeBuilder patchObjectNode = new PatchObjectBuilder()
                .identityResource(DEFAULT_IDM_IDENTITY_RESOURCE)
                .identityAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .ignoredFields(emptySet())
                .uuid("d9d4f495-e875-32e0-b5a1-a4a6e1b9770f");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .description("Prompt for missing preferences on 3rd login")
                .add(loginCountDecisionNode, queryFilterDecisionNode, pageNode, patchObjectNode)
                .entryNode(loginCountDecisionNode)
                .connect(loginCountDecisionNode, TRUE_OUTCOME_ID, queryFilterDecisionNode)
                .connect(loginCountDecisionNode, FALSE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(queryFilterDecisionNode, TRUE_OUTCOME_ID, pageNode)
                .connect(queryFilterDecisionNode, FALSE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(pageNode, patchObjectNode)
                .connect(patchObjectNode, PATCHED.toString(), TREE_NODE_SUCCESS_ID)
                .connect(patchObjectNode, FAILURE.toString(), TREE_NODE_FAILURE_ID)
                .build();

        logger.debug("created the platform progressive profile tree {}", treeName);
    }

    private void installPlatformForgottenUsernameTree() throws SSOException, SMSException, IOException {
        String treeName = "PlatformForgottenUsername";

        NodeBuilder attributeCollectorNode = new AttributeCollectorBuilder().validateInputs(false).required(true)
                .identityAttribute(DEFAULT_IDM_MAIL_ATTRIBUTE)
                .attributesToCollect(List.of(DEFAULT_IDM_MAIL_ATTRIBUTE))
                .uuid("d82c8d16-19ad-3176-9665-453cfb2e55f0");

        NodeBuilder pageNode = new PageNodeBuilder().pageHeader(singletonMap(ENGLISH, "Forgotten Username"))
                .pageDescription(singletonMap(ENGLISH,
                        "Enter your email address or <a href=\"#/service/PlatformLogin\">Sign in</a>"))
                .addNode(attributeCollectorNode, AttributeCollectorNode.class.getSimpleName(),
                        NODE_VERSION, AttributeCollectorBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .uuid("a684ecee-e76f-3522-b732-86a895bc8436");

        NodeBuilder identifyExistingUserNode = new IdentifyExistingUserBuilder().identifier("")
                .identityAttribute(DEFAULT_IDM_MAIL_ATTRIBUTE)
                .uuid("b53b3a3d-6ab9-3ce0-a682-29151c9bde11");

        NodeBuilder emailSuspendNode = new EmailSuspendBuilder().objectLookup(true)
                .emailAttribute(DEFAULT_IDM_MAIL_ATTRIBUTE).emailTemplateName("forgottenUsername")
                .emailSuspendMessage(singletonMap(ENGLISH,
                        "An email has been sent to the address you entered. Click the link in that email to proceed."))
                .identityAttribute(DEFAULT_IDM_MAIL_ATTRIBUTE)
                .uuid("9f61408e-3afb-333e-90cd-f1b20de6f466");

        NodeBuilder innerTreeEvaluatorNode = new InnerTreeEvaluatorBuilder()
                .tree("PlatformLogin")
                .uuid("72b32a1f-754b-31c0-9b36-95e0cb6cde7f");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .description("Forgotten Username Tree")
                .add(pageNode, identifyExistingUserNode, emailSuspendNode, innerTreeEvaluatorNode)
                .entryNode(pageNode)
                .connect(pageNode, identifyExistingUserNode)
                .connect(identifyExistingUserNode, TRUE_OUTCOME_ID, emailSuspendNode)
                .connect(identifyExistingUserNode, FALSE_OUTCOME_ID, emailSuspendNode)
                .connect(emailSuspendNode, innerTreeEvaluatorNode)
                .connect(innerTreeEvaluatorNode, TRUE_OUTCOME_ID, TREE_NODE_SUCCESS_ID)
                .connect(innerTreeEvaluatorNode, FALSE_OUTCOME_ID, TREE_NODE_FAILURE_ID)
                .build();

        logger.debug("created the platform forgotten username tree {}", treeName);
    }

    private void installPlatformResetPasswordTree() throws SSOException, SMSException, IOException {
        String treeName = "PlatformResetPassword";

        NodeBuilder attributeCollectorNode = new AttributeCollectorBuilder().validateInputs(false).required(true)
                .identityAttribute(DEFAULT_IDM_MAIL_ATTRIBUTE)
                .attributesToCollect(List.of(DEFAULT_IDM_MAIL_ATTRIBUTE))
                .uuid("66f041e1-6a60-328b-85a7-e228a89c3799");

        NodeBuilder emailPageNode = new PageNodeBuilder().pageHeader(singletonMap(ENGLISH, "Reset Password"))
                .pageDescription(singletonMap(ENGLISH,
                        "Enter your email address or <a href=\"#/service/PlatformLogin\">Sign in</a>"))
                .addNode(attributeCollectorNode, AttributeCollectorNode.class.getSimpleName(),
                        NODE_VERSION, AttributeCollectorBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .uuid("093f65e0-80a2-35f8-876b-1c5722a46aa2");

        NodeBuilder identifyExistingUserNode = new IdentifyExistingUserBuilder()
                .identifier(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .identityAttribute(DEFAULT_IDM_MAIL_ATTRIBUTE)
                .uuid("072b030b-a126-32f4-b237-4f342be9ed44");

        NodeBuilder emailSuspendNode = new EmailSuspendBuilder().objectLookup(true)
                .emailAttribute(DEFAULT_IDM_MAIL_ATTRIBUTE).emailTemplateName("resetPassword")
                .emailSuspendMessage(singletonMap(ENGLISH,
                        "An email has been sent to the address you entered. Click the link in that email to proceed."))
                .identityAttribute(DEFAULT_IDM_MAIL_ATTRIBUTE)
                .uuid("7f39f831-7fbd-3198-8ef4-c628eba02591");

        NodeBuilder validatedPasswordNode = new ValidatedPasswordBuilder().validateInput(true)
                .passwordAttribute(DEFAULT_IDM_PASSWORD_ATTRIBUTE)
                .uuid("44f683a8-4163-3352-bafe-57c2e008bc8c");

        NodeBuilder passwordPageNode = new PageNodeBuilder().pageHeader(singletonMap(ENGLISH, "Reset Password"))
                .pageDescription(singletonMap(ENGLISH, "Change password"))
                .addNode(validatedPasswordNode, ValidatedPasswordNode.class.getSimpleName(),
                        NODE_VERSION, ValidatedPasswordBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .uuid("03afdbd6-6e79-39b1-a5f8-597834fa83a4");

        NodeBuilder patchObjectNode = new PatchObjectBuilder()
                .identityResource(DEFAULT_IDM_IDENTITY_RESOURCE)
                .identityAttribute(DEFAULT_IDM_MAIL_ATTRIBUTE)
                .ignoredFields(emptySet())
                .uuid("ea5d2f1c-4608-332e-87d3-aa3d998e5135");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .description("Reset Password Tree")
                .add(emailPageNode, identifyExistingUserNode, emailSuspendNode, passwordPageNode, patchObjectNode)
                .entryNode(emailPageNode)
                .connect(emailPageNode, identifyExistingUserNode)
                .connect(identifyExistingUserNode, TRUE_OUTCOME_ID, emailSuspendNode)
                .connect(identifyExistingUserNode, FALSE_OUTCOME_ID, emailSuspendNode)
                .connect(emailSuspendNode, passwordPageNode)
                .connect(passwordPageNode, patchObjectNode)
                .connect(patchObjectNode, PATCHED.toString(), TREE_NODE_SUCCESS_ID)
                .connect(patchObjectNode, FAILURE.toString(), TREE_NODE_FAILURE_ID)
                .build();
        logger.debug("created the platform reset password tree {}", treeName);
    }

    private void installPlatformUpdatePassword() throws SSOException, SMSException, IOException {
        String treeName = "PlatformUpdatePassword";

        NodeBuilder sessionDataNode = new SessionDataBuilder()
                .sessionDataKey("UserToken")
                .sharedStateKey("userName")
                .uuid("fc490ca4-5c00-3124-9bbe-3554a4fdf6fb");

        NodeBuilder attributePresentDecisionNode = new AttributePresentDecisionBuilder()
                .setPresentAttribute("password")
                .setIdentityAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .uuid("3295c76a-cbf4-3aae-933c-36b1b5fc2cb1");

        NodeBuilder verifyExistingValidatedPasswordNode = new ValidatedPasswordBuilder()
                .passwordAttribute("password")
                .validateInput(false)
                .uuid("735b90b4-5681-35ed-ac3f-678819b6e058");

        NodeBuilder verifyPasswordPageNode = new PageNodeBuilder()
                .pageHeader(singletonMap(ENGLISH, "Verify Existing Password"))
                .pageDescription(singletonMap(ENGLISH, "Enter current password"))
                .addNode(verifyExistingValidatedPasswordNode, ValidatedPasswordNode.class.getSimpleName(),
                        NODE_VERSION, ValidatedPasswordBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .uuid("a3f390d8-8e4c-31f2-b47b-fa2f1b5f87db");

        NodeBuilder dataStoreDecisionNode = new DataStoreBuilder().uuid("14bfa6bb-1487-3e45-bba0-28a21ed38046");

        NodeBuilder updatePasswordValidatedPasswordNode = new ValidatedPasswordBuilder()
                .validateInput(TRUE)
                .passwordAttribute(DEFAULT_IDM_PASSWORD_ATTRIBUTE)
                .uuid("7cbbc409-ec99-3f19-878c-75bd1e06f215");

        NodeBuilder updatePasswordPageNode = new PageNodeBuilder()
                .pageHeader(singletonMap(ENGLISH, "Update Password"))
                .pageDescription(singletonMap(ENGLISH, "Enter new password"))
                .addNode(updatePasswordValidatedPasswordNode, ValidatedPasswordNode.class.getSimpleName(),
                        NODE_VERSION, ValidatedPasswordBuilder.DEFAULT_DISPLAY_NAME, systemProperties)
                .uuid("e2c420d9-28d4-3f8c-a0ff-2ec19b371514");

        NodeBuilder emailSuspendNode = new EmailSuspendBuilder().objectLookup(true)
                .emailAttribute(DEFAULT_IDM_MAIL_ATTRIBUTE).identityAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .emailSuspendMessage(singletonMap(ENGLISH, "An email has been sent to your address, please verify "
                        + " your email address to update your password. Click the link in that email to proceed."))
                .emailTemplateName("updatePassword")
                .uuid("32bb90e8-976a-3b52-98d5-da10fe66f21d");

        NodeBuilder patchObjectNode = new PatchObjectBuilder().patchAsObject(true)
                .identityResource(DEFAULT_IDM_IDENTITY_RESOURCE)
                                              .identityAttribute(DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                .ignoredFields(singleton("userName"))
                .uuid("d2ddea18-f006-35ce-8623-e36bd4e3c7c5");

        treeBuilderFactory.builder(realm)
                .name(treeName)
                .description("Update password using active session")
                .add(sessionDataNode, attributePresentDecisionNode, verifyPasswordPageNode, dataStoreDecisionNode,
                        updatePasswordPageNode, emailSuspendNode, patchObjectNode)
                .entryNode(sessionDataNode)
                .connect(sessionDataNode, attributePresentDecisionNode)
                .connect(attributePresentDecisionNode, TRUE_OUTCOME_ID, verifyPasswordPageNode)
                .connect(attributePresentDecisionNode, FALSE_OUTCOME_ID, emailSuspendNode)
                .connect(emailSuspendNode, updatePasswordPageNode)
                .connect(verifyPasswordPageNode, dataStoreDecisionNode)
                .connect(dataStoreDecisionNode, TRUE_OUTCOME_ID, updatePasswordPageNode)
                .connect(dataStoreDecisionNode, FALSE_OUTCOME_ID, TREE_NODE_FAILURE_ID)
                .connect(updatePasswordPageNode, patchObjectNode)
                .connect(patchObjectNode, PATCHED.toString(), TREE_NODE_SUCCESS_ID)
                .connect(patchObjectNode, FAILURE.toString(), TREE_NODE_FAILURE_ID)
                .build();

        logger.debug("created the platform update password tree {}", treeName);
    }
}
