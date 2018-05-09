/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
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
