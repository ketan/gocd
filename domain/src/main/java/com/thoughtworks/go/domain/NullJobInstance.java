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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.TimeProvider;

import java.util.Date;

public class NullJobInstance extends JobInstance {
    public NullJobInstance(String name) {
        super(name, new TimeProvider());
    }

    public long getId() {
        return 0;
    }

    public JobState getState() {
        return JobState.Unknown;
    }

    public JobResult getResult() {
        return JobResult.Unknown;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public Date getStartedDateFor(JobState jobState) {
        return new Date(0);
    }

    public JobState currentStatus() {
        return JobState.Waiting;
    }

    public String displayStatusWithResult() {
        return getState().toLowerCase();
    }

    public JobInstance mostRecentPassed(JobInstance champion) {
        return champion;
    }

    public String buildLocator() {
        return "NULLJOB";
    }

    public String buildLocatorForDisplay() {
        return buildLocator();
    }
}
