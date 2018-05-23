package com.afollestad.iconrequest.remote

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Build.MANUFACTURER
import android.os.Build.MODEL
import android.os.Build.PRODUCT
import android.os.Build.VERSION
import com.afollestad.iconrequest.AppModel
import com.afollestad.iconrequest.ArcticConfig
import com.afollestad.iconrequest.ArcticRequest
import com.afollestad.iconrequest.R.string
import com.afollestad.iconrequest.SendInteractor
import com.afollestad.iconrequest.UriTransformer
import com.afollestad.iconrequest.extensions.dateFormat
import com.afollestad.iconrequest.extensions.deleteRelevantChildren
import com.afollestad.iconrequest.extensions.drawableName
import com.afollestad.iconrequest.extensions.log
import com.afollestad.iconrequest.extensions.osVersionName
import com.afollestad.iconrequest.extensions.toHtml
import com.afollestad.iconrequest.extensions.toUri
import com.afollestad.iconrequest.extensions.writeAll
import com.afollestad.iconrequest.extensions.writeIconTo
import com.afollestad.iconrequest.extensions.zipInto
import io.reactivex.Observable
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.File
import java.util.Date
import okhttp3.RequestBody

private fun isNullOrEmpty(value: String?): Boolean {
  return value == null || value.isEmpty()
}

/** @author Aidan Follestad (afollestad) */
internal class RealSendInteractor(private val context: Context) : SendInteractor {

  @Throws(Exception::class)
  override fun send(
    selectedApps: List<AppModel>,
    request: ArcticRequest
  ): Observable<Boolean> {
    val config = request.config
    "Preparing your request to send...".log(
        TAG
    )
    if (selectedApps.isEmpty()) {
      throw Exception("No apps were selected to send.")
    } else if (isNullOrEmpty(
            config.emailRecipient
        ) && isNullOrEmpty(config.apiKey)
    ) {
      throw Exception(
          "You must either specify a recipient email or a request manager API key."
      )
    }

    val cacheFolder = config.actualCacheFolder(context)
    if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
      throw Exception(
          "Unable to find or create cache folder: ${cacheFolder.absolutePath}"
      )
    }

    val filesToZip = mutableListOf<File>()
    val isRemote = !isNullOrEmpty(config.apiKey)

    // Save app icons
    "Saving icons...".log(TAG)

    for (app in selectedApps) {
      val drawable = app.getIcon(context)
      if (drawable !is BitmapDrawable) {
        "Icon for ${app.code} didn't return a BitmapDrawable.".log(
            TAG
        )
        continue
      }
      val icon = drawable.bitmap
      val file = File(cacheFolder, "${app.pkg}.png")
      filesToZip.add(file)
      try {
        icon.writeIconTo(file)
        "Saved icon: ${file.absolutePath}".log(
            TAG
        )
      } catch (e: Exception) {
        throw Exception("Failed to save an icon: " + e.message, e)
      }

    }

    // Create appfilter
    "Creating appfilter...".log(TAG)

    var xmlSb: StringBuilder? = null
    var jsonSb: StringBuilder? = null
    if (!isRemote) {
      xmlSb = StringBuilder(
          "<resources>\n"
              + "    <iconback img1=\"iconback\" />\n"
              + "    <iconmask img1=\"iconmask\" />\n"
              + "    <iconupon img1=\"iconupon\" />\n"
              + "    <scale factor=\"1.0\" />"
      )
    } else {
      jsonSb = StringBuilder("{\n    \"components\": [")
    }
    for ((index, app) in selectedApps.withIndex()) {
      val name = app.name
      val drawableName = name.drawableName()
      if (xmlSb != null) {
        xmlSb.append("\n\n    <!-- ")
        xmlSb.append(name)
        xmlSb.append(" -->\n")
        xmlSb.append(
            "    <item\n"
                + "        component=\"ComponentInfo{${app.code}}\"\n"
                + "        drawable=\"$drawableName\" />"
        )
      }
      if (jsonSb != null) {
        if (index > 0) jsonSb.append(",")
        jsonSb.append("\n        {\n")
        jsonSb.append("            \"name\": \"$name\",\n")
        jsonSb.append("            \"pkg\": \"${app.pkg}\",\n")
        jsonSb.append("            \"componentInfo\": \"${app.code}\",\n")
        jsonSb.append("            \"drawable\": \"$drawableName\"\n")
        jsonSb.append("        }")
      }
      "Added ${app.code} to the new generated appfilter file...".log(
          TAG
      )
    }

    if (xmlSb != null) {
      xmlSb.append("\n\n</resources>")
      val newAppFilter = File(cacheFolder, "appfilter.xml")
      filesToZip.add(newAppFilter)
      try {
        xmlSb.toString()
            .writeAll(newAppFilter)
        "Generated appfilter saved to ${newAppFilter.absolutePath}".log(
            TAG
        )
      } catch (e: Exception) {
        throw Exception(
            "Failed to write your request appfilter.xml file: ${e.message}", e
        )
      }

    }
    if (jsonSb != null) {
      jsonSb.append("\n    ]\n}")
      if (!isRemote) {
        val newAppFilter = File(cacheFolder, "appfilter.json")
        filesToZip.add(newAppFilter)
        try {
          jsonSb.toString()
              .writeAll(newAppFilter)
          "Generated appfilter JSON saved to: ${newAppFilter.absolutePath}".log(
              TAG
          )
        } catch (e: Exception) {
          throw Exception(
              "Failed to write your request appfilter.json file: ${e.message}", e
          )
        }

      }
    }

    if (filesToZip.isEmpty()) {
      throw Exception("There are no PNG files to put into the ZIP archive.")
    }

    // Zip everything into an archive
    "Creating ZIP...".log(TAG)

    val zipFile = File(cacheFolder, "IconRequest-${Date().dateFormat()}.zip")
    try {
      filesToZip.zipInto(zipFile)
      "ZIP created at ${zipFile.absolutePath}".log(
          TAG
      )
    } catch (e: Exception) {
      throw Exception("Failed to create the request ZIP file: ${e.message}", e)
    }

    // Cleanup files
    "Cleaning up files...".log(TAG)
    cacheFolder.deleteRelevantChildren()

    // Send request to the backend server
    return if (isRemote) {
      val api = createApi(config.apiHost!!, config.apiKey!!)
      val archiveFileBody = RequestBody.create(MediaType.parse("multipart/form-data"), zipFile)
      val archiveFile = MultipartBody.Part.createFormData("archive", "icons.zip", archiveFileBody)
      val appsJson = MultipartBody.Part.createFormData("apps", jsonSb.toString())
      api.performRequest(archiveFile, appsJson)
          .doOnNext {
            "Request uploaded to the server!".log(TAG)
          }
          .map { true }
    } else {
      return launchIntent(zipFile, request.uriTransformer, config, selectedApps)
    }
  }

  private fun getEmailBody(
    selectedApps: List<AppModel>,
    config: ArcticConfig
  ): String {
    val sb = StringBuilder()
    if (config.emailHeader != null && config.emailHeader.isNotEmpty()) {
      sb.append(config.emailHeader.replace("\n", "<br/>"))
      sb.append("<br/><br/>")
    }

    for (i in selectedApps.indices) {
      if (i > 0) {
        sb.append("<br/><br/>")
      }
      val app = selectedApps[i]
      sb.append("Name: <b>${app.name}</b><br/>")
      sb.append("Code: <b>${app.code}</b><br/>")
      sb.append("Link: https://play.google.com/store/apps/details?id=${app.pkg}<br/>")
    }

    if (config.includeDeviceInfo) {
      sb.append(
          "<br/><br/>OS: ${VERSION.RELEASE} $osVersionName<br/>Device: $MANUFACTURER $MODEL ($PRODUCT)"
      )
      if (config.emailFooter != null) {
        sb.append("<br/>")
        sb.append(config.emailFooter.replace("\n", "<br/>"))
      }
    } else if (config.emailFooter != null) {
      sb.append("<br/><br/>")
      sb.append(config.emailFooter.replace("\n", "<br/>"))
    }
    return sb.toString()
  }

  private fun createApi(
    host: String,
    apiToken: String
  ): RequestManagerApi {
    val httpClient = OkHttpClient.Builder()
    httpClient.addInterceptor { chain ->
      val request = chain.request()
          .newBuilder()
          .addHeader("TokenID", apiToken)
          .addHeader("Accept", "application/json")
          .addHeader("User-Agent", "afollestad/arctic-icon-request")
          .build()
      chain.proceed(request)
    }
    val retrofit = Retrofit.Builder()
        .baseUrl(host)
        .client(httpClient.build())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build()
    return retrofit.create(RequestManagerApi::class.java)
  }

  private fun launchIntent(
    zipFile: File,
    uriTransformer: UriTransformer?,
    config: ArcticConfig,
    selectedApps: List<AppModel>
  ): Observable<Boolean> {
    return Observable.fromCallable {
      // Send email intent
      "Launching intent!".log(TAG)
      val zipUri = zipFile.toUri()
      val newUri = uriTransformer?.invoke(zipUri) ?: zipUri
      if (zipUri.toString() != newUri.toString()) {
        "Transformed URI $zipUri -> $newUri".log(
            TAG
        )
      }
      val emailIntent = Intent(Intent.ACTION_SEND)
          .putExtra(Intent.EXTRA_EMAIL, arrayOf(config.emailRecipient!!))
          .putExtra(Intent.EXTRA_SUBJECT, config.emailSubject)
          .putExtra(Intent.EXTRA_TEXT, getEmailBody(selectedApps, config).toHtml())
          .putExtra(Intent.EXTRA_STREAM, newUri)
          .setType("application/zip")
      context.startActivity(
          Intent.createChooser(emailIntent, context.getString(string.send_using))
      )
    }
        .map { true }
  }

  companion object {
    private const val TAG = "RealSendInteractor"
  }
}
