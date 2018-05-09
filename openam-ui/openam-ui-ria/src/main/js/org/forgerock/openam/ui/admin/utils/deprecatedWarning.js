/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
  * @module org/forgerock/openam/ui/admin/utils/deprecatedWarning
  */
define([
], () =>
    function deprecatedWarning (deprecated, replacement) {
        console.warn(`${deprecated} is marked as deprecated. \nPlease use ${replacement}`);
    }
);
