package com.splitreceipt.myapplication

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.widget.Toast
import kotlinx.android.synthetic.main.alert_dialog_share_group.*

class ShareGroupHelper(var context: Context, var firebaseGroupId: String) {

    private var shareText: String = "😊 Join our expense group with Easy Split here: " +
            " (https://www.easy-splitapp.com). This is our group code: $firebaseGroupId"
    private val whatsAppPackage = "com.whatsapp"

    private fun showInviteDialog() {
        val diagView = LayoutInflater.from(context).inflate(R.layout.alert_dialog_share_group, null)
        val builder = AlertDialog.Builder(context).setTitle("Share")
            .setView(diagView).show()
        val shareGroupHelper = ShareGroupHelper(context, firebaseGroupId)
        builder.copyLinkButton2.setOnClickListener {
            shareGroupHelper.clipboardShareCopy()
        }
        builder.whatsappShareButton2.setOnClickListener {
            shareGroupHelper.shareViaWhatsapp()
        }
        builder.shareEmailButton2.setOnClickListener {
            shareGroupHelper.shareViaEmail()
        }
        builder.shareContinue.setOnClickListener {
            builder.dismiss()
        }
    }

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