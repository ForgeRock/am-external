<!--
The contents of this file are subject to the terms of the Common Development and Distribution License (the License). You may not use this file except in compliance with the License.

You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the specific language governing permission and limitations under the License.
When distributing Covered Software, include this CDDL Header Notice in each file and include the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header, with the fields enclosed by brackets [] replaced by your own identifying information: "Portions copyright [year] [name of copyright owner]".

Copyright 2017-2024 ForgeRock AS.
-->

# am-external

This repository contains OpenAM and AM source code that you can use to modify and extend AM functionality. The repository contains branches corresponding to the version of OpenAM/AM that you're extending. At the time of writing this includes;


Product Version   |  Branch Name
------------------|-----------------
AM 7.5.0          |  releases/7.5.0
AM 7.3.1          |  releases/7.3.1
AM 7.3.0          |  releases/7.3.0
AM 7.2.1          |  releases/7.2.1
AM 7.2.0          |  releases/7.2.0
AM 7.1.4          |  releases/7.1.4
AM 7.1.3          |  releases/7.1.3
AM 7.1.2          |  releases/7.1.2
AM 7.1.1          |  releases/7.1.1
AM 7.1.0          |  releases/7.1.0
AM 7.0.2          |  releases/7.0.2
AM 7.0.1          |  releases/7.0.1
AM 7.0.0          |  releases/7.0.0
AM 6.5.5          |  releases/6.5.5
AM 6.5.4          |  releases/6.5.4
AM 6.5.3          |  releases/6.5.3
AM 6.5.2.3        |  releases/6.5.2.3
AM 6.5.2.2        |  releases/6.5.2.2
AM 6.5.2.1        |  releases/6.5.2.1
AM 6.5.2          |  releases/6.5.2
AM 6.5.1          |  releases/6.5.1
AM 6.5.0.2        |  releases/6.5.0.2
AM 6.5.0.1        |  releases/6.5.0.1
AM 6.5.0          |  releases/6.5.0
AM 6.0.0.7        |  releases/6.0.0.7
AM 6.0.0.6        |  releases/6.0.0.6
AM 6.0.0.5        |  releases/6.0.0.5
AM 6.0.0.4        |  releases/6.0.0.4
AM 6.0.0.3        |  releases/6.0.0.3
AM 6.0.0.2        |  releases/6.0.0.2
AM 6.0.0.1        |  releases/6.0.0.1
AM 6.0.0          |  releases/6.0.0
AM 5.5.2          |  releases/5.5.2
AM 5.5.1          |  releases/5.5.1
AM 5.5.0          |  releases/5.5.0
AM 5.1.1          |  releases/14.1.1  
AM 5.1.0          |  releases/14.1.0
AM 5.0            |  releases/14.0.0
OpenAM 13.5.0     |  releases/13.5.0
OpenAM 13.0.0     |  releases/13.0.0  
OpenAM 12.0.0     |  releases/12.0.0

### What code is available?

Looking at the 5.5.0 branch we can see that it contains the following directories, each containing code shipped with the AM 5.5.0;

```
openam-auth-trees        # The auth tree nodes and API classes  
openam-authentication    # The authentication modules  
openam-samples           # Sample code referred to in the AM 5.5 docs  
openam-ui                # The UI code   
openam-federation        # Code used to build the fedlet etc. n.b. This doesn't build as it's dependent on other elements of the am build. Best modify, build class files, unpack original war, jars, replace class file and rebuild jar, wars.
```

Each directory contains a README that explains more about how to use that code.

Note that not all branches contain the same directories. For example, `openam-auth-tree` will only exist in releases/5.5.0 onwards.

The `openam-authentication` and `openam-ui` only exists in releases/14.0.0 and later because the the full source code for those products is available in the [openam-sustaining-external](https://stash.forgerock.org/projects/OPENAM/repos/openam-sustaining-external/browse) repository.

## How to build this code

In order to build the code in this repository you will need to configure maven to be able to authenticate to the ForgeRock maven repository. You can do that by following the advice in this ForgeRock knowledge base article;
<https://backstage.forgerock.com/knowledge/kb/article/a74096897>

Once done you can clone the repository, checkout the branch corresponding to the version of OpenAM or AM that you're working with and run `mvn clean install`;

```
git clone https://github.com/ForgeRock/am-external.git
cd am-external
git checkout releases/5.5.0
mvn clean install
```
You can also just build sub directories of the repository. For example if you only want to build the authentication modules;

```
git clone https://github.com/ForgeRock/am-external.git
cd am-external
git checkout releases/5.5.0
cd openam-authentication
mvn clean install
```

**N.B.** AM 7.0.0 should be compiled and run using Java 11.

## Licensing

The code in this repository is licensed under the [CDDL license](https://forum.forgerock.com/cddlv1-0/) and may be used as the basis for your own projects.
Enjoy!
