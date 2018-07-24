/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver.pom

import spock.lang.Specification

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
class PomTest extends Specification {

    def 'test strip key'(){

        Pom p = new Pom(null,null,null)

        when:
        String v = '${onetwo}'

        then:
        p.stripKey(v) == 'onetwo'

        when:
        v = '${ onetwo  }'
        then:
        p.stripKey(v) == 'onetwo'

        when:
        v = '${ onetwo}'
        then:
        p.stripKey(v) == 'onetwo'


        when:
        v = '    ${ onetwo}'
        then:
        p.stripKey(v) == 'onetwo'
    }
}
