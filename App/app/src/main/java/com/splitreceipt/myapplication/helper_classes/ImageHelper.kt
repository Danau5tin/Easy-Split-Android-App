package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.splitreceipt.myapplication.ExpenseOverviewActivity
import com.splitreceipt.myapplication.a_sync_classes.ASyncSaveImage
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

object ImageHelper {

    fun handleNewImage(context: Context, uri: Uri, view: de.hdodenhof.circleimageview.CircleImageView) {
        // Able to set a new image. Fix rotation issues if present. Upload to Firebase. Save locally.
        view.setImageURI(uri)
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val newBitmap: Bitmap
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            newBitmap = rotateBitmap(context, uri, bitmap)!!
        } else {
            newBitmap = bitmap
        }
        ExpenseOverviewActivity.firebaseDbHelper!!.uploadGroupProfileImage(newBitmap)
        val aSyncSaveImage =
            ASyncSaveImage(
                true,
                context,
                ExpenseOverviewActivity.currentGroupFirebaseId!!
            )
        aSyncSaveImage.execute(newBitmap)
        val lastEdit = System.currentTimeMillis().toString()
        ExpenseOverviewActivity.firebaseDbHelper!!.setGroupImageLastEdit(lastEdit)
        SqlDbHelper(context)
            .setLastImageEdit(lastEdit, ExpenseOverviewActivity.currentSqlGroupId)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun rotateBitmap(context: Context?, photoUri: Uri?, bitmap: Bitmap): Bitmap? {
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
    private fun getOrientation(context: Context, photoUri: Uri): Int {
        val cursor: Cursor? = context.contentResolver.query(
            photoUri,
            arrayOf(MediaStore.Images.ImageColumns.ORIENTATION),
            null, null, null)
        if (cursor?.getCount() != 1) {
            cursor?.close()
            return -1
        }
        cursor.moveToFirst()
        val orientation: Int = cursor.getInt(0)
        cursor.close()
        return orientation
    }

    fun loadImageFromStorage(context: Context, fileName: String, extension: String = ".jpg"): Bitmap? {
        val directory: File
        return try {
            directory = context.getDir(ASyncSaveImage.profileImageDir, Context.MODE_PRIVATE)

            val f = File(directory, "$fileName$extension")
            val b = BitmapFactory.decodeStream(FileInputStream(f))
            b
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

}