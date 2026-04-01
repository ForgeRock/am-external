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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.forgerock.am.trees.api.Tree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScriptedTreeWrapperTest {

    @Mock
    Tree tree;

    @InjectMocks
    ScriptedTreeWrapper scriptedTreeWrapper;


    @Test
    void name() {
        // given
        given(tree.getName()).willReturn("treeName");

        // when
        String name = scriptedTreeWrapper.name();

        // then
        assertThat(name).isEqualTo("treeName");
    }

    @Test
    void identityResource() {
        // given
        given(tree.getIdentityResource()).willReturn("identityResource");

        // when
        String identityResource = scriptedTreeWrapper.identityResource();

        // then
        assertThat(identityResource).isEqualTo("identityResource");
    }

    @Test
    void innerJourney() {
        // given
        given(tree.innerTreeOnly()).willReturn(true);

        // when
        boolean innerTree = scriptedTreeWrapper.innerJourney();

        // then
        assertThat(innerTree).isTrue();
    }

    @Test
    void mustRun() {
        // given
        given(tree.mustRun()).willReturn(true);

        // when
        boolean mustRun = scriptedTreeWrapper.mustRun();

        // then
        assertThat(mustRun).isTrue();
    }
}