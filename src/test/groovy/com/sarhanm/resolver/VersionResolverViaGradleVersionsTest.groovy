package com.sarhanm.resolver

import com.sarhanm.IntegrationSpec
import org.apache.commons.io.FileUtils

/**
 * @author Mohammad Sarhan ( mohammad@ ) 
 * @date 2/11/16
 */
class VersionResolverViaGradleVersionsTest extends IntegrationSpec {

    //Child classes can override this method to re-run all tests with a different version of gradle.
    String getAlternateGradleVersion(){ null }

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

    def 'test version resolution'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }
        """.stripIndent()

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        def java = file('src/main/java/Hello.java')
        java.parentFile.mkdirs()
        java.text = 'public class Hello{}'

        when:
        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-lang:commons-lang')
        setupGradlRunVersion(runner)
        def result = runner.build()

        then:

        //We should inherit the version of commons-lang from the commons-configuration dependencies
        result.output.contains("commons-lang:commons-lang:2.6\n")
    }

    def 'resolve release platform override of transitive dependency'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }
        """.stripIndent()

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.0'
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-lang:commons-lang')
        setupGradlRunVersion(runner)
        def result = runner.build()

        then:
        //release platform is setting commons-lang to 2.0, so that should be the version
        // of our transitive dependency regardless of what version is defined in that pom.
        result.output.contains 'commons-lang:commons-lang:2.6 -> 2.0\n'

    }

    def 'resolve via gradle resolution of transitive dep'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """

        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
            //compile 'commons-lang:commons-lang:2.0'
        }
        """.stripIndent()


        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-lang:commons-lang')
        setupGradlRunVersion(runner)
        def result = runner.build()

        then:
        // This is the default gradle strategy. Gradle picks the newest and since we don't have a pinned version
        // in the manifest, the latest version is selected.
        result.output.contains 'commons-lang:commons-lang:2.6\n'

        result.output.contains 'commons-configuration:commons-configuration:1.10\n'

    }

    def 'hard pinned version in build with def in version manifest with transitive dep'() {
        buildFile << DEFAULT_BUILD + DEFAULT_MANIFEST + """
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
            compile 'commons-lang:commons-lang:2.0'
        }
        """.stripIndent()

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.5'
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-lang:commons-lang')
        setupGradlRunVersion(runner)
        def result = runner.build()

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

        versionsFile.text = """
        modules:
          'commons-configuration:commons-configuration': '1.10'
        """.stripIndent()

        when:
        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')
        setupGradlRunVersion(runner)
        def result = runner.build()

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

        versionsFile.text = """
        modules:
          'commons-lang:commons-lang': '2.5'
        """.stripIndent()

        when:

        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')
        setupGradlRunVersion(runner)
        def result = runner.build()

        then:
        // Allowing pinned version in build.gradle to override manifest.
        result.output ==~ /(?ms).*commons-configuration:commons-configuration:\+ -> [^\s]*\n.*/

    }

    def 'resolve using versioner dynamic version'() {
        buildFile << """
        plugins{
            id 'com.sarhanm.version-resolver'
        }
        apply plugin: 'java'
        apply plugin: 'maven'

        dependencies{
            compile 'test:test-jar:1.n.n.master+'
        }

        repositories{
            maven{ url file('build/.m2/repository').path }
        }

        """.stripIndent()

        when:
        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'test:test-jar')
        runner.withDebug(true)
        setupGradlRunVersion(runner)
        def result = runner.build()

        then:
        result.output ==~ /(?ms).*test:test-jar:1.n.n.master\+ -> 1.0.2.master.abcdef\n.*/
    }

    def 'resolve using versioner dynamic version with auto'() {
        buildFile << """
        plugins{
            id 'com.sarhanm.version-resolver'
        }
        apply plugin: 'java'
        apply plugin: 'maven'

        dependencies{
            compile 'test:test-jar:auto'
        }

        repositories{
            maven{ url file('build/.m2/repository').path }
        }

        """.stripIndent() + DEFAULT_MANIFEST

        versionsFile.text = """
        modules:
          'test:test-jar': '1.n.n.master+'
        """.stripIndent()

        when:
        def result = runSuccessfully('build', 'dependencyInsight', '--dependency', 'test:test-jar')

        then:
        result.output ==~ /(?ms).*test:test-jar:auto -> 1.0.2.master.abcdef\n.*/
    }


    def 'resolve without manifest'() {
        buildFile << DEFAULT_BUILD + """
        dependencies{
            compile 'commons-configuration:commons-configuration:1.9'
        }
        """.stripIndent()

        when:
        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration')
        setupGradlRunVersion(runner)
        def result = runner.build()

        then:
        result.output.contains "commons-configuration:commons-configuration:1.9\n"

    }

    def 'test pom generation with nebula-publish'() {

        given: 'Apply the nebula-resolved-dependencies'

        def nebulaVersion
        def gradleVersion = getAlternateGradleVersion()

        if(!gradleVersion)
            nebulaVersion = '+'
        else if(gradleVersion.startsWith('4'))
            nebulaVersion = '5.1.0'
        else
            nebulaVersion = '4.8.1'

        buildFile << """
        buildscript {
          repositories { jcenter() }
          dependencies {
            classpath 'com.netflix.nebula:nebula-publishing-plugin:$nebulaVersion'
          }
        }
        """.stripIndent() + DEFAULT_BUILD + DEFAULT_MANIFEST + '''

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
        def runner = getRunner(true, 'build', 'generatePomFileForMavenTestPubPublication')
        setupGradlRunVersion(runner)
        def result = runner.build()

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
        def runner = getRunner(true, 'build', 'generatePomFileForMavenTestPubPublication')
        setupGradlRunVersion(runner)
        def result = runner.build()

        then:
        def f = file("build/publications/mavenTestPub/pom-default.xml")
        f.exists()

        def root = new XmlSlurper().parse(file("build/publications/mavenTestPub/pom-default.xml"))
        def dep = root.dependencies.dependency
        dep.artifactId.text() == "commons-configuration"
        dep.version.text() == 'auto'
    }

    def 'test via versionManifest configuration'() {
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
        def runner = getRunner(true, "build", "--info")
        setupGradlRunVersion(runner)
        def result = runner.build()

        then:
        true

    }

    def 'test multi project build'(){
        buildFile << DEFAULT_BUILD

        def baseScript = """
        apply plugin: 'com.sarhanm.version-resolver'
        apply plugin: 'java'

        """.stripIndent()

        addSubproject("api", baseScript)
        addSubproject("shared", baseScript)

        addSubproject("impl", baseScript + """

        dependencies{
            compile project(':api')
            compile project(':shared')
        }
        """.stripIndent())

        addSubproject("job", baseScript + """

        dependencies{
            compile project(':api')
            //Shared should come along for the ride as a project dependency
            runtime project(':impl')
        }
        """.stripIndent())

        when:
        def runner = getRunner(true, "clean","build", "--info")
        setupGradlRunVersion(runner)
        def result = runner.build()

        then:
        true

    }

    def 'resolve via http options.url'(){
        buildFile << DEFAULT_BUILD + """
        versionResolver{
            versionManifest{
                url = 'https://raw.githubusercontent.com/sarhanm/gradle-versioner/master/src/test/resources/test-repo/test/manifest/2.0/manifest-2.0.yaml'
            }
        }
        dependencies{
            compile 'commons-configuration:commons-configuration:auto'
        }
        """.stripIndent()

        def java = file('src/main/java/Hello.java')
        java.parentFile.mkdirs()
        java.text = 'public class Hello{}'

        when:
        def runner = getRunner(true, 'build', 'dependencyInsight', '--dependency', 'commons-configuration:commons-configuration', '--stacktrace')
        setupGradlRunVersion(runner)
        def result = runner.build()

        then:
        println result.output
        //We should inherit the version of commons-lang from the commons-configuration dependencies
        result.output.contains("commons-configuration:commons-configuration:auto -> 1.9\n")
    }

    def setupLocalepo() {
        def repo = file('build/.m2/repository')
        repo.mkdirs()
        FileUtils.copyDirectory(new File("src/test/resources/test-repo"), repo)
    }

    def setupGradlRunVersion(runner){
        def ver = alternateGradleVersion
        if(ver)
            runner.withGradleVersion(alternateGradleVersion)
    }
}
