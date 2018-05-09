/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/common/util/array/arrayify"
], (arrayify) => {
    describe("org/forgerock/openam/ui/common/array/arrayify", () => {
        context("when argument is", () => {
            context("an array", () => {
                context("of length 0", () => {
                    it("it returns an empty array", () => {
                        const args = [];

                        expect(arrayify(args)).to.be.an.instanceOf(Array).and.be.empty;
                    });
                });
                context("of length 1", () => {
                    it("it returns an array that contains the same elements", () => {
                        const args = ["a"];

                        expect(arrayify(args)).to.be.an.instanceOf(Array).and.have.members(args);
                    });
                });
            });
        });
        context("when argument is not an array", () => {
            it("it returns the argument wrapped in an array", () => {
                const args = "a";

                expect(arrayify(args)).to.be.eql([args]);
            });
        });
    });
});
