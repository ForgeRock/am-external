/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/services/realm/AuthenticationService
 */
define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSServiceUtils",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/Promise"
], ($, _, AbstractDelegate, Constants, SMSServiceUtils, JSONSchema, JSONValues, fetchUrl, Promise) => {
    const obj = new AbstractDelegate(`${Constants.host}${Constants.context}/json`);

    obj.authentication = {
        get (realm) {
            return SMSServiceUtils.schemaWithValues(obj, fetchUrl.default("/realm-config/authentication", { realm }));
        },
        update (realm, data) {
            return obj.serviceCall({
                url: fetchUrl.default("/realm-config/authentication", { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "PUT",
                data: JSON.stringify(data)
            });
        },
        chains: {
            all (realm) {
                return Promise.all([
                    obj.serviceCall({
                        url: fetchUrl.default("/realm-config/authentication/chains?_queryFilter=true", { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }),
                    obj.serviceCall({
                        url: fetchUrl.default("/realm-config/authentication", { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    })
                ]).then((response) => {
                    const chainsData = response[0];
                    const authenticationData = response[1];

                    _.each(chainsData[0].result, (chainData) => {
                        if (chainData._id === authenticationData[0].adminAuthModule) {
                            chainData.defaultConfig = chainData.defaultConfig || {};
                            chainData.defaultConfig.adminAuthModule = true;
                        }

                        if (chainData._id === authenticationData[0].orgConfig) {
                            chainData.defaultConfig = chainData.defaultConfig || {};
                            chainData.defaultConfig.orgConfig = true;
                        }
                    });

                    return {
                        values: chainsData[0]
                    };
                });
            },
            create (realm, data) {
                return obj.serviceCall({
                    url: fetchUrl.default("/realm-config/authentication/chains?_action=create", { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST",
                    data: JSON.stringify(data)
                });
            },
            get (realm, name) {
                var moduleName;

                return Promise.all([
                    obj.serviceCall({
                        url: fetchUrl.default("/realm-config/authentication", { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }),
                    obj.serviceCall({
                        url: fetchUrl.default(`/realm-config/authentication/chains/${name}`, { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }),
                    obj.serviceCall({
                        url: fetchUrl.default("/realm-config/authentication/modules?_queryFilter=true", { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    })
                ]).then((response) => {
                    const authenticationData = response[0];
                    const chainData = response[1];
                    const modulesData = response[2];

                    if (chainData[0]._id === authenticationData[0].adminAuthModule) {
                        chainData[0].adminAuthModule = true;
                    }

                    if (chainData[0]._id === authenticationData[0].orgConfig) {
                        chainData[0].orgConfig = true;
                    }

                    _.each(chainData[0].authChainConfiguration, (chainLink) => {
                        moduleName = _.find(modulesData[0].result, { _id: chainLink.module });
                        // The server allows for deletion of modules that are in use within a chain. The chain itself
                        // will still have a reference to the deleted module.
                        // Below we are checking if the module is present. If it isn't the type is left undefined
                        if (moduleName) {
                            chainLink.type = moduleName.type;
                        }
                    });

                    return {
                        chainData: chainData[0],
                        modulesData: _.sortBy(modulesData[0].result, "_id")
                    };
                });
            },
            remove (realm, name) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/chains/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "DELETE"
                });
            },
            update (realm, name, data) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/chains/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "PUT",
                    data: JSON.stringify(data)
                });
            }
        },
        modules: {
            all (realm) {
                return obj.serviceCall({
                    url: fetchUrl.default("/realm-config/authentication/modules?_queryFilter=true", { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                }).done(SMSServiceUtils.sortResultBy("_id"));
            },
            create (realm, data, type) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/modules/${type}?_action=create`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST",
                    data: JSON.stringify(data)
                });
            },
            get (realm, name, type) {
                function getInstance () {
                    return obj.serviceCall({
                        url: fetchUrl.default(`/realm-config/authentication/modules/${type}/${name}`, { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    });
                }

                return Promise.all([
                    this.schema(realm, type),
                    getInstance(),
                    this.types.get(realm, type)
                ]).then((response) => {
                    return {
                        name: response[2].name,
                        schema: response[0],
                        values: new JSONValues(response[1][0])
                    };
                });
            },
            exists (realm, name) {
                var promise = $.Deferred(),
                    query = `_queryFilter=${encodeURIComponent(`_id eq "${name}"`)}&_fields=_id`,
                    request = obj.serviceCall({
                        url: fetchUrl.default(
                            `/realm-config/authentication/modules?${query}`, { realm }
                        ),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    });

                request.done((data) => {
                    promise.resolve(data.result.length > 0);
                });
                return promise;
            },
            remove (realm, name, type) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/modules/${type}/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "DELETE"
                });
            },
            update (realm, name, type, data) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/modules/${type}/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "PUT",
                    data
                }).then((response) => new JSONValues(response));
            },
            types: {
                all (realm) {
                    return obj.serviceCall({
                        url: fetchUrl.default("/realm-config/authentication/modules?_action=getAllTypes", { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "POST"
                    }).done(SMSServiceUtils.sortResultBy("name"));
                },
                get (realm, type) {
                    // TODO: change this to a proper server-side call when OPENAM-7242 is implemented
                    return obj.authentication.modules.types.all(realm).then((data) => {
                        return _.findWhere(data.result, { "_id": type });
                    });
                }
            },
            schema (realm, type) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/modules/${type}?_action=schema`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                }).then((response) => new JSONSchema(response));
            }
        }
    };

    return obj;
});
