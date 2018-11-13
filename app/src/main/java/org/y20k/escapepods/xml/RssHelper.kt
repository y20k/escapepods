/*
 * RssHelper.kt
 * Implements the RssHelper class
 * A RssHelper reads and parses podcast RSS feeds
 *
 * This file is part of
 * ESCAPEPODS - Free and Open Podcast App
 *
 * Copyright (c) 2018 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.escapepods.xml

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.y20k.escapepods.core.Episode
import org.y20k.escapepods.core.Podcast
import org.y20k.escapepods.helpers.DateHelper
import org.y20k.escapepods.helpers.FileHelper
import org.y20k.escapepods.helpers.Keys
import org.y20k.escapepods.helpers.LogHelper
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/*
 * RssHelper class
 */
class RssHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(RssHelper::class.java.name)


    /* Main class variables */
    private var podcast: Podcast = Podcast()


    /* Read RSS feed from given input stream - async using coroutine */
    suspend fun read(context: Context, localFileUri: Uri, remotePodcastFeedLocation: String): Podcast {
        return suspendCoroutine {cont ->
            // store remote feed location
            podcast.remotePodcastFeedLocation = remotePodcastFeedLocation

            LogHelper.e(TAG, "RSS read") // todo remove

            // try parsing
            val stream: InputStream = FileHelper.getTextFileStream(context, localFileUri)
            try {
                // create XmlPullParser for InputStream
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(stream, null)
                parser.nextTag();
                // start reading rss feed
                podcast = parseFeed(parser)
            } catch (exception : Exception) {
                exception.printStackTrace()
            } finally {
                stream.close()
            }

            // sort episodes - newest episode first
            podcast.episodes.sortByDescending { it.publicationDate }

            // return parsing result
            cont.resume(podcast)
        }
    }


    /* Parses whole RSS feed */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseFeed(parser: XmlPullParser): Podcast {
        parser.require(XmlPullParser.START_TAG, Keys.XML_NAME_SPACE, Keys.RSS_RSS)
        while (parser.next() != XmlPullParser.END_TAG) {
            // abort loop early if no start tag
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // read only relevant tags
            when (parser.name) {
                // found a podcast
                Keys.RSS_PODCAST -> readPodcast(parser)
                // skip to next tag
                else -> XmlHelper.skip(parser)
            }
        }
        return podcast
    }


    /* Reads podcast element - within feed */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readPodcast(parser: XmlPullParser) {
        parser.require(XmlPullParser.START_TAG, Keys.XML_NAME_SPACE, Keys.RSS_PODCAST)

        LogHelper.e(TAG, "parsing podcast") // todo remove

        while (parser.next() != XmlPullParser.END_TAG) {
            // abort loop early if no start tag
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // read only relevant tags
            when (parser.name) {
                // found a podcast name
                Keys.RSS_PODCAST_NAME -> podcast.name = readPodcastName(parser, Keys.XML_NAME_SPACE)
                // found a podcast description
                Keys.RSS_PODCAST_DESCRIPTION -> podcast.description = readPodcastDescription(parser, Keys.XML_NAME_SPACE)
                // found a podcast remoteImageFileLocation
                Keys.RSS_PODCAST_COVER_ITUNES -> podcast.remoteImageFileLocation = readPodcastCoverItunes(parser, Keys.XML_NAME_SPACE)
                Keys.RSS_PODCAST_COVER -> podcast.remoteImageFileLocation = readPodcastCover(parser, Keys.XML_NAME_SPACE)
                // found an episode
                Keys.RSS_EPISODE -> {
                    val episode: Episode = readEpisode(parser, podcast)
                    podcast.episodes.add(episode)
                }
                // skip to next tag
                else -> XmlHelper.skip(parser)
            }
        }

    }


    /* Reads episode element - within podcast element (within feed) */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEpisode(parser: XmlPullParser, podcast: Podcast): Episode {

        LogHelper.e(TAG, "parsing episode") // todo remove

        parser.require(XmlPullParser.START_TAG, Keys.XML_NAME_SPACE, Keys.RSS_EPISODE)

        // initialize episode
        val episode: Episode = Episode()
        episode.podcastName = podcast.name
        episode.cover = podcast.cover

        while (parser.next() != XmlPullParser.END_TAG) {
            // abort loop early if no start tag
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // read only relevant tags
            when (parser.name) {
                // found episode title
                Keys.RSS_EPISODE_GUID -> episode.guid = readEpisodeGuid(parser, Keys.XML_NAME_SPACE)
                // found episode title
                Keys.RSS_EPISODE_TITLE -> episode.title = readEpisodeTitle(parser, Keys.XML_NAME_SPACE)
                // found episode description
                Keys.RSS_EPISODE_DESCRIPTION -> episode.description = readEpisodeDescription(parser, Keys.XML_NAME_SPACE)
                // found episode publication date
                Keys.RSS_EPISODE_PUBLICATION_DATE -> episode.publicationDate = readEpisodePublicationDate(parser, Keys.XML_NAME_SPACE)
                // found episode audio link
                Keys.RSS_EPISODE_AUDIO_LINK -> episode.remoteAudioFileLocation = readEpisodeAudioLink(parser, Keys.XML_NAME_SPACE)
                // skip to next tag
                else -> XmlHelper.skip(parser)
            }
        }
        return episode
    }


    /* PODCAST: read name */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPodcastName(parser: XmlPullParser, nameSpace: String?): String {
        parser.require(XmlPullParser.START_TAG, nameSpace, Keys.RSS_PODCAST_NAME)
        val name = XmlHelper.readText(parser)
        parser.require(XmlPullParser.END_TAG, nameSpace, Keys.RSS_PODCAST_NAME)
        return name
    }


    /* PODCAST: read description */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPodcastDescription(parser: XmlPullParser, nameSpace: String?): String {
        parser.require(XmlPullParser.START_TAG, nameSpace, Keys.RSS_PODCAST_DESCRIPTION)
        val summary = XmlHelper.readText(parser)
        parser.require(XmlPullParser.END_TAG, nameSpace, Keys.RSS_PODCAST_DESCRIPTION)
        return summary
    }


    /* EPISODE: read GUID */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readEpisodeGuid(parser: XmlPullParser, nameSpace: String?): String {
        parser.require(XmlPullParser.START_TAG, nameSpace, Keys.RSS_EPISODE_GUID)
        val title = XmlHelper.readText(parser)
        parser.require(XmlPullParser.END_TAG, nameSpace, Keys.RSS_EPISODE_GUID)
        return title
    }


    /* EPISODE: read title */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readEpisodeTitle(parser: XmlPullParser, nameSpace: String?): String {
        parser.require(XmlPullParser.START_TAG, nameSpace, Keys.RSS_EPISODE_TITLE)
        val title = XmlHelper.readText(parser)
        parser.require(XmlPullParser.END_TAG, nameSpace, Keys.RSS_EPISODE_TITLE)
        return title
    }


    /* EPISODE: read description */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readEpisodeDescription(parser: XmlPullParser, nameSpace: String?): String {
        parser.require(XmlPullParser.START_TAG, nameSpace, Keys.RSS_EPISODE_DESCRIPTION)
        val summary = XmlHelper.readText(parser)
        parser.require(XmlPullParser.END_TAG, nameSpace, Keys.RSS_EPISODE_DESCRIPTION)
        return summary
    }


    /* EPISODE: read publication date */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readEpisodePublicationDate(parser: XmlPullParser, nameSpace: String?): Date {
        parser.require(XmlPullParser.START_TAG, nameSpace, Keys.RSS_EPISODE_PUBLICATION_DATE)
        val publicationDate = XmlHelper.readText(parser)
        parser.require(XmlPullParser.END_TAG, nameSpace, Keys.RSS_EPISODE_PUBLICATION_DATE)
        return DateHelper.convertRFC2822(publicationDate)
    }


    /* PODCAST: read remoteImageFileLocation - standard tag variant */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPodcastCover(parser: XmlPullParser, nameSpace: String?): String {
        var link = String()
        parser.require(XmlPullParser.START_TAG, nameSpace, Keys.RSS_PODCAST_COVER)
        while (parser.next() != XmlPullParser.END_TAG) {
            // abort loop early if no start tag
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // read only relevant tags
            when (parser.name) {
                // found episode cover
                Keys.RSS_PODCAST_COVER_URL -> link = readPodcastCoverUrl(parser, nameSpace)
                // skip to next tag
                else -> XmlHelper.skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, nameSpace, Keys.RSS_PODCAST_COVER)
        return link
    }


    /* PODCAST: read remoteImageFileLocation URL - within remoteImageFileLocation*/
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPodcastCoverUrl(parser: XmlPullParser, nameSpace: String?): String {
        parser.require(XmlPullParser.START_TAG, nameSpace, Keys.RSS_PODCAST_COVER_URL)
        val link = XmlHelper.readText(parser)
        parser.require(XmlPullParser.END_TAG, nameSpace, Keys.RSS_PODCAST_COVER_URL)
        return link
    }


    /* PODCAST: read remoteImageFileLocation - itunes tag variant */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPodcastCoverItunes(parser: XmlPullParser, nameSpace: String?): String {
        var link = String()
        parser.require(XmlPullParser.START_TAG, nameSpace, Keys.RSS_PODCAST_COVER_ITUNES)
        val tag = parser.name
        if (tag == Keys.RSS_PODCAST_COVER_ITUNES) {
            link = parser.getAttributeValue(null, Keys.RSS_PODCAST_COVER_ITUNES_URL)
            parser.nextTag()
        }
        parser.require(XmlPullParser.END_TAG, nameSpace, Keys.RSS_PODCAST_COVER_ITUNES)
        return link
    }


    /* EPISODE: read audio link */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readEpisodeAudioLink(parser: XmlPullParser, nameSpace: String?): String {
        var link = String()
        parser.require(XmlPullParser.START_TAG, nameSpace, Keys.RSS_EPISODE_AUDIO_LINK)
        val tag = parser.name
        val type = parser.getAttributeValue(null, Keys.RSS_EPISODE_AUDIO_LINK_TYPE)
        if (tag == Keys.RSS_EPISODE_AUDIO_LINK) {
            if (type.contains("audio")) {
                link = parser.getAttributeValue(null, Keys.RSS_EPISODE_AUDIO_LINK_URL)
                parser.nextTag()
            }
        }
        parser.require(XmlPullParser.END_TAG, nameSpace, Keys.RSS_EPISODE_AUDIO_LINK)
        return link
    }

}