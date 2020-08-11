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
 * Copyright 2018-2019 ForgeRock AS.
 */

/**
 * @module api/crest/url
 */

import Constants from "org/forgerock/openam/ui/common/util/Constants";
import fetchUrl from "api/fetchUrl";

/**
 * Creates a URL to be used as input for a CREST resource.
 * @param {string} path Path to the resource. Must start with a forward slash.
 * @param {string} [realm] The realm to use when constructing the URL. Maybe an absolute realm or alias.
 * @returns {string} URL string to be used as input for a CREST resource.
 * @throws {Error} If path does not start with a forward slash.
 * @example
 * new CRESTv2(url("/realm-config/federation/circlesoftrust", realm));
 */
const url = (path, realm) => `${Constants.context}/json${fetchUrl(path, { realm })}`;

export default url;
