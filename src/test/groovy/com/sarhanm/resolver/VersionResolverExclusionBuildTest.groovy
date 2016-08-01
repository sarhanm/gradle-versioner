package com.sarhanm.resolver

import com.sarhanm.IntegrationSpec
import org.apache.commons.io.FileUtils
import spock.lang.Ignore

/**
 * @author Mohammad Sarhan ( mohammad@ )
 * @date 2/11/16
 */
class VersionResolverExclusionBuildTest extends IntegrationSpec {

    static BUILDSCRIPT = '''

    buildscript {
      repositories { jcenter() }
      dependencies {
        classpath "io.spring.gradle:dependency-management-plugin:0.6.0.RELEASE"
        classpath 'com.netflix.nebula:nebula-publishing-plugin:4.8.1'
      }
    }
    '''.stripIndent()

    static BUILD = '''

    plugins{
        id 'com.sarhanm.version-resolver'
    }
    apply plugin: 'java'
    apply plugin: 'maven'

    repositories{
        jcenter()
        System.setProperty('maven.repo.local', file('build/.m2/repository').path)
        mavenLocal()
    }
    '''.stripIndent()

    static DEFAULT_BUILD = BUILDSCRIPT + BUILD

    static DEPENDENCY_MANAGEMENT = '''

    def resolverAction = project.findProperty('versionResolverAction')
    dependencyManagement {
        overriddenByDependencies = false
        resolutionStrategy { ResolutionStrategy s ->
            s.eachDependency(resolverAction)
        }
    }
    '''.stripIndent()

    static DEFAULT_MANIFEST = '''

    versionResolver{
        versionManifest{
            url = "file://" + file('build/versions.yaml')
        }
    }
    '''.stripIndent()

    static PRINT_CLASSPAHT_TASK = '''

    task printClasspath << {
            println "Classpath: " + configurations.runtime.asPath
    }
    '''.stripIndent()

    def versionsFile

    def setup() {
        versionsFile = file("build/versions.yaml")
        versionsFile.parentFile.mkdirs()

        addJavaFile()

        versionsFile.text = """
        modules:
          'test:transitive-exclude': 1.0
        """.stripIndent()

        def repo = file('build/.m2/repository')
        repo.mkdirs()
        FileUtils.copyDirectory(new File("src/test/resources/test-repo"), repo)
    }

    def 'direct exclusion on classpath'() {
        buildFile << DEFAULT_BUILD +
                DEFAULT_MANIFEST +
                PRINT_CLASSPAHT_TASK +
                '''
        dependencies{
            compile ('test:transitive-exclude:auto'){
                exclude module: 'commons-logging'
            }
        }
        '''.stripIndent()

        when:
        def result = runSuccessfully("printClasspath")

        then: 'Make sure that the excluded dependency is not on the classpath'
        !result.output.substring(result.output.indexOf("Classpath:")).contains("commons-logging")

    }

    def 'transitive exclusion on classpath'() {
        buildFile << DEFAULT_BUILD +
                DEFAULT_MANIFEST +
                PRINT_CLASSPAHT_TASK +
                '''
        apply plugin: 'io.spring.dependency-management'
        dependencies{
            compile ('test:transitive-exclude:auto')
        }
        '''.stripIndent() + DEPENDENCY_MANAGEMENT

        when:
        def result = runSuccessfully("printClasspath")

        then: 'Make sure that the excluded dependency is not on the classpath'
        !result.output.substring(result.output.indexOf("Classpath:")).contains("commons-logging")
    }


    def 'direct exclusion in pom'() {

        buildFile << DEFAULT_BUILD +
                DEFAULT_MANIFEST +
                '''
        group = 'test'
        version = '2.0'

        dependencies{
            compile ('test:transitive-exclude:auto'){
                exclude module: 'foobar'
            }
        }

        apply plugin: 'maven-publish'
         publishing{
            publications {
                mavenTestPub(MavenPublication) {
                    from project.components.java
                }
            }
        }

        '''.stripIndent()

        when:
        def result = runSuccessfully("publishToMavenLocal")

        then: 'gradle can handle direct exclusion without plugin support'

        def root = new XmlSlurper().parse(file("build/publications/mavenTestPub/pom-default.xml"))

        def node = root.dependencies.dependency

        // we expect size of 1 because gradle only handle direct exclusions
        // and not ones that are added via a plugin, at least not currently.
        node.exclusions.size() == 1
        node.exclusions.exclusion.artifactId.text() == 'foobar'
    }

    def addJavaFile() {
        //Need a java file to actually make compile go and resolution fail when we have an issue
        //otherwise resolution fails silently
        def javaFile = file("src/main/java/HelloWorld.java")
        javaFile.parentFile.mkdirs()
        javaFile.text = """
        public class HelloWorld{
            public static void main(String[] args){
            }
        }
        """.stripIndent()
    }
}
