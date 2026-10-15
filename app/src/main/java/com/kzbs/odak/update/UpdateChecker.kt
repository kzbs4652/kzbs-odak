package com.kzbs.odak.update

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.kzbs.odak.BuildConfig
import com.kzbs.odak.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker(private val context: Context) {

    companion object {
        // Direct GitHub API raw content — zero CDN cache delay
        const val DEFAULT_UPDATE_URL =
            "https://api.github.com/repos/kzbs4652/kzbs-odak/contents/version.json"
        const val FALLBACK_UPDATE_URL =
            "https://raw.githubusercontent.com/kzbs4652/kzbs-odak/main/version.json"
    }

    /**
     * Check for updates.
     * @param showUpToDateToast — if true, shows feedback even when up-to-date
     * @param onComplete — callback with result: true = update found, false = no update or error
     * @param onStatusMessage — callback with a status message string for UI display (replaces Toast)
     */
    fun checkForUpdates(
        showUpToDateToast: Boolean = false,
        onComplete: ((Boolean) -> Unit)? = null,
        onStatusMessage: ((String) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var url = URL(DEFAULT_UPDATE_URL)
                var connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3.raw")
                connection.setRequestProperty("User-Agent", "KZBS-ODAK-App")
                connection.setRequestProperty("Cache-Control", "no-cache")
                connection.useCaches = false
                connection.instanceFollowRedirects = true

                var responseCode = connection.responseCode

                // If API fails or is rate limited, try fallback raw URL
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    url = URL(FALLBACK_UPDATE_URL)
                    connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Cache-Control", "no-cache")
                    connection.useCaches = false
                    connection.instanceFollowRedirects = true
                    responseCode = connection.responseCode
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    val updateInfo = Gson().fromJson(json, UpdateModel::class.java)

                    withContext(Dispatchers.Main) {
                        if (updateInfo != null && updateInfo.latestVersionCode > BuildConfig.VERSION_CODE) {
                            showUpdateDialog(updateInfo)
                            onStatusMessage?.invoke("🚀 Yeni yazılım güncellemesi mevcut!")
                            onComplete?.invoke(true)
                        } else {
                            val msg = context.getString(R.string.version_up_to_date, BuildConfig.VERSION_NAME)
                            onStatusMessage?.invoke(msg)
                            // Only show toast if no UI callback is handling the message
                            if (showUpToDateToast && onStatusMessage == null) {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            onComplete?.invoke(false)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val msg = "Sürüm kontrolü yapılamadı (Hata: $responseCode)"
                        onStatusMessage?.invoke(msg)
                        if (showUpToDateToast && onStatusMessage == null) {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        onComplete?.invoke(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Güncelleme sunucusuna bağlanılamadı"
                    onStatusMessage?.invoke(msg)
                    if (showUpToDateToast && onStatusMessage == null) {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                    onComplete?.invoke(false)
                }
            }
        }
    }

    private fun showUpdateDialog(updateInfo: UpdateModel) {
        if (context !is Activity || context.isFinishing) return

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_update, null)
        val tvVersion = view.findViewById<TextView>(R.id.tvDialogVersionName)
        val tvChangelog = view.findViewById<TextView>(R.id.tvDialogChangelog)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseUpdateDialog)
        val btnLater = view.findViewById<Button>(R.id.btnUpdateLater)
        val btnNow = view.findViewById<Button>(R.id.btnUpdateNow)
        val layoutProgress = view.findViewById<LinearLayout>(R.id.layoutDownloadProgress)
        val pbDownload = view.findViewById<ProgressBar>(R.id.pbUpdateDownload)
        val tvPercent = view.findViewById<TextView>(R.id.tvDownloadPercent)
        val layoutButtons = view.findViewById<LinearLayout>(R.id.layoutUpdateButtons)

        tvVersion.text = "Sürüm: ${updateInfo.latestVersionName}"
        tvChangelog.text = updateInfo.changelog

        val dialog = AlertDialog.Builder(context, R.style.Theme_KZBSODAK)
            .setView(view)
            .setCancelable(!updateInfo.isMandatory)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnLater.setOnClickListener {
            dialog.dismiss()
        }

        btnNow.setOnClickListener {
            layoutProgress.visibility = View.VISIBLE
            layoutButtons.visibility = View.GONE
            btnClose.visibility = View.GONE
            downloadAndInstallApk(updateInfo.apkUrl, pbDownload, tvPercent, dialog)
        }

        dialog.show()
    }

    private fun downloadAndInstallApk(
        apkUrl: String,
        progressBar: ProgressBar,
        tvPercent: TextView,
        dialog: AlertDialog
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(apkUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connect()

                val fileLength = connection.contentLength
                val apkFile = File(context.cacheDir, "KZBS_ODAK_Update.apk")
                if (apkFile.exists()) apkFile.delete()

                val input: InputStream = connection.inputStream
                val output = FileOutputStream(apkFile)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    if (fileLength > 0) {
                        val progress = ((total * 100) / fileLength).toInt()
                        withContext(Dispatchers.Main) {
                            progressBar.progress = progress
                            tvPercent.text = "%$progress İndiriliyor..."
                        }
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    installApk(apkFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "İndirme başarısız oldu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
            }
        }
    }

    private fun installApk(file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Yükleme başlatılamadı: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
