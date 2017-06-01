/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.paulhammant.buildradiatorextension;

import com.google.common.io.LineReader;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;

public class BuildRadiatorInterop {

    private final String buildIdEnvVar;
    private final String artifactEnvVar;
    private final String radiatorCodeEnvVar;
    private final String radiatorSecretEnvVar;
    private String rootArtifactId;
    private boolean trace;
    private java.util.List<String> steps = new ArrayList<>();
    private Properties stepMap = new Properties();
    private String buildRadiatorURL;
    private int currStep = -1;
    private String lastStep = "";
    private boolean env_var_warning = false;
    private boolean hasStarted = false;
    private boolean hasFailed = false;

    BuildRadiatorInterop(String buildIdEnvVar, String artifactEnvVar,
                         String radiatorCodeEnvVar, String radiatorSecretEnvVar) {
        this.buildIdEnvVar = buildIdEnvVar;
        this.artifactEnvVar = artifactEnvVar;
        this.radiatorCodeEnvVar = radiatorCodeEnvVar;
        this.radiatorSecretEnvVar = radiatorSecretEnvVar;
    }

    void executionEvent(String phase, String execution, String currentArtifactId, String status) {

        String artifactAndPhaseAndExecution = currentArtifactId + "/" + phase + "/" + execution;

        if (trace) {
            systemErr().println("Artifact/Phase/Execution: " + artifactAndPhaseAndExecution
                    + " (" +  status + ")");
        }

        if (steps.size() == 0) {
            return;
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

        if (nextPhaseAndExecution.equals(artifactAndPhaseAndExecution) || nextPhaseAndExecution.equals("*")) {
            if (status.equals("failed")) {
                stepFailedNotification(lastStep);
                hasFailed = true;
            } else {
                if (hasStarted) {
                    stepPassedAndStartStepNotification(lastStep, nextStep);
                } else {
                    startStepNotification(nextStep);
                }
                lastStep = nextStep;
                hasStarted = true;
                currStep++;
            }
        }
    }

    void projectProperties(Properties properties, String artifactId) {
        rootArtifactId = artifactId;
        buildRadiatorURL = properties.getProperty("buildradiator.baseurl", "https://buildradiator.org");
        trace = Boolean.parseBoolean(properties.getProperty("buildradiator.trace", "false"));
        int st = 0;
        while (properties.getProperty("buildradiator." + st) != null) {
            String[] stepDef = properties.getProperty("buildradiator." + st).split("=");
            steps.add(stepDef[0]);
            stepMap.put(stepDef[0], stepDef[1]);
            st++;
        }
    }

    void executionResult(boolean success, boolean failure) {
        if (success && !lastStep.equals("")) {
            stepPassedNotification(lastStep);
        } else if (failure && !lastStep.equals("") && !hasFailed) {
            stepFailedNotification( lastStep);
        }
    }

    private void stepPassedNotification(String step) {
        stepNotification(null, step, "stepPassed");
    }

    private void stepPassedAndStartStepNotification(String pStep, String step) {
        stepNotification(pStep, step, "stepPassedAndStartStep");
    }

    private void startStepNotification(String step) {
        stepNotification(null, step, "startStep");
    }

    private void stepFailedNotification(String step) {
        stepNotification(null, step, "stepFailed");
    }

    private void stepNotification(String pStep, String step, String stateChg) {
        if (varsAreMissing() || !this.artifactEnvVar.equals(this.rootArtifactId)) {
            if (!env_var_warning) {
                systemErr().println("BuildRadiatorEventSpy: 'artifactEnvVar', 'buildIdEnvVar', 'radiatorCodeEnvVar' and 'radiatorSecretEnvVar' all " +
                        "have to be set as environmental variables before Maven is invoked, if you want " +
                        "your radiator to be updated. Additionally, 'artifactEnvVar' needs to match the root " +
                        "artifact being built. Note: This technology is for C.I. daemons only, not developer workstations!");
                systemErr().println("  buildingThisArtifact (env var): " + artifactEnvVar);
                systemErr().println("  buildId (env var): " + buildIdEnvVar);
                systemErr().println("  radiatorCode (env var): " + radiatorCodeEnvVar);
                if (radiatorSecretEnvVar == null) {
                    systemErr().println("  radiatorSecret (env var): null");
                } else {
                    systemErr().println("  radiatorSecret (env var): REDACTED (len:" + radiatorSecretEnvVar.length() + ")");
                }
            }
            env_var_warning = true;
            return;
        }
        try {
            String urlParameters = "build=" + this.buildIdEnvVar + "&step=" + step + "&secret=" + this.radiatorSecretEnvVar;
            if (pStep != null) {
                urlParameters = urlParameters + "&pStep=" + pStep;
            }
            String op = postUpdate(new URL(buildRadiatorURL + "/r/" + this.radiatorCodeEnvVar + "/" + stateChg), urlParameters);
            if (!op.equals("OK")) {
                systemErr().println("POST to buildradiator.org failed with " + op);
            }
        } catch (IOException e) {
            systemErr().println("POST to buildradiator.org failed with " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected String postUpdate(URL url, String urlParameters) throws IOException {
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }
        return new LineReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))
                .readLine();
    }

    protected PrintStream systemErr() {
        return System.err;
    }

    private boolean varsAreMissing() {
        return this.radiatorCodeEnvVar == null || this.artifactEnvVar == null || this.buildIdEnvVar == null || this.radiatorSecretEnvVar == null
                || this.radiatorCodeEnvVar.equals("") || this.artifactEnvVar.equals("") || this.buildIdEnvVar.equals("") || this.radiatorSecretEnvVar.equals("");
    }

}
