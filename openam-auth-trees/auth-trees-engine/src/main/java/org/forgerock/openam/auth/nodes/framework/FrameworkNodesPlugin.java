/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.framework;

import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_TREES_SERVICE_NAME;
import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_TREES_SERVICE_VERSION;
import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_FAILURE_ID;
import static com.sun.identity.authentication.util.ISAuthConstants.TREE_NODE_SUCCESS_ID;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.FALSE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.TRUE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.framework.builders.RetryLimitDecisionBuilder.REJECT_OUTCOME;
import static org.forgerock.openam.auth.nodes.framework.builders.RetryLimitDecisionBuilder.RETRY_OUTCOME;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.ACCOUNT_EXISTS;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.NO_ACCOUNT;
import static org.forgerock.openam.core.realms.Realms.root;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.inject.Inject;

import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.nodes.NodesPlugin;
import org.forgerock.openam.auth.nodes.SetPersistentCookieNode;
import org.forgerock.openam.auth.nodes.framework.builders.AccountLockoutBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.AnonymousUserBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.CreatePasswordBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.DataStoreBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.HotpGeneratorBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.NodeBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.OtpDecisionBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.OtpEmailSenderBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.PasswordCollectorBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.PersistentCookieDecisionBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.ProvisionDynamicAccountBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.ProvisionIdmAccountBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.RetryLimitDecisionBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.SetPersistentCookieBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.SetSuccessUrlBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.SocialFacebookBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.SocialGoogleBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.TreeBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.UsernameCollectorBuilder;
import org.forgerock.openam.auth.nodes.framework.builders.ZeroPageLoginBuilder;
import org.forgerock.openam.authentication.NodeRegistry;
import org.forgerock.openam.plugins.AmPlugin;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.StartupType;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/** Plugin for nodes that are tightly coupled to the tree engine. */
public class FrameworkNodesPlugin extends AbstractNodeAmPlugin {

    private static final String TREES_CONTAINER = "treesContainer";
    private static final String ENCODING_TYPE = "AES";
    private static final int NUMBER_OF_SIGNED_BITS = 256;

    private final ServiceConfigManagerFactory scmFactory;
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final AnnotatedServiceRegistry serviceRegistry;
    private final NodeRegistry nodeRegistry;

    /**
     * DI-enabled constructor.
     * @param scmFactory An SMS service config manager factory.
     * @param serviceRegistry The service registry.
     * @param nodeRegistry The node registry.
     */
    @Inject
    public FrameworkNodesPlugin(ServiceConfigManagerFactory scmFactory, AnnotatedServiceRegistry serviceRegistry,
                                NodeRegistry nodeRegistry) {
        this.scmFactory = scmFactory;
        this.serviceRegistry = serviceRegistry;
        this.nodeRegistry = nodeRegistry;
    }

    @Override
    public String getPluginVersion() {
        return "2.0.0";
    }

    @Override
    public Map<Class<? extends AmPlugin>, String> getDependencies() {
        return singletonMap(NodesPlugin.class, "2.0.0");
    }

    @Override
    protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
        return ImmutableMap.of(
            "1.0.0", asList(
                    ChoiceCollectorNode.class,
                    InnerTreeEvaluatorNode.class,
                    ModifyAuthLevelNode.class),
            "2.0.0", asList(
                    PageNode.class,
                    SetPersistentCookieNode.class));
    }

    @Override
    public void onStartup(StartupType startupType) throws PluginException {
        super.onStartup(startupType);
        if (startupType == StartupType.FIRST_TIME_DEMO_INSTALL) {
            try {
                installExampleTree();
                installRetryLimitTree();
                installPersistentCookieTree();
                installHmacOneTimePasswordTree();
                installSocialAuthFacebookIDMTree();
                installSocialAuthGoogleAnonymousUserTree();
                installSocialAuthGoogleDynamicProfileWithPasswordTree();
            } catch (SMSException | SSOException e) {
                logger.error("failed to create example trees", e);
                throw new PluginException("Could not create an example tree", e);
            }
        }
    }

    private void installExampleTree() throws PluginException, SSOException, SMSException {
        String treeName = "Example";
        ServiceConfigManager scm = scmFactory.create(AUTH_TREES_SERVICE_NAME, AUTH_TREES_SERVICE_VERSION);
        ServiceConfig treesContainer = scm.getOrganizationConfig(root().asDN(), null).getSubConfig(TREES_CONTAINER);

        NodeBuilder usernameNode = new UsernameCollectorBuilder();
        NodeBuilder passwordNode = new PasswordCollectorBuilder();
        NodeBuilder dataStoreDecisionNode = new DataStoreBuilder();
        NodeBuilder zeroPageLoginNode = new ZeroPageLoginBuilder();

        TreeBuilder.builder(nodeRegistry, serviceRegistry, treesContainer)
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

    private void installHmacOneTimePasswordTree() throws PluginException, SSOException, SMSException {
        String treeName = "HmacOneTimePassword";
        ServiceConfigManager scm = scmFactory.create(AUTH_TREES_SERVICE_NAME, AUTH_TREES_SERVICE_VERSION);
        ServiceConfig treesContainer = scm.getOrganizationConfig(root().asDN(), null).getSubConfig(TREES_CONTAINER);

        NodeBuilder usernameNode = new UsernameCollectorBuilder();
        NodeBuilder passwordNode = new PasswordCollectorBuilder();
        NodeBuilder dataStoreDecisionNode = new DataStoreBuilder();
        NodeBuilder otpGeneratorNode = new HotpGeneratorBuilder();
        NodeBuilder otpSenderNode = new OtpEmailSenderBuilder().hostName("mail.example.com");
        NodeBuilder otpDecisionNode = new OtpDecisionBuilder();

        TreeBuilder.builder(nodeRegistry, serviceRegistry, treesContainer)
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

    private void installRetryLimitTree() throws PluginException, SSOException, SMSException {
        String treeName = "RetryLimit";
        ServiceConfigManager scm = scmFactory.create(AUTH_TREES_SERVICE_NAME, AUTH_TREES_SERVICE_VERSION);
        ServiceConfig treesContainer = scm.getOrganizationConfig(root().asDN(), null).getSubConfig(TREES_CONTAINER);

        NodeBuilder usernameNode = new UsernameCollectorBuilder();
        NodeBuilder passwordNode = new PasswordCollectorBuilder();
        NodeBuilder dataStoreDecisionNode = new DataStoreBuilder();
        NodeBuilder retryLimitNode = new RetryLimitDecisionBuilder();
        NodeBuilder accountLockoutNode = new AccountLockoutBuilder();

        TreeBuilder.builder(nodeRegistry, serviceRegistry, treesContainer)
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

    private void installPersistentCookieTree() throws PluginException, SSOException, SMSException {
        String treeName = "PersistentCookie";
        ServiceConfigManager scm = scmFactory.create(AUTH_TREES_SERVICE_NAME, AUTH_TREES_SERVICE_VERSION);
        ServiceConfig treesContainer = scm.getOrganizationConfig(root().asDN(), null).getSubConfig(TREES_CONTAINER);

        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(ENCODING_TYPE);
        } catch (NoSuchAlgorithmException e) {
            throw new PluginException("Failed to generate HMAC key for persistent cookie tree plugin.");
        }
        keyGenerator.init(NUMBER_OF_SIGNED_BITS);
        char[] hmacKey = Base64.encode(new String(keyGenerator.generateKey().getEncoded()).getBytes()).toCharArray();

        NodeBuilder usernameNode = new UsernameCollectorBuilder();
        NodeBuilder passwordNode = new PasswordCollectorBuilder();
        NodeBuilder dataStoreDecisionNode = new DataStoreBuilder();
        NodeBuilder persistentCookieDecision = new PersistentCookieDecisionBuilder()
                .hmacKey(hmacKey)
                .useSecureCookie(false);
        NodeBuilder setPersistentCookieNode = new SetPersistentCookieBuilder()
                .hmacKey(hmacKey)
                .useSecureCookie(false);

        TreeBuilder.builder(nodeRegistry, serviceRegistry, treesContainer)
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

    private void installSocialAuthGoogleAnonymousUserTree() throws PluginException, SSOException, SMSException {
        String treeName = "Google-AnonymousUser";
        ServiceConfigManager scm = scmFactory.create(AUTH_TREES_SERVICE_NAME, AUTH_TREES_SERVICE_VERSION);
        ServiceConfig treesContainer = scm.getOrganizationConfig(root().asDN(), null).getSubConfig(TREES_CONTAINER);
        NodeBuilder setSuccessUrlNode = new SetSuccessUrlBuilder();
        NodeBuilder anonymousUserNode = new AnonymousUserBuilder();
        NodeBuilder socialGoogleNode = new SocialGoogleBuilder().clientId("aClientId").clientSecret("aSecret");
        TreeBuilder.builder(nodeRegistry, serviceRegistry, treesContainer)
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

    private void installSocialAuthFacebookIDMTree() throws PluginException, SSOException, SMSException {
        String treeName = "Facebook-ProvisionIDMAccount";
        ServiceConfigManager scm = scmFactory.create(AUTH_TREES_SERVICE_NAME, AUTH_TREES_SERVICE_VERSION);
        ServiceConfig treesContainer = scm.getOrganizationConfig(root().asDN(), null).getSubConfig(TREES_CONTAINER);

        NodeBuilder provisionIDMAccountNode = new ProvisionIdmAccountBuilder();
        NodeBuilder socialFacebookNode = new SocialFacebookBuilder().clientId("aClientId").clientSecret("aSecret");
        TreeBuilder.builder(nodeRegistry, serviceRegistry, treesContainer)
                .name(treeName)
                .add(socialFacebookNode, provisionIDMAccountNode)
                .entryNode(socialFacebookNode)
                .connect(socialFacebookNode, ACCOUNT_EXISTS.name(), TREE_NODE_SUCCESS_ID)
                .connect(socialFacebookNode, NO_ACCOUNT.name(), provisionIDMAccountNode)
                .connect(provisionIDMAccountNode, TREE_NODE_SUCCESS_ID)
                .build();

        logger.debug("created the example tree {}", treeName);
    }

    private void installSocialAuthGoogleDynamicProfileWithPasswordTree()
            throws PluginException, SSOException, SMSException {
        String treeName = "Google-DynamicAccountCreation";
        ServiceConfigManager scm = scmFactory.create(AUTH_TREES_SERVICE_NAME, AUTH_TREES_SERVICE_VERSION);
        ServiceConfig treesContainer = scm.getOrganizationConfig(root().asDN(), null).getSubConfig(TREES_CONTAINER);

        NodeBuilder provisionDynamicAccountNode = new ProvisionDynamicAccountBuilder();
        NodeBuilder socialGoogleNode = new SocialGoogleBuilder().clientId("aClientId").clientSecret("aSecret");
        NodeBuilder otpGeneratorNode = new HotpGeneratorBuilder();
        NodeBuilder otpSenderNode = new OtpEmailSenderBuilder().hostName("mail.example.com");
        NodeBuilder otpDecisionNode = new OtpDecisionBuilder();
        NodeBuilder otpCollectorRetryLimitDecisionNode = new RetryLimitDecisionBuilder();
        NodeBuilder createPasswordNode = new CreatePasswordBuilder();
        TreeBuilder.builder(nodeRegistry, serviceRegistry, treesContainer)
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
}
