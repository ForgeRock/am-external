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
 * Copyright 2026 Ping Identity Corporation.
 */

import $ from "jquery";
import { escape } from "lodash";
import { t } from "i18next";
import BootstrapDialog from "org/forgerock/commons/ui/common/components/BootstrapDialog";

/**
 * Shows the result of the PingOne Worker connection test.
 * @param {object} results - The JSON response from the server.
 * @param {string} [status="success"] - The status of the test ("success" or "error").
 */
export default function PingOneWorkerTestDialog (results, status = "success") {
    const isError = status === "error";
    const alertClass = isError ? "alert-danger" : "alert-success";
    const iconClass = isError ? "fa-exclamation-triangle" : "fa-check-circle";

    /**
     * Converts camelCase to Title Case (e.g., environmentId -> Environment ID)
     * @param {string} str - The key to format.
     * @returns {string} The formatted label.
     */
    const formatLabel = (str) => {
        const result = str
            .replace(/([A-Z])/g, " $1")
            .replace(/^./, (s) => s.toUpperCase())
            .replace(/Id\b/g, "ID")
            .replace(/Url\b/g, "URL")
            .replace(/Api\b/g, "API");
        return result.trim();
    };

    const listItems = Object.entries(results).map(([key, value]) => `
        <div class="result-item">
            <div class="result-label">
                ${escape(formatLabel(key))}
            </div>
            <div class="result-value">
                ${escape(value)}
            </div>
        </div>
    `).join("");

    const resultsHtml = `
        <div class="pingone-test-results">
            <div class="alert ${alertClass}">
                <i class="fa ${iconClass} fa-lg"></i>
                <span>${isError ? t("pingone.worker.test.error") : t("pingone.worker.test.success")}</span>
            </div>
            <div class="well well-sm">
                ${listItems}
            </div>
        </div>
    `;

    BootstrapDialog.show({
        title: t("pingone.worker.test.title"),
        type: BootstrapDialog.TYPE_PRIMARY,
        message: $(resultsHtml),
        buttons: [{
            label: t("common.form.close"),
            cssClass: isError ? "btn-danger" : "btn-primary",
            action (dialog) {
                dialog.close();
            }
        }]
    });
}
