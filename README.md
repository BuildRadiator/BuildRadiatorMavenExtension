# TODO

Place this Jar in the `<maven-install-root>/lib/ext/` folder

## Configuring Steps 

In your project's POM:

```
  <properties>
    <buildradiator.0>Compile=*</buildradiator.0>
    <buildradiator.1>Unit_Tests=process-test-resources/default-testResources</buildradiator.1>
    <buildradiator.2>Integration_Tests=test/integration-tests</buildradiator.2>
    <buildradiator.3>Functional_Tests=test/functional-tests</buildradiator.3>
    <buildradiator.4>Package=*</buildradiator.4>
  </properties>

```

## Important env-vars 

You need to set these for each CI initiated build:

```
export buildNumber=1117
export radiatorCode=tclpwffsdbtsggefjt
```

## Researching where to make step changes

In your project's POM:

```
  <properties>
    <buildradiator.trace>true</buildradiator.trace>
  </properties>
```

### Sample of Maven Phases/Executions

1. validate/enforce.versions
2. validate/enforce-versions
3. process-resources/default-resources
4. compile/default-compile
5. process-test-resources/default-testResources
6. test-compile/default-testCompile
7. test/default-test
8. test/unit-tests
9. test/integration-tests
10. test/functional-tests
11. package/default-jar
12. package/fat-jar
13. install/default-install

Notice that JUnit has three executions - that is as the buildraditor project has it. [See pom](/paul-hammant/buildradiator/blob/master/pom.xml)