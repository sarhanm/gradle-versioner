/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver.loaders

import spock.lang.Specification

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class YamlLoaderTest extends Specification{


    def 'test yaml loading'(){

        def file = new File("src/test/resources/version-manifest-test.yaml")
        def data = file.text
        def yamlLoader = new YamlLoader()

        when:
        def manifest = yamlLoader.load(data)

        then:
        def version = manifest.getVersion("com.coinfling", "auth-service-api")
        version == '1.0-SNAPSHOT'

        when:
        version = manifest.getVersion('com.foobar', 'jonny-service-impl')

        then:
        version == '1.3.4.master.abcfb498'
    }

    def 'test bad yaml file'(){
        def file = new File("src/test/resources/version-manifest-malformed.yaml")

        def data = file.text
        def yamlLoader = new YamlLoader()

        when:
        def manifest = yamlLoader.load(data)

        then:
        manifest == null
    }

}
