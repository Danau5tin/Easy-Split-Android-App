package com.splitreceipt.myapplication

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.splitreceipt.myapplication.data.DbHelper
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_COL_ID
import com.splitreceipt.myapplication.data.DbManager.GroupTable.GROUP_TABLE_NAME

class GroupSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_settings)
    }

    fun deleteGroupButton(view: View) {
        AlertDialog.Builder(this).apply {
            setIcon(R.drawable.vector_warning_yellow)
            setTitle("Are you ABSOLUTELY sure?")
            setMessage("This group will be deleted for ALL users involved, not just yourself.")
            setPositiveButton("Yes delete", object: DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    val dbHelper = DbHelper(context)
                    val write = dbHelper.writableDatabase
                    val whereClause = "$GROUP_COL_ID = ?"
                    val whereArgs = arrayOf(ReceiptOverviewActivity.getSqlGroupId)
                    write.delete(GROUP_TABLE_NAME, whereClause, whereArgs)
                    dbHelper.close()

                    Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                    onBackPressed()
                }
            })
            setNegativeButton("No, cancel", object: DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog?.cancel()
                }
            })
        }.show()
    }
}
