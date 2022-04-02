package com.udacity

import android.app.NotificationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.udacity.databinding.ActivityDetailBinding

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
        binding.contentDetail.statusValue.text =
            if (isSuccessful) getString(R.string.success) else getString(

                R.string.fail
            )
        binding.contentDetail.okBtn.setOnClickListener {
            finish()
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
}
