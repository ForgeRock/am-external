/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import React from "react";
import { t } from "i18next";

const Loading = () => (
    <p className="loading text-muted">
        <i className="fa fa-circle-o-notch fa-spin fa-2x fa-fw" />
        <span>{ t("console.common.loading") }</span>
    </p>
);

export default Loading;
