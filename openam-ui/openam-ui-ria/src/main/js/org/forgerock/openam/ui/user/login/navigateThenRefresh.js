/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

define([], () =>
    (event) => {
        event.preventDefault();
        window.location.href = event.target.href;
        window.location.reload();
    }
);
