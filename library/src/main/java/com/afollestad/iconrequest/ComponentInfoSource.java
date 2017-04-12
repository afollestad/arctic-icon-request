package com.afollestad.iconrequest;

import java.util.ArrayList;
import java.util.HashSet;

interface ComponentInfoSource {

  ArrayList<AppModel> getInstalledApps(HashSet<String> filter);
}
