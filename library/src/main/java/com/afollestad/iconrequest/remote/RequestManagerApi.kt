package com.afollestad.iconrequest.remote

import io.reactivex.Observable
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RequestManagerApi {
  @Multipart
  @POST("/v1/request")
  fun performRequest(
    @Part archive: MultipartBody.Part,
    @Part appsJson: MultipartBody.Part
  ): Observable<ApiResponse>
}
