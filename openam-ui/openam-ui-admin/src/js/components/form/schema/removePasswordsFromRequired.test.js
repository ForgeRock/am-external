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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

import { expect } from "chai";

import removePasswordsFromRequired from "./removePasswordsFromRequired";

describe("components/form/schema/removePasswordsFromRequired", () => {
    it("removes required passwords from a flat schema", () => {
        const schema = {
            type: "object",
            required: ["one", "three"],
            properties: {
                one: {
                    title: "One",
                    type: "string",
                    _isPassword: true
                },
                two: {
                    title: "Two",
                    type: "string",
                    _isPassword: true
                },
                three: {
                    title: "Three",
                    type: "string"
                }
            }
        };

        expect(removePasswordsFromRequired(schema)).eql({
            type: "object",
            required: ["three"],
            properties: {
                one: {
                    title: "One",
                    type: "string",
                    _isPassword: true
                },
                two: {
                    title: "Two",
                    type: "string",
                    _isPassword: true
                },
                three: {
                    title: "Three",
                    type: "string"
                }
            }
        });
    });

    it("removes required passwords from a nested schema", () => {
        const schema = {
            type: "object",
            required: ["one", "three"],
            properties: {
                one: {
                    title: "One",
                    type: "string",
                    _isPassword: true
                },
                two: {
                    type: "object",
                    required: ["five", "six"],
                    properties: {
                        four: {
                            title: "Four",
                            type: "string",
                            _isPassword: true
                        },
                        five: {
                            title: "Five",
                            type: "string"
                        },
                        six: {
                            title: "Six",
                            type: "string",
                            _isPassword: true
                        }
                    }
                },
                three: {
                    title: "Three",
                    type: "string"
                }
            }
        };

        expect(removePasswordsFromRequired(schema)).eql({
            type: "object",
            required: ["three"],
            properties: {
                one: {
                    title: "One",
                    type: "string",
                    _isPassword: true
                },
                two: {
                    type: "object",
                    required: ["five"],
                    properties: {
                        four: {
                            title: "Four",
                            type: "string",
                            _isPassword: true
                        },
                        five: {
                            title: "Five",
                            type: "string"
                        },
                        six: {
                            title: "Six",
                            type: "string",
                            _isPassword: true
                        }
                    }
                },
                three: {
                    title: "Three",
                    type: "string"
                }
            }
        });
    });
});
