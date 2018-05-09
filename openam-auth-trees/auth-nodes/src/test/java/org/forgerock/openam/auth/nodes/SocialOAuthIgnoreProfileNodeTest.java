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
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 */
public class SocialOAuthIgnoreProfileNodeTest {

    @Mock
    private SocialOAuthIgnoreProfileNode.Config config;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void processReturnsTrueWhenSharedSessionIsValid() throws Exception {
        //GIVEN
        JsonValue sharedState = JsonValue.json(object(
            field("userInfo", object(
                    field("userNames", object(
                            field("test", Collections.singletonList("user"))
                    ))
            ))
        ));

        TreeContext context = new TreeContext(sharedState, new ExternalRequestContext.Builder().build(), emptyList());
        SocialOAuthIgnoreProfileNode node = new SocialOAuthIgnoreProfileNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        Assert.assertEquals(process.outcome, "outcome");
    }

    @Test
    public void processSetUserInSessionWhenSharedSessionIsValid() throws Exception {
        //GIVEN
        JsonValue sharedState = JsonValue.json(object(
                field("userInfo", object(
                        field("userNames", object(
                                field("test", Collections.singletonList("user"))
                        ))
                ))
        ));

        TreeContext context = new TreeContext(sharedState, new ExternalRequestContext.Builder().build(), emptyList());
        SocialOAuthIgnoreProfileNode node = new SocialOAuthIgnoreProfileNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        Assert.assertTrue(process.sharedState.isDefined(USERNAME));
        Assert.assertEquals(process.sharedState.get(USERNAME).asString(), "user");
    }
}
