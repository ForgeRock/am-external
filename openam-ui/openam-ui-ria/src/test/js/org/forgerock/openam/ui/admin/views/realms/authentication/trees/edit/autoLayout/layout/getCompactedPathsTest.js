/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getCompactedPaths",
    "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getAllPaths",
    "../data/SampleData"
], (getCompactedPaths, getAllPaths, data) => {
    describe(
        "org/forgerock/openam/ui/admin/views/realms/authentication/trees/edit/autoLayout/layout/getCompactedPaths",
        () => {
            const { one, two, three, four, five, six, seven, eight, nine, success } = data.tree.nodes;
            const paths = getAllPaths.default(data.tree.entryNodeId, data.tree.nodes);
            const compactedPaths = [
                [{ one }, { two }, { five }, { four }, { six }],
                [null, null, { seven }],
                [null, null, { eight }],
                [null, null, { nine }],
                [null, { three }, { success }]
            ];

            it("removes all repeating nodes in all paths and removes empty paths", () => {
                expect(getCompactedPaths.default(paths)).to.deep.equal(compactedPaths);
            });
        });
});
