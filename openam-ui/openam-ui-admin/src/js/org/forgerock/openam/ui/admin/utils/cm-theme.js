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
import { EditorView } from "@codemirror/view";
import { HighlightStyle, syntaxHighlighting } from "@codemirror/language";
import { tags } from "@lezer/highlight";

const forgerockTheme = EditorView.theme({
    "&": {
        border: "1px solid #dbdbdb",
        backgroundColor: "#ffffff",
        color: "#2C2233"
    },
    ".cm-gutters": {
        color: "#777",
        backgroundColor: "#f7f7f7"
    },
    ".cm-placeholder": {
        opacity: 0.5
    }
});

const forgerockHighlightStyle = HighlightStyle.define([
    { tag: tags.keyword, color: "#BD8D46" },
    { tag: tags.tagName, color: "#BD8D46" },
    { tag: tags.bracket, color: "#BD8D46" },
    { tag: tags.variableName, color: "#2C2233" },
    { tag: tags.operator, color: "#1A9481" },
    { tag: tags.number, color: "#005869" },
    { tag: tags.comment, color: "#999999" },
    { tag: tags.string, color: "#8DB500" },
    { tag: tags.atom, color: "#A00059" },
    { tag: tags.attributeName, color: "#A00059" },
    { tag: tags.propertyName, color: "#A00059" },
    { tag: tags.bool, color: "#A00059" }
]);

export const forgerockCmTheme = [
    forgerockTheme,
    syntaxHighlighting(forgerockHighlightStyle)
];
