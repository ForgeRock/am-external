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
 * Copyright 2025 ForgeRock AS.
-->
The fido-mds3-blob.jwt and fido-mds3-root.crt files in this directory are copies of those which can be obtained from the [FIDO Alliance](https://fidoalliance.org/metadata/).

These files are used in the unit tests to help validate that AM is capable of downloading and processing the metadata 
from the FIDO Alliance.

If the certification in this directory expires, or requires updating then a new certificate should be able to be 
obtained from the relevant location as specified by the FIDO alliance in the link above.

Current locations for this are as follows (but may change if the FIDO Alliance updates their metadata service):
- FIDO Metadata Service v3.0 BLOB: https://mds3.fidoalliance.org/
- FIDO Metadata Service v3.0 Root Certificate: https://valid.r3.roots.globalsign.com/

Similarly, the remaining files in this directory are copies of those which
can be obtained from the FIDO Alliance via their [MDS3 Test Server Resources](https://mds3.fido.tools/):

If the mds-test-root.crt expires, or requires updating then a new certificate can be
obtained from the FIDO alliance using the link above, specifying https://am.localtest.me/openam
as the 'Server Endpoint'. The following jwt files can also be regenerated from the URLs presented, if required:

- invalid-signature-blob.jwt (Obtained via URL #2)
- invalid-certificate-chain-blob.jwt (Obtained via URL #4)
- revoked.certificate-blob.jwt (Obtained via URL #5)

These files are used here to help validate that AM correctly handles the processing of metadata in accordance with
the metadata processing rules set out in the
[FIDO Metadata Specification : Metadata Process Rules](http://fidoalliance.org/specs/mds/fido-metadata-service-v3.0-ps-20210518.html#metadata-blob-object-processing-rules).
