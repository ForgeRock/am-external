/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "sinon",
    "org/forgerock/openam/ui/admin/views/realms/common/isValidId"
], (sinon, isValidId) => {
    describe("org/forgerock/openam/ui/admin/views/realms/common/isValidId", () => {
        isValidId = isValidId.default;

        context("When the Id starts with the '#' character", () => {
            it("The validation fails", () => {
                const id = "#badger";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id contains but does not start with the '#' character ", () => {
            it("The validation passes", () => {
                const id = "badger#badger";
                expect(isValidId(id)).eql(true);
            });
        });

        context("When the Id starts with the '\"' character", () => {
            it("The validation fails", () => {
                const id = "\"badger";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id contains but does not start with the '\"' character ", () => {
            it("The validation passes", () => {
                const id = "badger\"badger";
                expect(isValidId(id)).eql(true);
            });
        });

        context("When the Id starts with the space character", () => {
            it("The validation fails", () => {
                const id = " badger";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id ends with the space character", () => {
            it("The validation fails", () => {
                const id = "badger ";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id contains but does not start or end with the space character ", () => {
            it("The validation passes", () => {
                const id = "badger badger";
                expect(isValidId(id)).eql(true);
            });
        });

        context("When the Id contains the '\\' character", () => {
            it("The validation fails", () => {
                const id = "badger\\badger";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id contains the '/' character", () => {
            it("The validation fails", () => {
                const id = "badger/badger";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id contains the '+' character", () => {
            it("The validation fails", () => {
                const id = "badger+badger";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id contains the ';' character", () => {
            it("The validation fails", () => {
                const id = "badger;badger";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id contains the ',' character", () => {
            it("The validation fails", () => {
                const id = "badger,badger";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id is '.'", () => {
            it("The validation fails", () => {
                const id = ".";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id starts with a '.'", () => {
            it("The validation passes", () => {
                const id = ".badger";
                expect(isValidId(id)).eql(true);
            });
        });

        context("When the Id contains the '.' character", () => {
            it("The validation passes", () => {
                const id = "badger.badger";
                expect(isValidId(id)).eql(true);
            });
        });

        context("When the Id is'..'", () => {
            it("The validation fails", () => {
                const id = "..";
                expect(isValidId(id)).eql(false);
            });
        });

        context("When the Id starts with a '..'", () => {
            it("The validation passes", () => {
                const id = "..badger";
                expect(isValidId(id)).eql(true);
            });
        });

        context("When the Id contains the '..' characters", () => {
            it("The validation passes", () => {
                const id = "badger..badger";
                expect(isValidId(id)).eql(true);
            });
        });

        context("When the Id is empty", () => {
            it("The validation passes", () => {
                const id = "";
                expect(isValidId(id)).eql(true);
            });
        });
    });
});
