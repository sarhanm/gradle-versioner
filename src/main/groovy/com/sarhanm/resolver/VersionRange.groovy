package com.sarhanm.resolver

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 *
 * @author mohammad sarhan
 */
class VersionRange {

    private static final regex_range = ~/(\[|\()($Version.regex_version)?,($Version.regex_version)?(\]|\))/
    private static final regex_dynamic_version = ~/($Version.regex_version)\+/

    def Version lower
    def Version upper
    def boolean lowerInclusive
    def boolean upperInclusive

    def valid

    VersionRange(String range) {

        def matcher = range =~ regex_range

        if (matcher.matches()) {
            parseRange(matcher)
            valid = true
            return
        }

        matcher = range =~ regex_dynamic_version
        if (matcher.matches()) {
            parseDynamicVersion(matcher)
            valid = true
            return
        }

        valid = false
    }

    def boolean contains(String v) {
        def version = new Version(v)

        if (!version.valid)
            return false

        if (!(lower?.branch == version.branch || upper?.branch == version.branch))
            return false

        if (lower && (lower > version || (!lowerInclusive && lower == version)))
            return false

        if (upper && (upper < version || (!upperInclusive && upper == version)))
            return false

        return true
    }

    /**
     *
     * @return a dynamic version consisting of only the numeric 3 part version
     */
    String getNumericDynamicVersion() {
        //We will return the lower version
        if (!valid)
            return null

        String num = ""
        if (lower.major && lower.major != "n")
            num += lower.major
        else
            return "+"

        if (lower.minor && lower.minor != "n")
            num += ".${lower.minor}"
        else
            return "${num}+"

        if (lower.point && lower.point != "n")
            num += ".${lower.point}"

        return "${num}+"

    }

    private parseRange(def Matcher matcher) {

        lowerInclusive = matcher[0][1] == "["
        upperInclusive = matcher[0][16] == "]"

        def String lowerString = matcher[0][2]
        def String upperString = matcher[0][9]

        //".a" is a dummy commit hash to make the version valid.
        // It doesn't matter what the commit hash is, since this is a version range object.
        if (lowerString) {
            lower = new Version(lowerString + ".a")
        }

        if (upperString)
            upper = new Version(upperString + ".a")

        if (!lower && !upper)
            throw new IllegalArgumentException("Cannot have both lower and upper ranges missing")
    }

    private parseDynamicVersion(def Matcher matcher) {
        lower = new Version(matcher[0][1] + ".a")
        lowerInclusive = true
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VersionRange{");
        sb.append("lower=").append(lower);
        sb.append(", upper=").append(upper);
        sb.append(", lowerInclusive=").append(lowerInclusive);
        sb.append(", upperInclusive=").append(upperInclusive);
        sb.append(", valid=").append(valid);
        sb.append('}');
        return sb.toString();
    }
}
