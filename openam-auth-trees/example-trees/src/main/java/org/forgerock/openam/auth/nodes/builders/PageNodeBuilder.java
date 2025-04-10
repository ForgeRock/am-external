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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.builders;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.framework.PageNode.ChildNodeConfig.DISPLAY_NAME_FIELD;
import static org.forgerock.openam.auth.nodes.framework.PageNode.ChildNodeConfig.ID_FIELD;
import static org.forgerock.openam.auth.nodes.framework.PageNode.ChildNodeConfig.NODE_TYPE_FIELD;
import static org.forgerock.openam.auth.nodes.framework.PageNode.ChildNodeConfig.NODE_VERSION_FIELD;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.forgerock.am.trees.api.NodeBuilder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.nodes.framework.PageNode;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import com.iplanet.am.util.SystemPropertiesWrapper;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * The {@link PageNode} Builder.
 */
public class PageNodeBuilder extends AbstractNodeBuilder implements PageNode.Config {

    private Map<Locale, String> pageHeader;
    private Map<Locale, String> pageDescription;
    private String stage;
    private List<String> nodeConfigs = new ArrayList<>();
    private List<NodeBuilder> nodeBuilders = new ArrayList<>();

    /**
     * The constructor.
     */
    public PageNodeBuilder() {
        super("Page Node", PageNode.class);
    }

    /**
     * Set the page header.
     *
     * @param pageHeader the page header
     * @return the header
     */
    public PageNodeBuilder pageHeader(Map<Locale, String> pageHeader) {
        this.pageHeader = pageHeader;
        return this;
    }

    /**
     * Sets the page description.
     *
     * @param pageDescription the page description
     * @return the description
     */
    public PageNodeBuilder pageDescription(Map<Locale, String> pageDescription) {
        this.pageDescription = pageDescription;
        return this;
    }

    /**
     * Sets the stage.
     *
     * @param stage the stage
     * @return the stage
     */
    public PageNodeBuilder stage(String stage) {
        this.stage = stage;
        return this;
    }

    /**
     * Adds nodes to the Page Node.
     *
     * @param nodeBuilder      the builder associated with the node
     * @param nodeType         the type of the node
     * @param nodeVersion      the version of the node
     * @param displayName      the display name of the node
     * @param systemProperties the SystemPropertiesWrapper object
     * @return the page node builder
     */
    public PageNodeBuilder addNode(NodeBuilder nodeBuilder, String nodeType, int nodeVersion, String displayName,
                                   SystemPropertiesWrapper systemProperties) {
        JsonValue jsonNodeConfig = json(object(field(ID_FIELD, nodeBuilder.getId()), field(NODE_TYPE_FIELD, nodeType),
                field(NODE_VERSION_FIELD, nodeVersion), field(DISPLAY_NAME_FIELD, displayName)));
        nodeConfigs.add(PageNode.ChildNodeConfig.create(jsonNodeConfig, systemProperties).toString());
        nodeBuilders.add(nodeBuilder);
        return this;
    }

    @Override
    public Map<Locale, String> pageHeader() {
        return pageHeader;
    }

    @Override
    public Map<Locale, String> pageDescription() {
        return pageDescription;
    }

    @Override
    public String stage() {
        return stage;
    }

    @Override
    public List<String> nodes() {
        return nodeConfigs;
    }

    @Override
    public void build(AnnotatedServiceRegistry serviceRegistry) throws SMSException, SSOException {
        for (NodeBuilder nodeBuilder : nodeBuilders) {
            nodeBuilder.build(serviceRegistry);
        }
        super.build(serviceRegistry);
    }

}
