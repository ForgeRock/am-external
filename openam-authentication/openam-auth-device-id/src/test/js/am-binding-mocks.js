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
 * Copyright 2014-2021 ForgeRock AS.
 */


/* For client-side.js */
var fontDetector = {detect: function() {}},
    console = {warn: function() {}},
    output = {value: ''},
    autoSubmitDelay = 0;

function submit() {}

function JavaImporter () {
    return {
        JsonValue: {
            object: function () {

            },
            json: function () {
                return {
                    put: function () {

                    }
                };
            }
        },
        InvalidRequestException: function () {

        },
        UserInfoClaims: function () {

        },
        Claim: function () {

        },
        LinkedHashMap: function () {
            return {
                put: function () {

                },
                remove: function () {

                }
            };
        },
        ArrayList: function () {
            return {
                add: function () {

                },
                addAll: function () {

                },
                size: function () {

                },
                toArray: function () {
                    return {
                        forEach: function () {

                        }
                    };
                }
            };
        }
    };
}

/* For server-side.js */
var logger = {
        messageEnabled: function() {

        },
        warning: function() {

        },
        message: function() {

        },
        error: function () {

        }
    },
    sharedState = {
        get: function(value) {
            return {
                getDeviceProfiles: function(username, realm) {

                },
                saveDeviceProfiles: function(username, realm, vals) {

                }
            };
        }
    },
    username = 'demo',
    realm = '/',
    clientScriptOutputData = '{}',
    FAILED, SUCCESS,
    rawProfile = {
        get: function () {
            return {
                get: rawProfile.get
            };
        }
    },
    normalizedProfile = {
        get: function () {
            return {
                asString: function () {

                },
                isNotNull: function () {

                }
            };
        }
    },
    org = {
        forgerock: {
            json: {

            },
            oauth2: {
                core: {
                    exceptions: {

                    }
                }
            },
            openidconnect: {

            },
            http: {
                protocol: {
                    Request: function () {
                        return {
                            setUri: function () {

                            },
                            setMethod: function () {

                            },
                            setEntity: function () {

                            },
                            getHeaders: function () {

                            }
                        };
                    },
                    Response: function () {
                        return {
                            getStatus: function () {

                            }
                        };
                    },
                    Status: {

                    }
                }
            }
        }
    },
    java = {
        util: {

        }
    },
    selectedIdp,
    scopes = {
        toArray: function () {
            return {
                forEach: function () {

                }
            };
        }
    },
    claimObjects,
    requestedTypedClaims,
    accessToken = {
        setField: function () {

        }
    },
    token = {
        setMayAct: function () {

        }
    },
    httpClient = {
        send: function () {
            return {
                getOrThrow: function () {

                }
            };
        }
    },
    identity = {
        getAttribute: function () {
            return {
                toArray: function () {
                    return [

                    ];
                }
            };
        }
    },
    session = {
        getProperty: function () {

        }
    };

if (!String.prototype.trim) {
    String.prototype.trim = function () {
        return this.replace(/^\s+|\s+$/g, '');
    };
}
