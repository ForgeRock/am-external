<!-- 
The contents of this file are subject to the terms of the Common Development and Distribution License (the License). You may not use this file except in compliance with the License.

You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the specific language governing permission and limitations under the License.
When distributing Covered Software, include this CDDL Header Notice in each file and include the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL Header, with the fields enclosed by brackets [] replaced by your own identifying information: "Portions copyright [year] [name of copyright owner]".

Copyright 2017 ForgeRock AS.
-->

# am-external

This repository contains OpenAM and AM source code that you can use to modify and extend AM functionality. The repository contains branches co-responding to the version of OpenAM/AM that you're extending. At the time of writing this includes;


Product Version   |  Branch Name
------------------|-----------------
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
openam-auth-trees		# The auth tree nodes and API classes  
openam-authentication	# The authentication modules  
openam-samples			# Sample code referred to in the AM 5.5 docs  
openam-ui				# The UI code   
```

Each directory contains a README that explains more about how to use that code. 

Note that not all branches contain the same directories. For example, `openam-auth-tree` will only exist in releases/5.5.0 onwards. 

The `openam-authentication` and `openam-ui` only exists in releases/14.0.0 and later because the the full source code for those products is available in the [openam-sustaining-external](https://stash.forgerock.org/projects/OPENAM/repos/openam-sustaining-external/browse) repository. 

## How to build this code

In order to build the code in this repository you will need to configure maven to be able to authenticate to the ForgeRock maven repository. You can do that by following the advice in this ForgeRock knowledge base article;
<https://backstage.forgerock.com/knowledge/kb/article/a74096897>

It's also easier to work with the repository if you [create](https://confluence.atlassian.com/bitbucketserver045/using-bitbucket-server/controlling-access-to-code/using-ssh-keys-to-secure-git-operations/creating-ssh-keys) an SSH key and [add](https://confluence.atlassian.com/bitbucketserver045/using-bitbucket-server/controlling-access-to-code/using-ssh-keys-to-secure-git-operations/ssh-access-keys-for-system-use) it to your Bitbucket profile to allow you to clone the source code with an SSH URL.

Once done you can clone the repository, checkout the branch corresponding to the version of OpenAM or AM that you're working with and run `mvn clean install`;

```
git clone ssh://git@stash.forgerock.org:7999/openam/am-external.git
cd am-external
git checkout releases/5.5.0
mvn clean install
```
You can also just build sub directories of the repository. For example if you only want to build the authentication modules;

```
git clone ssh://git@stash.forgerock.org:7999/openam/am-external.git
cd am-external
git checkout releases/5.5.0
cd openam-authentication
mvn clean install
```

## Licensing

The code in this repository is licensed under the [CDDL license](https://forum.forgerock.com/cddlv1-0/) and may be used as the basis for your own projects.
Enjoy!

![ForgeRock Logo](https://www.forgerock.com/app/uploads/2017/02/forgerock-logo-400px-square-300x300.png) 