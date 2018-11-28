package br.com.suamusica.rxmediaplayer.utils

import android.net.Uri
import android.util.Base64
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.ParserException
import com.google.android.exoplayer2.drm.DrmInitData
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.google.android.exoplayer2.source.hls.playlist.HlsMasterPlaylist
import com.google.android.exoplayer2.source.hls.playlist.HlsMediaPlaylist
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylist
import com.google.android.exoplayer2.upstream.ParsingLoadable
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.Queue
import java.util.regex.Pattern

class CustomHlsPlaylistParser: ParsingLoadable.Parser<HlsPlaylist> {

  @Throws(IOException::class)
  override fun parse(uri: Uri, inputStream: InputStream): HlsPlaylist {
    val reader = BufferedReader(InputStreamReader(inputStream))
    val extraLines = ArrayDeque<String>()
    var line:String?
    try
    {
      if (!checkPlaylistHeader(reader))
      {
        throw UnrecognizedInputFormatException("Input does not start with the #EXTM3U header.",
            uri)
      }
      line = reader.readLine()
      while (line != null)
      {
        line = line.trim { it <= ' ' }
        if (line.isEmpty())
        {
          // Do nothing.
        }
        else if (line.startsWith(TAG_STREAM_INF))
        {
          extraLines.add(line)
          return parseMasterPlaylist(CustomHlsPlaylistParser.LineIterator(extraLines, reader), uri.toString())
        }
        else if (line.startsWith(TAG_TARGET_DURATION)
            || line.startsWith(TAG_MEDIA_SEQUENCE)
            || line.startsWith(TAG_MEDIA_DURATION)
            || line.startsWith(TAG_KEY)
            || line.startsWith(TAG_BYTERANGE)
            || line == TAG_DISCONTINUITY
            || line == TAG_DISCONTINUITY_SEQUENCE
            || line == TAG_ENDLIST)
        {
          extraLines.add(line)
          return parseMediaPlaylist(CustomHlsPlaylistParser.LineIterator(extraLines, reader), uri.toString())
        }
        else
        {
          extraLines.add(line)
        }
        line = reader.readLine()
      }
    }

    finally
    {
      Util.closeQuietly(reader)
    }
    throw ParserException("Failed to parse the playlist, could not identify any tags.")
  }

  private class LineIterator(private val extraLines: Queue<String>, private val reader: BufferedReader) {

    private var next:String? = null

    @Throws(IOException::class)
    operator fun hasNext():Boolean {
      if (next != null)
      {
        return true
      }
      if (!extraLines.isEmpty())
      {
        next = extraLines.poll()
        return true
      }
      next = reader.readLine()
      while (next != null)
      {
        next = next?.trim { it <= ' ' }
        if (next?.isEmpty() == false)
        {
          return true
        }
        next = reader.readLine()
      }
      return false
    }

    @Throws(IOException::class)
    operator fun next():String? {
      var result:String? = null
      if (hasNext())
      {
        result = next
        next = null
      }
      return result
    }

  }

  companion object {

    private const val PLAYLIST_HEADER = "#EXTM3U"

    private const val TAG_PREFIX = "#EXT"

    private const val TAG_VERSION = "#EXT-X-VERSION"
    private const val TAG_PLAYLIST_TYPE = "#EXT-X-PLAYLIST-TYPE"
    private const val TAG_STREAM_INF = "#EXT-X-STREAM-INF"
    private const val TAG_MEDIA = "#EXT-X-MEDIA"
    private const val TAG_TARGET_DURATION = "#EXT-X-TARGETDURATION"
    private const val TAG_DISCONTINUITY = "#EXT-X-DISCONTINUITY"
    private const val TAG_DISCONTINUITY_SEQUENCE = "#EXT-X-DISCONTINUITY-SEQUENCE"
    private const val TAG_PROGRAM_DATE_TIME = "#EXT-X-PROGRAM-DATE-TIME"
    private const val TAG_INIT_SEGMENT = "#EXT-X-MAP"
    private const val TAG_INDEPENDENT_SEGMENTS = "#EXT-X-INDEPENDENT-SEGMENTS"
    private const val TAG_MEDIA_DURATION = "#EXTINF"
    private const val TAG_MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE"
    private const val TAG_START = "#EXT-X-START"
    private const val TAG_ENDLIST = "#EXT-X-ENDLIST"
    private const val TAG_KEY = "#EXT-X-KEY"
    private const val TAG_BYTERANGE = "#EXT-X-BYTERANGE"
    private const val TAG_GAP = "#EXT-X-GAP"

    private const val TYPE_AUDIO = "AUDIO"
    private const val TYPE_VIDEO = "VIDEO"
    private const val TYPE_SUBTITLES = "SUBTITLES"
    private const val TYPE_CLOSED_CAPTIONS = "CLOSED-CAPTIONS"

    private const val METHOD_NONE = "NONE"
    private const val METHOD_AES_128 = "AES-128"
    private const val METHOD_SAMPLE_AES = "SAMPLE-AES"
    // Replaced by METHOD_SAMPLE_AES_CTR. Keep for backward compatibility.
    private const val METHOD_SAMPLE_AES_CENC = "SAMPLE-AES-CENC"
    private const val METHOD_SAMPLE_AES_CTR = "SAMPLE-AES-CTR"
    private const val KEYFORMAT_IDENTITY = "identity"
    private const val KEYFORMAT_WIDEVINE_PSSH_BINARY = "urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"
    private const val KEYFORMAT_WIDEVINE_PSSH_JSON = "com.widevine"

    private const val BOOLEAN_TRUE = "YES"
    private const val BOOLEAN_FALSE = "NO"

    private const val ATTR_CLOSED_CAPTIONS_NONE = "CLOSED-CAPTIONS=NONE"

    private val REGEX_AVERAGE_BANDWIDTH = Pattern.compile("AVERAGE-BANDWIDTH=(\\d+)\\b")
    private val REGEX_AUDIO = Pattern.compile("AUDIO=\"(.+?)\"")
    private val REGEX_BANDWIDTH = Pattern.compile("[^-]BANDWIDTH=(\\d+)\\b")
    private val REGEX_CODECS = Pattern.compile("CODECS=\"(.+?)\"")
    private val REGEX_RESOLUTION = Pattern.compile("RESOLUTION=(\\d+x\\d+)")
    private val REGEX_FRAME_RATE = Pattern.compile("FRAME-RATE=([\\d\\.]+)\\b")
    private val REGEX_TARGET_DURATION = Pattern.compile("$TAG_TARGET_DURATION:(\\d+)\\b")
    private val REGEX_VERSION = Pattern.compile("$TAG_VERSION:(\\d+)\\b")
    private val REGEX_PLAYLIST_TYPE = Pattern.compile("$TAG_PLAYLIST_TYPE:(.+)\\b")
    private val REGEX_MEDIA_SEQUENCE = Pattern.compile("$TAG_MEDIA_SEQUENCE:(\\d+)\\b")
    private val REGEX_MEDIA_DURATION = Pattern.compile("$TAG_MEDIA_DURATION:([\\d\\.]+)\\b")
    private val REGEX_TIME_OFFSET = Pattern.compile("TIME-OFFSET=(-?[\\d\\.]+)\\b")
    private val REGEX_BYTERANGE = Pattern.compile("$TAG_BYTERANGE:(\\d+(?:@\\d+)?)\\b")
    private val REGEX_ATTR_BYTERANGE = Pattern.compile("BYTERANGE=\"(\\d+(?:@\\d+)?)\\b\"")
    private val REGEX_METHOD = Pattern.compile(
        "METHOD=("
            + METHOD_NONE
            + "|"
            + METHOD_AES_128
            + "|"
            + METHOD_SAMPLE_AES
            + "|"
            + METHOD_SAMPLE_AES_CENC
            + "|"
            + METHOD_SAMPLE_AES_CTR
            + ")"
            + "\\s*(,|$)")
    private val REGEX_KEYFORMAT = Pattern.compile("KEYFORMAT=\"(.+?)\"")
    private val REGEX_URI = Pattern.compile("URI=\"(.+?)\"")
    private val REGEX_IV = Pattern.compile("IV=([^,.*]+)")
    private val REGEX_TYPE = Pattern.compile(("TYPE=(" + TYPE_AUDIO + "|" + TYPE_VIDEO
        + "|" + TYPE_SUBTITLES + "|" + TYPE_CLOSED_CAPTIONS + ")"))
    private val REGEX_LANGUAGE = Pattern.compile("LANGUAGE=\"(.+?)\"")
    private val REGEX_NAME = Pattern.compile("NAME=\"(.+?)\"")
    private val REGEX_GROUP_ID = Pattern.compile("GROUP-ID=\"(.+?)\"")
    private val REGEX_INSTREAM_ID = Pattern.compile("INSTREAM-ID=\"((?:CC|SERVICE)\\d+)\"")
    private val REGEX_AUTOSELECT = compileBooleanAttrPattern("AUTOSELECT")
    private val REGEX_DEFAULT = compileBooleanAttrPattern("DEFAULT")
    private val REGEX_FORCED = compileBooleanAttrPattern("FORCED")

    @Throws(IOException::class)
    private fun checkPlaylistHeader(reader: BufferedReader):Boolean {
      var last = reader.read()
      if (last == 0xEF)
      {
        if (reader.read() != 0xBB || reader.read() != 0xBF)
        {
          return false
        }
        // The playlist contains a Byte Order Mark, which gets discarded.
        last = reader.read()
      }
      last = skipIgnorableWhitespace(reader, true, last)
      val playlistHeaderLength = PLAYLIST_HEADER.length
      for (i in 0 until playlistHeaderLength)
      {
        if (last != PLAYLIST_HEADER[i].toInt())
        {
          return false
        }
        last = reader.read()
      }
      last = skipIgnorableWhitespace(reader, false, last)
      return Util.isLinebreak(last)
    }

    @Throws(IOException::class)
    private fun skipIgnorableWhitespace(reader: BufferedReader, skipLinebreaks:Boolean, c:Int):Int {
      var codePoint = c
      while (codePoint != -1 && Character.isWhitespace(codePoint) && (skipLinebreaks || !Util.isLinebreak(codePoint)))
      {
        codePoint = reader.read()
      }
      return codePoint
    }

    @Throws(IOException::class)
    private fun parseMasterPlaylist(iterator:CustomHlsPlaylistParser.LineIterator, baseUri:String): HlsMasterPlaylist {
      val variantUrls = HashSet<String>()
      val audioGroupIdToCodecs = HashMap<String, String>()
      val variants = ArrayList<HlsMasterPlaylist.HlsUrl>()
      val audios = ArrayList<HlsMasterPlaylist.HlsUrl>()
      val subtitles = ArrayList<HlsMasterPlaylist.HlsUrl>()
      val mediaTags = ArrayList<String>()
      val tags = ArrayList<String>()
      var muxedAudioFormat: Format? = null
      var muxedCaptionFormats:MutableList<Format>? = null
      var noClosedCaptions = false

      var line : String?
      while (iterator.hasNext())
      {
        line = iterator.next()

        if (line?.startsWith(TAG_PREFIX) == true)
        {
          // We expose all tags through the playlist.
          tags.add(line)
        }

        if (line?.startsWith(TAG_MEDIA) == true)
        {
          // Media tags are parsed at the end to include codec information from #EXT-X-STREAM-INF
          // tags.
          mediaTags.add(line)
        }
        else if (line?.startsWith(TAG_STREAM_INF) == true)
        {
          noClosedCaptions = noClosedCaptions or line.contains(ATTR_CLOSED_CAPTIONS_NONE)
          var bitrate = parseIntAttr(line, REGEX_BANDWIDTH)
          val averageBandwidthString = parseOptionalStringAttr(line, REGEX_AVERAGE_BANDWIDTH)
          if (averageBandwidthString != null)
          {
            // If available, the average bandwidth attribute is used as the variant's bitrate.
            bitrate = Integer.parseInt(averageBandwidthString)
          }
          val codecs = parseOptionalStringAttr(line, REGEX_CODECS)
          val resolutionString = parseOptionalStringAttr(line, REGEX_RESOLUTION)
          var width:Int
          var height:Int
          if (resolutionString != null)
          {
            val widthAndHeight = resolutionString.split(("x").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            width = Integer.parseInt(widthAndHeight[0])
            height = Integer.parseInt(widthAndHeight[1])
            if (width <= 0 || height <= 0)
            {
              // Resolution string is invalid.
              width = Format.NO_VALUE
              height = Format.NO_VALUE
            }
          }
          else
          {
            width = Format.NO_VALUE
            height = Format.NO_VALUE
          }
          var frameRate = Format.NO_VALUE.toFloat()
          val frameRateString = parseOptionalStringAttr(line, REGEX_FRAME_RATE)
          if (frameRateString != null)
          {
            frameRate = java.lang.Float.parseFloat(frameRateString)
          }
          val audioGroupId = parseOptionalStringAttr(line, REGEX_AUDIO)
          if (audioGroupId != null && codecs != null)
          {
            audioGroupIdToCodecs[audioGroupId] = Util.getCodecsOfType(codecs, C.TRACK_TYPE_AUDIO)
          }
          line = iterator.next() // #EXT-X-STREAM-INF's URI.
          line?.let {
            if (variantUrls.add(it))
            {
              val format = Format.createVideoContainerFormat(Integer.toString(variants.size),
                  MimeTypes.APPLICATION_M3U8, null, codecs, bitrate, width, height, frameRate, null, 0)
              variants.add(HlsMasterPlaylist.HlsUrl(line, format))
            }
          }
        }
      }

      for (i in mediaTags.indices)
      {
        line = mediaTags[i]
        @C.SelectionFlags val selectionFlags = parseSelectionFlags(line)
        val uri = parseOptionalStringAttr(line, REGEX_URI)
        val id = parseStringAttr(line, REGEX_NAME)
        val language = parseOptionalStringAttr(line, REGEX_LANGUAGE)
        val groupId = parseOptionalStringAttr(line, REGEX_GROUP_ID)
        val format: Format
        when (parseStringAttr(line, REGEX_TYPE)) {
          TYPE_AUDIO -> {
            val codecs = audioGroupIdToCodecs[groupId]
            val sampleMimeType = if (codecs != null) MimeTypes.getMediaMimeType(codecs) else null
            format = Format.createAudioContainerFormat(id, MimeTypes.APPLICATION_M3U8, sampleMimeType,
                codecs, Format.NO_VALUE, Format.NO_VALUE, Format.NO_VALUE, null, selectionFlags,
                language)
            if (uri == null)
            {
              muxedAudioFormat = format
            }
            else
            {
              audios.add(HlsMasterPlaylist.HlsUrl(uri, format))
            }
          }
          TYPE_SUBTITLES -> {
            format = Format.createTextContainerFormat(id, MimeTypes.APPLICATION_M3U8,
                MimeTypes.TEXT_VTT, null, Format.NO_VALUE, selectionFlags, language)
            subtitles.add(HlsMasterPlaylist.HlsUrl(uri, format))
          }
          TYPE_CLOSED_CAPTIONS -> {
            val instreamId = parseStringAttr(line, REGEX_INSTREAM_ID)
            val mimeType:String
            val accessibilityChannel:Int
            if (instreamId.startsWith("CC"))
            {
              mimeType = MimeTypes.APPLICATION_CEA608
              accessibilityChannel = Integer.parseInt(instreamId.substring(2))
            }
            else
            /* starts with SERVICE */ {
              mimeType = MimeTypes.APPLICATION_CEA708
              accessibilityChannel = Integer.parseInt(instreamId.substring(7))
            }
            if (muxedCaptionFormats == null)
            {
              muxedCaptionFormats = ArrayList()
            }
            muxedCaptionFormats.add(Format.createTextContainerFormat(id, null, mimeType, null,
                Format.NO_VALUE, selectionFlags, language, accessibilityChannel))
          }
          else -> {}
        }// Do nothing.
      }

      if (noClosedCaptions)
      {
        muxedCaptionFormats = mutableListOf()
      }
      return HlsMasterPlaylist(baseUri, tags, variants, audios, subtitles, muxedAudioFormat,
          muxedCaptionFormats)
    }

    @C.SelectionFlags
    private fun parseSelectionFlags(line:String?):Int {
      return ((if (parseBooleanAttribute(line, REGEX_DEFAULT, false)) C.SELECTION_FLAG_DEFAULT else 0)
          or (if (parseBooleanAttribute(line, REGEX_FORCED, false)) C.SELECTION_FLAG_FORCED else 0)
          or (if (parseBooleanAttribute(line, REGEX_AUTOSELECT, false)) C.SELECTION_FLAG_AUTOSELECT else 0))
    }

    @Throws(IOException::class)
    private fun parseMediaPlaylist(iterator:CustomHlsPlaylistParser.LineIterator, baseUri:String): HlsMediaPlaylist {
      @HlsMediaPlaylist.PlaylistType var playlistType = HlsMediaPlaylist.PLAYLIST_TYPE_UNKNOWN
      var startOffsetUs = C.TIME_UNSET
      var mediaSequence:Long = 0
      var version = 1 // Default version == 1.
      var targetDurationUs = C.TIME_UNSET
      var hasIndependentSegmentsTag = false
      var hasEndTag = false
      var initializationSegment: HlsMediaPlaylist.Segment? = null
      val segments = ArrayList<HlsMediaPlaylist.Segment>()
      val tags = ArrayList<String>()

      var segmentDurationUs:Long = 0
      var hasDiscontinuitySequence = false
      var playlistDiscontinuitySequence = 0
      var relativeDiscontinuitySequence = 0
      var playlistStartTimeUs:Long = 0
      var segmentStartTimeUs:Long = 0
      var segmentByteRangeOffset:Long = 0
      var segmentByteRangeLength = C.LENGTH_UNSET.toLong()
      var segmentMediaSequence:Long = 0
      var hasGapTag = false

      var encryptionKeyUri:String? = null
      var encryptionIV:String? = null
      var drmInitData: DrmInitData? = null

      var line:String?
      while (iterator.hasNext())
      {
        line = iterator.next()

        if (line?.startsWith(TAG_PREFIX) == true)
        {
          // We expose all tags through the playlist.
          tags.add(line)
        }

        if (line?.startsWith(TAG_PLAYLIST_TYPE) == true)
        {
          val playlistTypeString = parseStringAttr(line, REGEX_PLAYLIST_TYPE)
          if ("VOD" == playlistTypeString)
          {
            playlistType = HlsMediaPlaylist.PLAYLIST_TYPE_VOD
          }
          else if ("EVENT" == playlistTypeString)
          {
            playlistType = HlsMediaPlaylist.PLAYLIST_TYPE_EVENT
          }
        }
        else if (line?.startsWith(TAG_START) == true)
        {
          startOffsetUs = (parseDoubleAttr(line, REGEX_TIME_OFFSET) * C.MICROS_PER_SECOND).toLong()
        }
        else if (line?.startsWith(TAG_INIT_SEGMENT) == true)
        {
          val uri = parseStringAttr(line, REGEX_URI)
          val byteRange = parseOptionalStringAttr(line, REGEX_ATTR_BYTERANGE)
          if (byteRange != null)
          {
            val splitByteRange = byteRange.split(("@").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            segmentByteRangeLength = java.lang.Long.parseLong(splitByteRange[0])
            if (splitByteRange.size > 1)
            {
              segmentByteRangeOffset = java.lang.Long.parseLong(splitByteRange[1])
            }
          }
          initializationSegment = HlsMediaPlaylist.Segment(uri, segmentByteRangeOffset, segmentByteRangeLength)
          segmentByteRangeOffset = 0
          segmentByteRangeLength = C.LENGTH_UNSET.toLong()
        }
        else if (line?.startsWith(TAG_TARGET_DURATION) == true)
        {
          targetDurationUs = parseIntAttr(line, REGEX_TARGET_DURATION) * C.MICROS_PER_SECOND
        }
        else if (line?.startsWith(TAG_MEDIA_SEQUENCE) == true)
        {
          mediaSequence = parseLongAttr(line, REGEX_MEDIA_SEQUENCE)
          segmentMediaSequence = mediaSequence
        }
        else if (line?.startsWith(TAG_VERSION) == true)
        {
          version = parseIntAttr(line, REGEX_VERSION)
        }
        else if (line?.startsWith(TAG_MEDIA_DURATION) == true)
        {
          segmentDurationUs = (parseDoubleAttr(line, REGEX_MEDIA_DURATION) * C.MICROS_PER_SECOND).toLong()
        }
        else if (line?.startsWith(TAG_KEY) == true)
        {
          val method = parseOptionalStringAttr(line, REGEX_METHOD)
          val keyFormat = parseOptionalStringAttr(line, REGEX_KEYFORMAT)
          encryptionKeyUri = null
          encryptionIV = null
          if (METHOD_NONE != method)
          {
            encryptionIV = parseOptionalStringAttr(line, REGEX_IV)
            if (KEYFORMAT_IDENTITY == keyFormat || keyFormat == null)
            {
              if (METHOD_AES_128 == method)
              {
                // The segment is fully encrypted using an identity key.
                encryptionKeyUri = parseStringAttr(line, REGEX_URI)
              }
              else
              {
                // Do nothing. Samples are encrypted using an identity key, but this is not supported.
                // Hopefully, a traditional DRM alternative is also provided.
              }
            }
            else if (method != null)
            {
              val schemeData = parseWidevineSchemeData(line, keyFormat)
              if (schemeData != null)
              {
                drmInitData = DrmInitData(
                    if (((METHOD_SAMPLE_AES_CENC == method || METHOD_SAMPLE_AES_CTR == method)))
                      C.CENC_TYPE_cenc
                    else
                      C.CENC_TYPE_cbcs,
                    schemeData)
              }
            }
          }
        }
        else if (line?.startsWith(TAG_BYTERANGE) == true)
        {
          val byteRange = parseStringAttr(line, REGEX_BYTERANGE)
          val splitByteRange = byteRange.split(("@").toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
          segmentByteRangeLength = java.lang.Long.parseLong(splitByteRange[0])
          if (splitByteRange.size > 1)
          {
            segmentByteRangeOffset = java.lang.Long.parseLong(splitByteRange[1])
          }
        }
        else if (line?.startsWith(TAG_DISCONTINUITY_SEQUENCE) == true)
        {
          hasDiscontinuitySequence = true
          playlistDiscontinuitySequence = Integer.parseInt(line.substring(line.indexOf(':') + 1))
        }
        else if (line == TAG_DISCONTINUITY)
        {
          relativeDiscontinuitySequence++
        }
        else if (line?.startsWith(TAG_PROGRAM_DATE_TIME) == true)
        {
          if (playlistStartTimeUs == 0L)
          {
            val programDatetimeUs = C.msToUs(Util.parseXsDateTime(line.substring(line.indexOf(':') + 1)))
            playlistStartTimeUs = programDatetimeUs - segmentStartTimeUs
          }
        }
        else if (line == TAG_GAP)
        {
          hasGapTag = true
        }
        else if (line == TAG_INDEPENDENT_SEGMENTS)
        {
          hasIndependentSegmentsTag = true
        }
        else if (line == TAG_ENDLIST)
        {
          hasEndTag = true
        }
        else if (line != null && !line.startsWith("#"))
        {
          val segmentEncryptionIV:String? = when {
            encryptionKeyUri == null -> null
            encryptionIV != null -> encryptionIV
            else -> java.lang.Long.toHexString(segmentMediaSequence)
          }
          segmentMediaSequence++
          if (segmentByteRangeLength == C.LENGTH_UNSET.toLong())
          {
            segmentByteRangeOffset = 0
          }
          segments.add(
              HlsMediaPlaylist.Segment(
                  URLEncoder.encode(line, "UTF-8"),
                  initializationSegment,
                  segmentDurationUs,
                  relativeDiscontinuitySequence,
                  segmentStartTimeUs,
                  encryptionKeyUri,
                  segmentEncryptionIV,
                  segmentByteRangeOffset,
                  segmentByteRangeLength,
                  hasGapTag))
          segmentStartTimeUs += segmentDurationUs
          segmentDurationUs = 0
          if (segmentByteRangeLength != C.LENGTH_UNSET.toLong())
          {
            segmentByteRangeOffset += segmentByteRangeLength
          }
          segmentByteRangeLength = C.LENGTH_UNSET.toLong()
          hasGapTag = false
        }
      }
      return HlsMediaPlaylist(
          playlistType,
          baseUri,
          tags,
          startOffsetUs,
          playlistStartTimeUs,
          hasDiscontinuitySequence,
          playlistDiscontinuitySequence,
          mediaSequence,
          version,
          targetDurationUs,
          hasIndependentSegmentsTag,
          hasEndTag,
          /* hasProgramDateTime= */ playlistStartTimeUs != 0L,
          drmInitData,
          segments)
    }

    @Throws(ParserException::class)
    private fun parseWidevineSchemeData(line:String?, keyFormat:String): DrmInitData.SchemeData? {
      if (KEYFORMAT_WIDEVINE_PSSH_BINARY == keyFormat)
      {
        val uriString = parseStringAttr(line, REGEX_URI)
        return DrmInitData.SchemeData(C.WIDEVINE_UUID, MimeTypes.VIDEO_MP4,
            Base64.decode(uriString.substring(uriString.indexOf(',')), Base64.DEFAULT))
      }
      if (KEYFORMAT_WIDEVINE_PSSH_JSON == keyFormat)
      {
        try
        {
          return DrmInitData.SchemeData(C.WIDEVINE_UUID, "hls", line!!.toByteArray(charset(C.UTF8_NAME)))
        }
        catch (e: UnsupportedEncodingException) {
          throw ParserException(e)
        }

      }
      return null
    }

    @Throws(ParserException::class)
    private fun parseIntAttr(line:String?, pattern: Pattern):Int {
      return Integer.parseInt(parseStringAttr(line, pattern))
    }

    @Throws(ParserException::class)
    private fun parseLongAttr(line:String?, pattern: Pattern):Long {
      return java.lang.Long.parseLong(parseStringAttr(line, pattern))
    }

    @Throws(ParserException::class)
    private fun parseDoubleAttr(line:String?, pattern: Pattern):Double {
      return java.lang.Double.parseDouble(parseStringAttr(line, pattern))
    }

    private fun parseOptionalStringAttr(line:String?, pattern: Pattern):String? {
      val matcher = pattern.matcher(line ?: "")
      return if (matcher.find()) matcher.group(1) else null
    }

    @Throws(ParserException::class)
    private fun parseStringAttr(line:String?, pattern: Pattern):String {
      val matcher = pattern.matcher(line ?: "")
      if (matcher.find() && matcher.groupCount() == 1)
      {
        return matcher.group(1)
      }
      throw ParserException("Couldn't match " + pattern.pattern() + " in " + line)
    }

    private fun parseBooleanAttribute(line:String?, pattern: Pattern, defaultValue:Boolean):Boolean {
      val matcher = pattern.matcher(line ?: "")
      return if (matcher.find()) {
        matcher.group(1) == BOOLEAN_TRUE
      } else defaultValue
    }

    private fun compileBooleanAttrPattern(attribute:String): Pattern {
      return Pattern.compile("$attribute=($BOOLEAN_FALSE|$BOOLEAN_TRUE)")
    }
  }

}