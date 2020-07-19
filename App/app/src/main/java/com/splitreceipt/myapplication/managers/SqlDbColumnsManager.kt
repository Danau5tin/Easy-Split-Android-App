package com.splitreceipt.myapplication.managers

import android.provider.BaseColumns

object SqlDbColumnsManager {

    object GroupTable : BaseColumns {
        const val GROUP_TABLE_NAME = "user_groups"
        const val GROUP_COL_ID = BaseColumns._ID
        const val GROUP_COL_FIREBASE_ID = "group_unique_id"
        const val GROUP_COL_NAME = "group_name"
        const val GROUP_COL_PARTICIPANTS_LAST_EDIT = "items"
        const val GROUP_COL_SETTLEMENTS = "settlements"
        const val GROUP_COL_USER = "sql_user"
        const val GROUP_COL_LAST_IMAGE_EDIT = "last_image_edit"
        const val GROUP_COL_BASE_CURRENCY = "base_currency"
        const val GROUP_COL_BASE_CURRENCY_UI_SYMBOL = "ui_symbol"
    }

    object ExpenseTable: BaseColumns {
        const val EXPENSE_TABLE_NAME = "expenses"
        const val EXPENSE_COL_ID = BaseColumns._ID
        const val EXPENSE_COL_FIREBASE_ID = "firebase_unique_id"
        const val EXPENSE_COL_DATE = "date"
        const val EXPENSE_COL_TITLE = "title"
        const val EXPENSE_COL_TOTAL = "total_cost"
        const val EXPENSE_COL_PAID_BY = "paid_by"
        const val EXPENSE_COL_CONTRIBUTIONS = "contributions"
        const val EXPENSE_COL_SCANNED = "scanned"
        const val EXPENSE_COL_LAST_EDIT = "last_edit"
        const val EXPENSE_COL_CURRENCY_CODE = "expense_currency"
        const val EXPENSE_COL_UI_SYMBOL = "ui_currency_symbol"
        const val EXPENSE_COL_EXCHANGE_RATE = "exchange_rate"
        const val EXPENSE_COL_FK_GROUP_ID = "group_id"
    }

    object ReceiptItemsTable: BaseColumns {
        const val ITEMS_TABLE_NAME = "receipt_items"
        const val ITEMS_COL_ID = BaseColumns._ID
        const val ITEMS_COL_NAME = "item_name"
        const val ITEMS_COL_VALUE = "item_value"
        const val ITEMS_COL_OWNERSHIP = "ownership"
        const val ITEMS_COL_FK_EXPENSE_ID = "receipt_id"
    }

    object CurrencyTable: BaseColumns {
        const val CURRENCY_TABLE_NAME = "currencies"
        const val CURRENCY_COL_ID = BaseColumns._ID
        const val CURRENCY_COL_CODE = "currency_code"
        const val CURRENCY_COL_BASE = "base_currency"
        const val CURRENCY_COL_RATE = "rate"
        const val CURRENCY_COL_LAST_UPDATE = "last_update"
    }

    object ParticipantTable: BaseColumns {
        const val PARTICIPANT_TABLE_NAME = "participants"
        const val PARTICIPANT_COL_ID= BaseColumns._ID
        const val PARTICIPANT_COL_F_BASE_KEY = "f_base_key"
        const val PARTICIPANT_COL_U_NAME = "user_name"
        const val PARTICIPANT_COL_U_BALANCE = "user_balance"
        const val PARTICIPANTS_COL_FK_GROUP_ID = "group_id"
    }

}