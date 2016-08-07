package com.sarhanm.resolver

import org.junit.Test

import static junit.framework.TestCase.assertEquals
import static junit.framework.TestCase.assertFalse
import static junit.framework.TestCase.assertTrue

/**
 *
 * @author mohammad sarhan
 */
class VersionTest {

    @Test
    void testVersionValid() {
        Version.metaClass.static.isValidVersion << { String it -> return new Version(it).valid }

        //Valid version strings
        assertTrue Version.isValidVersion("1.2.3.master.abcdf")
        assertTrue Version.isValidVersion("1.2.3.feature-foobar.abcdf")
        assertTrue Version.isValidVersion("1.2.3.4.hotfix-my_fix.abcdf")
        assertTrue Version.isValidVersion("134.2543.3768.master.abcdf")

        //invalid version strings
        assertFalse Version.isValidVersion("1.2.3.master.abcdfg")
        assertFalse Version.isValidVersion("1.2.3")
        assertFalse Version.isValidVersion("1.2.3.abcdf")
        assertFalse Version.isValidVersion("1.2.3.master")
        assertFalse Version.isValidVersion("1.2.3.4.5.master.abcdf")
        assertFalse Version.isValidVersion("1.2.3..master.abcdf")

        assert Version.isValidVersion('1.0.12.master.f7810ef')
        assert Version.isValidVersion('1.0.16.master.66e8edb')
    }

    @Test
    void testVersionParse() {
        def v = new Version("1.2.3.master.abcdf")
        assertEquals '1', v.major
        assertEquals '2', v.minor
        assertEquals '3', v.point
        assertEquals 'master', v.branch
        assertEquals null, v.hotfix

        v = new Version("132.768.1034532.feature-foobar.abcdf")
        assertEquals '132', v.major
        assertEquals '768', v.minor
        assertEquals '1034532', v.point
        assertEquals 'feature-foobar', v.branch
        assertEquals null, v.hotfix

        v = new Version("132.768.1034532.32.hotfix-a-big-hotfix.abcdf")
        assertEquals '132', v.major
        assertEquals '768', v.minor
        assertEquals '1034532', v.point
        assertEquals 'hotfix-a-big-hotfix', v.branch
        assertEquals '32', v.hotfix

    }

    @Test
    void testToString() {
        def v = new Version("1.2.3.master.ab4f")
        assert v.version == '1.2.3.master.ab4f'
    }
}
