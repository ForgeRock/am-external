/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/admin/views/api/calculateHeight
 */

const calculateHeight = () => {
    const NAVBAR_HEIGHT = 76;
    const FOOTER_HEIGHT = 81;
    return window.innerHeight - NAVBAR_HEIGHT - FOOTER_HEIGHT;
};

export default calculateHeight;
