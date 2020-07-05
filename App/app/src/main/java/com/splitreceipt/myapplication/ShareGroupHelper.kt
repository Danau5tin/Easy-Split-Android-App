package com.splitreceipt.myapplication

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

class ShareGroupHelper(var context: Context, firebaseGroupId: String) {

    private var shareText: String = "Join our expense group by getting Easy Split" +
            " (https://www.easysplitapp.com) and joining with group code $firebaseGroupId"
    private val whatsAppPackage = "com.whatsapp"

    fun clipboardShareCopy() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("simple text", shareText)
        clipboard.setPrimaryClip(clipData)
        Toast.makeText(context, "Copied.", Toast.LENGTH_SHORT).show()
    }

    fun shareViaEmail() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, shareText)
        intent.type = "text/plain"
        context.startActivity(intent)
    }

    fun shareViaWhatsapp(){
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, shareText)
        intent.type = "text/plain"
        val packageInstalled = isPackageInstalled(whatsAppPackage, context.packageManager)
        if (packageInstalled) {
            intent.`package` = whatsAppPackage
        } else {
            Toast.makeText(context, "whatsApp not installed", Toast.LENGTH_SHORT).show()
        }
        context.startActivity(intent)
    }

    private fun isPackageInstalled(packName: String, packMan: PackageManager) : Boolean{
        try {
            packMan.getPackageInfo(packName, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }
}