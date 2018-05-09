/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/realms/updateDefaultServerAdvancedFqdnMap
 */

import _ from "lodash";
import $ from "jquery";
import ServersService from "org/forgerock/openam/ui/admin/services/global/ServersService";

const getKey = (alias) => `com.sun.identity.server.fqdnMap[${alias}]`;

const updateDefaultServerAdvancedFqdnMap = (realm, currentDnsAliases, originalDnsAliases) => {
    const createFqdnMapKeys = (aliases) => _.map(aliases, (alias) => {
        return getKey(alias);
    });

    const newDnsAliases = _.difference(currentDnsAliases, originalDnsAliases);
    const fqdnMapKeysToAdd = createFqdnMapKeys(newDnsAliases);
    const fqdnMapKeysToRemove = createFqdnMapKeys(_.difference(originalDnsAliases, currentDnsAliases));
    const deferred = $.Deferred();

    ServersService.servers.get(
        ServersService.servers.DEFAULT_SERVER,
        ServersService.servers.ADVANCED_SECTION
    ).then((data) => {
        const valuesWithoutRemovedKeys = _.removeByValues(
            data.values.raw, "key", [...fqdnMapKeysToRemove, ...fqdnMapKeysToAdd]);

        const newValues = _.map(newDnsAliases, (value) => {
            const key = getKey(value);
            return { key, value };
        });

        ServersService.servers.update(
            ServersService.servers.ADVANCED_SECTION,
            { [ServersService.servers.ADVANCED_SECTION] : valuesWithoutRemovedKeys.concat(newValues) },
            ServersService.servers.DEFAULT_SERVER
        ).then(deferred.resolve, deferred.reject);
    }, deferred.reject);

    return deferred;
};

export default updateDefaultServerAdvancedFqdnMap;
