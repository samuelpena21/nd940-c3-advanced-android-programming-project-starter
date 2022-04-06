package com.udacity

import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.udacity.databinding.ActivityDetailBinding

private const val OPEN_DOWNLOAD = 3

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val fileName = intent.getStringExtra(MainActivity.FILE_NAME_ARG)
        val isSuccessful = intent.getBooleanExtra(MainActivity.IS_SUCCESSFUL_ARG, false)

        binding.contentDetail.fileNameValue.text = fileName

        if (isSuccessful) {
            binding.contentDetail.statusValue.text = getString(R.string.success)
        } else {
            binding.contentDetail.statusValue.text = getString(R.string.fail)
            binding.contentDetail.statusValue.setTextColor(Color.RED)
        }

        binding.contentDetail.statusValue.text =
            if (isSuccessful) getString(R.string.success) else getString(

                R.string.fail
            )
        binding.contentDetail.backBtn.setOnClickListener {
            finish()
        }
        binding.contentDetail.viewFileBtn.setOnClickListener {
            openDownloadedFile(isSuccessful)
        }

        cancelNotification()
    }

    private fun cancelNotification() {
        val notificationManager = ContextCompat.getSystemService(
            applicationContext,
            NotificationManager::class.java
        ) as NotificationManager
        notificationManager.cancelAll()
    }

    private fun openDownloadedFile(isSuccessful: Boolean) {
        if (!isSuccessful) return

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, OPEN_DOWNLOAD)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OPEN_DOWNLOAD -> {
                val path = data?.data?.path ?: ""
                val uri = Uri.parse(path)
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.setDataAndType(uri, "*/*")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            }
        }
    }
}
