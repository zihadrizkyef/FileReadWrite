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
    private val CONTENT = "Halo Zihad"
    private val CONTENT_OVERWRITE = "Halo Zihad, di overwrite"

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val existingFile = findPublic(DIRECTORY, FILE_NAME, TYPE)
                    if (existingFile != null) {
                        overWritePublic(existingFile, CONTENT_OVERWRITE)
                    } else {
                        writeNewPublic(DIRECTORY, FILE_NAME, TYPE, CONTENT)
                    }
                    readPublic(DIRECTORY, FILE_NAME, TYPE)
                } else {
                    writePublicOld(DIRECTORY, FILE_NAME, TYPE, CONTENT)
                    readPublicOld(DIRECTORY, FILE_NAME, TYPE)
                }
            } else {
                log("Cannot write public file due to permission issue")
            }
        }
    }

    private fun proceedPublicFile() {
        if (haveWritePublicPermission()) {
            log("Permission to write external is granted")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val existingFile = findPublic(DIRECTORY, FILE_NAME, TYPE)
                if (existingFile != null) {
                    overWritePublic(existingFile, CONTENT_OVERWRITE)
                } else {
                    writeNewPublic(DIRECTORY, FILE_NAME, TYPE, CONTENT)
                }
                readPublic(DIRECTORY, FILE_NAME, TYPE)
            } else {
                writePublicOld(DIRECTORY, FILE_NAME, TYPE, CONTENT)
                readPublicOld(DIRECTORY, FILE_NAME, TYPE)
            }
        } else {
            log("Asking permission...")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION
            )
        }
    }

    /**
     * [fileName] should not have extension in it. The extension will be automatically
     * added regarding to [type]
     */
    private fun writeNewPublic(
        directory: String,
        fileName: String,
        type: String,
        content: String
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
                    it?.write(content.toByteArray())
                }
            } else {
                log("Fail to write public file : Uri null")
            }
        } catch (e: IOException) {
            log("Fail to write public file : ${e.localizedMessage}")
        }
    }

    /**
     * [existingUri] can be obtained with [findPublic] function
     */
    private fun overWritePublic(existingUri: Uri, content: String) {
        try {
            contentResolver.openOutputStream(existingUri, "rwt")?.use {
                it.write(content.toByteArray())
                log("Public file overwritten successfully $existingUri")
            }
        } catch (e: IOException) {
            log("Fail to write public file : ${e.localizedMessage}")
        }
    }

    /**
     * check whether there is existing file
     * if file is exists, it will return the uri to be used with [overWritePublic]
     *
     * [fileName] should not have extension in it. The extension will be automatically
     * added regarding to [type]
     */
    private fun findPublic(
        directory: String,
        fileName: String,
        type: String
    ): Uri? {
        //must include "/" in end
        val newDir = if (directory.last() == '/') directory else "$directory/"

        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        val newFName = if (fileName.endsWith(extension!!)) fileName else "$fileName.$extension"

        val contentUri = MediaStore.Files.getContentUri("external")
        val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
        val selectionArgs = arrayOf(newDir)
        contentResolver.query(contentUri, null, selection, selectionArgs, null).use {
            var uri: Uri? = null
            return if (it!!.count == 0) {
                null
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
                uri
            }
        }
    }

    /**
     * [fileName] should not have extension in it. The extension will be automatically
     * added regarding to [type]
     */
    private fun writePublicOld(
        directory: String,
        fileName: String,
        type: String,
        content: String
    ) {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        val newFName = if (fileName.endsWith(extension!!)) fileName else "$fileName.$extension"

        val folder = File(Environment.getExternalStorageDirectory(), directory)
        folder.mkdirs()

        val file = File(folder, newFName)

        log("Writing public file $file")
        FileOutputStream(file).use {
            it.write(content.toByteArray())
        }
    }

    /**
     * [fileName] should not have extension in it. The extension will be automatically
     * added regarding to [type]
     */
    private fun readPublicOld(directory: String, fileName: String, type: String) {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        val newFName = if (fileName.endsWith(extension!!)) fileName else "$fileName.$extension"

        val folder = File(Environment.getExternalStorageDirectory(), directory)
        folder.mkdirs()

        val file = File(folder, newFName)
        FileInputStream(file).bufferedReader().useLines { lines ->
            log("Reading public file : "+lines.joinToString())
        }
    }

    /**
     * [fileName] should not have extension in it. The extension will be automatically
     * added regarding to [type]
     */
    private fun readPublic(
        directory: String,
        fileName: String,
        type: String
    ) {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        val newFName = if (fileName.endsWith(extension!!)) fileName else "$fileName.$extension"

        val contentUri = MediaStore.Files.getContentUri("external")
        val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
        val selectionArgs = arrayOf(directory)

        contentResolver.query(contentUri, null, selection, selectionArgs, null).use { cursor ->
            var uri: Uri? = null
            if (cursor!!.count == 0) {
                log("Try reading public file at $directory, but file not found")
            } else {
                while (cursor.moveToNext()) {
                    val findName =
                        cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                    if (findName == newFName) {
                        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
                        uri = ContentUris.withAppendedId(contentUri, id)
                        break
                    }
                }
                if (uri == null) {
                    log("Try reading public file at $fileName, but file not found")
                } else {
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val size: Int = inputStream!!.available()
                        val bytes = ByteArray(size)
                        inputStream.read(bytes)
                        inputStream.close()
                        val jsonString = String(bytes, StandardCharsets.UTF_8)
                        log("Reading public file : $jsonString")
                    } catch (e: IOException) {
                        log("Fail to read public file ${e.localizedMessage}")
                    }
                }
            }
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
        val fileContent = "Halo Zihad"
        writePrivate(directory, fileName, fileContent)
        readPrivate(directory, fileName)
    }

    private fun writePrivate(directory: String, fileName: String, fileContent: String) {
        val rootDir = getDir("", MODE_PRIVATE)
        val fileDir = File(rootDir, directory)
        fileDir.mkdirs()
        val file = File(fileDir, fileName)
        log("Writing private file : $file")
        FileOutputStream(file).use {
            it.write(fileContent.toByteArray())
        }
    }

    private fun readPrivate(directory: String, fileName: String) {
        val rootDir = getDir("", MODE_PRIVATE)
        val fileDir = File(rootDir, directory)
        FileInputStream(File(fileDir, fileName)).bufferedReader().useLines { lines ->
            log("Reading private file : " + lines.fold("") { some, text ->
                "$some\n$text"
            }.trim())
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