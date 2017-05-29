package com.paulhammant.buildradiatorextension;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class BuildRadiatorInteropTest {

    @Test
    public void regularSeriesOfFourSteps() {

        DbgBuildRadiatorInterop bri = new DbgBuildRadiatorInterop("1", "thisartifact", "rc123", "scrt");

        Properties props = new Properties();
        defineFourSteps(props);
        props.setProperty("buildradiator.trace", "true");

        seriesOfOperations(bri, props);

        assertThat(bri.baos.toString(), equalTo("Artifact/Phase/Execution: thisartifact/phase0/eee (started)\n" +
                "Artifact/Phase/Execution: thisartifact/phase0/eee (passed)\n" +
                "Artifact/Phase/Execution: thisartifact/phase1/execution1 (started)\n" +
                "Artifact/Phase/Execution: thisartifact/phase1/execution1 (passed)\n" +
                "Artifact/Phase/Execution: thisartifact/aaa/bbb (started)\n" +
                "Artifact/Phase/Execution: thisartifact/aaa/bbb (passed)\n" +
                "Artifact/Phase/Execution: thisartifact/zzz/zzzz (started)\n" +
                "Artifact/Phase/Execution: thisartifact/zzz/zzzz (failed)\n"));

        assertThat(bri.posts.toString().replace("https://buildradiator.org/r/rc123/","")
                .replace("&"," "), equalTo(
                "startStep build=1 step=one secret=scrt\n" +
                "stepPassedAndStartStep build=1 step=two secret=scrt pStep=one\n" +
                "stepPassedAndStartStep build=1 step=three secret=scrt pStep=two\n" +
                "stepPassedAndStartStep build=1 step=four secret=scrt pStep=three\n" +
                "stepFailed build=1 step=four secret=scrt\n"));
    }

    @Test
    public void differentSeriesOfFourSteps() {

        DbgBuildRadiatorInterop bri = new DbgBuildRadiatorInterop("1", "thisartifact", "rc123", "scrt");

        Properties props = new Properties();
        defineFourSteps(props);
        props.setProperty("buildradiator.trace", "true");

        bri.projectProperties(props, "thisartifact");
        bri.executionEvent("phase0", "eee", "thisartifact", "started");
        bri.executionEvent("phase0", "eee", "thisartifact", "passed");
        bri.executionEvent("phase1", "execution1", "thisartifact", "started");
        bri.executionEvent("phase1", "execution1", "thisartifact", "passed");
        bri.executionEvent("aaa", "bbb", "thisartifact", "started");
        bri.executionEvent("aaa", "bbb", "thisartifact", "failed");
        bri.executionResult(false, true);

        assertThat(bri.baos.toString(), equalTo(
                "Artifact/Phase/Execution: thisartifact/phase0/eee (started)\n" +
                "Artifact/Phase/Execution: thisartifact/phase0/eee (passed)\n" +
                "Artifact/Phase/Execution: thisartifact/phase1/execution1 (started)\n" +
                "Artifact/Phase/Execution: thisartifact/phase1/execution1 (passed)\n" +
                "Artifact/Phase/Execution: thisartifact/aaa/bbb (started)\n" +
                "Artifact/Phase/Execution: thisartifact/aaa/bbb (failed)\n"));

        assertThat(bri.posts.toString().replace("https://buildradiator.org/r/rc123/","")
                .replace("&"," "), equalTo(
                "startStep build=1 step=one secret=scrt\n" +
                "stepPassedAndStartStep build=1 step=two secret=scrt pStep=one\n" +
                "stepPassedAndStartStep build=1 step=three secret=scrt pStep=two\n" +
                "stepFailed build=1 step=three secret=scrt\n"));
    }

    private void defineFourSteps(Properties props) {
        props.setProperty("buildradiator.0", "one=*");
        props.setProperty("buildradiator.1", "two=thisartifact/phase1/execution1");
        props.setProperty("buildradiator.2", "three=thisartifact/aaa/bbb");
        props.setProperty("buildradiator.3", "four=*");
    }

    @Test
    public void unmatchedArtifactDoesntActivateExtension() {

        DbgBuildRadiatorInterop bri = new DbgBuildRadiatorInterop("1", "efewrewrer", "rc123", "scrt");

        Properties props = new Properties();

        seriesOfOperations(bri, props);

        assertThat(bri.baos.toString(), equalTo(""));
        assertThat(bri.posts.toString(), equalTo(""));
    }

    @Test
    public void buildIdMustBeSpecified() {

        DbgBuildRadiatorInterop bri = new DbgBuildRadiatorInterop(null, "thisartifact", "rc123", "scrt");

        Properties props = new Properties();
        defineFourSteps(props);

        seriesOfOperations(bri, props);

        assertThat(bri.baos.toString(), equalTo("BuildRadiatorEventSpy: 'artifactEnvVar', 'buildIdEnvVar', 'radiatorCodeEnvVar' and 'radiatorSecretEnvVar' all have to be set as environmental variables before Maven is invoked, if you want your radiator to be updated. Additionally, 'artifactEnvVar' needs to match the root artifact being built. Note: This technology is for C.I. daemons only, not developer workstations!\n" +
                "  artifactEnvVar: thisartifact\n" +
                "  buildIdEnvVar: null\n" +
                "  radiatorCodeEnvVar: rc123\n" +
                "  radiatorSecretEnvVar: REDACTED (len:4)\n"));
        assertThat(bri.posts.toString(), equalTo(""));
    }

    @Test
    public void radiatorCodeMustBeSpecified() {

        DbgBuildRadiatorInterop bri = new DbgBuildRadiatorInterop("1", "thisartifact", null, "scrt");

        Properties props = new Properties();
        defineFourSteps(props);

        seriesOfOperations(bri, props);

        assertThat(bri.baos.toString(), equalTo("BuildRadiatorEventSpy: 'artifactEnvVar', 'buildIdEnvVar', 'radiatorCodeEnvVar' and 'radiatorSecretEnvVar' all have to be set as environmental variables before Maven is invoked, if you want your radiator to be updated. Additionally, 'artifactEnvVar' needs to match the root artifact being built. Note: This technology is for C.I. daemons only, not developer workstations!\n" +
                "  artifactEnvVar: thisartifact\n" +
                "  buildIdEnvVar: 1\n" +
                "  radiatorCodeEnvVar: null\n" +
                "  radiatorSecretEnvVar: REDACTED (len:4)\n"));
        assertThat(bri.posts.toString(), equalTo(""));
    }

    @Test
    public void secretMustBeSpecified() {

        DbgBuildRadiatorInterop bri = new DbgBuildRadiatorInterop("1", "thisartifact", "rc123", null);

        Properties props = new Properties();
        defineFourSteps(props);

        seriesOfOperations(bri, props);

        assertThat(bri.baos.toString(), equalTo("BuildRadiatorEventSpy: 'artifactEnvVar', 'buildIdEnvVar', 'radiatorCodeEnvVar' and 'radiatorSecretEnvVar' all have to be set as environmental variables before Maven is invoked, if you want your radiator to be updated. Additionally, 'artifactEnvVar' needs to match the root artifact being built. Note: This technology is for C.I. daemons only, not developer workstations!\n" +
                "  artifactEnvVar: thisartifact\n" +
                "  buildIdEnvVar: 1\n" +
                "  radiatorCodeEnvVar: rc123\n" +
                "  radiatorSecretEnvVar: null\n"));
        assertThat(bri.posts.toString(), equalTo(""));
    }

    private void seriesOfOperations(DbgBuildRadiatorInterop bri, Properties props) {
        bri.projectProperties(props, "thisartifact");
        bri.executionEvent("phase0", "eee", "thisartifact", "started");
        bri.executionEvent("phase0", "eee", "thisartifact", "passed");
        bri.executionEvent("phase1", "execution1", "thisartifact", "started");
        bri.executionEvent("phase1", "execution1", "thisartifact", "passed");
        bri.executionEvent("aaa", "bbb", "thisartifact", "started");
        bri.executionEvent("aaa", "bbb", "thisartifact", "passed");
        bri.executionEvent("zzz", "zzzz", "thisartifact", "started");
        bri.executionEvent("zzz", "zzzz", "thisartifact", "failed");
        bri.executionResult(false, true);
    }

    private static class DbgBuildRadiatorInterop extends BuildRadiatorInterop {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        StringBuilder posts = new StringBuilder();

        public DbgBuildRadiatorInterop(String buildIdEnvVar, String artifactEnvVar, String radiatorCodeEnvVar, String radiatorSecretEnvVar) {
            super(buildIdEnvVar, artifactEnvVar, radiatorCodeEnvVar, radiatorSecretEnvVar);
        }

        @Override
        protected PrintStream systemErr() {
            return ps;
        }

        @Override
        protected String postUpdate(URL url, String urlParameters) throws IOException {
            posts.append(url).append(" ").append(urlParameters).append("\n");
            return "OK";
        }
    }
}
