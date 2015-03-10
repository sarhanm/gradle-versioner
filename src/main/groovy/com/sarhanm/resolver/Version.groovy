package com.sarhanm.resolver

import groovy.transform.ToString

/**
 *
 * @author mohammad sarhan
 */
@ToString
class Version implements  Comparable<Version>{


    //example: 1.2.3.master.abcd
    //example: 1.2.3.4.feature-foobar.fedc
    protected static final regex_version = ~/([0-9n]+)\.([0-9n]+)\.([0-9n]+)(\.([0-9n]+))?\.([0-9a-zA-Z_-]+)/
    protected static final regex_full_version = ~/$regex_version\.([abcdefABCDEF0-9]+)/

    def version
    def major
    def minor
    def point
    def hotfix
    def branch
    def commit

    def boolean valid

    Version(String v) {

        def matcher = v =~ regex_full_version

        if(!matcher.matches())
        {
            valid = false
            return
        }

        version = v

        def resolve = { it && it == 'n' ? null:  it}

        major = resolve(matcher[0][1])
        minor = resolve(matcher[0][2])
        point = resolve(matcher[0][3])
        hotfix = resolve(matcher[0][5])
        branch = resolve(matcher[0][6])
        commit = matcher[0][7]

        valid = true
    }

    def int compareTo(Version other)
    {
        // Compare major, minor, point, hotfix as integers;
        // compare branch alphabetically; ignore commit hash
        getIntVersionNum(major) <=> getIntVersionNum(other.major) ?:
        getIntVersionNum(minor) <=> getIntVersionNum(other.minor) ?:
        getIntVersionNum(point) <=> getIntVersionNum(other.point) ?:
        getIntVersionNum(hotfix) <=> getIntVersionNum(other.hotfix) ?:
        branch <=> other.branch
    }

    def int getIntVersionNum( String ver )
    {
        if ( ver == null || ver == '' || ver == 'n' )
        {
            return -1
        }

        Integer.parseInt(ver)
    }

    def boolean equals(Object obj) {
        this.compareTo(obj) == 0
    }
}
