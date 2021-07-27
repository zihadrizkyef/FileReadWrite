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
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.*
import java.nio.charset.StandardCharsets


class MainActivity : AppCompatActivity() {
    private val REQUEST_PERMISSION = 2462
    private val DIRECTORY = Environment.DIRECTORY_DOCUMENTS + "/ZihadTestFile/"
    private val TYPE = "text/plain"
    private val FILE_NAME = "file-nganu"

    private lateinit var textView: TextView
    private val textBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textView)

        //proceedPrivateFile()
        proceedExternalFile()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                log("permission to write external granted")
                replace(DIRECTORY, FILE_NAME, TYPE)
                readExternal(DIRECTORY, FILE_NAME, TYPE)
            } else {
                log("function cannot run properly due to permission issue")
            }
        }
    }

    private fun proceedExternalFile() {
        if (haveWriteExternalPermission()) {
            log("permission to write external granted")
            replace(DIRECTORY, FILE_NAME, TYPE)
            readExternal(DIRECTORY, FILE_NAME, TYPE)
        } else {
            log("asking permission")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION
            )
        }
    }

    private fun writeExternal(
        directory: String,
        fileName: String,
        type: String
    ) {
        //must include "/" in end
        val newDir = if (directory.last() == '/') directory else "$directory/"

        try {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            values.put(MediaStore.MediaColumns.MIME_TYPE, type)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, newDir)
            val uri = contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                values
            )
            if (uri != null) {
                contentResolver.openOutputStream(uri).use {
                    it?.write("Helo efribadih".toByteArray())
                }
                log("File created successfully")
            } else {
                log("Uri null")
            }
        } catch (e: IOException) {
            log("Fail to create file : ${e.localizedMessage}")
        }
    }

    private fun replace(
        directory: String,
        fileName: String,
        type: String
    ) {
        //must include "/" in end
        val newDir = if (directory.last() == '/') directory else "$directory/"

        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        val newFName = if (fileName.endsWith(extension!!)) fileName else fileName+extension

        val contentUri = MediaStore.Files.getContentUri("external")
        val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
        val selectionArgs = arrayOf(newDir)
        contentResolver.query(contentUri, null, selection, selectionArgs, null).use {
            var uri: Uri? = null
            if (it!!.count == 0) {
                log("count 0, existing file not found, write new file| $selection ${selectionArgs.joinToString()}")
                writeExternal(directory, fileName, type)
            } else {
                while (it.moveToNext()) {
                    val found =
                        it.getString(it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                    if (found == newFName) {
                        val id = it.getLong(it.getColumnIndex(MediaStore.MediaColumns._ID))
                        uri = ContentUris.withAppendedId(contentUri, id)
                        break
                    }
                }
                if (uri == null) {
                    log("existing file not found, write new file| $selection ${selectionArgs.joinToString()}")
                    writeExternal(directory, fileName, type)
                } else {
                    try {
                        val outputStream = contentResolver.openOutputStream(uri, "rwt")
                        outputStream!!.write("This is overwritten data".toByteArray())
                        outputStream.close()
                        log("File written successfully")
                    } catch (e: IOException) {
                        log("Fail to write file")
                    }
                }
            }
        }
    }

    private fun deleteExistingExternal(
        directory: String,
        fileName: String,
        type: String
    ) {
        //must include "/" in front and end
        var newDir = directory
        if (newDir.first() != '/') {
            newDir = "/$newDir"
        }
        if (newDir.last() != '/') {
            newDir += "/"
        }

        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        val newFName = if (fileName.endsWith(extension!!)) fileName else fileName+extension

        val contentUri = MediaStore.Files.getContentUri("external")
        val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=? AND " + MediaStore.MediaColumns.DISPLAY_NAME + "=?"
        val selectionArgs = arrayOf(newDir, newFName)
        log("delete "+contentResolver.delete(contentUri, selection, selectionArgs)+" rows")
    }

    private fun readExternal(
        directory: String,
        fileName: String,
        type: String
    ) {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        val newFName = if (fileName.endsWith(extension!!)) fileName else "$fileName.$extension"
        log("find $newFName")

        val contentUri = MediaStore.Files.getContentUri("external")
        val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
        val selectionArgs = arrayOf(directory)

        contentResolver.query(contentUri, null, selection, selectionArgs, null).use { cursor ->
            var uri: Uri? = null
            if (cursor!!.count == 0) {
                log("No file found in $directory")
            } else {
                while (cursor.moveToNext()) {
                    val findName =
                        cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                    log("found $newFName")
                    if (findName == newFName) {
                        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
                        uri = ContentUris.withAppendedId(contentUri, id)
                        break
                    }
                }
                if (uri == null) {
                    log("$fileName not found")
                } else {
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val size: Int = inputStream!!.available()
                        val bytes = ByteArray(size)
                        inputStream.read(bytes)
                        inputStream.close()
                        val jsonString = String(bytes, StandardCharsets.UTF_8)
                        log(jsonString)
                    } catch (e: IOException) {
                        log("Fail to read file")
                    }
                }
            }
        }
    }

    private fun haveWriteExternalPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun proceedPrivateFile() {
        val directory = "Zihad/Anu/Sub"
        val fileName = "test.text"
        val fileContent = "asem belekeke mueeueueue"
        writePrivate(directory, fileName, fileContent)
        log(readPrivate(directory, fileName))
    }

    private fun writePrivate(directory: String, fileName: String, fileContent: String) {
        val rootDir = getDir("", MODE_PRIVATE)
        val fileDir = File(rootDir, directory)
        fileDir.mkdirs()
        val file = File(fileDir, fileName)
        log(file.toString())
        FileOutputStream(file).use {
            it.write(fileContent.toByteArray())
        }
    }

    private fun readPrivate(directory: String, fileName: String): String {
        val rootDir = getDir("", MODE_PRIVATE)
        val fileDir = File(rootDir, directory)
        FileInputStream(File(fileDir, fileName)).bufferedReader().useLines { lines ->
            return lines.fold("") { some, text ->
                "$some\n$text"
            }.trim()
        }
    }

    private fun log(`object`: Any) {
        if (textBuilder.isEmpty()) {
            textBuilder.append(`object`.toString())
        } else {
            textBuilder.append("\n\n${`object`}")
        }
        textView.text = textBuilder.toString()
    }
}