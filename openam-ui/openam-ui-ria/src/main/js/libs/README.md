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
 * Copyright 2017 ForgeRock AS. All Rights Reserved
-->
# AM XUI Core-js custom build

## Instructions
In order not to include the whole set of ES5/ES6/ES7 polyfills, AM uses  [custom build][customBuild] facility provided by core-js.
You can build a custom core-js bundle using grunt:
```
$ yarn run grunt build:moduleA,moduleB -- --path=core-js-<version>-custom uglify
```
where `moduleA` and `moduleB` are modules to be included in the bundle. This will output 2 files in the `node_modules/core-js` folder: `core-js-<version>-custom.js` and `core-js-<version>-custom.js.map`. They both need to be copied over to the `openam-ui/openam-ui-ria/src/main/js/libs` folder.

## Core-js versions

### 2.5.0
Modules included: `es6.object.assign`, `es6.symbol`, `es6.array.iterator`
```
$ yarn run grunt build:es6.object.assign,es6.symbol,es6.array.iterator -- --path=core-js-<version>-custom uglify
```

[customBuild]: https://github.com/zloirock/core-js#custom-build-from-the-command-line
