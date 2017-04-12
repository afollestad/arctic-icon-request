package com.afollestad.iconrequest;

import java.util.List;

interface SendInteractor {

  boolean send(List<AppModel> selectedApps, PolarRequest request) throws Exception;
}
