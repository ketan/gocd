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
package com.thoughtworks.go.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class UrlUtil {

    private static final String UTF_8 = "UTF-8";

    public static String encodeInUtf8(String url) {
        String[] parts = url.split("/");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            try {
                builder.append(URLEncoder.encode(part, UTF_8));
            } catch (UnsupportedEncodingException e) {
                bomb(e);
            }
            if (i < parts.length - 1) {
                builder.append('/');
            }
        }
        if (url.endsWith("/")) {
            builder.append('/');
        }
        return builder.toString();
    }

    public static String urlWithQuery(String oldUrl, String paramName, String paramValue) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(oldUrl);
        uriBuilder.addParameter(paramName, paramValue);
        return uriBuilder.toString();
    }

    public static String getQueryParamFromUrl(String url, String paramName) {
        try {
            List<NameValuePair> queryParams = new URIBuilder(url).getQueryParams();
            for (NameValuePair pair : queryParams) {
                if (pair.getName().equals(paramName)) {
                    return pair.getValue();
                }
            }
            return StringUtils.EMPTY;
        } catch (URISyntaxException e) {
            return StringUtils.EMPTY;
        }
    }

    public static String concatPath(String baseUrl, String path) {
        StringBuilder builder = new StringBuilder(baseUrl);
        if(!baseUrl.endsWith("/")) {
            builder.append('/');
        }
        builder.append(path);
        return builder.toString();
    }

    public static String urlWithoutCredentials(String originalUrl) {
        try {
            if (isSupportedUrl(originalUrl)) {
                String[] credentials = getCredentials(originalUrl);
                if (credentials != null) {

                    URI url = new URI(originalUrl);
                    return new URI(url.getScheme(), null, url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getFragment()).toString();
                }
            }
            return originalUrl;
        } catch (URISyntaxException e) {
            return originalUrl;
        }
    }

    public static String urlWithCredentials(String urlWithoutCredentials, String username, String password) {
        try {
            if (isSupportedUrl(urlWithoutCredentials)) {
                String credentials = "";
                // intentionally not checking `blank` whitespace is still a valid (though unlikely) username/password
                if (username != null && !username.equals("")) {
                    credentials += username;
                }

                if (password != null && !password.equals("")) {
                    credentials += ":" + password;
                }

                URI url = new URI(urlWithoutCredentials);

                return new URI(url.getScheme(), credentials.equals("") ? null : credentials, url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getFragment()).toString();
            }
            return urlWithoutCredentials;
        } catch (URISyntaxException e) {
            return urlWithoutCredentials;
        }
    }

    public static String getUsername(String originalUrl) {
        try {
            if (isSupportedUrl(originalUrl)) {
                String[] credentials = getCredentials(originalUrl);
                if (credentials != null) {
                    if ("".equals(credentials[0])) {
                        return null;
                    } else {
                        return credentials[0];
                    }
                }
            }
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static String getPassword(String originalUrl) {
        try {
            if (isSupportedUrl(originalUrl)) {
                String[] credentials = getCredentials(originalUrl);
                if (credentials != null && credentials.length >= 2) {
                    if ("".equals(credentials[1])) {
                        return null;
                    } else {
                        return credentials[1];
                    }
                }
            }
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean isSupportedUrl(String originalUrl) throws URISyntaxException {
        if (isNotBlank(originalUrl) && (originalUrl.startsWith("http") || originalUrl.startsWith("https"))) {
            new URI(originalUrl);
            return true;
        }

        return false;
    }

    private static String[] getCredentials(String originalUrl) throws URISyntaxException {

        String userInfo = new URI(originalUrl).getUserInfo();
        if (isNotBlank(userInfo)) {
            return userInfo.split(":", 2);
        }
        return null;
    }

}
