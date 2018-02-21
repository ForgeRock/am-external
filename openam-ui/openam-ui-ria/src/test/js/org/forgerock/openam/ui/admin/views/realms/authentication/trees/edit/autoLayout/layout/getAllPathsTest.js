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
 * Copyright 2017 ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getAllPaths",
    "../data/SampleData"
], (getAllPaths, data) => {
    describe("org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getAllPaths",
        () => {
            const { one, two, three, four, five, six, seven, eight, nine, success } = data.tree.nodes;
            const paths = [
                [{ one }, { two }, { five }, { four }, { six }],
                [{ one }, { two }, { four }, { five }],
                [{ one }, { two }, { four }, { six }],
                [{ one }, { two }, { five }, { four }],
                [{ one }, { two }, { six }],
                [{ one }, { two }, { seven }],
                [{ one }, { two }, { eight }],
                [{ one }, { two }, { nine }],
                [{ one }, { three }, { success }]
            ];

            it("returns all paths for the tree", () => {
                expect(
                    getAllPaths.default(data.tree.entryNodeId, data.tree.nodes)
                ).to.deep.equal(paths);
            });
        });
});
