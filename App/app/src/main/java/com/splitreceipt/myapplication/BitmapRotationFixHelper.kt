package com.splitreceipt.myapplication

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi

class BitmapRotationFixHelper {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun rotateBitmap(
        context: Context?,
        photoUri: Uri?,
        bitmap: Bitmap
    ): Bitmap? {
        var newBbitmap = bitmap
        val orientation = getOrientation(context!!, photoUri!!)
        if (orientation <= 0) {
            return newBbitmap
        }
        val matrix = Matrix()
        matrix.postRotate(orientation.toFloat())
        newBbitmap =
            Bitmap.createBitmap(newBbitmap, 0, 0, newBbitmap.width, newBbitmap.height, matrix, false)
        return newBbitmap
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getOrientation(
        context: Context,
        photoUri: Uri
    ): Int {
        val cursor: Cursor? = context.contentResolver.query(
            photoUri,
            arrayOf(MediaStore.Images.ImageColumns.ORIENTATION),
            null,
            null,
            null
        )
        if (cursor?.getCount() != 1) {
            cursor?.close()
            return -1
        }
        cursor.moveToFirst()
        val orientation: Int = cursor.getInt(0)
        cursor.close()
        return orientation
    }


}