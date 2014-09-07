package com.sarhanm.resolver

import org.junit.Test

import static org.junit.Assert.*

/**
 *
 * @author mohammad sarhan
 */
class VersionDynamicTest {


    @Test
    void testDynamicVersionValid()
    {
        VersionRange.metaClass.static.isValidRange << { String it -> return new VersionRange(it).valid }

        assertTrue VersionRange.isValidRange("n.n.n.master+")
        assertTrue VersionRange.isValidRange("1.n.n.master+")
        assertTrue VersionRange.isValidRange("1.3.n.master+")
        assertTrue VersionRange.isValidRange("1.4.2.master+")
        assertTrue VersionRange.isValidRange("1.4.2.4.hotfix-now+")

        assertFalse VersionRange.isValidRange("1.n.n.master")
        assertFalse VersionRange.isValidRange("1.n.master+")
        assertFalse VersionRange.isValidRange("1.n.n-master+")
        assertFalse VersionRange.isValidRange("1.2.3")
        assertFalse VersionRange.isValidRange("1.4.master+")
        assertFalse VersionRange.isValidRange("1.n.master+")
    }


    @Test
    void testDynamicVersion()
    {
        def r = new VersionRange("1.n.n.master+")

        assertNotNull r
        assertEquals '1', r.lower.major
        assertEquals null, r.lower.minor
        assertEquals null, r.lower.point
        assertNull r.lower.hotfix
        assertEquals 'master', r.lower.branch
        assertNull r.upper


        r = new VersionRange("1.3.5.feature-foobar+")

        assertNotNull r
        assertEquals '1', r.lower.major
        assertEquals '3', r.lower.minor
        assertEquals '5', r.lower.point
        assertNull r.lower.hotfix
        assertEquals 'feature-foobar', r.lower.branch
        assertNull r.upper

        r = new VersionRange("1.3.5.6.hotfix-foobar+")

        assertNotNull r
        assertEquals '1', r.lower.major
        assertEquals '3', r.lower.minor
        assertEquals '5', r.lower.point
        assertEquals '6', r.lower.hotfix
        assertEquals 'hotfix-foobar', r.lower.branch
        assertNull r.upper
    }

    @Test
    void testContainsDynamicVersion()
    {
        def r = new VersionRange('1.2.3.master+')
        assert r.contains('1.2.3.master.a')
        assert r.contains('1.2.4.master.a')
        assert r.contains('2.2.3.master.a')
        assert r.contains('1.3.0.master.a')

        assert !r.contains('1.1.3.master.a')
        assert !r.contains('1.1.3.develop.a')

        r = new VersionRange('1.n.n.master+')

        assert r.contains('1.0.12.master.f7810ef')
        assert r.contains('1.0.16.master.66e8edb')
    }




}
