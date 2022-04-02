package com.udacity

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.DownloadManager.STATUS_SUCCESSFUL
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.udacity.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action
    private var selectedSource = DownloadSource.NO_SELECTION

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
            download(selectedSource)
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
        val intent = Intent(applicationContext, DetailActivity::class.java)

        val isSuccess = isDownloadSuccessful()
        intent.putExtra(FILE_NAME_ARG, selectedSource.name)
        intent.putExtra(IS_SUCCESSFUL_ARG, isSuccess)

        pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        action = NotificationCompat.Action(
            R.drawable.ic_assistant_black_24dp,
            "Go to Download Details",
            pendingIntent
        )

        val builder = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_assistant_black_24dp)
            .setContentTitle(
                applicationContext
                    .getString(R.string.notification_title)
            )
            .setContentText(applicationContext.getString(R.string.notification_description))
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

    private fun download(downloadSource: DownloadSource) {
        if (downloadSource == DownloadSource.NO_SELECTION) {
            Toast.makeText(this, getString(R.string.please_select_one_option), Toast.LENGTH_SHORT)
                .show()
            binding.contentMain.customButton.stopLoading()
            return
        }

        val request =
            DownloadManager.Request(Uri.parse(downloadSource.url))
                .setTitle(getString(R.string.app_name))
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.
    }

    fun onRadioButtonClicked(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked
            when (view.getId()) {
                binding.contentMain.glide.id ->
                    if (checked) {
                        selectedSource = DownloadSource.GLIDE
                    }
                binding.contentMain.projectStarter.id ->
                    if (checked) {
                        selectedSource = DownloadSource.PROJECT_STARTER
                    }

                binding.contentMain.retrofit.id ->
                    if (checked) {
                        selectedSource = DownloadSource.RETROFIT
                    }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    companion object {
        private const val CHANNEL_ID = "channelId"
        private const val CHANNEL_NAME = "Download Channel"
        const val FILE_NAME_ARG = "file_name_arg"
        const val IS_SUCCESSFUL_ARG = "is_successful_arg"
    }

    enum class DownloadSource(val url: String) {
        NO_SELECTION(""),
        GLIDE(url = "https://github.com/bumptech/glide"),
        PROJECT_STARTER(
            url = "https://github.com/udacity/nd940-c3-advanced-android" +
                "-programming-project-starter"
        ),
        RETROFIT(url = "https://github.com/square/retrofit"),
    }
}
