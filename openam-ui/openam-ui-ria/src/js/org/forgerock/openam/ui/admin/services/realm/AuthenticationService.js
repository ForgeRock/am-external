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
 * Copyright 2015-2018 ForgeRock AS.
 */

import { each, findWhere, sortBy } from "lodash";
import $ from "jquery";

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";
import SMSServiceUtils from "org/forgerock/openam/ui/admin/services/SMSServiceUtils";

/**
 * @module org/forgerock/openam/ui/admin/services/realm/AuthenticationService
 */
const AuthenticationService = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

AuthenticationService.authentication = {
    get (realm) {
        return SMSServiceUtils.schemaWithValues(AuthenticationService,
            fetchUrl("/realm-config/authentication", { realm }));
    },
    update (realm, data) {
        return AuthenticationService.serviceCall({
            url: fetchUrl("/realm-config/authentication", { realm }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "PUT",
            data: JSON.stringify(data)
        });
    },
    chains: {
        all (realm) {
            return Promise.all([
                AuthenticationService.serviceCall({
                    url: fetchUrl("/realm-config/authentication/chains?_queryFilter=true", { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                }),
                AuthenticationService.serviceCall({
                    url: fetchUrl("/realm-config/authentication", { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                })
            ]).then(([chainsData, authenticationData]) => {
                each(chainsData.result, (chainData) => {
                    if (chainData._id === authenticationData.adminAuthModule) {
                        chainData.defaultConfig = chainData.defaultConfig || {};
                        chainData.defaultConfig.adminAuthModule = true;
                    }

                    if (chainData._id === authenticationData.orgConfig) {
                        chainData.defaultConfig = chainData.defaultConfig || {};
                        chainData.defaultConfig.orgConfig = true;
                    }
                });

                return {
                    values: chainsData
                };
            });
        },
        create (realm, data) {
            return AuthenticationService.serviceCall({
                url: fetchUrl("/realm-config/authentication/chains?_action=create", { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST",
                data: JSON.stringify(data)
            });
        },
        get (realm, name) {
            var moduleName;

            return Promise.all([
                AuthenticationService.serviceCall({
                    url: fetchUrl("/realm-config/authentication", { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                }),
                AuthenticationService.serviceCall({
                    url: fetchUrl(`/realm-config/authentication/chains/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                }),
                AuthenticationService.serviceCall({
                    url: fetchUrl("/realm-config/authentication/modules?_queryFilter=true", { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                })
            ]).then(([authenticationData, chainData, modulesData]) => {
                if (chainData._id === authenticationData.adminAuthModule) {
                    chainData.adminAuthModule = true;
                }

                if (chainData._id === authenticationData.orgConfig) {
                    chainData.orgConfig = true;
                }

                chainData.authChainConfiguration = chainData.authChainConfiguration || [];

                each(chainData.authChainConfiguration, (chainLink) => {
                    moduleName = find(modulesData.result, { _id: chainLink.module });
                    // The server allows for deletion of modules that are in use within a chain. The chain itself
                    // will still have a reference to the deleted module.
                    // Below we are checking if the module is present. If it isn't the type is left undefined
                    if (moduleName) {
                        chainLink.type = moduleName.type;
                    }
                });

                return {
                    chainData,
                    modulesData: sortBy(modulesData.result, "_id")
                };
            });
        },
        remove (realm, name) {
            return AuthenticationService.serviceCall({
                url: fetchUrl(`/realm-config/authentication/chains/${name}`, { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "DELETE"
            });
        },
        update (realm, name, data) {
            return AuthenticationService.serviceCall({
                url: fetchUrl(`/realm-config/authentication/chains/${name}`, { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "PUT",
                data: JSON.stringify(data)
            });
        }
    },
    modules: {
        all (realm) {
            return AuthenticationService.serviceCall({
                url: fetchUrl("/realm-config/authentication/modules?_queryFilter=true", { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            }).then(({ result }) => sortBy(result, "_id"));
        },
        create (realm, data, type) {
            return AuthenticationService.serviceCall({
                url: fetchUrl(`/realm-config/authentication/modules/${type}?_action=create`, { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST",
                data: JSON.stringify(data)
            });
        },
        get (realm, name, type) {
            function getInstance () {
                return AuthenticationService.serviceCall({
                    url: fetchUrl(`/realm-config/authentication/modules/${type}/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                });
            }

            return Promise.all([
                this.schema(realm, type),
                getInstance(),
                this.types.get(realm, type)
            ]).then(([schema, values, type]) => {
                return {
                    name: type.name,
                    schema,
                    values: new JSONValues(values)
                };
            });
        },
        exists (realm, name) {
            var promise = $.Deferred(),
                query = `_queryFilter=${encodeURIComponent(`_id eq "${name}"`)}&_fields=_id`,
                request = AuthenticationService.serviceCall({
                    url: fetchUrl(
                        `/realm-config/authentication/modules?${query}`, { realm }
                    ),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                });

            request.then((data) => {
                promise.resolve(data.result.length > 0);
            });
            return promise;
        },
        remove (realm, name, type) {
            return AuthenticationService.serviceCall({
                url: fetchUrl(`/realm-config/authentication/modules/${type}/${name}`, { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "DELETE"
            });
        },
        update (realm, name, type, data) {
            return AuthenticationService.serviceCall({
                url: fetchUrl(`/realm-config/authentication/modules/${type}/${name}`, { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "PUT",
                data
            }).then((response) => new JSONValues(response));
        },
        types: {
            all (realm) {
                return AuthenticationService.serviceCall({
                    url: fetchUrl("/realm-config/authentication/modules?_action=getAllTypes", { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                }).then(({ result }) => sortBy(result, "name"));
            },
            get (realm, type) {
                // TODO: change this to a proper server-side call when OPENAM-7242 is implemented
                return AuthenticationService.authentication.modules.types.all(realm)
                    .then((response) => findWhere(response, { "_id": type }));
            }
        },
        schema (realm, type) {
            return AuthenticationService.serviceCall({
                url: fetchUrl(`/realm-config/authentication/modules/${type}?_action=schema`, { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }).then((response) => new JSONSchema(response));
        }
    }
};

export default AuthenticationService;
