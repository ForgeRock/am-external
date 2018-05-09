/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([
    "i18next"
], (i18next) => {
    const i18n = i18next;

    i18n.t = i18next.t.bind(i18next);

    return i18n;
});
