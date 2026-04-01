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
 * Copyright 2026 Ping Identity Corporation.
-->
## Node Versioning

### Creating a New Version of a Node
The following steps specify how to create a new version of an existing node. `CustomAuthNodeV2` provides a useful
example to follow.
1. **Create a Java class for the new version.**
2. **Add the `@Node.VersionMetadata` annotation to the class**, including
   - `name`: This should match either the `name` property of a previous version, or the class name of the previous
     version if it does not have `name` defined.
   - `version`: An integer that is higher than the previous version. The previous version is `1` if not defined.
3. **Optionally, create an upgrader for the new version.**
   1. Create a class that implements the `NodeVersionUpgrader` interface. It should also be annotated with
      `@NodeVersionUpgrader.UpgraderMetadata`, which includes
      - `version`: The new version of the node.
      - `previousUpgrader`: The upgrader class for the previous version. If the previous version does not have an
        upgrader, this should be `NoUpgrades.class`.
   2. Implement the `upgrade()` method, which maps the JSON configuration of the previous version to the new version.
   3. Define the `upgrader` property in the `@Node.VersionMetadata` annotation, pointing to the upgrader class.

### Removing a Version of a Node
In some circumstances, it may be necessary to remove a version of a node. In order to minimise the impact on customers,
follow these steps:
1. **Ensure a newer version of the node exists.**
2. **Mark the old version as deprecated**:
   1. Add the `@Deprecated` annotation to the corresponding class.
   2. Add the node version to the [AM Feature Deprecation Tracker](
      https://docs.google.com/spreadsheets/d/10u1ZaUR4kXecJiY65WkdEQLoARxrpkzLnSOAl-EF8SM/edit?usp=sharing) spreadsheet.
3. **Allow time for customers to migrate**: Under normal circumstances, the minimum time to allow customers to migrate
   is 12 months. This may be reduced in the case of a severe security vulnerability.
4. **Ensure no customers are using the old version**.
   1. Directly notify any customers who are using the old version.
5. **Remove the old version**: Delete the old version of the node from the codebase once all customers have stopped 
   using it.
