package com.sarhanm.resolver

import com.sarhanm.IntegrationSpec
import org.apache.commons.io.FileUtils

/**
 * @author Mohammad Sarhan ( mohammad@ ) 
 * @date 2/11/16
 */
class VersionResolverBuildTest extends IntegrationSpec {

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

    def setup(){
        versionsFile = file("build/versions.yaml")
        versionsFile.parentFile.mkdirs()
    }

    def 'test version resolution'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:

        //We should inherit the version of commons-lang from the commons-configuration dependencies
        result.output.contains("commons-lang:commons-lang:2.6\n")
    }

    def 'resolve release platform override of transitive dependency'(){
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.0'
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:
        //release platform is setting commons-lang to 2.0, so that should be the version
        // of our transitive dependency regardless of what version is defined in that pom.
        result.output.contains 'commons-lang:commons-lang:2.6 -> 2.0\n'

    }

    def 'resolve hard pinned version in build file'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """

        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
            //compile 'commons-lang:commons-lang:2.0'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:
        // This is the default gradle strategy. Gradle picks the newest and since we don't have a pinned version
        // in the manifest, the latest version is selected.
        result.output.contains 'commons-lang:commons-lang:2.6\n'

    }

    def 'hard pinned version in build with def in version manifest with transitive dep'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
            compile 'commons-lang:commons-lang:2.0'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.5'
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-lang:commons-lang')

        then:
        // exists in the version manifest and is overridden by the build.gradle file
        result.output.contains('commons-lang:commons-lang:2.6 -> 2.0\n')
    }

    def 'resolve hard pinned version in build with auto transitive'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:1.9'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')

        then:
        // Allowing pinned version in build.gradle to override manifest.
        result.output.contains "commons-configuration:commons-configuration:1.9\n"

    }

    def 'resolve using gradles dynamic version'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:+'
        }
        """.stripIndent()

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.5'
        """.stripIndent()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')

        then:
        // Allowing pinned version in build.gradle to override manifest.
        result.output ==~ /(?ms).*commons-configuration:commons-configuration:\+ -> [^\s]*\n.*/

    }

    def 'resolve without manifest'() {
        buildFile << DEFAULT_BUILD + """
        dependencies{
            compile 'commons-configuration:commons-configuration:1.9'
        }
        """.stripIndent()

        addJavaFile()

        when:
        def result = runSuccessfully('dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')

        then:
        result.output.contains "commons-configuration:commons-configuration:1.9\n"

    }

    def 'test pom generation with nebula-publish'() {

        given: 'Apply the nebula-resolved-dependencies'

        buildFile <<  '''
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

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when: 'Generate the pom file'
        runSuccessfully('generatePomFileForMavenTestPubPublication')

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

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        runSuccessfully('generatePomFileForMavenTestPubPublication')

        then:
        def f = file("build/publications/mavenTestPub/pom-default.xml")
        f.exists()

        def root = new XmlSlurper().parse(file("build/publications/mavenTestPub/pom-default.xml"))
        def dep = root.dependencies.dependency
        dep.artifactId.text() == "commons-configuration"
        dep.version.text() == 'auto'
    }

    def 'test non-java pom resolution'(){
        buildFile << """
        plugins{
            id 'com.sarhanm.version-resolver'
        }

        apply plugin: 'maven-publish'

        configurations{
            myconfig
        }

        dependencies{
            myconfig 'commons-configuration:commons-configuration:auto'
        }

        task archive(type: Tar) {
            from('lifecycle-flow.yaml')
        }

        publish.dependsOn archive

        publishing {
            publications {
                myModule(MavenPublication) {
                    artifact(archive)
                }
            }
        }
        """.stripIndent() + DEFAULT_MANIFEST

        addJavaFile()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        runSuccessfully('build', "--stacktrace")

        then:
        true
    }

    def 'test via configuration'(){
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

        addJavaFile()

        def repo = file('build/.m2/repository')
        repo.mkdirs()
        FileUtils.copyDirectory(new File("src/test/resources/test-repo"), repo)

        when:
        def runner = getRunner(true,"build", "--info")
        runner.forwardOutput()
        def r = runner.build()

        then:
        println r.output

    }
    def addJavaFile() {
        //Need a java file to actually make compile go and resolution fail when we have an issue
        //otherwise resolution fails silently
        def javaFile = file("src/main/java/HelloWorld.java")
        javaFile.parentFile.mkdirs()
        javaFile.text =  """
        public class HelloWorld{
            public static void main(String[] args){
            }
        }
        """.stripIndent()
    }
}
