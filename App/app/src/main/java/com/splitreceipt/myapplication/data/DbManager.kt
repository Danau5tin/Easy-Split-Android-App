package com.splitreceipt.myapplication.data

import android.provider.BaseColumns

object DbManager {

    object GroupTable : BaseColumns {
        const val GROUP_TABLE_NAME = "user_groups"
        const val GROUP_COL_ID = BaseColumns._ID
        const val GROUP_COL_UNIQUE_ID = "group_unique_id"
        const val GROUP_COL_NAME = "group_name"
        const val GROUP_COL_CATEGORY = "category"
        const val GROUP_COL_PARTICIPANTS = "items"
        const val GROUP_COL_BALANCES = "balances_string"
        const val GROUP_COL_SETTLEMENTS = "settlements"
        const val GROUP_COL_USER = "sql_user"
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
        const val RECEIPT_COL_FK_GROUP_ID = "group_id"
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