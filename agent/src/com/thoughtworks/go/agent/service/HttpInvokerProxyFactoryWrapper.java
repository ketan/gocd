/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.util.URLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.HttpInvokerRequestExecutor;
import org.springframework.stereotype.Service;

@Service
public class HttpInvokerProxyFactoryWrapper extends HttpInvokerProxyFactoryBean {

    public HttpInvokerProxyFactoryWrapper() {
        this.setServiceInterface(com.thoughtworks.go.remote.BuildRepositoryRemote.class);
    }

    @Autowired
    public void setUrlService(URLService urlService) {
        super.setServiceUrl(urlService.getBuildRepositoryURL());
    }

    @Override
    @Autowired
    public void setHttpInvokerRequestExecutor(HttpInvokerRequestExecutor httpInvokerRequestExecutor) {
        super.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor);
    }
}
