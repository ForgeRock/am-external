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
 * Copyright 2025 Ping Identity Corporation.
 */

// Add Signals SDK namespace to the window object if not present
if (!window._pingOneSignals) {
    window._pingOneSignals = {
        init: async () => { throw new Error("PingOne Signals SDK not loaded"); },
        getData: async () => "",
        pauseBehavioralData: () => {},
        resumeBehavioralData: () => {}
    };
}

/**
 * P1Protect - Class to interact with the underlying PingOne Signals SDK
 */
export class P1Protect {
    /**
     * Get device data collected by Signals SDK
     * @returns {Promise<string>}
     */
    static async getData () {
        return await window._pingOneSignals.getData();
    }

    /**
     * Initialize and start the PingOne Signals SDK
     * @param {object} options - init parameters
     * @returns {Promise<void>}
     */
    static async start (options) {
        try {
            await import("../../../../../../libs/ping-signals-sdk.js");
        } catch (err) {
            console.error("Error loading ping signals", err);
        }

        await window._pingOneSignals.init(options);

        if (options.behavioralDataCollection === true) {
            window._pingOneSignals.resumeBehavioralData();
        }
    }

    /**
     * Pause behavioral data collection
     */
    static pauseBehavioralData () {
        window._pingOneSignals.pauseBehavioralData();
    }

    /**
     * Resume behavioral data collection
     */
    static resumeBehavioralData () {
        window._pingOneSignals.resumeBehavioralData();
    }
}
