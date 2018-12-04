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
 * Copyright 2017 ForgeRock AS.
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
