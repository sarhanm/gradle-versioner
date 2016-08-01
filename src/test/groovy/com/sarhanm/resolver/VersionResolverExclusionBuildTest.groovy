package com.sarhanm.resolver

import com.sarhanm.IntegrationSpec
import org.apache.commons.io.FileUtils
import spock.lang.Ignore

/**
 * @author Mohammad Sarhan ( mohammad@ )
 * @date 2/11/16
 */
class VersionResolverExclusionBuildTest extends IntegrationSpec {

    static DEFAULT_BUILD = '''
    plugins{
        id 'com.sarhanm.version-resolver'
        id "io.spring.dependency-management" version "0.6.0.RELEASE"
    }

    def resolverAction = project.findProperty('versionResolverAction')
    dependencyManagement {
        overriddenByDependencies = false
        resolutionStrategy { ResolutionStrategy s ->
            s.eachDependency(resolverAction)
        }
    }

    apply plugin: 'java'
    apply plugin: 'maven'

    repositories{
        jcenter()
        System.setProperty('maven.repo.local', file('build/.m2/repository').path)
        mavenLocal()
    }

    '''.stripIndent()

    static DEFAULT_MANIFEST = '''

    versionResolver{
        versionManifest{
            url = "file://" + file('build/versions.yaml')
        }
    }
    '''.stripIndent()

    def versionsFile

    def setup() {
        versionsFile = file("build/versions.yaml")
        versionsFile.parentFile.mkdirs()
    }

    def 'test version resolution'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """

        dependencies{
            compile 'test:transitive-exclude:auto'
        }
        """.stripIndent()

        def repo = file('build/.m2/repository')
        repo.mkdirs()
        FileUtils.copyDirectory(new File("src/test/resources/test-repo"), repo)

        addJavaFile()

        versionsFile.text = """
        modules:
          'test:transitive-exclude': 1.0
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencies')

        then:
        !result.output.contains("FAILED")
        // This should now be excluded
        !result.output.contains("commons-logging")
    }

    @Ignore("Need to update the nebula plugin so it does the right thing for exclusions")
    def 'exclusion in generated pom'() {
        buildFile << '''
        buildscript {
          repositories { jcenter() }
          dependencies {
            classpath 'com.netflix.nebula:nebula-publishing-plugin:4.8.1'
          }
        }
        '''.stripIndent() + DEFAULT_BUILD + DEFAULT_MANIFEST + """

        group = 'test'
        version = '2.0'

        apply plugin: 'maven-publish'
        apply plugin: 'nebula.maven-excludes'
        apply plugin: "nebula.maven-resolved-dependencies"

        dependencies{
            compile 'test:transitive-exclude:auto'
        }

        publishing{
            publications {
                mavenTestPub(MavenPublication) {
                    from project.components.java
                }
            }
        }

        """.stripIndent()

        def repo = file('build/.m2/repository')
        repo.mkdirs()
        FileUtils.copyDirectory(new File("src/test/resources/test-repo"), repo)

        addJavaFile()

        versionsFile.text = """
        modules:
          'test:transitive-exclude': 1.0
        """.stripIndent()

        when:
        runSuccessfully("build", 'publishToMavenLocal')

        then:

        def root = new XmlSlurper().parse(file("build/publications/mavenTestPub/pom-default.xml"))

        def node = root.dependencies.dependency

        node.exclusions.exclusion.artifactId.text() == 'commons-logging'
        node.version.text() == '1.0'
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
