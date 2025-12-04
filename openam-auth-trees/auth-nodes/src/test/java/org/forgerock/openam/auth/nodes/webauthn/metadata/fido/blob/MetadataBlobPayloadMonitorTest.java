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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.forgerock.openam.utils.Time;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MetadataBlobPayloadMonitorTest {

    @TempDir
    private Path tmpDirectory;

    @BeforeAll
    static void setup() {
        TimeTravelUtil.useFastForwardClock();
    }

    @AfterAll
    static void tearDown() {
        TimeTravelUtil.resetClock();
    }

    @Test
    void shouldDetectFileExpiry() throws Exception {
        Path mdsBlob = tmpDirectory.resolve("mds.jwt");
        Path otherFile = tmpDirectory.resolve("other.jwt");
        Files.createFile(mdsBlob);
        Files.createFile(otherFile);

        MetadataBlobPayloadMonitor monitor = new MetadataBlobPayloadMonitor(
                Instant.MAX, mdsBlob.toAbsolutePath().toString());
        assertThat(monitor.isExpired()).isFalse();

        // Assert that only the in use file and not all files in the directory are being monitored
        Files.delete(otherFile);
        assertThat(Files.exists(otherFile)).isFalse();
        Thread.sleep(10);
        assertThat(monitor.isExpired()).isFalse();

        Files.delete(mdsBlob);
        assertThat(Files.exists(mdsBlob)).isFalse();
        for (int i = 0; i < 100 && !monitor.isExpired(); i++) {
            Thread.sleep(10);
        }
        assertThat(monitor.isExpired()).isTrue();

        monitor.reset();
        assertThat(monitor.isExpired()).isFalse();
    }

    @Test
    void shouldDetectUrlExpiry() throws Exception {
        MetadataBlobPayloadMonitor monitor = new MetadataBlobPayloadMonitor(
                Time.instant().plus(Duration.of(365, ChronoUnit.DAYS)), "https://mds.localtest.me");

        assertThat(monitor.isExpired()).isFalse();
        TimeTravelUtil.fastForward(365, TimeUnit.DAYS);

        assertThat(monitor.isExpired()).isTrue();

        monitor.reset();
        assertThat(monitor.isExpired()).isFalse();

        // Checks that one day is added to the expiry time if the current time is past the next update time to
        // avoid spamming the endpoint
        TimeTravelUtil.fastForward(1, TimeUnit.DAYS);
        assertThat(monitor.isExpired()).isTrue();

    }
}
