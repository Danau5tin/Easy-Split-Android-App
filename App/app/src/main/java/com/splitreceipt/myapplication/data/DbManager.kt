package com.splitreceipt.myapplication.data

import android.provider.BaseColumns

object DbManager {

    object AccountTable : BaseColumns {
        const val ACCOUNT_TABLE_NAME = "user_accounts"
        const val ACCOUNT_COL_ID = BaseColumns._ID
        const val ACCOUNT_COL_UNIQUE_ID = "account_unique_id"
        const val ACCOUNT_COL_NAME = "account_name"
        const val ACCOUNT_COL_CATEGORY = "category"
        const val ACCOUNT_COL_PARTICIPANTS = "items"
        const val ACCOUNT_COL_BALANCES = "balances_string"
        const val ACCOUNT_COL_WHO_OWES_WHO = "who_owes_who"
    }

    object ReceiptTable: BaseColumns {
        const val RECEIPT_TABLE_NAME = "receipts"
        const val RECEIPT_COL_ID = BaseColumns._ID
        const val RECEIPT_COL_UNIQUE_ID = "receipt_unique_id"
        const val RECEIPT_COL_DATE = "date"
        const val RECEIPT_COL_TITLE = "title"
        const val RECEIPT_COL_TOTAL = "total_cost"
        const val RECEIPT_COL_PAID_BY = "paid_by"
        const val RECEIPT_COL_CONTRIBUTIONS = "contributions"
        const val RECEIPT_COL_FK_ACCOUNT_ID = "account_id"
    }

    object ReceiptItemsTable: BaseColumns {
        const val ITEMS_TABLE_NAME = "receipt_items"
        const val ITEMS_COL_ID = BaseColumns._ID
        const val ITEMS_COL_NAME = "item_name"
        const val ITEMS_COL_VALUE = "item_value"
        const val ITEMS_COL_WHOME = "for_whome"
        const val ITEMS_COL_FK_RECEIPT_ID = "receipt_id"
    }

}