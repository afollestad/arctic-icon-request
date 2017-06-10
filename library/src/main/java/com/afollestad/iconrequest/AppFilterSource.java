package com.afollestad.iconrequest;

import android.support.annotation.Nullable;
import java.util.HashSet;

interface AppFilterSource {

  @Nullable
  HashSet<String> load(String filterName, boolean errorOnInvalidDrawables) throws Exception;
}
