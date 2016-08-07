package com.sarhanm.resolver

import org.junit.BeforeClass
import org.junit.Test

import static org.junit.Assert.*

/**
 *
 * @author mohammad sarhan
 */
class VersionRangeTest {

    @Test
    void testValidRange() {
        VersionRange.metaClass.static.isValidRange << { String it -> return new VersionRange(it).valid }

        assertTrue VersionRange.isValidRange('[1.2.3.master,]')
        assertTrue VersionRange.isValidRange('[1.2.3.master,)')
        assertTrue VersionRange.isValidRange('(1.2.3.master,]')
        assertTrue VersionRange.isValidRange('(1.2.3.master,)')

        assertTrue VersionRange.isValidRange('[,1.2.3.master]')
        assertTrue VersionRange.isValidRange('[,1.2.3.master)')
        assertTrue VersionRange.isValidRange('(,1.2.3.master]')
        assertTrue VersionRange.isValidRange('(,1.2.3.master)')

        assertTrue VersionRange.isValidRange('[1.2.3.master,]')
        assertTrue VersionRange.isValidRange('[1.2.3.master,)')
        assertTrue VersionRange.isValidRange('(1.2.3.master,]')
        assertTrue VersionRange.isValidRange('(1.2.3.master,)')

        assertTrue VersionRange.isValidRange('[1.2.3.master,4.5.32.master]')
        assertTrue VersionRange.isValidRange('[1.2.3.master,4.5.32.master)')
        assertTrue VersionRange.isValidRange('(1.2.3.master,4.5.32.master]')
        assertTrue VersionRange.isValidRange('(1.n.n.master,4.5.32.master)')
    }


    @Test
    void testRange() {
        def r = new VersionRange("[1.2.3.master,]")
        assertNotNull r
        assertTrue r.lowerInclusive
        assertTrue r.upperInclusive

        assertEquals '1', r.lower.major
        assertEquals '2', r.lower.minor
        assertEquals '3', r.lower.point
        assertEquals 'master', r.lower.branch

        assertNull r.upper


        r = new VersionRange("[1.2.3.master,4.3.2.foobar]")
        assertNotNull r
        assertTrue r.lowerInclusive
        assertTrue r.upperInclusive

        assertEquals '1', r.lower.major
        assertEquals '2', r.lower.minor
        assertEquals '3', r.lower.point
        assertEquals 'master', r.lower.branch

        assertNotNull r.upper
        assertEquals '4', r.upper.major
        assertEquals '3', r.upper.minor
        assertEquals '2', r.upper.point
        assertEquals 'foobar', r.upper.branch


        r = new VersionRange("[,4.3.2.foobar]")
        assertNotNull r
        assertTrue r.lowerInclusive
        assertTrue r.upperInclusive

        assertNull r.lower

        assertNotNull r.upper
        assertEquals '4', r.upper.major
        assertEquals '3', r.upper.minor
        assertEquals '2', r.upper.point
        assertEquals 'foobar', r.upper.branch
    }

    @Test(expected = IllegalArgumentException)
    void testInvalidRange() {
        new VersionRange("[,]")
    }

    @Test
    void testInclusiveExclusiveRange() {
        def r = new VersionRange("[1.2.3.master,]")
        assertNotNull r
        assertTrue r.lowerInclusive
        assertTrue r.upperInclusive

        r = new VersionRange("(1.2.3.master,]")
        assertNotNull r
        assertFalse r.lowerInclusive
        assertTrue r.upperInclusive

        r = new VersionRange("(1.2.3.master,)")
        assertNotNull r
        assertFalse r.lowerInclusive
        assertFalse r.upperInclusive
    }

    @Test
    void testContainsLower() {
        def r = new VersionRange("[1.2.3.master,]")
        assert !r.contains('1.2.3.foobar.abc')

        assert r.contains('1.2.3.master.a')
        assert r.contains('1.2.4.master.a')
        assert !r.contains('1.2.2.master.a')

        r = new VersionRange("(1.2.3.master,]")
        assert !r.contains('1.2.3.master')

    }


    @Test
    void testContainsUpper() {
        def r = new VersionRange("[,1.2.3.master]")
        assert !r.contains('1.2.3.foobar.abc')

        assert r.contains('1.1.3.master.a')
        assert r.contains('1.1.4.master.a')
        assert !r.contains('1.2.4.master.a')
        assert !r.contains('2.2.3.master.a')
        assert !r.contains('1.3.3.master.a')

        r = new VersionRange("[,1.2.3.master)")
        assert !r.contains('1.2.3.master')
    }

    @Test
    void testContainsRange() {
        def r = new VersionRange("[1.n.3.master,2.0.0.master]")
        assert r.contains('1.2.3.master.a')
        assert r.contains('1.4.3098.master.a')
        assert r.contains('1.999.3.master.a')
        assert r.contains('2.0.0.master.a')
        assert r.contains('1.1.3.master.a')
        assert r.contains('1.1.4.master.a')

        assert !r.contains('3.0.3.master.a')
        assert !r.contains('2.0.3.master.a')
        assert !r.contains('2.1.0.master.a')
    }
}
