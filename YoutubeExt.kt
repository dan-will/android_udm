package com.github.udm.extract

//....
object YoutubeExt: CommonMethods {
  override fun onFileExt(mPara: JSONObject, context0: Context?): JSONObject {
    try {
      val mClient = OkHttpClient()
      val pageLink = mPara.getString(FI_PAGE_URL)
      
      // Check Id Match
    } catch (ex0: Exception) {
      ex0.printStackTrace()
    } finally {
      val jsFiles = mPara.getJSONArray(FI_FILE_URLS)
      if (jsFiles.length() > 0) {
        when (jsFiles.length()) {
          1 -> mPara.put(FI_URL_TASK, LK_URL_B)
          else -> mPara.put(FI_URL_TASK, LK_URL_C)
