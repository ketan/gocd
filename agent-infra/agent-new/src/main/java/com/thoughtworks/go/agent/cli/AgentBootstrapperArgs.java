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

package com.thoughtworks.go.agent.cli;

import com.beust.jcommander.Parameter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;
import java.net.URL;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class AgentBootstrapperArgs {

    public enum SslMode {
        FULL, NONE, NO_VERIFY_HOST
    }

    private static String SERVER_URL = "serverUrl";
    private static String SSL_VERIFICATION_MODE = "sslVerificationMode";
    private static String ROOT_CERT_FILE = "rootCertFile";

    @Parameter(names = "-serverUrl",
            description = "The GoCD server URL. Example: http://gocd.example.com:8153/go",
            required = true,
            validateWith = ServerUrlValidator.class)
    private URL serverUrl;

    @Parameter(names = "-rootCertFile",
            description = "The root certificate from the certificate chain of the GoCD server (in PEM format)",
            validateWith = CertificateFileValidator.class)
    private File rootCertFile;

    @Parameter(names = "-sslVerificationMode",
            description = "The SSL verification mode.")
    private SslMode sslVerificationMode = SslMode.FULL;

    @Parameter(names = "-help",
            help = true,
            description = "Print this help")
    boolean help;

}
