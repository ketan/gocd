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

package com.thoughtworks.go.api.agentservices.converters;

import com.google.protobuf.MessageLite;
import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.protobufs.artifacts.ArtifactPlanProto;
import com.thoughtworks.go.protobufs.artifacts.ArtifactStoreProto;
import com.thoughtworks.go.protobufs.artifacts.ArtifactTypeProto;
import com.thoughtworks.go.protobufs.materials.GitConfigProto;
import com.thoughtworks.go.protobufs.materials.GitProto;
import com.thoughtworks.go.protobufs.materials.GitRevisionProto;
import com.thoughtworks.go.protobufs.pipelineconfig.EnvironmentVariableProto;
import com.thoughtworks.go.protobufs.plugin.ConfigurationPropertyProto;
import com.thoughtworks.go.protobufs.tasks.ExecProto;
import com.thoughtworks.go.protobufs.tasks.JobIdentifierProto;
import com.thoughtworks.go.protobufs.tasks.PipelineIdentifierProto;
import com.thoughtworks.go.protobufs.tasks.StageIdentifierProto;
import com.thoughtworks.go.protobufs.work.MaterialProto;
import com.thoughtworks.go.protobufs.work.WorkProto;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.toprotobuf.TaskConverterFactory;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@SuppressWarnings("ALL")
public class Converter {
    public static MessageLite toProtoWork(BuildWork work, List<Task> tasks) {
        BuildAssignment assignment = work.getAssignment();
        JobIdentifier jobIdentifier = assignment.getJobIdentifier();

        final List<EnvironmentVariableProto> envVars = assignment.initialEnvironmentVariableContext()
                .getAll()
                .stream()
                .map(Converter::toProto)
                .collect(toList());

        final List<ArtifactPlanProto> artifactPlans = assignment.getArtifactPlans()
                .stream()
                .map(Converter::toProto)
                .collect(toList());

        List<ArtifactStoreProto> artifactStores = assignment.getArtifactStores()
                .stream()
                .map(Converter::toProto)
                .collect(toList());

        return WorkProto.newBuilder()
                .setJobIdentifier(toProto(jobIdentifier))
                .setFetchMaterials(assignment.shouldFetchMaterials())
                .setCleanWorkingDirectory(assignment.shouldCleanWorkingDir())
                .setBuildWorkingDirectory(assignment.getWorkingDirectory().getPath())
                .addAllEnvironmentVariable(envVars)
                .addAllMaterial(toProto(assignment.getMaterialRevisions()))
                .addAllTask(tasks.stream().map(Converter::toProto).collect(toList()))
                .addAllArtifactPlan(artifactPlans)
                .addAllArtifactStore(artifactStores)
                .build();

    }

    private static ArtifactStoreProto toProto(ArtifactStore store) {
        return ArtifactStoreProto.newBuilder()
                .setId(store.getId())
                .setPluginId(store.getPluginId())
                .addAllProperty(store.stream().map(Converter::toProto).collect(toList()))
                .build();
    }

    private static ConfigurationPropertyProto toProto(ConfigurationProperty configurationProperty) {
        return ConfigurationPropertyProto.newBuilder()
                .setName(configurationProperty.getConfigKeyName())
                .setValue(configurationProperty.getValue())
                .setSecure(configurationProperty.isSecure())
                .build();
    }

    private static ArtifactPlanProto toProto(ArtifactPlan artifactPlan) {
        return ArtifactPlanProto.newBuilder()
                .setType(toProto(artifactPlan.getArtifactPlanType()))
                .setSource(artifactPlan.getSrc())
                .setDestination(artifactPlan.getDest())
                .build();
    }

    private static ArtifactTypeProto toProto(ArtifactPlanType type) {
        switch (type) {
            case unit:
                return ArtifactTypeProto.UNIT;
            case file:
                return ArtifactTypeProto.FILE;
            case external:
                return ArtifactTypeProto.EXTERNAL;
            default:
                throw new IllegalArgumentException(String.format("Artifact type %s is unknown.", type));
        }
    }

    public static EnvironmentVariableProto toProto(EnvironmentVariableContext.EnvironmentVariable environmentVariable) {
        return EnvironmentVariableProto.newBuilder()
                .setName(environmentVariable.name())
                .setValue(environmentVariable.value())
                .setSecure(environmentVariable.isSecure())
                .build();
    }

    public static JobIdentifierProto toProto(JobIdentifier jobIdentifier) {
        PipelineIdentifierProto pipelineIdentifier = PipelineIdentifierProto.newBuilder()
                .setPipelineName(jobIdentifier.getPipelineName())
                .setPipelineCounter(jobIdentifier.getPipelineCounter())
                .build();

        StageIdentifierProto stageIdentifier = StageIdentifierProto.newBuilder()
                .setStageCounter(Long.parseLong(jobIdentifier.getStageCounter()))
                .setStageName(jobIdentifier.getStageName())
                .setPipelineIdentifier(pipelineIdentifier)
                .build();

        return JobIdentifierProto.newBuilder()
                .setJobName(jobIdentifier.getBuildName())
                .setStageIdentifier(stageIdentifier)
                .build();
    }

    public static ExecProto toProto(Task task) {
        return new TaskConverterFactory().toTask(task);
    }

    public static List<MaterialProto> toProto(MaterialRevisions materialRevisions) {
        return StreamSupport.stream(materialRevisions.spliterator(), false)
                .map(Converter::toProto)
                .collect(toList());
    }

    public static MaterialProto toProto(MaterialRevision materialRevision) {
        MaterialProto.Builder builder = MaterialProto.newBuilder();
        if (materialRevision.getMaterial() instanceof GitMaterial) {
            builder.setGit(toProto(materialRevision, (GitMaterial) materialRevision.getMaterial()));
        }
        return builder.build();
    }

    public static GitProto toProto(MaterialRevision materialRevision, GitMaterial material) {
        return GitProto.newBuilder()
                .setConfig(toProto(material))
                .setPrevious(toProto(materialRevision.getModifications().first()))
                .setLatest(toProto(materialRevision.getModifications().last()))
                .build();
    }

    public static GitConfigProto toProto(GitMaterial material) {
        GitConfigProto.Builder builder = GitConfigProto.newBuilder()
                .setUrl(material.urlForCommandLine())
                .setBranch(material.getBranch())
                .setShallow(material.isShallowClone());

        if (isNotBlank(material.getUserName())) {
            builder.setUsername(material.getUsername());
        }

        if (isNotBlank(material.getPassword())) {
            builder.setPassword(material.getPassword());
        }
        return builder.build();
    }

    public static GitRevisionProto toProto(Modification modification) {
        return GitRevisionProto.newBuilder().setSha(modification.getRevision()).build();
    }
}
