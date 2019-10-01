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

import com.thoughtworks.go.agent.cli.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.ssl.CertificateFileParser;
import com.thoughtworks.go.agent.system.GoAgentProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.net.ssl.HostnameVerifier;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import static java.lang.String.format;

@Slf4j
@Component
public class GoCDServerHttpClient {
    private final GoAgentProperties agentProperties;
    private final AgentBootstrapperArgs agentBootstrapperArgs;

    @Autowired
    public GoCDServerHttpClient(GoAgentProperties agentProperties, AgentBootstrapperArgs agentBootstrapperArgs) {
        this.agentProperties = agentProperties;
        this.agentBootstrapperArgs = agentBootstrapperArgs;
    }

    @Bean
    public CloseableHttpClient httpClient() {
        try {
            final SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true).setSoKeepAlive(true).build();

            return HttpClients.custom()
                    .useSystemProperties()
                    .setDefaultSocketConfig(socketConfig)
                    .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                    .setSSLSocketFactory(getSslConnectionSocketFactory())
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            log.error(format("Failed to create http client. Errors :%s", e));
            throw new RuntimeException(e);
        }
    }

//    public String requestToken() throws IOException {
//        CloseableHttpResponse response = httpClient().execute(new HttpGet(buildUrl("/go/token")));
//        return response.getEntity().toString();
//    }

//    private URI buildUrl(String path) {
//        try {
//            return new URIBuilder(agentBootstrapperArgs.getServerUrl().toURI())
//                    .setPath(path)
//                    .build();
//        } catch (URISyntaxException e) {
//            log.error(format("Failed to build url using path %s. Errors: %s", path, e));
//            throw new RuntimeException(e);
//        }
//    }

    private SSLConnectionSocketFactory getSslConnectionSocketFactory() throws IOException, GeneralSecurityException {
        HostnameVerifier hostnameVerifier = getHostnameVerifier(agentBootstrapperArgs.getSslVerificationMode());
        TrustStrategy trustStrategy = trustStrategy(agentBootstrapperArgs.getSslVerificationMode());
        KeyStore trustStore = agentTruststore();

        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();

        if (trustStore != null || trustStrategy != null) {
            sslContextBuilder.loadTrustMaterial(trustStore, trustStrategy);
        }

        return new SSLConnectionSocketFactory(sslContextBuilder.build(),
                agentProperties.getSslProtocol(),
                agentProperties.getCipherSuites(),
                hostnameVerifier);
    }

    private TrustStrategy trustStrategy(AgentBootstrapperArgs.SslMode sslVerificationMode) {
        if (sslVerificationMode == AgentBootstrapperArgs.SslMode.NONE) {
            return TrustAllStrategy.INSTANCE;
        } else {
            return null;
        }
    }

    private HostnameVerifier getHostnameVerifier(AgentBootstrapperArgs.SslMode sslVerificationMode) {
        if (sslVerificationMode == AgentBootstrapperArgs.SslMode.FULL) {
            return new DefaultHostnameVerifier();
        } else {
            return NoopHostnameVerifier.INSTANCE;
        }
    }

    private KeyStore agentTruststore() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        KeyStore trustStore = null;
        List<X509Certificate> certificates = new CertificateFileParser().certificates(agentBootstrapperArgs.getRootCertFile());
        for (X509Certificate certificate : certificates) {
            if (trustStore == null) {
                trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
            }
            trustStore.setCertificateEntry(certificate.getSubjectX500Principal().getName(), certificate);
        }

        return trustStore;
    }

    public void getWork() {

    }
}
