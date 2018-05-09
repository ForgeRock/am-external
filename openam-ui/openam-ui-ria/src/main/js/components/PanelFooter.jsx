/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import { t } from "i18next";
import React from "react";

const PanelFooter = () => (
    <div className="panel-footer clearfix">
        <input
            className="btn btn-primary pull-right"
            data-save
            name="submitForm"
            type="button"
            value={ t("common.form.saveChanges") }
        />
    </div>
);

export default PanelFooter;
