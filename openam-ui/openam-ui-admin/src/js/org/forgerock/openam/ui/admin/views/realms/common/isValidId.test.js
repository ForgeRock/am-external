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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

import { expect } from "chai";

import isValidId from "./isValidId";

describe("org/forgerock/openam/ui/admin/views/realms/common/isValidId", () => {
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
