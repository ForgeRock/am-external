/*
 * Copyright 2011-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import "babel-polyfill";
import "regenerator-runtime/runtime";

import ProcessConfiguration from "org/forgerock/commons/ui/common/main/ProcessConfiguration";
import QRCodeReader from "org/forgerock/openam/server/util/QRCodeReader";

/**
 * Enables openam-core/src/main/java/org/forgerock/openam/utils/qr/GenerationUtils.java to access
 * QRCode generation.
 */
window.QRCodeReader = QRCodeReader;

ProcessConfiguration.loadEventHandlers();
