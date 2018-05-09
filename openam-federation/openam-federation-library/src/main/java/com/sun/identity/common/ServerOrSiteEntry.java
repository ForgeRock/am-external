/*
 * Copyright 2012-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.common;

public class ServerOrSiteEntry {

    private String id;
    private String url;
    private String originalUrl;

    public ServerOrSiteEntry(String serverEntry) {
        int index = serverEntry.indexOf("|");
        if (index != -1) {
            // Keep a copy of the original URL to avoid cases where the OpenAM context is using mixed-case,
            // for example /OpenAM rather than say /openam
            originalUrl = serverEntry.substring(0, index);
            url = originalUrl.toLowerCase();
            id = serverEntry.substring(index + 1, serverEntry.length());

            index = id.indexOf("|");
            if (index != -1) {
                id = id.substring(0, 2);
            }
        } else {
            throw new IllegalArgumentException("Invalid server entry: " + serverEntry);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
