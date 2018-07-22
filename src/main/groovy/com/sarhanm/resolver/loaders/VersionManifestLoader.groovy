/**
 * Copyright (c) 2018 Zillow, Inc
 */
package com.sarhanm.resolver.loaders

import com.sarhanm.resolver.VersionManifest

/**
 *
 * @author Mohammad Sarhan (mohammad@)
 */
interface VersionManifestLoader {

    VersionManifest load(String text)
}
