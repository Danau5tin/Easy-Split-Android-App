package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.splitreceipt.myapplication.a_sync_classes.ASyncSaveImage
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

object LocalStorageHelper {

    fun loadImageFromStorage(context: Context, profile: Boolean, fileName: String, extension: String = ".jpg"): Bitmap? {
        val directory: File
        return try {
            directory = if (profile) {
                context.getDir(ASyncSaveImage.profileImageDir, Context.MODE_PRIVATE)
            } else {
                context.getDir(ASyncSaveImage.scannedImageDir, Context.MODE_PRIVATE)
            }
            val f = File(directory, "$fileName$extension")
            val b = BitmapFactory.decodeStream(FileInputStream(f))
            b
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }
}