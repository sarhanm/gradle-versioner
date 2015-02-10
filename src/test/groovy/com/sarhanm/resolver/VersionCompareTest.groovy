package com.sarhanm.resolver

import org.junit.Test

/**
 *
 * @author mohammad sarhan
 */
class VersionCompareTest {


    def v = { new Version(it)}

    @Test
    void testEquality()
    {
        assert v('1.2.3.master.abc') == v('1.2.3.master.abc')
        assert v('1.2.3.4.master.abc') == v('1.2.3.4.master.abc')
        assert v('1.2.3.master.abc') != v('1.2.3.feature.abc')
        assert v('1.2.n.master.abc') <  v('1.2.3.master.abc')

        assert v('1.3.n.master.abc') >  v('1.2.3.master.abc')
        assert v('1.3.n.n.master.abc') >  v('1.2.3.master.abc')
        assert v('1.3.n.n.master.abc') <  v('1.3.0.0.master.abc')
        assert v('1.3.n.master.abc') <  v('1.3.0.0.master.abc')
        assert v('1.3.2.master.abc') <  v('1.3.2.1.master.abc')
    }

}
