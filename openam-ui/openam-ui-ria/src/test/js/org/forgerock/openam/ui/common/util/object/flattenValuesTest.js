/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/common/util/object/flattenValues"
], (flattenValues) => {
    describe("org/forgerock/openam/ui/common/object/flattenValues", () => {
        it("unwraps an object's single element array values", () => {
            const object = {
                none: "none",
                one: ["one"],
                many: ["one", "two"]
            };

            expect(flattenValues(object)).to.be.eql({
                none: "none",
                one: "one",
                many: ["one", "two"]
            });
        });
    });
});
