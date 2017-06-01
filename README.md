# BuildRadiator Maven extension (Event Spy)

Event spies are Maven extensions - not a Maven plugin as such.

This one goes hand in hand with a [Build Radiator](//github.com//paul-hammant/buildradiator) site.

A build is a number of conceptual steps. Steps starting, passing and failing are events that this
extension can pass to the build radiator (build cancellation is is outside the control of this tech).

## Setting in the extension in for your build

In order to do the step updates on buildradiator.org ...

Go and get the JAR from [Maven Central](https://repo.maven.apache.org/maven2/com/paulhammant/buildradiatorextension/)

Check that into (say) `lib/`, then configure your build script to do:

```
mvn <phase name> -Dmaven.ext.class.path=lib/buildradiatorextension-1.1.jar
```

That's where you're using a service like "Circle CI", and you're happy to check in a 10K jar.

If you're using an on-premises Jenkins, you may prefer to place the Jar in the `<maven-install-root>/lib/ext/` folder. 

## Configuring Steps 

In your project's POM, you need to identify artifactId/phases/executions where a step starts:

```
<properties>
  <buildradiator.0>Compile=*</buildradiator.0>
  <buildradiator.1>Unit_Tests=buildradiator/process-test-resources/default-testResources</buildradiator.1>
  <buildradiator.2>Integration_Tests=buildradiator/test/integration-tests</buildradiator.2>
  <buildradiator.3>Functional_Tests=buildradiator/test/functional-tests</buildradiator.3>
  <buildradiator.4>Package=*</buildradiator.4>
</properties>
```

One step can be multiple artifactId/phases/executions, of course, especially for a multi-module project.  
The above was taken from the `BuildRadiator` project. 
[See pom](//github.com//paul-hammant/buildradiator/blob/master/pom.xml)

If you mis-type a artifactId/phases/executions to the right-hand side of a `=`, then the build will look like
this after it has finished:

![](https://cloud.githubusercontent.com/assets/82182/26393757/ce22ad8c-4038-11e7-8878-5d3b1be0cbf0.png)

It will also look like that if you mis-type a step-name on the left-hand side of a `=`.

## Targeting a radiator other than buildradiator.org

If you have hosted your own build radiator server:

```
<properties>
  <buildradiator.baseurl>"https://buildradiator.mycompany.com"</buildradiator.baseurl>
</properties>
```

Be sure to get the `http` vs `https` right.

## Important environment variables 

You need to set these for each CI initiated build, before Maven is launched:

```
export buildIdEnvVar=<the build number from Jenkin or the commit hash etc>
export radiatorCodeEnvVar=<radiator code from when you created the radiator>
export secret=<radiator secret from when you created the radiator>
```

Don't do these on your dev workstation, because updating the build radiator is the business of your CI daemon.

## Researching where to make step changes

In your project's POM:

```
<properties>
  <buildradiator.trace>true</buildradiator.trace>
</properties>
```

### Sample of Maven Phases/Executions

Artifact/Phase/Executions for `BuildRaditor` itself:

1. buildradiator/validate/enforce.versions
1. buildradiator/process-resources/default-resources
1. buildradiator/compile/default-compile
1. buildradiator/process-test-resources/default-testResources
1. buildradiator/test-compile/default-testCompile
1. buildradiator/test/default-test
1. buildradiator/test/unit-tests
1. buildradiator/test/integration-tests
1. buildradiator/test/functional-tests
1. buildradiator/package/default-jar
1. buildradiator/package/fat-jar
1. buildradiator/install/default-install

Notice that JUnit has three executions - because that's how the "surefire" plugin was set up.  
[See pom](//github.com//paul-hammant/buildradiator/blob/master/pom.xml). 

Other projects may differ for artifacts, phases and executions. 
Here are those from [an ordinary multi-module project on GitHub](https://github.com/jamesward/maven-multi-module-example):

1. multi/install/default-install
1. core/process-resources/default-resources
1. core/compile/default-compile
1. core/process-test-resources/default-testResources
1. core/test-compile/default-testCompile
1. core/test/default-test
1. core/package/default-jar
1. core/install/default-install
1. app/process-resources/default-resources
1. app/compile/default-compile
1. app/process-test-resources/default-testResources
1. app/test-compile/default-testCompile
1. app/test/default-test
1. app/package/default-war
1. app/package/default
1. app/install/default-install
