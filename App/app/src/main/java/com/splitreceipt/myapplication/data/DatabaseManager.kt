package com.splitreceipt.myapplication.data

import android.provider.BaseColumns

object DatabaseManager {

    object AccountTable : BaseColumns {
        const val ACCOUNT_TABLE_NAME = "user_account"
        const val ACCOUNT_ID = BaseColumns._ID
        const val ACCOUNT_COL_DATE = "date"
        const val ACCOUNT_COL_PAID_BY = "paid_by"
        const val ACCOUNT_COL_ITEMS = "items"
        const val ACCOUNT_COL_ITEMS_VALUE = "items_value"
    }

}