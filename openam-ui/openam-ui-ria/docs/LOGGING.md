<!--
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
 * Copyright 2019 ForgeRock AS. All Rights Reserved
-->

# Logging <!-- omit in toc -->

- [Overview](#overview)
- [Enabling](#enabling)
- [Namespaces](#namespaces)
- [Message Formatting](#message-formatting)

## Overview

Application logging is provided via [`debug`][github-debug].

By default, all logging output is disabled.

## Enabling

Enable logging to your browser's developer console ([Chrome][chrome-console] or [Firefox][firefox-console]) as follows:

Enable all logging:

```javascript
localStorage.debug = "forgerock:am:user:*";
```

Enable logging for a particular namespace:

```javascript
localStorage.debug = "forgerock:am:user:<NAMESPACE>:*";
```

For a full description of available syntax, see `debug`'s [README][github-debug].

## Namespaces

| Namespace | Description |
|-|-|
forgerock:am:user | Root namespace |
forgerock:am:user:view | View rendering & loading |
forgerock:am:user:view:partial | Partial loading |
forgerock:am:user:view:template | Template loading |

## Message Formatting

- Keep messages clear and concise
- Use backticks when quoting text, for example: ``Could not find `Message`.``
- Always complete a message with a period `.`

[chrome-console]: https://developers.google.com/web/tools/chrome-devtools/console
[firefox-console]: https://developer.mozilla.org/en-US/docs/Tools/Web_Console/Opening_the_Web_Console
[github-debug]: https://github.com/visionmedia/debug
