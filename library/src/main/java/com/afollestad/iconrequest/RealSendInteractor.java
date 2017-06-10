package com.afollestad.iconrequest;

import static com.afollestad.bridge.Bridge.post;
import static com.afollestad.iconrequest.FileUtil.writeAll;
import static com.afollestad.iconrequest.IRLog.log;
import static com.afollestad.iconrequest.IRUtils.isEmpty;
import static com.afollestad.iconrequest.ZipUtil.zip;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import com.afollestad.bridge.Bridge;
import com.afollestad.bridge.MultipartForm;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

class RealSendInteractor implements SendInteractor {

  private static final String TAG = RealSendInteractor.class.getSimpleName();
  private static final String RM_HOST = "https://polar.aidanfollestad.com";
  private final Context context;

  RealSendInteractor(Context context) {
    this.context = context;
  }

  @Override
  public boolean send(List<AppModel> selectedApps, PolarRequest request) throws Exception {
    PolarConfig config = request.config;
    log(TAG, "Preparing your request to send...");
    if (selectedApps.size() == 0) {
      throw new Exception("No apps were selected to send.");
    } else if (isEmpty(config.emailRecipient()) && isEmpty(config.apiKey())) {
      throw new Exception(
          "You must either specify a recipient email or a request manager API key.");
    }

    final File cacheFolder = new File(config.cacheFolder());
    if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
      throw new Exception(
          "Unable to find or create cache folder: " + cacheFolder.getAbsolutePath());
    }

    final ArrayList<File> filesToZip = new ArrayList<>();
    final boolean isRemote = !isEmpty(config.apiKey());

    // Save app icons
    log(TAG, "Saving icons...");

    for (AppModel app : selectedApps) {
      final Drawable drawable = app.getIcon(context);
      if (!(drawable instanceof BitmapDrawable)) {
        log(TAG, "Icon for " + app.code() + " didn't return a BitmapDrawable.");
        continue;
      }
      final BitmapDrawable bDrawable = (BitmapDrawable) drawable;
      final Bitmap icon = bDrawable.getBitmap();
      final File file = new File(cacheFolder, String.format("%s.png", app.pkg()));
      filesToZip.add(file);
      try {
        FileUtil.writeIcon(file, icon);
        log(TAG, "Saved icon: " + file.getAbsolutePath());
      } catch (final Exception e) {
        throw new Exception("Failed to save an icon: " + e.getMessage(), e);
      }
    }

    // Create appfilter
    log(TAG, "Creating appfilter...");

    StringBuilder xmlSb = null;
    StringBuilder jsonSb = null;
    if (!isRemote) {
      xmlSb =
          new StringBuilder(
              "<resources>\n"
                  + "    <iconback img1=\"iconback\" />\n"
                  + "    <iconmask img1=\"iconmask\" />\n"
                  + "    <iconupon img1=\"iconupon\" />\n"
                  + "    <scale factor=\"1.0\" />");
    } else {
      jsonSb = new StringBuilder("{\n" + "    \"components\": [");
    }
    int index = 0;
    for (AppModel app : selectedApps) {
      final String name = app.name();
      final String drawableName = IRUtils.drawableName(name);
      if (xmlSb != null) {
        xmlSb.append("\n\n    <!-- ");
        xmlSb.append(name);
        xmlSb.append(" -->\n");
        xmlSb.append(
            String.format(
                "    <item\n"
                    + "        component=\"ComponentInfo{%s}\"\n"
                    + "        drawable=\"%s\" />",
                app.code(), drawableName));
      }
      if (jsonSb != null) {
        if (index > 0) jsonSb.append(",");
        jsonSb.append("\n        {\n");
        jsonSb.append(String.format("            \"%s\": \"%s\",\n", "name", name));
        jsonSb.append(String.format("            \"%s\": \"%s\",\n", "pkg", app.pkg()));
        jsonSb.append(String.format("            \"%s\": \"%s\",\n", "componentInfo", app.code()));
        jsonSb.append(String.format("            \"%s\": \"%s\"\n", "drawable", drawableName));
        jsonSb.append("        }");
      }
      log(TAG, "Added " + app.code() + " to the new generated appfilter file...");
      index++;
    }

    if (xmlSb != null) {
      xmlSb.append("\n\n</resources>");
      final File newAppFilter = new File(cacheFolder, "appfilter.xml");
      filesToZip.add(newAppFilter);
      try {
        writeAll(newAppFilter, xmlSb.toString());
        log(TAG, "Generated appfilter saved to " + newAppFilter.getAbsolutePath());
      } catch (final Exception e) {
        throw new Exception(
            "Failed to write your request appfilter.xml file: " + e.getMessage(), e);
      }
    }
    if (jsonSb != null) {
      jsonSb.append("\n    ]\n}");
      if (!isRemote) {
        final File newAppFilter = new File(cacheFolder, "appfilter.json");
        filesToZip.add(newAppFilter);
        try {
          writeAll(newAppFilter, jsonSb.toString());
          log(TAG, "Generated appfilter JSON saved to: " + newAppFilter.getAbsolutePath());
        } catch (final Exception e) {
          throw new Exception(
              "Failed to write your request appfilter.json file: " + e.getMessage(), e);
        }
      }
    }

    if (filesToZip.size() == 0) {
      throw new Exception("There are no PNG files to put into the ZIP archive.");
    }

    // Zip everything into an archive
    log(TAG, "Creating ZIP...");

    final SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
    final File zipFile =
        new File(cacheFolder, String.format("IconRequest-%s.zip", df.format(new Date())));
    try {
      zip(zipFile, filesToZip.toArray(new File[filesToZip.size()]));
      log(TAG, "ZIP created at " + zipFile.getAbsolutePath());
    } catch (final Exception e) {
      throw new Exception("Failed to create the request ZIP file: " + e.getMessage(), e);
    }

    // Cleanup files
    log(TAG, "Cleaning up files...");
    final File[] files = cacheFolder.listFiles();
    for (File fi : files) {
      if (!fi.isDirectory()
          && (fi.getName().endsWith(".png")
              || fi.getName().endsWith(".xml")
              || fi.getName().endsWith(".json"))) {
        if (fi.delete()) {
          log(TAG, "Deleted: " + fi.getAbsolutePath());
        }
      }
    }

    // Send request to the backend server
    if (isRemote) {
      Bridge.config()
          .host(RM_HOST)
          .defaultHeader("TokenID", config.apiKey())
          .defaultHeader("Accept", "application/json")
          .defaultHeader("User-Agent", "afollestad/icon-request")
          .validators(new RemoteValidator());
      try {
        MultipartForm form = new MultipartForm();
        form.add("archive", zipFile);
        form.add("apps", new JSONObject(jsonSb.toString()).toString());
        post("/v1/request").throwIfNotSuccess().body(form).request();
        log(TAG, "Request uploaded to the server!");
      } catch (Exception e) {
        throw new Exception("Failed to send icons to the backend: " + e.getMessage(), e);
      }
    }

    if (!isRemote) {
      // Send email intent
      log(TAG, "Launching intent!");
      final Uri zipUri = Uri.fromFile(zipFile);
      final Uri newUri = request.uriTransformer.apply(zipUri);
      if (!zipUri.toString().equals(newUri.toString())) {
        log(TAG, "Transformed URI %s -> %s", zipUri.toString(), newUri.toString());
      }
      final Intent emailIntent =
          new Intent(Intent.ACTION_SEND)
              .putExtra(Intent.EXTRA_EMAIL, new String[] {config.emailRecipient()})
              .putExtra(Intent.EXTRA_SUBJECT, config.emailSubject())
              .putExtra(Intent.EXTRA_TEXT, Html.fromHtml(getEmailBody(selectedApps, config)))
              .putExtra(Intent.EXTRA_STREAM, newUri)
              .setType("application/zip");
      context.startActivity(
          Intent.createChooser(emailIntent, context.getString(R.string.send_using)));
    }
    log(TAG, "Done!");

    return isRemote;
  }

  private String getEmailBody(List<AppModel> selectedApps, PolarConfig config) {
    StringBuilder sb = new StringBuilder();
    if (!isEmpty(config.emailHeader())) {
      sb.append(config.emailHeader().replace("\n", "<br/>"));
      sb.append("<br/><br/>");
    }

    for (int i = 0; i < selectedApps.size(); i++) {
      if (i > 0) {
        sb.append("<br/><br/>");
      }
      final AppModel app = selectedApps.get(i);
      sb.append(String.format("Name: <b>%s</b><br/>", app.name()));
      sb.append(String.format("Code: <b>%s</b><br/>", app.code()));
      sb.append(
          String.format("Link: https://play.google.com/store/apps/details?id=%s<br/>", app.pkg()));
    }

    if (config.includeDeviceInfo()) {
      sb.append(
          String.format(
              Locale.getDefault(),
              "<br/><br/>OS: %s %s<br/>Device: %s %s (%s)",
              Build.VERSION.RELEASE,
              IRUtils.getOSVersionName(Build.VERSION.SDK_INT),
              Build.MANUFACTURER,
              Build.MODEL,
              Build.PRODUCT));
      if (config.emailFooter() != null) {
        sb.append("<br/>");
        sb.append(config.emailFooter().replace("\n", "<br/>"));
      }
    } else if (config.emailFooter() != null) {
      sb.append("<br/><br/>");
      sb.append(config.emailFooter().replace("\n", "<br/>"));
    }
    return sb.toString();
  }
}
