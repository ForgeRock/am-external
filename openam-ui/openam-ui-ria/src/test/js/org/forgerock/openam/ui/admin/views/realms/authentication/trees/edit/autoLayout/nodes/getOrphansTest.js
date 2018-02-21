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
    "../data/SampleData",
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getAllPaths",
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getColumns",
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/nodes/getOrphans"
], (data, getAllPaths, getColumns, getOrphans) => {
    describe("org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/nodes/getOrphans", () => {
        const nodes = data.tree.nodes;
        const { orphan1, orphan2 } = nodes;
        const paths = getAllPaths.default(data.tree.entryNodeId, data.tree.nodes);
        const columns = getColumns.default(paths);
        it("returns orphaned nodes", () => {
            expect(getOrphans.default(nodes, columns)).to.deep.equal({
                orphan1, orphan2
            });
        });
    });
});
