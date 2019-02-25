package br.com.suamusica.rxmediaplayer.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.offline.StreamKey;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylist;
import com.google.android.exoplayer2.util.MimeTypes;

public final class CustomHlsMasterPlaylist extends HlsPlaylist {

  /** Represents an empty master playlist, from which no attributes can be inherited. */
  public static final CustomHlsMasterPlaylist EMPTY =
      new CustomHlsMasterPlaylist(
          /* baseUri= */ "",
          /* tags= */ Collections.<String>emptyList(),
          /* variants= */ Collections.<CustomHlsMasterPlaylist.HlsUrl>emptyList(),
          /* audios= */ Collections.<CustomHlsMasterPlaylist.HlsUrl>emptyList(),
          /* subtitles= */ Collections.<CustomHlsMasterPlaylist.HlsUrl>emptyList(),
          /* muxedAudioFormat= */ null,
          /* muxedCaptionFormats= */ Collections.<Format>emptyList(),
          /* hasIndependentSegments= */ false,
          /* variableDefinitions= */ Collections.<String, String>emptyMap());

  public static final int GROUP_INDEX_VARIANT = 0;
  public static final int GROUP_INDEX_AUDIO = 1;
  public static final int GROUP_INDEX_SUBTITLE = 2;

  /**
   * Represents a url in an HLS master playlist.
   */
  public static final class HlsUrl {

    /**
     * The http url from which the media playlist can be obtained.
     */
    public final String url;
    /**
     * Format information associated with the HLS url.
     */
    public final Format format;

    /**
     * Creates an HLS url from a given http url.
     *
     * @param url The url.
     * @return An HLS url.
     */
    public static CustomHlsMasterPlaylist.HlsUrl createMediaPlaylistHlsUrl(String url) {
      Format format =
          Format.createContainerFormat(
              "0",
              /* label= */ null,
              MimeTypes.APPLICATION_M3U8,
              /* sampleMimeType= */ null,
              /* codecs= */ null,
              /* bitrate= */ Format.NO_VALUE,
              /* selectionFlags= */ 0,
              /* language= */ null);
      return new CustomHlsMasterPlaylist.HlsUrl(url, format);
    }

    /**
     * @param url See {@link #url}.
     * @param format See {@link #format}.
     */
    public HlsUrl(String url, Format format) {
      this.url = url;
      this.format = format;
    }

  }

  /**
   * The list of variants declared by the playlist.
   */
  public final List<HlsUrl> variants;
  /**
   * The list of demuxed audios declared by the playlist.
   */
  public final List<CustomHlsMasterPlaylist.HlsUrl> audios;
  /**
   * The list of subtitles declared by the playlist.
   */
  public final List<CustomHlsMasterPlaylist.HlsUrl> subtitles;

  /**
   * The format of the audio muxed in the variants. May be null if the playlist does not declare any
   * muxed audio.
   */
  public final Format muxedAudioFormat;
  /**
   * The format of the closed captions declared by the playlist. May be empty if the playlist
   * explicitly declares no captions are available, or null if the playlist does not declare any
   * captions information.
   */
  public final List<Format> muxedCaptionFormats;
  /** Contains variable definitions, as defined by the #EXT-X-DEFINE tag. */
  public final Map<String, String> variableDefinitions;

  /**
   * @param baseUri See {@link #baseUri}.
   * @param tags See {@link #tags}.
   * @param variants See {@link #variants}.
   * @param audios See {@link #audios}.
   * @param subtitles See {@link #subtitles}.
   * @param muxedAudioFormat See {@link #muxedAudioFormat}.
   * @param muxedCaptionFormats See {@link #muxedCaptionFormats}.
   * @param hasIndependentSegments See {@link #hasIndependentSegments}.
   * @param variableDefinitions See {@link #variableDefinitions}.
   */
  public CustomHlsMasterPlaylist(
      String baseUri,
      List<String> tags,
      List<CustomHlsMasterPlaylist.HlsUrl> variants,
      List<CustomHlsMasterPlaylist.HlsUrl> audios,
      List<CustomHlsMasterPlaylist.HlsUrl> subtitles,
      Format muxedAudioFormat,
      List<Format> muxedCaptionFormats,
      boolean hasIndependentSegments,
      Map<String, String> variableDefinitions) {
    super(baseUri, tags, hasIndependentSegments);
    this.variants = Collections.unmodifiableList(variants);
    this.audios = Collections.unmodifiableList(audios);
    this.subtitles = Collections.unmodifiableList(subtitles);
    this.muxedAudioFormat = muxedAudioFormat;
    this.muxedCaptionFormats = muxedCaptionFormats != null
        ? Collections.unmodifiableList(muxedCaptionFormats) : null;
    this.variableDefinitions = Collections.unmodifiableMap(variableDefinitions);
  }

  @Override
  public CustomHlsMasterPlaylist copy(List<StreamKey> streamKeys) {
    return new CustomHlsMasterPlaylist(
        baseUri,
        tags,
        copyRenditionsList(variants, GROUP_INDEX_VARIANT, streamKeys),
        copyRenditionsList(audios, GROUP_INDEX_AUDIO, streamKeys),
        copyRenditionsList(subtitles, GROUP_INDEX_SUBTITLE, streamKeys),
        muxedAudioFormat,
        muxedCaptionFormats,
        hasIndependentSegments,
        variableDefinitions);
  }

  /**
   * Creates a playlist with a single variant.
   *
   * @param variantUrl The url of the single variant.
   * @return A master playlist with a single variant for the provided url.
   */
  public static CustomHlsMasterPlaylist createSingleVariantMasterPlaylist(String variantUrl) {
    List<CustomHlsMasterPlaylist.HlsUrl> variant = Collections.singletonList(CustomHlsMasterPlaylist.HlsUrl.createMediaPlaylistHlsUrl(variantUrl));
    List<CustomHlsMasterPlaylist.HlsUrl> emptyList = Collections.emptyList();
    return new CustomHlsMasterPlaylist(
        null,
        Collections.<String>emptyList(),
        variant,
        emptyList,
        emptyList,
        /* muxedAudioFormat= */ null,
        /* muxedCaptionFormats= */ null,
        /* hasIndependentSegments= */ false,
        /* variableDefinitions= */ Collections.<String, String>emptyMap());
  }

  private static List<CustomHlsMasterPlaylist.HlsUrl> copyRenditionsList(
      List<CustomHlsMasterPlaylist.HlsUrl> renditions, int groupIndex, List<StreamKey> streamKeys) {
    List<CustomHlsMasterPlaylist.HlsUrl> copiedRenditions = new ArrayList<>(streamKeys.size());
    for (int i = 0; i < renditions.size(); i++) {
      CustomHlsMasterPlaylist.HlsUrl rendition = renditions.get(i);
      for (int j = 0; j < streamKeys.size(); j++) {
        StreamKey streamKey = streamKeys.get(j);
        if (streamKey.groupIndex == groupIndex && streamKey.trackIndex == i) {
          copiedRenditions.add(rendition);
          break;
        }
      }
    }
    return copiedRenditions;
  }

}