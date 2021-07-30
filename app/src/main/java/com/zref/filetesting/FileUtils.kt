package com.zref.filetesting

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

object FileUtils {

    /**
     * Read and Write private files
     */
    object Private {
        fun write(
            context: Context,
            directory: String,
            fileName: String,
            fileContent: String
        ) {
            val rootDir = context.getDir("", Context.MODE_PRIVATE)
            val fileDir = File(rootDir, directory)
            fileDir.mkdirs()
            val file = File(fileDir, fileName)
            FileOutputStream(file).use {
                it.write(fileContent.toByteArray())
            }
        }

        fun isExists(context: Context, directory: String, fileName: String): Boolean {
            val rootDir = context.getDir("", AppCompatActivity.MODE_PRIVATE)
            val fileDir = File(rootDir, directory)
            val file = File(fileDir, fileName)
            return file.exists()
        }

        fun read(context: Context, directory: String, fileName: String): String {
            val rootDir = context.getDir("", AppCompatActivity.MODE_PRIVATE)
            val fileDir = File(rootDir, directory)
            FileInputStream(File(fileDir, fileName)).bufferedReader().useLines { lines ->
                return lines.joinToString()
            }
        }
    }

    /**
     * Read and Write public files
     */
    object Public {
        /**
         * [fileName] should not have extension in it. The extension will be automatically
         * added regarding to [type]
         */
        fun write(
            context: Context,
            directory: String,
            fileName: String,
            type: String,
            content: String
        ) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeAndroidQ(context, directory, fileName, type, content)
            } else {
                writeBelowAndroidQ(directory, fileName, type, content)
            }

        }

        /**
         * [fileName] should not have extension in it. The extension will be automatically
         * added regarding to [type]
         */
        private fun writeAndroidQ(
            context: Context,
            directory: String,
            fileName: String,
            type: String,
            content: String
        ) {
            //must include "/" in end
            val newDir = if (directory.last() == '/') directory else "$directory/"

            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            values.put(MediaStore.MediaColumns.MIME_TYPE, type)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, newDir)
            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                values
            )!!
            context.contentResolver.openOutputStream(uri).use {
                it?.write(content.toByteArray())
            }
        }

        /**
         * check whether there is existing file
         * if file is exists, it will return the uri to be used with [overWritePublic]
         *
         * [fileName] should not have extension in it. The extension will be automatically
         * added regarding to [type]
         */
        fun find(
            context: Context,
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
            context.contentResolver.query(contentUri, null, selection, selectionArgs, null).use {
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
         * We can get [existingUri] with [findPublic] function
         */
        fun overWrite(context: Context, existingUri: Uri, content: String) {
            context.contentResolver.openOutputStream(existingUri, "rwt")?.use {
                it.write(content.toByteArray())
            }
        }

        /**
         * [fileName] should not have extension in it. The extension will be automatically
         * added regarding to [type]
         */
        private fun writeBelowAndroidQ(
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
            FileOutputStream(file).use {
                it.write(content.toByteArray())
            }
        }

        /**
         * [fileName] should not have extension in it. The extension will be automatically
         * added regarding to [type]
         */
        fun read(
            context: Context,
            directory: String,
            fileName: String,
            type: String
        ): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                readBelowAndroidQ(directory, fileName, type)
            } else {
                readAboveAndroidQ(context, directory, fileName, type)
            }
        }

        /**
         * [fileName] should not have extension in it. The extension will be automatically
         * added regarding to [type]
         */
        private fun readBelowAndroidQ(directory: String, fileName: String, type: String): String {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
            val newFName = if (fileName.endsWith(extension!!)) fileName else "$fileName.$extension"

            val folder = File(Environment.getExternalStorageDirectory(), directory)
            folder.mkdirs()

            val file = File(folder, newFName)
            FileInputStream(file).bufferedReader().useLines { lines ->
                return lines.joinToString()
            }
        }

        /**
         * [fileName] should not have extension in it. The extension will be automatically
         * added regarding to [type]
         */
        private fun readAboveAndroidQ(
            context: Context,
            directory: String,
            fileName: String,
            type: String
        ): String? {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
            val newFName = if (fileName.endsWith(extension!!)) fileName else "$fileName.$extension"

            val contentUri = MediaStore.Files.getContentUri("external")
            val selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?"
            val selectionArgs = arrayOf(directory)

            context.contentResolver.query(contentUri, null, selection, selectionArgs, null)
                .use { cursor ->
                    var uri: Uri? = null
                    if (cursor!!.count != 0) {
                        while (cursor.moveToNext()) {
                            val findName =
                                cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
                            if (findName == newFName) {
                                val id =
                                    cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))
                                uri = ContentUris.withAppendedId(contentUri, id)
                                break
                            }
                        }
                        if (uri != null) {
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val size: Int = inputStream!!.available()
                            val bytes = ByteArray(size)
                            inputStream.read(bytes)
                            inputStream.close()
                            return String(bytes, StandardCharsets.UTF_8)
                        }
                    }
                }
            return null
        }
    }
}