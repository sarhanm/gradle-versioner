package com.sarhanm.resolver

import nebula.test.IntegrationSpec

/**
 * @author Mohammad Sarhan ( mohammad@ ) 
 * @date 2/11/16
 */
class VersionResolverBuildTest extends IntegrationSpec{

    //WORK IN PROGRESS. NOT YET COMPLETE
    def 'test version resolution'(){
        buildFile << """
        apply plugin: 'java'
        apply plugin: 'maven'
        apply plugin: 'com.sarhanm.version-resolver'


        repositories{
            jcenter()
            mavenCentral()
        }


        versionResolver{
            versionManifest{
                url = "file://" + file('versions.yaml')
            }
        }

        println "File: " + file('versions.yaml')

        dependencies{
            compile 'com.google.guava:guava:auto'
        }
        """.stripIndent()

        //Need a java file to actually make compile go and resolution fail when we have an issue
        //otherwise resolution fails silently
        def javaFile = new File(buildFile.parentFile, "src/main/java/HelloWorld.java")
        javaFile.parentFile.mkdirs()
        javaFile << """
        public class HelloWorld{
            public static void main(String[] args){

            }
        }
        """

        def versionsFile = new File(buildFile.parentFile, "versions.yaml")
        versionsFile << """
        modules:
          'com.google.guava:guava': 19.0
        """.stripIndent()

        when:
        def result = runTasks("build", "dependencies")
        println "OUTPUT: "
        println result.standardOutput
        println "ERROR: "
        println result.standardError
        then:
        result.success
    }
}
