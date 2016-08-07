package com.sarhanm.resolver

import org.junit.Test

/**
 *
 * @author mohammad sarhan
 */
class VersionCompareTest {


    def v = { new Version(it) }

    @Test
    void testEquality() {
        assert v('1.2.3.master.abc') == v('1.2.3.master.abc')
        assert v('1.2.3.4.master.abc') == v('1.2.3.4.master.abc')
        assert v('1.2.3.master.abc') != v('1.2.3.feature.abc')
        assert v('1.2.n.master.abc') < v('1.2.3.master.abc')

        assert v('1.3.n.master.abc') > v('1.2.3.master.abc')
        assert v('1.3.n.n.master.abc') > v('1.2.3.master.abc')
        assert v('1.3.n.n.master.abc') < v('1.3.0.0.master.abc')
        assert v('1.3.n.master.abc') < v('1.3.0.0.master.abc')
        assert v('1.3.2.master.abc') < v('1.3.2.1.master.abc')

        // Verify major version numbers are compared numerically
        assert v('1.2.3.master.abc') < v('5.2.3.master.abc')
        assert v('10.2.3.master.abc') > v('5.2.3.master.abc')

        // Verify minor version numbers are compared numerically
        assert v('1.2.3.master.abc') < v('1.5.3.master.abc')
        assert v('1.10.3.master.abc') > v('1.5.3.master.abc')

        // Verify point version numbers are compared numerically
        assert v('1.2.3.master.abc') < v('1.2.5.master.abc')
        assert v('1.2.10.master.abc') > v('1.2.5.master.abc')

        // Verify hotfix version numbers are compared numerically
        assert v('1.2.3.4.master.abc') < v('1.2.3.5.master.abc')
        assert v('1.2.3.10.master.abc') > v('1.2.3.5.master.abc')

        // Verify branch names are compared alphabetically
        assert v('1.2.3.beta1.abc') < v('1.2.3.beta2.abc')
        assert v('1.2.3.beta10.abc') < v('1.2.3.beta2.abc')

        // Verify commit hash is ignored
        assert v('1.2.3.master.abc') == v('1.2.3.master.def')
    }

}
