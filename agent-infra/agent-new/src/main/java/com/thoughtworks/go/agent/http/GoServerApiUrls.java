/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.agent.http;

import org.apache.http.client.utils.URIBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

class GoServerApiUrls {
    private final URL serverUrl;

    GoServerApiUrls(URL serverUrl) {
        String url = serverUrl.toString();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        try {
            this.serverUrl = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    URI tokenUrl() {
        return buildUrl("token");
    }

    private URI buildUrl(String path) {
        try {
            return new URIBuilder(serverUrl.toURI())
                    .setPath(serverUrl.getPath() + "/agent_services/" + path)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    URI registerUrl() {
        return buildUrl("register");
    }

    URI cookieUrl() {
        return buildUrl("cookie");
    }
}
