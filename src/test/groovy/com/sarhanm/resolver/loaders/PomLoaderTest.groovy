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
        def loader = new PomLoader()
        def manifest = loader.load(text)

        then:
        manifest.getVersion("com.foobar", "jonny") == '1.3.64'
        manifest.getVersion("com.foobar", "grace") == '777.2.64.master.abcfsd453'

        ! manifest.getVersion("com.foobar", "fisher")
    }

    def 'test load empty pom'(){
        def file = new File("src/test/resources/empty-bom.pom")
        def text = file.text

        when:
        def loader = new PomLoader()
        def manifest = loader.load(text)

        then:
        manifest
    }

    def 'test load bad pom'(){
        def file = new File("src/test/resources/bad-bom.pom")
        def text = file.text

        when:
        def loader = new PomLoader()
        def manifest = loader.load(text)

        then:
        manifest
    }
}
