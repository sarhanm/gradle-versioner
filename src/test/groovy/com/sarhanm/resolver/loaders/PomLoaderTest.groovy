/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver.loaders

import spock.lang.Specification

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class PomLoaderTest extends Specification {

    def 'test load pom'(){

        def file = new File("src/test/resources/bom.pom")
        def text = file.text

        when:
        def loader = new PomLoader(null)
        def manifest = loader.load(text)

        then:
        manifest.getVersion("com.foobar", "jonny") == '1.3.66'
        manifest.getVersion("com.foobar", "grace") == '777.2.64.master.abcfsd453'

        ! manifest.getVersion("com.foobar", "fisher")
    }

    def 'test load empty pom'(){
        def file = new File("src/test/resources/empty-bom.pom")
        def text = file.text

        when:
        def loader = new PomLoader(null)
        def manifest = loader.load(text)

        then:
        manifest
    }

    def 'test load bad pom'(){
        def file = new File("src/test/resources/bad-bom.pom")
        def text = file.text

        when:
        def loader = new PomLoader(null)
        def manifest = loader.load(text)

        then:
        manifest
    }

    def 'test load with parent pom'(){
        def file = new File("src/test/resources/bom-with-parent.pom")
        def text = file.text

        def loader = Spy(PomLoader){
            resolveFile('com.foobar','release-platform','1.0-SNAPSHOT') >>  new File("src/test/resources/bom.pom")
        }

        when:

        def manifest = loader.load(text)

        then:

        manifest.getVersion("com.foobar", "jonny") == '1.3.64-override-property'
        manifest.getVersion("com.foobar", "grace") == 'override-dependency-node-version'
        manifest.getVersion("com.foobar", "bob") == '1.666.64'

        manifest.size() == 4
    }
}
