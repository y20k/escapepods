/*
 * DownloadHelper.kt
 * Implements the DownloadHelper class
 * A DownloadHelper provides helper methods for downloading files
 *
 * This file is part of
 * ESCAPEPODS - Free and Open Podcast App
 *
 * Copyright (c) 2018 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.escapepods.helpers

import java.net.URL


/*
 * DownloadHelper class
 */
class DownloadHelper {

    /* Checks if given feed string is XML */
    fun isXml(feedUrl: String): Boolean {
        // FIRST check if NOT XML
        if (!feedUrl.startsWith("http", true)) return false
        if (!feedUrlIsParsable(feedUrl)) return false

        // THEN check if XML
        if (feedUrl.endsWith("xml", true)) return true
        return (mimeTypeIsXML(feedUrl))
    }


    /* Tries to parse feed URL string as URL */
    private fun feedUrlIsParsable(feedUrl: String): Boolean {
        try {
            URL(feedUrl)
        } catch (e: Exception) {
            return false
        }
        return true
    }


    /* Checks if mime type is "text/xml" */
    private fun mimeTypeIsXML(feedUrl: String): Boolean {
        // todo implement using https://developer.android.com/reference/java/net/URLConnection#guessContentTypeFromName(java.lang.String)
        // todo async
        return false
    }
}