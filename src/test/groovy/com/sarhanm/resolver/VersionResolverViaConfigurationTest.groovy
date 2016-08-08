package com.sarhanm.resolver

import com.sarhanm.IntegrationSpec
import org.apache.commons.io.FileUtils

/**
 * @author Mohammad Sarhan ( mohammad@ ) 
 * @date 8/7/16
 */
class VersionResolverViaConfigurationTest extends IntegrationSpec {

    static DEFAULT_BUILD = '''
    plugins{
        id 'com.sarhanm.version-resolver'
    }
    apply plugin: 'java'
    apply plugin: 'maven'

    repositories {
        System.setProperty('maven.repo.local', file('build/.m2/repository').path)
        mavenLocal()
        jcenter()
    }
    '''.stripIndent()

    def setup() {
        setupLocalepo()
    }

    def 'test single versionManifest configuration'(){
        buildFile << DEFAULT_BUILD + '''

        dependencies{
            versionManifest 'test:manifest:1.0@yaml'
            compile 'commons-configuration:commons-configuration:auto'
        }
        '''.stripIndent()

        when:
        def r = runSuccessfully("build", 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration', '--stacktrace')

        then:
        r.output.contains("commons-configuration:commons-configuration:auto -> 1.10\n")

    }

    def 'test multiple versionManifest configurations with same artifact'(){
        buildFile << DEFAULT_BUILD + '''

        dependencies{
            versionManifest 'test:manifest:1.0@yaml'
            versionManifest 'test:manifest:2.0@yaml'
            compile 'commons-configuration:commons-configuration:auto'
        }
        '''.stripIndent()

        when:
        def r = runSuccessfully("build", 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration', '--stacktrace')

        then: 'latest version of artifact gets resolved. no merge occurs'

        r.output.contains("commons-configuration:commons-configuration:auto -> 1.9\n")

    }


    def 'test multiple versionManifest configurations'(){
        buildFile << DEFAULT_BUILD + '''

        dependencies{
            versionManifest 'test:manifest:1.0@yaml'
            versionManifest 'test:dev-manifest:2.0@yaml'
            compile 'commons-configuration:commons-configuration:auto'
        }
        '''.stripIndent()

        when:
        def r = runSuccessfully("build", 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration', '--stacktrace')

        then: 'Both artifacts are merged, in the order they are declared.'

        r.output.contains("commons-configuration:commons-configuration:auto -> 1.8\n")

    }

    def 'test multiple versionManifest configurations and verify order'(){
        buildFile << DEFAULT_BUILD + '''

        dependencies{
            //Verify that declaration order matters, not version numbers.
            versionManifest 'test:dev-manifest:2.0@yaml'
            versionManifest 'test:manifest:1.0@yaml'
            compile 'commons-configuration:commons-configuration:auto'
        }
        '''.stripIndent()

        when:
        def r = runSuccessfully("build", 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration', '--stacktrace')

        then: 'Both artifacts are merged, in the order they are declared, regardless of version numbers'

        r.output.contains("commons-configuration:commons-configuration:auto -> 1.10\n")

    }

    def 'test  configurations and options url'(){
        buildFile << DEFAULT_BUILD + '''

        dependencies{
            versionManifest 'test:manifest:1.0@yaml'
            compile 'commons-configuration:commons-configuration:auto'
        }

         versionResolver{
            versionManifest{
                url = "file://" + file('build/versions.yaml')
            }
        }
        '''.stripIndent()

        def versionsFile = file("build/versions.yaml")
        versionsFile.parentFile.mkdirs()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.8'
        """.stripIndent()

        when:
        def r = runSuccessfully("build", 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')

        then: 'Both artifacts are merged, with options.url overriding configurations'

        r.output.contains("commons-configuration:commons-configuration:auto -> 1.8\n")
    }

    def setupLocalepo() {
        def repo = file('build/.m2/repository')
        repo.mkdirs()
        FileUtils.copyDirectory(new File("src/test/resources/test-repo"), repo)
    }
}
