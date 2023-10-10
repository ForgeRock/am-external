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
 * Copyright 2017-2023 ForgeRock AS.
 */


package ${package};

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;

/**
 * A node that checks to see if zero-page login headers have specified username and whether that username is in a group
 * permitted to use zero-page login headers.
 */
@Node.Metadata(outcomeProvider  = AbstractDecisionNode.OutcomeProvider.class,
               configClass      = ${authNodeName}.Config.class)
public class ${authNodeName} extends AbstractDecisionNode {

    private final Pattern DN_PATTERN = Pattern.compile("^[a-zA-Z0-9]=([^,]+),");
    private final Logger logger = LoggerFactory.getLogger(${authNodeName}.class);
    private final Config config;
    private final Realm realm;
    private String username = null;

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The header name for zero-page login that will contain the identity's username.
         */
        @Attribute(order = 100)
        default String usernameHeader() {
            return "X-OpenAM-Username";
        }

        /**
         * The header name for zero-page login that will contain the identity's password.
         */
        @Attribute(order = 200)
        default String passwordHeader() {
            return "X-OpenAM-Password";
        }

        /**
         * The group name (or fully-qualified unique identifier) for the group that the identity must be in.
         */
        @Attribute(order = 300)
        default String groupName() {
            return "zero-page-login";
        }
    }


    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     * @param realm The realm the node is in.
     */
    @Inject
    public ${authNodeName}(@Assisted Config config, @Assisted Realm realm) {
        this.config = config;
        this.realm = realm;
    }

    @Override
    public Action process(TreeContext context) {
        NodeState nodeState = context.getStateFor(this);
        if (nodeState.isDefined("username") && nodeState.isDefined("password")) {
            return goTo(true).build();
        }
        boolean hasUsername = context.request.headers.containsKey(config.usernameHeader());
        boolean hasPassword = context.request.headers.containsKey(config.passwordHeader());

        if (!hasUsername || !hasPassword) {
            return goTo(false).build();
        }

        String username = context.request.headers.get(config.usernameHeader()).get(0);
        String password = context.request.headers.get(config.passwordHeader()).get(0);
        AMIdentity userIdentity = IdUtils.getIdentity(username, realm.asDN());
        try {
            if (userIdentity != null && userIdentity.isExists() && userIdentity.isActive()
                    && isMemberOfGroup(userIdentity, config.groupName())) {
                this.username = username;
                nodeState.putShared(USERNAME, username);
                nodeState.putTransient(PASSWORD, password);
                return goTo(true).build();
            }
        } catch (IdRepoException | SSOException e) {
            logger.warn("Error locating user '{}' ", username, e);
        }
        return goTo(false).build();
    }

    private boolean isMemberOfGroup(AMIdentity userIdentity, String groupName) {
        try {
            Set<AMIdentity> userGroups = userIdentity.getMemberships(IdType.GROUP);
            for (AMIdentity group : userGroups) {
                if (groupName.equals(group.getName())) {
                    return true;
                }
                Matcher dnMatcher = DN_PATTERN.matcher(group.getName());
                if (dnMatcher.find() && dnMatcher.group(1).equals(groupName)) {
                    return true;
                }
            }
        } catch (IdRepoException | SSOException e) {
            logger.warn("Could not load groups for user {}", userIdentity);
        }
        return false;
    }

    /**
     * Audit the username of the user that will be authenticated.
     *
     * @return The audit entry detail.
     */
    @Override
    public JsonValue getAuditEntryDetail() {
        if (username != null) {
            return json(object(field("username", username)));
        } else {
            return json(object());
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
                new InputState("username", false),
                new InputState("password", false)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
                new OutputState("username", Map.of("true", true, "false", false)),
                new OutputState("password", Map.of("true", true, "false", false))
        };
    }
}
