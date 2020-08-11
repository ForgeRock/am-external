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

import URI from "urijs";

const createUri = (fragment) => {
    const uri = URI().fragment(fragment);

    // Build path components
    const pathComponents = uri.path().split("/");
    pathComponents.splice(pathComponents.lastIndexOf("XUI"), 1, "ui-admin");

    // Build URI
    uri.path(pathComponents.join("/"));

    return uri.toString();
};

export default createUri;
