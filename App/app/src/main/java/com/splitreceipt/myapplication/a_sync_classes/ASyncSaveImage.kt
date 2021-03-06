package com.splitreceipt.myapplication.a_sync_classes

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.AsyncTask
import com.splitreceipt.myapplication.NewGroupCreationActivity
import java.io.*

class ASyncSaveImage(private var profileImage: Boolean, private var context: Context,
                     private var filename: String, private var extension: String=".jpg") :
    AsyncTask<Bitmap, Void, String>() {


    private lateinit var directory: File

    companion object {
        const val profileImageDir: String = "profileImageDir"
    }

    override fun doInBackground(vararg params: Bitmap?): String {
        val cw = ContextWrapper(context)
        val bitmap = params[0]
        // /data/data/com.splitreceipt.myapplication/app_imageDir
        directory = cw.getDir(profileImageDir, Context.MODE_PRIVATE)

        // Create imageDir
        val myPath = File(directory, "$filename$extension")
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(myPath)
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        NewGroupCreationActivity.profileImageSavedLocally = true
        return directory.absolutePath
    }

}