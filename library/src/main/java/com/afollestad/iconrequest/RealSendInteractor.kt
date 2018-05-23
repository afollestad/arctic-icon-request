package com.afollestad.iconrequest

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Build.MANUFACTURER
import android.os.Build.MODEL
import android.os.Build.PRODUCT
import android.os.Build.VERSION
import com.afollestad.bridge.Bridge
import com.afollestad.bridge.Bridge.post
import com.afollestad.bridge.MultipartForm
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
import org.json.JSONObject
import java.io.File
import java.util.Date

private fun isNullOrEmpty(value: String?): Boolean {
  return value == null || value.isEmpty()
}

/** @author Aidan Follestad (afollestad) */
internal class RealSendInteractor(private val context: Context) : SendInteractor {

  @Throws(Exception::class)
  override fun send(
    selectedApps: List<AppModel>,
    request: ArcticRequest
  ): Boolean {
    val config = request.config
    "Preparing your request to send...".log(TAG)
    if (selectedApps.isEmpty()) {
      throw Exception("No apps were selected to send.")
    } else if (isNullOrEmpty(config!!.emailRecipient) && isNullOrEmpty(config.apiKey)) {
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
        "Icon for ${app.code} didn't return a BitmapDrawable.".log(TAG)
        continue
      }
      val icon = drawable.bitmap
      val file = File(cacheFolder, "${app.pkg}.png")
      filesToZip.add(file)
      try {
        icon.writeIconTo(file)
        "Saved icon: ${file.absolutePath}".log(TAG)
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
      "Added ${app.code} to the new generated appfilter file...".log(TAG)
    }

    if (xmlSb != null) {
      xmlSb.append("\n\n</resources>")
      val newAppFilter = File(cacheFolder, "appfilter.xml")
      filesToZip.add(newAppFilter)
      try {
        xmlSb.toString()
            .writeAll(newAppFilter)
        "Generated appfilter saved to ${newAppFilter.absolutePath}".log(TAG)
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
          "Generated appfilter JSON saved to: ${newAppFilter.absolutePath}".log(TAG)
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
      "ZIP created at ${zipFile.absolutePath}".log(TAG)
    } catch (e: Exception) {
      throw Exception("Failed to create the request ZIP file: ${e.message}", e)
    }

    // Cleanup files
    "Cleaning up files...".log(TAG)
    cacheFolder.deleteRelevantChildren()

    // Send request to the backend server
    if (isRemote) {
      Bridge.config()
          .host(RM_HOST)
          .defaultHeader("TokenID", config.apiKey)
          .defaultHeader("Accept", "application/json")
          .defaultHeader("User-Agent", "afollestad/arctic-icon-request")
          .validators(RemoteValidator())
      try {
        val form = MultipartForm()
        form.add("archive", zipFile)
        form.add("apps", JSONObject(jsonSb!!.toString()).toString())
        post("/v1/request").throwIfNotSuccess()
            .body(form)
            .request()
        "Request uploaded to the server!".log(TAG)
      } catch (e: Exception) {
        throw Exception("Failed to send icons to the backend: " + e.message, e)
      }

    }

    if (!isRemote) {
      // Send email intent
      "Launching intent!".log(TAG)
      val zipUri = zipFile.toUri()
      val newUri = request.uriTransformer?.invoke(zipUri) ?: zipUri
      if (zipUri.toString() != newUri.toString()) {
        "Transformed URI $zipUri -> $newUri".log(TAG)
      }
      val emailIntent = Intent(Intent.ACTION_SEND)
          .putExtra(Intent.EXTRA_EMAIL, arrayOf(config.emailRecipient!!))
          .putExtra(Intent.EXTRA_SUBJECT, config.emailSubject)
          .putExtra(Intent.EXTRA_TEXT, getEmailBody(selectedApps, config).toHtml())
          .putExtra(Intent.EXTRA_STREAM, newUri)
          .setType("application/zip")
      context.startActivity(
          Intent.createChooser(emailIntent, context.getString(R.string.send_using))
      )
    }
    "Done!".log(TAG)

    return isRemote
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

  companion object {
    private const val TAG = "RealSendInteractor"
    private const val RM_HOST = "https://polar.aidanfollestad.com"
  }
}
