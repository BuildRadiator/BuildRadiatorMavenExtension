/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.paulhammant.buildradiatorextension;

import com.google.common.io.LineReader;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.*;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Named
@Singleton
public class BuildRadiatorEventSpy extends AbstractEventSpy {

    private List<String> steps = new ArrayList<>();
    private Properties stepMap = new Properties();
    private int currStep = -1;
    private String lastStep = "";
    private String lastStepState = "";
    private String buildNumber;
    private String radiatorCode;
    private boolean trace = false;

    @Override
    public void init(Context context) {
        buildNumber = System.getenv("buildNumber");
        radiatorCode = System.getenv("radiatorCode");
    }

    @Override
    public void onEvent(Object event) throws Exception {

        try {

            if (lastStepState.equals("failed")) {
                return;
            }

            try{
                if (event instanceof ExecutionEvent) {
                    ExecutionEvent executionEvent = (ExecutionEvent) event;
                    if (steps.size() == 0) {
                        Properties properties = executionEvent.getProject().getProperties();
                        String property = properties.getProperty("buildradiator.trace");
                        this.trace = Boolean.parseBoolean(property);
                        int st = 0;
                        while (properties.getProperty("buildradiator." + st) != null) {
                            String[] stepDef = properties.getProperty("buildradiator." + st).split("=");
                            steps.add(stepDef[0]);
                            stepMap.put(stepDef[0], stepDef[1]);
                            st++;
                        }
                    }

                    String lifecyclePhase = executionEvent.getMojoExecution().getLifecyclePhase();
                    String phase = lifecyclePhase.substring(lifecyclePhase.lastIndexOf(':') + 1);
                    String execution = executionEvent.getMojoExecution().getExecutionId();
                    String phaseAndExecution = phase + "/" + execution;
                    if (trace) {
                        System.out.println("Phase/Execution: " + phaseAndExecution);
                    }

                    if (steps.size() == 0) {
                        return;
                    }
                    String status = executionEvent.getType().toString();
                    if (executionEvent.getType() == ExecutionEvent.Type.MojoStarted) {
                        status = "started";
                    } else if (executionEvent.getType() == ExecutionEvent.Type.MojoFailed) {
                        status = "failed";
                    } else if (executionEvent.getType() == ExecutionEvent.Type.MojoSucceeded) {
                        status = "passed";
                    }
                    String nextStep;
                    String nextPhaseAndExecution;
                    if (currStep +1 < steps.size()) {
                        nextStep = steps.get(currStep + 1);
                        nextPhaseAndExecution = stepMap.getProperty(nextStep);
                    } else {
                        nextStep = "";
                        nextPhaseAndExecution = "";
                    }

                    if (nextPhaseAndExecution.equals(phaseAndExecution) || nextPhaseAndExecution.equals("*")) {
                        if (lastStepState.equals("started")) {
                            stepNotification(lastStep,  "stepPassed");
                            stepNotification(nextStep, "startStep");
                            lastStep = nextStep;
                            currStep++;
                            return;
                        } else if (status.equals("failed")) {
                            stepNotification(lastStep, "stepFailed");
                            lastStepState = "failed";
                            return;
                        } else {
                            stepNotification(nextStep, "startStep");
                            lastStep = nextStep;
                            lastStepState = "started";
                            currStep++;
                            return;
                        }
                    }
                }
                if (event instanceof DefaultMavenExecutionResult) {
                    DefaultMavenExecutionResult dmer = (DefaultMavenExecutionResult) event;
                    ;
                    BuildSummary buildSummary = dmer.getBuildSummary(dmer.getProject());
                    if (buildSummary instanceof BuildSuccess && !lastStep.equals("")) {
                        stepNotification(lastStep, "stepPassed");
                    } else if (buildSummary instanceof BuildFailure && !lastStep.equals("")) {
                        stepNotification( lastStep, "stepFailed");
                    }
                }
            } catch (NullPointerException e) {
                // do nothing
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void stepNotification(String step, String stateChg) {
        if (radiatorCode != null && buildNumber != null) {
            try {
                URL url = new URL("https://buildradiator.org/r/" + this.radiatorCode + "/" + stateChg);
                String urlParameters  = "build="+ this.buildNumber + "&step=" + step;
                byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
                int    postDataLength = postData.length;
                HttpURLConnection conn= (HttpURLConnection) url.openConnection();
                conn.setDoOutput( true );
                conn.setInstanceFollowRedirects( false );
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty( "charset", "utf-8");
                conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
                conn.setUseCaches( false );
                try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                    wr.write( postData );
                }
                LineReader in = new LineReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                sb.append(in.readLine());
                if (!sb.toString().equals("OK")) {
                    System.err.println("POST to buildradiator.org failed with " + sb);
                }
            } catch (IOException e) {
                System.err.println("POST to buildradiator.org failed with " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
