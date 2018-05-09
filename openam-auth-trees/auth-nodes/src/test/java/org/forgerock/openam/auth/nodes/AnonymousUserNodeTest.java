/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test the anonymous user node.
 */
public class AnonymousUserNodeTest {

    @Mock
    private AnonymousUserNode.Config config;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void processSetTheUsernameInSharedStateWhenNoError() throws Exception {
        //GIVEN
        when(config.anonymousUserName()).thenReturn("anonymous");
        TreeContext context = new TreeContext(JsonValue.json(object(1)),
                new ExternalRequestContext.Builder().build(), emptyList());
        AnonymousUserNode node = new AnonymousUserNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        Assert.assertTrue(process.sharedState.isDefined(USERNAME));
        Assert.assertEquals(process.sharedState.get(USERNAME).asString(), config.anonymousUserName());
    }

    @Test
    public void processReplacesTheUsernameInSharedStateIfOneAlreadyExists() throws Exception {
        //GIVEN
        when(config.anonymousUserName()).thenReturn("anonymous");
        TreeContext context = new TreeContext(JsonValue.json(object(field(USERNAME, "this_user_will_be_replaced"))),
                new ExternalRequestContext.Builder().build(), emptyList());
        AnonymousUserNode node = new AnonymousUserNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        Assert.assertTrue(process.sharedState.isDefined(USERNAME));
        Assert.assertEquals(process.sharedState.get(USERNAME).asString(), config.anonymousUserName());
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void processThrowAnExceptionIfAnonymousUserIsNotSetInTheConfiguration() throws Exception {
        //GIVEN
        when(config.anonymousUserName()).thenReturn("");
        TreeContext context = new TreeContext(JsonValue.json(object(1)),
                new ExternalRequestContext.Builder().build(), emptyList());
        AnonymousUserNode node = new AnonymousUserNode(config);

        //WHEN
        node.process(context);

        //THEN
        //throw an exception
    }


}