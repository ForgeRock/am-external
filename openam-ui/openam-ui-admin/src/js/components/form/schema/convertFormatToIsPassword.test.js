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
 * Copyright 2019 ForgeRock AS.
 */

import { expect } from "chai";

import convertFormatToIsPassword from "./convertFormatToIsPassword";

describe("components/form/schema/convertFormatToIsPassword", () => {
    it("replaces format:password properties to _isPassword:true", () => {
        const schema = {
            properties: {
                one: {
                    format: "password"
                },
                two: {
                    format: "password"
                },
                three: {
                    properties: {
                        one: {
                            format: "password"
                        },
                        two: {
                            properties: {
                                one: {
                                    format: "password"
                                },
                                two: {
                                    format: "password"
                                }
                            }
                        }
                    }
                }
            },
            type: "object"
        };

        expect(convertFormatToIsPassword(schema)).eql({
            properties: {
                one: {
                    _isPassword: true
                },
                two: {
                    _isPassword: true
                },
                three: {
                    properties: {
                        one: {
                            _isPassword: true
                        },
                        two: {
                            properties: {
                                one: {
                                    _isPassword: true
                                },
                                two: {
                                    _isPassword: true
                                }
                            }
                        }
                    }
                }
            },
            type: "object"
        });
    });

    it("leaves other formats and properties untouched", () => {
        const schema = {
            properties: {
                one: {
                    format: "backwards",
                    foo: "bar"
                },
                two: {
                    format: "password",
                    foo: "bar"
                },
                three: {
                    properties: {
                        one: {
                            properties: {
                                one: {
                                    format: "backwards",
                                    foo: "bar"
                                },
                                two: {
                                    format: "password",
                                    foo: "bar"
                                }
                            }
                        }
                    }
                }
            },
            type: "object"
        };

        expect(convertFormatToIsPassword(schema)).eql({
            properties: {
                one: {
                    format: "backwards",
                    foo: "bar"
                },
                two: {
                    _isPassword: true,
                    foo: "bar"
                },
                three: {
                    properties: {
                        one: {
                            properties: {
                                one: {
                                    format: "backwards",
                                    foo: "bar"
                                },
                                two: {
                                    _isPassword: true,
                                    foo: "bar"
                                }
                            }
                        }
                    }
                }
            },
            type: "object"
        });
    });
});
