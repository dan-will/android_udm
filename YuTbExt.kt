package com.pro.ryder.ex.butu.ExV2.extract

import android.content.Context
import android.os.Build
import android.text.format.Formatter
import com.pro.ryder.ex.butu.AppType.*
import com.pro.ryder.ex.butu.Json.*
import com.pro.ryder.ex.butu.R
import com.pro.ryder.ex.butu.TestUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.format
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.nio.charset.Charset
import java.util.regex.Pattern

object YuTbExt : CommonMethods {

    var mNote = "This apps does not support Youtube under the rules."
    override fun onFileExt(mPara: JSONObject, context0: Context?): JSONObject {
        mPara.put(FI_ERROR_MSG, mNote)
        try {
            println("Youtube-Ext")
            val mClient = OkHttpClient()
            val pageLink = mPara.getString(FI_PAGE_URL)

            // Check Id Match
            val videoId = onExtractId(pageLink) ?: return mPara

            val request01 = Request.Builder().url("https://www.youtube.com/watch?v=$videoId&gl=US&hl=en&has_verified=1&bpctr=9999999999")
                .addHeader("User-Agent",TestUrl.__UserAgent[1])
                .build()

            // #Disable - Off line data test
            val response01 = mClient.newCall(request01).execute()
            if (!response01.isSuccessful) return mPara

            val pageData = response01.body?.string() ?: return mPara

            // #Test
            //val pageData = context0?.assets?.open("youtube_page_source")?.bufferedReader(Charset.defaultCharset()).use { it?.readText() } ?: return mPara

            // #TEST
            //ut0WriteData(data = pageData, mFilename = "youtube_page_source", mContext = context0!!)

            // #FUNCTION
            fun extractPlayerResponse(playerResponse: String?): JSONObject {
                playerResponse ?: return JSONObject()
                if (!playerResponse.trim().startsWith("{") && !playerResponse.trim().endsWith("}"))
                    return JSONObject()
                return JSONObject(playerResponse)
            }
            // Get video Info
            var videoInfo0 = JSONObject()
            var playerResponse0 = JSONObject()

            // check Some
            if ("""[\\"]status[\\"]\s*:\s*[\\"]LOGIN_REQUIRED""".toRegex().find(pageData) != null) {
                return mPara
            }

            if (videoInfo0.length() == 0 && playerResponse0.length() == 0) {
                playerResponse0 = extractPlayerResponse(ut0SearchRegex(
                    "$mYT0INITIAL0DATA0RE\\s*(?:var\\s+meta|</script|\\n)", pageData
                ))
            }

            if (videoInfo0.f0IsEmpty() && playerResponse0.f0IsEmpty()) {
                return mPara
            }

            val mTitle = playerResponse0.f0String("title") ?: playerResponse0.f0Object("videoDetails")?.f0String("title") ?: "Youtube ;-} $videoId"
            mPara.put(FI_TITLE, mTitle)
            // smuggled_data {...}

            val streamingFormats = playerResponse0.f0Object("streamingData")?.f0Array("formats") ?: JSONArray()
            streamingFormats.f0AddObject(
                playerResponse0.f0Object("streamingData")?.f0Array("adaptiveFormats") ?: JSONArray()
            )
            // conn {...}

            if (videoInfo0.has("conn")) {
                return mPara
            } else if (streamingFormats.length() > 0 ||
                videoInfo0.optString("url_encoded_fmt_stream_map", "").length >= 3
                || videoInfo0.optString("adaptive_fmts", "").length >= 3) {

                if (streamingFormats.f0IsEmpty()) return mPara

                val formats0 = JSONArray()
                val formatsSpace = JSONObject()
                for (nm0 in 0 until streamingFormats.length()) {
                    val fmt0 = streamingFormats.f0Object(nm0) ?: continue

                    val itag0 = ut0StrOrNull(fmt0.f0Get("itag")) ?: continue
                    formatsSpace.also { jb0 ->
                        jb0.put(itag0, JSONObject().apply {
                            ut0IntOrNull(fmt0.f0Get("audioSampleRate"))?.let { put("asr", it) }
                            ut0IntOrNull(fmt0.f0Get("contantLength"))?.let { put("filesize", it) }
                            ut0IntOrNull(fmt0.f0Get("fps"))?.let { put("fps", it) }
                            ut0IntOrNull(fmt0.f0Get("height"))?.let { put("height", it) }
                            if (itag0 != "43") ut0FloatOrNull(fmt0.f0Get("averageBitrate") ?: fmt0.f0Get("bitrate"), 1000)?.let { put("tbr", it) }
                            ut0IntOrNull(fmt0.f0Get("width"))?.let { put("width", it) }
                        })
                    }
                }

                // #TEST
                //ut0WriteData(data = streamingFormats.toString(), mFilename = "youtube_streaming_formats", mContext = context0!!)

                for (nm0 in 0 until streamingFormats.length()) {
                    val fmt0 = streamingFormats.f0Object(nm0) ?: continue
                    if (fmt0.has("drmFamilies") || fmt0.has("drm_families"))
                        continue

                    var url0 = ut0UrlOrNull(fmt0.f0Get("url"))
                    var cipher0: String? = null
                    var urlData = JSONObject()
                    if (url0 == null) continue
                    else {
                        cipher0 = null
                        urlData = ut0CompatParseQs(URL(url0).query)
                    }

                    val formatId = ut0StrOrNull(fmt0.f0Get("itag")) ?:
                    ut0StrOrNull(urlData.f0Get("itag")) ?: continue

                    if (!url0.contains("ratebypass")) url0 += "&ratebypass=yes"
                    val dct0 = JSONObject().apply {
                        put("format_id", formatId)
                        put("url", url0)
                    }


                    if (mFormats.has(formatId)) {
                        val know0 = mFormats.get(formatId)
                        if (know0 is Map<*, *>) dct0.toMapToObject(know0)
                    }
                    if (formatsSpace.has(formatId)) {
                        dct0.f0AppendEntry(formatsSpace.getJSONObject(formatId))
                    }

                    val fileSize0 = ut0StrOrNull(urlData.f0Get("size"))
                    val videoRes0 = if (fileSize0 != null) {
                        val matchObj02 = ut0MatchIdWithGroup("(?<width>\\d+)[xX](?<height>\\d+)\$", fileSize0, mapOf(1 to "width", 2 to "height"))
                        if (matchObj02.length() == 2) "${matchObj02.f0String("width")}x${matchObj02.f0String("height")}" else null
                    } else {
                        if (fmt0.f0Has(listOf("width", "height"))) "${fmt0.f0Get("width")}x${fmt0.f0Get("height")}" else null
                    }
                    fun extractFilesize(urlFileSize: String): Any? {
                        val match0 = Pattern.compile("\\bclen[=/](\\d+)").matcher(urlFileSize)
                        return ut0IntOrNull(if (match0.find()) match0.group(1) as Any else null)
                    }
                    val filesize0 = ut0IntOrNull(urlData.f0Get("clen") ?: extractFilesize(url0))
                    val quality0 = urlData.f0Get("quality") ?: fmt0.f0Get("quality")
                    val qualityLabel = urlData.f0Get("quality_label") ?: fmt0.f0Get("qualityLabel")

                    val tbr0 = ut0FloatOrNull(urlData.f0Get("bitrate"), 1000)
                    val fps0 = ut0IntOrNull(urlData.f0Get("fps")) ?: ut0IntOrNull(fmt0.f0Get("fps"))

                    val moreFields = JSONObject().apply {
                        filesize0?.let { put("filesize", it) }
                        tbr0?.let { put("tbr", it) }
                        fps0?.let { put("fps", it) }
                        put("format_note", qualityLabel ?: quality0)
                    }
                    dct0.f0AppendEntry(moreFields)

                    val type0 = ut0StrOrNull(urlData.f0Get("type") ?: fmt0.f0Get("mimeType"))
                    if (type0 != null) {
                        val typeSplit = type0.split(';')
                        val kindExt = if (typeSplit.isEmpty()) type0.split("/") else typeSplit[0].split("/")

                        if (kindExt.size == 2) {
                            val (kind0, _) = kindExt
                            //
                            if (kind0 in listOf("audio", "video")) {
                                val pattern0 = Pattern.compile(
                                    "(?<key>[a-zA-Z_-]+)=(?<quote>[\"']?)(?<val>.+?)(?<quotes>[\"']?)(?:;|$)")
                                    .matcher(type0)
                                if (pattern0.find()) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        if (pattern0.group("key") == "codecs") {
                                            pattern0.group("val")?.let {
                                                dct0.f0AppendEntry(ut0OnParseCodecs(it))
                                            }
                                        }
                                    } else {
                                        if (pattern0.group(1) == "codecs") {
                                            pattern0.group(1)?.let {
                                                dct0.f0AppendEntry(ut0OnParseCodecs(it))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // #TEST
                    //ut0WriteData(data = urlData.toString(), mFilename = "youtube_urlData_0a${nm0}", mContext = context0!!)

                    // #TEST
                    //ut0WriteData(data = fmt0.toString(), mFilename = "youtube_fmt0a__${nm0}", mContext = context0!!)

                    // #TEST
                    //ut0WriteData(data = dct0.toString(), mFilename = "youtube_dct0a_${nm0}.json", mContext = context0!!)
                    val onStrTemplate = fun (str0: String): String {
                        return String.format(context0!!.getString(R.string.media_opt_summary_d),
                        str0)
                    }

                    var summary00 = ""
                    dct0.f0String("ext")?.let { summary00 += "$it " }
                    //dct0.f0String("format_note")?.let { ita -> ut0IntOrNull() }

                    if (!dct0.has("width")) summary00 += onStrTemplate("audio only ")
                    dct0.f0String("format_note")?.let { summary00 += onStrTemplate("$it ") }
                    dct0.f0Get("tbr")?.let { ita -> ut0IntOrNull(ita)?.let { summary00 += onStrTemplate("${it}k ")} }
                    if (dct0.f0Has(listOf("width", "height"))) {
                        dct0.f0String("container")?.let { summary00 += onStrTemplate("$it ")}
                        dct0.f0String("vcodec")?.let { summary00 += onStrTemplate("$it ") }
                        dct0.f0Get("fps")?.let { ita ->
                            ut0IntOrNull(ita)?.let { summary00 += onStrTemplate("${it}fps ") } }
                    }
                    dct0.f0String("acodec")?.let { summary00 += onStrTemplate("$it") } ?: run { summary00 += onStrTemplate("Video Only ")}
                    dct0.f0Get("abr")?.let { ita -> ut0IntOrNull(ita)?.let { summary00 += "@${it}k"} }
                    dct0.f0Get("asr")?.let { ita -> ut0IntOrNull(ita)?.let { summary00 += " (${it}Hz)" } }

                    dct0.f0Get("filesize")?.let { ita ->
                        summary00 += " "
                        ut0IntOrNull(ita)?.let { summary00 +=
                            onStrTemplate(Formatter.formatFileSize(context0, it.toLong())) }}
                    //formats0.also { it.put(dct0) }

                    // File Add On Result Object
                    mPara.getJSONArray(FI_FILE_URLS).apply {
                        put(this.length(), JSONObject().apply {
                            put(FI_URL_SUB, dct0.getString("url"))
                            //put(FI_HEADER_SUB, "")
                            put(FI_FORMAT_CODE_SUB, dct0.getString("format_id"))
                            put(FI_OPTIONS_SUB, JSONObject().apply {
                                put(
                                    FI_HD_TITLE_SUB,
                                    String.format(context0!!.getString(R.string.media_options_c),
                                dct0.getString("format_id"), dct0.getString("ext"),
                                        if (!dct0.has("width")) "Audio Only" else
                                            videoRes0 ?: dct0.f0String("format_note")
                                    ))
                                put(FI_SUMMARY_SUB, summary00)
                            })

                            put(FI_SAVE_FILE_SUB, "${videoId}__${formatId}.${dct0.getString("ext")}")
                        })
                    }
                }
            } else {
                return mPara
            }
        } catch (ex0: Exception) {
            ex0.printStackTrace()
        } finally {

            val jsFiles = mPara.getJSONArray(FI_FILE_URLS)
            if (jsFiles.length() > 0) {

                when (jsFiles.length()) {
                    1 -> mPara.put(FI_URL_TASK, LK_URL_B)
                    else -> mPara.put(FI_URL_TASK, LK_URL_C)
                }
                mPara.put(FI_RESULT, true)

                //ut0WriteData(data = mPara.toString(), mFilename = "youtube_json_c", mContext = context0!!)
                //println("#YU::finally")
            }
        }
        return super.onFileExt(mPara, context0)
    }

    override fun onHost(): Regex = """((www|m)\.)?(youtube(\.googleapis)?.com|youtu\.be)(/)?""".toRegex()

    private fun onExtractId(url: String): String? {
        val matchGrp = Pattern.compile(mValidUrl).matcher(url)
        return if (matchGrp.find()) matchGrp.group(2) else null
    }

    fun JSONObject.toMapToObject(map0: Map<*, *>) {
        for (key0 in map0.keys) {
            ut0StrOrNull(key0 )?.let { put(it, map0[key0]) }
        }
    }

    val mYT0INITIAL0DATA0RE = "ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\s*;"

    val mPlaylistId = "(?:PL|LL|EC|UU|FL|RD|UL|TL|OLAK5uy_)[0-9A-Za-z-_]{10,}"
    val mValidUrl = "(?x)^ (" +
            "(?:https?://|//)" +
            "(?:(?:(?:(?:\\w+\\.)?[yY][oO][uU][tT][uU][bB][eE](?:-nocookie)?\\.com/|" +
            "(?:www\\.)?deturl\\.com/www\\.youtube\\.com/|" +
            "(?:www\\.)?pwnyoutube\\.com/|" +
            "(?:www\\.)?hooktube\\.com/|" +
            "(?:www\\.)?yourepeat\\.com/|" +
            "tube\\.majestyc\\.net/|" +
            "(?:(?:www|dev)\\.)?invidio\\.us/|" +
            "(?:www\\.)?invidiou\\.sh/|" +
            "(?:www\\.)?invidious\\.snopyta\\.org/|" +
            "(?:www\\.)?invidious\\.kabi\\.tk/|" +
            "(?:www\\.)?vid\\.wxzm\\.sx/|" +
            "youtube\\.googleapis\\.com/)" +
            "(?:.*?\\#/)?" +
            "(?:" +
            "(?:(?:v|embed|e)/(?!videoseries))" +
            "|(?:" +
            "(?:(?:watch|movie)(?:_popup)?(?:\\.php)?/?)?" +
            "(?:\\?|\\#!?)" +
            "(?:.*?[&;])??" +
            "v=" +
            ")" +
            "))" +
            "|(?:" +
            "youtu\\.be|" +
            "vid\\.plus|" +
            "zwearz\\.com/watch|" +
            ")/" +
            "|(?:www\\.)?cleanvideosearch\\.com/media/action/yt/watch\\?videoId=" +
            ")" +
            ")?" +
            "([0-9A-Za-z_-]{11})" +
            "(?!.*?\\blist=" +
            "(?:" +
            "$mPlaylistId|" +
            "WL" +
            ")" +
            ")" +
            "(.+)?" +
            "$"

    val mFormats = JSONObject().apply {
        put("5", mapOf("ext" to "flv", "width" to 400, "height" to 240, "acodec" to "mp3", "abr" to 64, "vcodec" to "h263"))
        put("6", mapOf("ext" to "flv", "width" to 450, "height" to 270, "acodec" to "mp3", "abr" to 64, "vcodec" to "h263"))
        put("13", mapOf("ext" to "3gp", "acodec" to "aac", "vcodec" to "mp4v"))
        put("17", mapOf("ext" to "3gp", "width" to 176, "height" to 144, "acodec" to "aac", "abr" to 24, "vcodec" to "mp4v"))
        put("18", mapOf("ext" to "mp4", "width" to 640, "height" to 360, "acodec" to "aac", "abr" to 96, "vcodec" to "h264"))
        put("22", mapOf("ext" to "mp4", "width" to 1280, "height" to 720, "acodec" to "aac", "abr" to 192, "vcodec" to "h264"))
        put("34", mapOf("ext" to "flv", "width" to 640, "height" to 360, "acodec" to "aac", "abr" to 128, "vcodec" to "h264"))
        put("35", mapOf("ext" to "flv", "width" to 854, "height" to 480, "acodec" to "aac", "abr" to 128, "vcodec" to "h264"))

        // itag 36 videos are either 320x180 (BaW_jenozKc) or 320x240 (__2ABJjxzNo), abr varies as well
        put("36", mapOf("ext" to "3gp", "width" to 320, "acodec" to "aac", "vcodec" to "mp4v"))
        put("37", mapOf("ext" to "mp4", "width" to 1920, "height" to 1080, "acodec" to "aac", "abr" to 192, "vcodec" to "h264"))
        put("38", mapOf("ext" to "mp4", "width" to 4096, "height" to 3072, "acodec" to "aac", "abr" to 192, "vcodec" to "h264"))
        put("43", mapOf("ext" to "webm", "width" to 640, "height" to 360, "acodec" to "vorbis", "abr" to 128, "vcodec" to "vp8"))
        put("44", mapOf("ext" to "webm", "width" to 854, "height" to 480, "acodec" to "vorbis", "abr" to 128, "vcodec" to "vp8"))
        put("45", mapOf("ext" to "webm", "width" to 1280, "height" to 720, "acodec" to "vorbis", "abr" to 192, "vcodec" to "vp8"))
        put("46", mapOf("ext" to "webm", "width" to 1920, "height" to 1080, "acodec" to "vorbis", "abr" to 192, "vcodec" to "vp8"))
        put("59", mapOf("ext" to "mp4", "width" to 854, "height" to 480, "acodec" to "aac", "abr" to 128, "vcodec" to "h264"))
        put("78", mapOf("ext" to "mp4", "width" to 854, "height" to 480, "acodec" to "aac", "abr" to 128, "vcodec" to "h264"))

        // 3D videos
        put("82", mapOf("ext" to "mp4", "height" to 360, "format_note" to "3D", "acodec" to "aac", "abr" to 128, "vcodec" to "h264", "preference" to -20))
        put("83", mapOf("ext" to "mp4", "height" to 480, "format_note" to "3D", "acodec" to "aac", "abr" to 128, "vcodec" to "h264", "preference" to -20))
        put("84", mapOf("ext" to "mp4", "height" to 720, "format_note" to "3D", "acodec" to "aac", "abr" to 192, "vcodec" to "h264", "preference" to -20))
        put("85", mapOf("ext" to "mp4", "height" to 1080, "format_note" to "3D", "acodec" to "aac", "abr" to 192, "vcodec" to "h264", "preference" to -20))
        put("100", mapOf("ext" to "webm", "height" to 360, "format_note" to "3D", "acodec" to "vorbis", "abr" to 128, "vcodec" to "vp8", "preference" to -20))
        put("101", mapOf("ext" to "webm", "height" to 480, "format_note" to "3D", "acodec" to "vorbis", "abr" to 192, "vcodec" to "vp8", "preference" to -20))
        put("102", mapOf("ext" to "webm", "height" to 720, "format_note" to "3D", "acodec" to "vorbis", "abr" to 192, "vcodec" to "vp8", "preference" to -20))

        // Apple HTTP Live Streaming
        put("91", mapOf("ext" to "mp4", "height" to 144, "format_note" to "HLS", "acodec" to "aac", "abr" to 48, "vcodec" to "h264", "preference" to -10))
        put("92", mapOf("ext" to "mp4", "height" to 240, "format_note" to "HLS", "acodec" to "aac", "abr" to 48, "vcodec" to "h264", "preference" to -10))
        put("93", mapOf("ext" to "mp4", "height" to 360, "format_note" to "HLS", "acodec" to "aac", "abr" to 128, "vcodec" to "h264", "preference" to -10))
        put("94", mapOf("ext" to "mp4", "height" to 480, "format_note" to "HLS", "acodec" to "aac", "abr" to 128, "vcodec" to "h264", "preference" to -10))
        put("95", mapOf("ext" to "mp4", "height" to 720, "format_note" to "HLS", "acodec" to "aac", "abr" to 256, "vcodec" to "h264", "preference" to -10))
        put("96", mapOf("ext" to "mp4", "height" to 1080, "format_note" to "HLS", "acodec" to "aac", "abr" to 256, "vcodec" to "h264", "preference" to -10))
        put("132", mapOf("ext" to "mp4", "height" to 240, "format_note" to "HLS", "acodec" to "aac", "abr" to 48, "vcodec" to "h264", "preference" to -10))
        put("151", mapOf("ext" to "mp4", "height" to 72, "format_note" to "HLS", "acodec" to "aac", "abr" to 24, "vcodec" to "h264", "preference" to -10))

        // DASH mp4 video
        put("133", mapOf("ext" to "mp4", "height" to 240, "format_note" to "DASH video", "vcodec" to "h264"))
        put("134", mapOf("ext" to "mp4", "height" to 360, "format_note" to "DASH video", "vcodec" to "h264"))
        put("135", mapOf("ext" to "mp4", "height" to 480, "format_note" to "DASH video", "vcodec" to "h264"))
        put("136", mapOf("ext" to "mp4", "height" to 720, "format_note" to "DASH video", "vcodec" to "h264"))
        put("137", mapOf("ext" to "mp4", "height" to 1080, "format_note" to "DASH video", "vcodec" to "h264"))
        put("138", mapOf("ext" to "mp4", "format_note" to "DASH video", "vcodec" to "h264"))  // Height can vary (https://github.com/ytdl-org/youtube-dl/issues/4559)
        put("160", mapOf("ext" to "mp4", "height" to 144, "format_note" to "DASH video", "vcodec" to "h264"))
        put("212", mapOf("ext" to "mp4", "height" to 480, "format_note" to "DASH video", "vcodec" to "h264"))
        put("264", mapOf("ext" to "mp4", "height" to 1440, "format_note" to "DASH video", "vcodec" to "h264"))
        put("298", mapOf("ext" to "mp4", "height" to 720, "format_note" to "DASH video", "vcodec" to "h264", "fps" to 60))
        put("299", mapOf("ext" to "mp4", "height" to 1080, "format_note" to "DASH video", "vcodec" to "h264", "fps" to 60))
        put("266", mapOf("ext" to "mp4", "height" to 2160, "format_note" to "DASH video", "vcodec" to "h264"))

        // Dash mp4 audio
        put("139", mapOf("ext" to "m4a", "format_note" to "DASH audio", "acodec" to "aac", "abr" to 48, "container" to "m4a_dash"))
        put("140", mapOf("ext" to "m4a", "format_note" to "DASH audio", "acodec" to "aac", "abr" to 128, "container" to "m4a_dash"))
        put("141", mapOf("ext" to "m4a", "format_note" to "DASH audio", "acodec" to "aac", "abr" to 256, "container" to "m4a_dash"))
        put("256", mapOf("ext" to "m4a", "format_note" to "DASH audio", "acodec" to "aac", "container" to "m4a_dash"))
        put("258", mapOf("ext" to "m4a", "format_note" to "DASH audio", "acodec" to "aac", "container" to "m4a_dash"))
        put("325", mapOf("ext" to "m4a", "format_note" to "DASH audio", "acodec" to "dtse", "container" to "m4a_dash"))
        put("328", mapOf("ext" to "m4a", "format_note" to "DASH audio", "acodec" to "ec-3", "container" to "m4a_dash"))

        // Dash webm
        put("167", mapOf("ext" to "webm", "height" to 360, "width" to 640, "format_note" to "DASH video", "container" to "webm", "vcodec" to "vp8"))
        put("168", mapOf("ext" to "webm", "height" to 480, "width" to 854, "format_note" to "DASH video", "container" to "webm", "vcodec" to "vp8"))
        put("169", mapOf("ext" to "webm", "height" to 720, "width" to 1280, "format_note" to "DASH video", "container" to "webm", "vcodec" to "vp8"))
        put("170", mapOf("ext" to "webm", "height" to 1080, "width" to 1920, "format_note" to "DASH video", "container" to "webm", "vcodec" to "vp8"))
        put("218", mapOf("ext" to "webm", "height" to 480, "width" to 854, "format_note" to "DASH video", "container" to "webm", "vcodec" to "vp8"))
        put("219", mapOf("ext" to "webm", "height" to 480, "width" to 854, "format_note" to "DASH video", "container" to "webm", "vcodec" to "vp8"))
        put("278", mapOf("ext" to "webm", "height" to 144, "format_note" to "DASH video", "container" to "webm", "vcodec" to "vp9"))
        put("242", mapOf("ext" to "webm", "height" to 240, "format_note" to "DASH video", "vcodec" to "vp9"))
        put("243", mapOf("ext" to "webm", "height" to 360, "format_note" to "DASH video", "vcodec" to "vp9"))
        put("244", mapOf("ext" to "webm", "height" to 480, "format_note" to "DASH video", "vcodec" to "vp9"))
        put("245", mapOf("ext" to "webm", "height" to 480, "format_note" to "DASH video", "vcodec" to "vp9"))
        put("246", mapOf("ext" to "webm", "height" to 480, "format_note" to "DASH video", "vcodec" to "vp9"))
        put("247", mapOf("ext" to "webm", "height" to 720, "format_note" to "DASH video", "vcodec" to "vp9"))
        put("248", mapOf("ext" to "webm", "height" to 1080, "format_note" to "DASH video", "vcodec" to "vp9"))
        put("271", mapOf("ext" to "webm", "height" to 1440, "format_note" to "DASH video", "vcodec" to "vp9"))

        // itag 272 videos are either 3840x2160 (e.g. RtoitU2A-3E) or 7680x4320 (sLprVF6d7Ug)
        put("272", mapOf("ext" to "webm", "height" to 2160, "format_note" to "DASH video", "vcodec" to "vp9"))
        put("302", mapOf("ext" to "webm", "height" to 720, "format_note" to "DASH video", "vcodec" to "vp9", "fps" to 60))
        put("303", mapOf("ext" to "webm", "height" to 1080, "format_note" to "DASH video", "vcodec" to "vp9", "fps" to 60))
        put("308", mapOf("ext" to "webm", "height" to 1440, "format_note" to "DASH video", "vcodec" to "vp9", "fps" to 60))
        put("313", mapOf("ext" to "webm", "height" to 2160, "format_note" to "DASH video", "vcodec" to "vp9"))
        put("315", mapOf("ext" to "webm", "height" to 2160, "format_note" to "DASH video", "vcodec" to "vp9", "fps" to 60))

        // Dash webm audio
        put("171", mapOf("ext" to "webm", "acodec" to "vorbis", "format_note" to "DASH audio", "abr" to 128))
        put("172", mapOf("ext" to "webm", "acodec" to "vorbis", "format_note" to "DASH audio", "abr" to 256))

        // Dash webm audio with opus inside
        put("249", mapOf("ext" to "webm", "format_note" to "DASH audio", "acodec" to "opus", "abr" to 50))
        put("250", mapOf("ext" to "webm", "format_note" to "DASH audio", "acodec" to "opus", "abr" to 70))
        put("251", mapOf("ext" to "webm", "format_note" to "DASH audio", "acodec" to "opus", "abr" to 160))

        // RTMP (unnamed)
        put("_rtmp", mapOf("protocol" to "rtmp"))

        // av01 video only formats sometimes served with "unknown" codecs
        put("394", mapOf("acodec" to "none", "vcodec" to "av01.0.05M.08"))
        put("395", mapOf("acodec" to "none", "vcodec" to "av01.0.05M.08"))
        put("396", mapOf("acodec" to "none", "vcodec" to "av01.0.05M.08"))
        put("397", mapOf("acodec" to "none", "vcodec" to "av01.0.05M.08"))
    }
}
