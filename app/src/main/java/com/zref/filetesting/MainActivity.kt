package com.zref.filetesting

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets


class MainActivity : AppCompatActivity() {
    private val REQUEST_PERMISSION = 2462
    private val DIRECTORY = Environment.DIRECTORY_DOCUMENTS + "/ZihadTestFile/"
    private val TYPE = "text/plain"
    private val FILE_NAME = "file-nganu"
    private val CONTENT = "Halo Zihad, ini adalah konten public file"
    private val CONTENT_OVERWRITE = "Halo Zihad, ini adalah konten public file setelah dioverwrite"

    private lateinit var textView: TextView
    private val textBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)

        proceedPrivateFile()
        proceedPublicFile()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                log("Permission to write public file is granted")
                val existingFile = FileUtils.Public.find(this, DIRECTORY, FILE_NAME, TYPE)
                if (existingFile != null) {
                    FileUtils.Public.overWrite(this, existingFile, CONTENT_OVERWRITE)
                } else {
                    FileUtils.Public.write(this, DIRECTORY, FILE_NAME, TYPE, CONTENT)
                }
                log(FileUtils.Public.read(this, DIRECTORY, FILE_NAME, TYPE))
            } else {
                log("Cannot write public file due to permission issue")
            }
        }
    }

    private fun proceedPublicFile() {
        if (haveWritePublicPermission()) {
            val existingFile = FileUtils.Public.find(this, DIRECTORY, FILE_NAME, TYPE)
            if (existingFile != null) {
                FileUtils.Public.overWrite(this, existingFile, CONTENT_OVERWRITE)
            } else {
                FileUtils.Public.write(this, DIRECTORY, FILE_NAME, TYPE, CONTENT)
            }
            log(FileUtils.Public.read(this, DIRECTORY, FILE_NAME, TYPE))
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION
            )
        }
    }

    private fun haveWritePublicPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun proceedPrivateFile() {
        val directory = "Zihad/TestFile/SubFolder"
        val fileName = "TestHello.txt"
        val fileContent = "Halo Zihad, ini adalah konten private file"
        FileUtils.Private.write(this, directory, fileName, fileContent)
        log(FileUtils.Private.read(this, directory, fileName))
    }

    private fun log(`object`: Any?) {
        if (textBuilder.isEmpty()) {
            textBuilder.append(`object`.toString())
        } else {
            textBuilder.append("\n\n${`object`}")
        }
        textView.text = textBuilder.toString()
    }
}