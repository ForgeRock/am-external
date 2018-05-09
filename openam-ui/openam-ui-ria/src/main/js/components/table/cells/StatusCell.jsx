/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { t } from "i18next";
import React from "react";

const StatusCell = (cell) => {
    if (!cell) { return; }
    if (cell.toLowerCase() === "active") {
        return (
            <span className="text-success">
                <i className="fa fa-check-circle" /> { t("common.form.active") }
            </span>
        );
    } else {
        return (
            <span className="text-warning">
                <i className="fa fa-ban" /> { t("common.form.inactive") }
            </span>
        );
    }
};

export default StatusCell;
