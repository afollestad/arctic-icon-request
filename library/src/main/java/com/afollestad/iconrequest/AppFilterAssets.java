package com.afollestad.iconrequest;

import static com.afollestad.iconrequest.FileUtil.closeQuietely;
import static com.afollestad.iconrequest.IRLog.log;
import static com.afollestad.iconrequest.IRUtils.isEmpty;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

class AppFilterAssets implements AppFilterSource {

  private static final String TAG = AppFilterAssets.class.getSimpleName();
  private final Context context;

  AppFilterAssets(Context context) {
    this.context = context;
  }

  @Override
  @Nullable
  public HashSet<String> load(String filterName, boolean errorOnInvalidDrawables) throws Exception {
    final HashSet<String> defined = new HashSet<>();
    if (isEmpty(filterName)) {
      return defined;
    }

    InputStream is;
    try {
      final AssetManager am = context.getAssets();
      log(TAG, "Loading your appfilter, opening: %s", filterName);
      is = am.open(filterName);
    } catch (final Throwable e) {
      throw new Exception("Failed to open " + filterName, e);
    }

    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    StringBuilder invalidDrawables = null;

    try {
      final String itemEndStr = "/>";
      final String componentStartStr = "component=\"ComponentInfo";
      final String drawableStartStr = "drawable=\"";
      final String endStr = "\"";
      final String commentStart = "<!--";
      final String commentEnd = "-->";

      String component = null;
      String drawable = null;

      String line;
      boolean inComment = false;

      while ((line = reader.readLine()) != null) {
        final String trimmedLine = line.trim();
        if (!inComment && trimmedLine.startsWith(commentStart)) {
          inComment = true;
        }
        if (inComment && trimmedLine.endsWith(commentEnd)) {
          inComment = false;
          continue;
        }

        if (inComment) continue;
        int start;
        int end;

        start = line.indexOf(componentStartStr);
        if (start != -1) {
          start += componentStartStr.length();
          end = line.indexOf(endStr, start);
          String ci = line.substring(start, end);
          if (ci.startsWith("{")) ci = ci.substring(1);
          if (ci.endsWith("}")) ci = ci.substring(0, ci.length() - 1);
          component = ci;
        }

        start = line.indexOf(drawableStartStr);
        if (start != -1) {
          start += drawableStartStr.length();
          end = line.indexOf(endStr, start);
          drawable = line.substring(start, end);
        }

        start = line.indexOf(itemEndStr);
        if (start != -1 && (component != null || drawable != null)) {
          log(TAG, "Found: %s (%s)", component, drawable);
          if (drawable == null || drawable.trim().isEmpty()) {
            log(TAG, "WARNING: Drawable shouldn't be null.");
            if (errorOnInvalidDrawables) {
              if (invalidDrawables == null) {
                invalidDrawables = new StringBuilder();
              }
              if (invalidDrawables.length() > 0) {
                invalidDrawables.append("\n");
              }
              invalidDrawables.append(
                  String.format("Drawable for %s was null or empty.\n", component));
            }
          } else {
            final Resources r = context.getResources();
            int identifier;
            try {
              identifier = r.getIdentifier(drawable, "drawable", context.getPackageName());
            } catch (Throwable t) {
              identifier = 0;
            }
            if (identifier == 0) {
              log(
                  TAG,
                  "WARNING: Drawable %s (for %s) doesn't match up with a resource.",
                  drawable,
                  component);
              if (errorOnInvalidDrawables) {
                if (invalidDrawables == null) {
                  invalidDrawables = new StringBuilder();
                }
                if (invalidDrawables.length() > 0) {
                  invalidDrawables.append("\n");
                }
                invalidDrawables.append(
                    String.format(
                        "Drawable %s (for %s) doesn't match up with a resource.\n",
                        drawable, component));
              }
            }
          }
          defined.add(component);
        }
      }

      if (invalidDrawables != null && invalidDrawables.length() > 0) {
        invalidDrawables.setLength(0);
        invalidDrawables.trimToSize();
        throw new Exception(invalidDrawables.toString());
      }
      log(TAG, "Found %d total app(s) in your appfilter.", defined.size());
    } catch (final Throwable e) {
      throw new Exception("Failed to read " + filterName, e);
    } finally {
      closeQuietely(reader);
      closeQuietely(is);
    }

    return defined;
  }
}
