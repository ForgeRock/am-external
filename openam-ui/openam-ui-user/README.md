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
 * Copyright 2015-2024 ForgeRock AS. All Rights Reserved
-->

# Access Management (AM) UI - User <!-- omit in toc -->

- [Overview](#overview)
- [Introduction](#introduction)
- [Building](#building)
  - [Prerequisites](#prerequisites)
  - [Scripts](#scripts)
    - [`start`](#start)
    - [`build`](#build)
    - [`test`](#test)
    - [`profile`](#profile)
  - [Polyfills](#polyfills)
  - [Upgrading Dependencies](#upgrading-dependencies)
- [Integrating the XUI Build Process](#integrating-the-xui-build-process)
  - [Building with Maven](#building-with-maven)
    - [Prerequisites](#prerequisites-1)
    - [Description](#description)
    - [Example](#example)
  - [Building with Yarn](#building-with-yarn)
    - [Example](#example-1)

## Overview

This module contains the Access Management (AM) user interface, named "XUI". The XUI delivers user-facing pages.

This document covers the build process to use when customizing the XUI.

## Introduction

The build process for XUI uses the *Webpack* resource bundler to manage dependencies, optimize deliverables, and package the output.

After making changes to the XUI, rebuilding the project using Webpack is necessary. Changes that required a rebuild include:

- JavaScript
- Templates
- Styles
- Translations

When complete, you can deploy the rebuilt output to your Access Management instances.

## Building

This section covers how to build the XUI.

### Prerequisites

- [Node.js][nodejs] (Recommended installed via [NVM][github-nvm])
- [Yarn][yarn]

With the prerequisites installed, run the following command to download the dependencies the XUI requires:

```sh
yarn install
```

When complete, you can use the scripts provided in the `package.json` file to build and run the XUI.

### Scripts

The XUI provides a number of scripts to help when customizing, building, and testing.

The XUI configuration provides the following scripts:

#### `start`

```js
yarn start // Shortcut for `yarn run start` (Recommended)
npm start // Alternative
```

Starts the XUI in development mode.

Development mode enables:

- Source maps
- Automatic rebuild on source changes
- Automatic browser refresh

The following environment variables are required to execute the `start` script:

- `AM_HOSTNAME`
- `AM_PORT`
- `AM_PATH`

If the required environment variables are not present, the script asks you to provide them.

When values for all required environment variables are provided, the script will save their values to an `.env` file (at the root of the UI project).

> ***Tip***
> [dotenv][github-dotenv] is used to load environment variables during the development build process. Environment variables are loaded in the following order:
> 1) System environment variables
> 1) `.env` file

If the required environment variables are found, the `start` script launches a dedicated [Webpack Dev Server][github-webpack-dev-server]. The XUI on that dedicated server connects to a separate instance of AM running on a different port, but using the same domain.

HTTP requests from the XUI are proxied to the target AM instance. The XUI behaves as it would in production, except with the addition of development tooling, such as automatic browser refreshing when code is changed.

To start developing with the XUI:
1. Start an AM server.
   For example, `http://am.example.com:8080/openam`.
2. Start an XUI development server by using the `yarn start` command.
   For example, running at `http://am.example.com:8081`.
3. In a web browser, navigate to the URL of your **AM instance**, but use the _port number_ of the **XUI development server**.
   For example, `http://am.example.com:8081/openam/XUI/#login`.

> ***Note***
JATO pages redirect to the deployment port of AM - `8080` instead of `8081`.

#### `build`

```js
yarn run build // Recommended
npm run build // Alternative
```

Performs a one-time build of the XUI, in production mode. Outputs to the `build` directory.

#### `test`

```js
yarn run test // Recommended
npm run test // Alternative
```

Compiles and runs the unit tests.

> ***Tip***
> Use this command in a separate terminal window with the `--watch` switch to continuously run tests while developing.

#### `profile`

```js
yarn run profile // Recommended
npm run profile // Alternative
```

Performs a one-time build of the XUI, in production mode, with profiling enabled.

Generates a report in the `build` directory named `report.html` detailing the structure of the bundles and chunks that were created as part of the production build.

### Polyfills

The babel-preset-env `useBuiltIns` option automatically detects and imports required polyfills.

### Upgrading Dependencies

Dependencies are "locked" to explicit versions through these mechanisms:

- The dependencies list in `package.json`.
  For example, `"lodash": "4.1.0"` specifies an explicit version, without using `"^"` or `"~"` characters.
- The `yarn.lock` file, generated by Yarn.
  Use the [`yarn outdated`][yarn-outdated] command to view out-of-date dependencies.

> ***Tip***
> [Visual Studio Code][vscode] shows package version information when hovering the cursor within `package.json`, as shown below:
>
> ![Visual Studio Code package.json hover](./docs/vscode-package-json-versions.gif)

## Integrating the XUI Build Process

You can integrate the XUI build with your existing build process.

### Building with Maven

The AM build process uses Maven, and also builds the XUI.

#### Prerequisites

- Maven

#### Description

You can execute the JavaScript build process by using the Maven tool.

Node.js and other supporting tools are installed locally and automatically when building with Maven, and they will not conflict with any globally installed tools.

#### Example

```sh
mvn clean install
```
The output is a ZIP artifact that is expanded and included in the AM .WAR file.

### Building with Yarn

You can execute the JavaScript build process directly by using the Yarn tool.

Node.js and other supporting tools are *not* installed automatically - you will need to manually install Node.js and Yarn.

#### Example

```sh
yarn run build
```
The result is production-ready XUI output in the `build` directory.

[github-dotenv]: https://github.com/motdotla/dotenv
[github-nvm]: https://github.com/creationix/nvm
[github-webpack-dev-server]: https://github.com/webpack/webpack-dev-server
[nodejs]: https://nodejs.org/en
[vscode]: https://code.visualstudio.com
[yarn-outdated]: https://yarnpkg.com/lang/en/docs/cli/outdated
[yarn]: https://yarnpkg.com