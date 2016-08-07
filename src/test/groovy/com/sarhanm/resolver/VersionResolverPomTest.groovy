package com.sarhanm.resolver

import com.sarhanm.IntegrationSpec
import org.apache.commons.io.FileUtils

/**
 * @author Mohammad Sarhan ( mohammad@ ) 
 * @date 2/11/16
 */
class VersionResolverPomTest extends IntegrationSpec {

    static DEFAULT_BUILD = '''
    plugins{
        id 'com.sarhanm.version-resolver'
    }
    apply plugin: 'java'
    apply plugin: 'maven'

    repositories{
        jcenter()
        mavenCentral()
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
        setupLocalepo()
    }

    def 'test pom generation with nebula-publish'() {

        given: 'Apply the nebula-resolved-dependencies'

        buildFile << '''
        buildscript {
          repositories { jcenter() }
          dependencies {
            classpath 'com.netflix.nebula:nebula-publishing-plugin:4.8.1'
          }
        }
        '''.stripIndent() + DEFAULT_BUILD + DEFAULT_MANIFEST + '''

        apply plugin: 'maven-publish'

        apply plugin: "nebula.maven-resolved-dependencies"

        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }

        publishing{
            publications {
                mavenTestPub(MavenPublication) {
                    from project.components.java
                    version project.version
                }
            }
        }
        '''.stripIndent()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when: 'Generate the pom file'
        def result = runSuccessfully('build', 'generatePomFileForMavenTestPubPublication')

        then: 'Pom should include resolved versions'
        def f = file("build/publications/mavenTestPub/pom-default.xml")
        f.exists()

        def root = new XmlSlurper().parse(file("build/publications/mavenTestPub/pom-default.xml"))
        def dep = root.dependencies.dependency
        dep.artifactId.text() == "commons-configuration"
        dep.version.text() == '1.10'
    }

    def 'test pom generation with publish with auto version'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        apply plugin: 'com.sarhanm.versioner'
        apply plugin: 'maven-publish'

        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }

        publishing{
            publications {
                mavenTestPub(MavenPublication) {
                    from project.components.java
                }
            }
        }

        """.stripIndent()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        runSuccessfully('build', 'generatePomFileForMavenTestPubPublication')

        then:
        def f = file("build/publications/mavenTestPub/pom-default.xml")
        f.exists()

        def root = new XmlSlurper().parse(file("build/publications/mavenTestPub/pom-default.xml"))
        def dep = root.dependencies.dependency
        dep.artifactId.text() == "commons-configuration"
        dep.version.text() == 'auto'
    }

    def 'test via versionManifest configuration and spring io configuration'() {
        buildFile << '''
        buildscript {
          repositories { jcenter() }
          dependencies {
            classpath "io.spring.gradle:dependency-management-plugin:0.6.0.RELEASE"
          }
        }

        plugins{
            id 'com.sarhanm.version-resolver'
        }

        apply plugin: 'java'
        apply plugin: 'io.spring.dependency-management'

        def resolverAction = project.findProperty('versionResolverAction')
        dependencyManagement {
            overriddenByDependencies = false
            resolutionStrategy { ResolutionStrategy s ->
                s.eachDependency(resolverAction)
            }
        }

        dependencies{
            versionManifest 'test:manifest:1.0@yaml'
            compile 'commons-configuration:commons-configuration:auto'
        }

        repositories{
            System.setProperty('maven.repo.local', file('build/.m2/repository').path)
            mavenLocal()
            jcenter()
        }

        '''.stripIndent()


        when:
        def result = runSuccessfully("build", "--info")

        then:
        true
        //TODO: verify the dependency classpath
        //TODO: verify the pom

    }

    def setupLocalepo() {
        def repo = file('build/.m2/repository')
        repo.mkdirs()
        FileUtils.copyDirectory(new File("src/test/resources/test-repo"), repo)
    }
}
