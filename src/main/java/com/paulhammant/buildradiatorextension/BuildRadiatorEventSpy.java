/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.paulhammant.buildradiatorextension;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.project.MavenProject;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class BuildRadiatorEventSpy extends AbstractEventSpy {

    private boolean projectPropertiesDone = false;

    private BuildRadiatorInterop buildRadiatorInterop;

    @Override
    public void init(Context context) {
        buildRadiatorInterop = new BuildRadiatorInterop(
                System.getenv("buildId"),
                System.getenv("buildingThisArtifact"),
                System.getenv("radiatorCode"),
                System.getenv("radiatorSecret"));
        System.err.println("*** BuildRadiatorEventSpy starting up");
    }

    @Override
    public void onEvent(Object event) throws Exception {

        try {

            try{
                if (event instanceof ExecutionEvent) {
                    ExecutionEvent executionEvent = (ExecutionEvent) event;
                    MavenProject project = executionEvent.getProject();
                    String lifecyclePhase = executionEvent.getMojoExecution().getLifecyclePhase();
                    String phase = lifecyclePhase.substring(lifecyclePhase.lastIndexOf(':') + 1);
                    String execution = executionEvent.getMojoExecution().getExecutionId();

                    String currentArtifactId = project.getArtifactId();

                    if (!projectPropertiesDone) {
                        this.buildRadiatorInterop.projectProperties(project.getProperties(), project.getArtifactId());
                        projectPropertiesDone = true;
                    }

                    String status = executionEvent.getType().toString();
                    if (executionEvent.getType() == ExecutionEvent.Type.MojoStarted) {
                        status = "started";
                    } else if (executionEvent.getType() == ExecutionEvent.Type.MojoFailed) {
                        status = "failed";
                    } else if (executionEvent.getType() == ExecutionEvent.Type.MojoSucceeded) {
                        status = "passed";
                    }

                    this.buildRadiatorInterop.executionEvent(phase, execution, currentArtifactId, status);

                }
                if (event instanceof DefaultMavenExecutionResult) {
                    DefaultMavenExecutionResult dmer = (DefaultMavenExecutionResult) event;
                    this.buildRadiatorInterop.executionResult(dmer.getBuildSummary(dmer.getProject()) instanceof BuildSuccess, dmer.getBuildSummary(dmer.getProject()) instanceof BuildFailure);
                }
            } catch (NullPointerException e) {
                // do nothing
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
