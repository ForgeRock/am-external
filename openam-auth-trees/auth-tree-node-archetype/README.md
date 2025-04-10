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
 * Copyright 2017-2025 Ping Identity Corporation.
-->
# auth-tree-node-archetype

A [Maven Archetype](http://maven.apache.org/archetype/maven-archetype-plugin/usage.html) that can be used to create a maven project for the generation of a [ForgeRock](https://www.forgerock.com/) [Authentication Tree Node](https://backstage.forgerock.com/docs/am/7.2/authentication-guide/about-authentication-modules-and-chains.html).

## Usage
Clone the AM repo and build the archetype project so the archetype definition is published to your local .m2 repository;

```
$ git clone ssh://git@stash.forgerock.org:7999/openam/openam.git
$ cd openam/openam-auth-trees/auth-tree-node-archetype
$ mvn clean install 
```

Now you can use the archetype:

```
mvn archetype:generate \
-DgroupId=<your auth node's groupId> \
-DartifactId=<your auth node's artefactId \
-Dversion=<your auth node's version> \
-DpackageName=<your auth node's package name> \
-DauthNodeName=<name of your auth node class> \
-DarchetypeGroupId=org.forgerock.am \
-DarchetypeArtifactId=auth-tree-node-archetype \
-DarchetypeVersion=<auth-tree-node-archetype pom version eg. 7.3.0> \
-DinteractiveMode=false
```

Most of these properties are standard for archetypes. The only property specific to this archetype is the authNodeName property. When your project is generated the value specified for the `authNodeName` property  will be used to name the Auth Tree Node java files and class names. For example, if authNodeName=MyNode your project will end up with a MyNode.java and MyNodePlugin.java classes being generated in the package specified by the `packageName` property.


Enjoy!
