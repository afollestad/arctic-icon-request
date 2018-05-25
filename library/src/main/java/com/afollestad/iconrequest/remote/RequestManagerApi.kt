package com.afollestad.iconrequest.remote

import io.reactivex.Observable
import okhttp3.MultipartBody
import retrofit2.http.Part

interface RequestManagerApi {
  fun performRequest(
    @Part archive: MultipartBody.Part,
    @Part appsJson: MultipartBody.Part
  ): Observable<Void>
}
