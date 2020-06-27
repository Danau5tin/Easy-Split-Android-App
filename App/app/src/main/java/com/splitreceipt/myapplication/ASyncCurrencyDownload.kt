package com.splitreceipt.myapplication

import android.os.AsyncTask
import android.util.Log
import com.splitreceipt.myapplication.data.CurrencyData
import com.splitreceipt.myapplication.data.SqlDbHelper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL

class ASyncCurrencyDownload(var sqlDbHelper: SqlDbHelper) : AsyncTask<String, Void, String>() {

    /*
    This class uses sqlHelper class to check if the base currency (As selected by user) exists in the database.
    If not exists then call fixer api and insert into Sql. If does exist and has not been updated in more than 7 days then update.
     */

    private val key = "1b856d9a3cf501d5fd35b7f8fc9d8c20"
    private var api = "https://data.fixer.io/api/latest?access_key=$key&base="
    private var currencyLastUpdate: Long = 0
    private var daysSinceUpdate = 8.0F

    override fun doInBackground(vararg params: String?): String {
        val currencyCode = params[0]

        currencyLastUpdate = sqlDbHelper.checkIfCurrencyExists(currencyCode!!)
        if (currencyLastUpdate != SqlDbHelper.CURRENCY_NON_EXISTENT){
            val timestampNow = System.currentTimeMillis()
            val milliSecsSinceUpdate = (timestampNow - currencyLastUpdate).toFloat()
            val secsSinceUpdate = milliSecsSinceUpdate / 1000
            val minsSinceUpdate = secsSinceUpdate / 60
            val hoursSinceUpdate = minsSinceUpdate / 60
            daysSinceUpdate = hoursSinceUpdate / 24
        }
        Log.i("Currency", "days since update $daysSinceUpdate")
        if (daysSinceUpdate > 7.0F) {
            api = "$api$currencyCode"
            val url = URL(api)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()
            val response = connection.responseCode
            if (response == 200) {
                Log.i("Currency", "API response 200")
                val inputStream = connection.inputStream
                val stringBuilder = StringBuilder()
                if (inputStream != null) {
                    val inputStreamReader = InputStreamReader(inputStream)
                    val bufferedReader = BufferedReader(inputStreamReader)
                    var nextLine = bufferedReader.readLine()
                    while (nextLine != null) {
                        stringBuilder.append(nextLine)
                        nextLine = bufferedReader.readLine()
                    }
                    return stringBuilder.toString()
                }
            }
            return ""
        }
        else return ""
    }

    override fun onPostExecute(result: String?) {
        if (result != "") {
            val jsonObject = JSONObject(result!!)
            val base = jsonObject.getString("base")
            val rates = jsonObject.getJSONObject("rates")
            val keys = rates.keys()
            val currencyList: ArrayList<CurrencyData> = ArrayList()
            val timestamp = System.currentTimeMillis()
            while (keys.hasNext()) {
                val currencyCode = keys.next()
                val currencyRate = rates.getDouble(currencyCode).toFloat()
                currencyList.add(CurrencyData(currencyCode, currencyRate))
            }
            if (currencyLastUpdate != SqlDbHelper.CURRENCY_NON_EXISTENT){
                sqlDbHelper.updateCurrency(base, currencyList, timestamp)
                Log.i("Currency", "Updated already existing base currency $base")
            }
            else {
                sqlDbHelper.insertCurrency(base, currencyList, timestamp)
                Log.i("Currency", "Inserted new base currency $base")
            }
        }
    }
}