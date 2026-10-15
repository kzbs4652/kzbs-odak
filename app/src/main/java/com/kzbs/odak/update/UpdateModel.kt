package com.kzbs.odak.update

import com.google.gson.annotations.SerializedName

data class UpdateModel(
    @SerializedName("latestVersionCode")
    val latestVersionCode: Int,

    @SerializedName("latestVersionName")
    val latestVersionName: String,

    @SerializedName("changelog")
    val changelog: String,

    @SerializedName("apkUrl")
    val apkUrl: String,

    @SerializedName("isMandatory")
    val isMandatory: Boolean = false
)
