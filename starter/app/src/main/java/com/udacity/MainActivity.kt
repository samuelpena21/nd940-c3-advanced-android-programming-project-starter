package com.udacity

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.DownloadManager.STATUS_SUCCESSFUL
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.udacity.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        initializeNotification()
        createNotificationChannel()

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        binding.contentMain.customButton.setOnClickListener {
            when (getRadioButtonSelectedSource()) {
                DownloadSource.NO_SELECTION -> {
                    Toast.makeText(
                        this,
                        getString(R.string.please_select_one_option),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    binding.contentMain.customButton.stopLoading()
                }
                DownloadSource.ENTERED_TEXT -> {
                    val textUrl = binding.contentMain.urlTv.text.toString()
                    if (checkIfUrlIsValid(textUrl)) {
                        download(textUrl)
                    } else {
                        Toast.makeText(this, "Please enter a valid Url", Toast.LENGTH_LONG).show()
                        binding.contentMain.customButton.stopLoading()
                    }
                }
                else -> {
                    download(getRadioButtonSelectedSource().url)
                }
            }
        }

        binding.contentMain.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when {
                checkedId == binding.contentMain.enterUrl.id -> showTextAnimated()
                binding.contentMain.enterUrl.visibility == View.VISIBLE -> hideTextAnimated()
                else -> binding.contentMain.enterUrl.visibility = View.GONE
            }
        }
    }

    private fun showTextAnimated() {
        binding.contentMain.urlTv.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .setListener(null)
                .alpha(1f)
                .duration = 500L
        }
    }

    private fun hideTextAnimated() {
        binding.contentMain.urlTv.apply {
            alpha = 1f
            animate()
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        visibility = View.GONE
                    }
                })
                .alpha(0f)
                .duration = 500L
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.contentMain.customButton.stopLoading()

            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            createNotification(id?.toInt() ?: 0)
        }
    }

    private fun initializeNotification() {
        notificationManager = ContextCompat.getSystemService(
            applicationContext,
            NotificationManager::class.java
        ) as NotificationManager
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
                .apply {
                    setShowBadge(false)
                }

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = getString(R.string.notification_description)

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun createNotification(notificationId: Int) {
        val detailActivityIntent = Intent(applicationContext, DetailActivity::class.java)

        val isSuccess = isDownloadSuccessful()
        val selectedSourceName = getRadioButtonSelectedSource().title

        detailActivityIntent.putExtra(FILE_NAME_ARG, selectedSourceName)
        detailActivityIntent.putExtra(IS_SUCCESSFUL_ARG, isSuccess)

        pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            detailActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        action = NotificationCompat.Action(
            R.drawable.ic_assistant_black_24dp,
            getString(R.string.go_to_download_details_action),
            pendingIntent
        )

        val builder = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_assistant_black_24dp)
            .setContentTitle(selectedSourceName)
            .setContentText(
                resources.getString(
                    R.string.notification_description,
                    selectedSourceName
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(action)

        notificationManager.notify(notificationId, builder.build())
    }

    @SuppressLint("Range")
    private fun isDownloadSuccessful(): Boolean {
        val query = DownloadManager.Query()
        query.setFilterById(downloadID)
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager

        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            return status == STATUS_SUCCESSFUL
        }

        return false
    }

    private fun download(url: String) {
        if (!hasPermissions()) {
            requestPermissions()
            binding.contentMain.customButton.stopLoading()
            return
        }

        val request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(getRadioButtonSelectedSource().title)
                .setDescription(getString(R.string.app_description))
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    FILE_PATH + getRadioButtonSelectedSource().name + EXTENSION
                )
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.
    }

    private fun getRadioButtonSelectedSource(): DownloadSource {
        return when (binding.contentMain.radioGroup.checkedRadioButtonId) {
            binding.contentMain.glide.id -> DownloadSource.GLIDE
            binding.contentMain.projectStarter.id -> DownloadSource.PROJECT_STARTER
            binding.contentMain.retrofit.id -> DownloadSource.RETROFIT
            binding.contentMain.enterUrl.id -> DownloadSource.ENTERED_TEXT

            else -> DownloadSource.NO_SELECTION
        }
    }

    private fun hasPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(
            this,
            WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(
                            this,
                            getString(R.string.permission_granted),
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.permission_not_granted),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
            }
        }
    }

    private fun checkIfUrlIsValid(url: String): Boolean {
        return Patterns.WEB_URL.matcher(url).matches()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    companion object {
        private const val CHANNEL_ID = "channelId"
        private const val CHANNEL_NAME = "Download Channel"
        private const val FILE_PATH = "/"
        private const val EXTENSION = ".zip"
        private const val PERMISSION_REQUEST_CODE = 1
        const val FILE_NAME_ARG = "file_name_arg"
        const val IS_SUCCESSFUL_ARG = "is_successful_arg"
    }

    enum class DownloadSource(val url: String, val title: String) {
        NO_SELECTION("", ""),
        GLIDE(url = "https://github.com/bumptech/glide", title = "Glide"),
        PROJECT_STARTER(
            url = "https://github.com/udacity/nd940-c3-advanced-android" +
                "-programming-project-starter",
            title = "Project Starter"
        ),
        RETROFIT(url = "https://github.com/square/retrofit", title = "Retrofit"),
        ENTERED_TEXT(url = "", title = "Custom Download"),
    }
}
