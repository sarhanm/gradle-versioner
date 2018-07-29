package com.sarhanm.resolver

import org.junit.BeforeClass
import org.junit.Test
import spock.lang.Specification

import static org.junit.Assert.*

/**
 *
 * @author mohammad sarhan
 */
class VersionRangeTest extends Specification{


    def testValidRange() {

        when:
        def t = 1

        then:
        isValidRange('[1.2.3.master,]')
        isValidRange('[1.2.3.master,)')
        isValidRange('(1.2.3.master,]')
        isValidRange('(1.2.3.master,)')

        isValidRange('[,1.2.3.master]')
        isValidRange('[,1.2.3.master)')
        isValidRange('(,1.2.3.master]')
        isValidRange('(,1.2.3.master)')

        isValidRange('[1.2.3.master,]')
        isValidRange('[1.2.3.master,)')
        isValidRange('(1.2.3.master,]')
        isValidRange('(1.2.3.master,)')

        isValidRange('[1.2.3.master,4.5.32.master]')
        isValidRange('[1.2.3.master,4.5.32.master)')
        isValidRange('(1.2.3.master,4.5.32.master]')
        isValidRange('(1.n.n.master,4.5.32.master)')
    }


    void testRange() {
        when:
        def r = new VersionRange("[1.2.3.master,]")

        then:
        r
        r.lowerInclusive
        r.upperInclusive
        ! r.upper

        '1' == r.lower.major
        '2' ==  r.lower.minor
        '3' == r.lower.point
        'master' ==  r.lower.branch

        when:
        r = new VersionRange("[1.2.3.master,4.3.2.foobar]")

        then:
        r
        r.lowerInclusive
        r.upperInclusive
        r.upper

        '1' == r.lower.major
        '2' == r.lower.minor
        '3' == r.lower.point
        'master' == r.lower.branch


        '4' == r.upper.major
        '3' == r.upper.minor
        '2' == r.upper.point
        'foobar' == r.upper.branch

        when:
        r = new VersionRange("[,4.3.2.foobar]")

        then:
        r
        r.lowerInclusive
        r.upperInclusive
        r.upper
        ! r.lower

        '4' == r.upper.major
        '3' == r.upper.minor
        '2' == r.upper.point
        'foobar' == r.upper.branch
    }

    void testInvalidRange() {

        when:
        new VersionRange("[,]")

        then:

        thrown IllegalArgumentException
    }


    void testInclusiveExclusiveRange() {
        when:
        def r = new VersionRange("[1.2.3.master,]")
        then:
        r
        r.lowerInclusive
        r.upperInclusive

        when:
        r = new VersionRange("(1.2.3.master,]")
        then:
        r
        ! r.lowerInclusive
        r.upperInclusive

        when:
        r = new VersionRange("(1.2.3.master,)")
        then:
        r
        ! r.lowerInclusive
        ! r.upperInclusive
    }


    void testContainsLower() {
        when:
        def r = new VersionRange("[1.2.3.master,]")
        then:
        !r.contains('1.2.3.foobar.abc')

        r.contains('1.2.3.master.a')
        r.contains('1.2.4.master.a')
        ! r.contains('1.2.2.master.a')

        when:
        r = new VersionRange("(1.2.3.master,]")

        then:
        ! r.contains('1.2.3.master')
    }



    void testContainsUpper() {

        when:
        def r = new VersionRange("[,1.2.3.master]")

        then:
        ! r.contains('1.2.3.foobar.abc')

        r.contains('1.1.3.master.a')
        r.contains('1.1.4.master.a')
        ! r.contains('1.2.4.master.a')
        ! r.contains('2.2.3.master.a')
        ! r.contains('1.3.3.master.a')

        when:
        r = new VersionRange("[,1.2.3.master)")

        then:
        ! r.contains('1.2.3.master')
    }


    void testContainsRange() {
        when:
        def r = new VersionRange("[1.n.3.master,2.0.0.master]")

        then:

        r.contains('1.2.3.master.a')
        r.contains('1.4.3098.master.a')
        r.contains('1.999.3.master.a')
        r.contains('2.0.0.master.a')
        r.contains('1.1.3.master.a')
        r.contains('1.1.4.master.a')

        ! r.contains('3.0.3.master.a')
        ! r.contains('2.0.3.master.a')
        ! r.contains('2.1.0.master.a')
    }

    void testNumericDynamicVersion(){
        when:
        def r = new VersionRange("1.2.n.master+")

        then:
        r.valid
        r.numericDynamicVersion == "1.2+"

        when:
        r = new VersionRange("1.n.n.feature_foobar+")

        then:
        r.valid
        r.numericDynamicVersion == '1+'

        when:
        r = new VersionRange("n.n.n.feature_foobar+")

        then:
        r.valid
        r.numericDynamicVersion == '+'


        when:
        r = new VersionRange("9.34.6832.develop+")

        then:
        r.valid
        r.numericDynamicVersion == '9.34.6832+'
    }


    private static void isValidRange(String val){
        assert new VersionRange(val).valid
    }
}
