/*
 * Copyright 2016-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import JSONEditorView from "org/forgerock/openam/ui/common/views/jsonSchema/editors/JSONEditorView";
import TogglableJSONEditorView from "org/forgerock/openam/ui/common/views/jsonSchema/editors/TogglableJSONEditorView";

/**
  * @param {Object} schemaValuePair Map of a schema property and its values.
  * @returns {Object} A new JSONEditorView.
  * @module org/forgerock/openam/ui/common/views/jsonSchema/iteratees/createJSONEditorView
  */
export default function (schemaValuePair) {
    const Editor = schemaValuePair.schema.hasEnableProperty() ? TogglableJSONEditorView : JSONEditorView;
    return new Editor(schemaValuePair);
}
