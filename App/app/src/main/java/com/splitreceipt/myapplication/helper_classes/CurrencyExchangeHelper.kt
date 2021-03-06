package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import android.util.Log
import com.splitreceipt.myapplication.CurrencySelectorActivity
import com.splitreceipt.myapplication.ExpenseOverviewActivity
import com.splitreceipt.myapplication.data.CurrencyUiData
import com.splitreceipt.myapplication.managers.SharedPrefManager

object CurrencyExchangeHelper {

    private var baseCurrencyCode = ExpenseOverviewActivity.currentGroupBaseCurrency!!
    const val EXCHANGE_RATE_OF_1: Float = 1.0F

    fun retrieveExchangeRate(expenseCurrencyCode: String, priorExchangeRate: Float?, sqlDbHelper: SqlDbHelper) : Float{
        return if (expenseCurrencyCode != baseCurrencyCode) {
            val exchangeRate: Float
            if (userIsNotEditingExpense(priorExchangeRate)) {
                exchangeRate = sqlDbHelper.retrieveExchangeRate(baseCurrencyCode, expenseCurrencyCode)
                Log.i("Currency", "Retrieved exchange rate for $expenseCurrencyCode is: $exchangeRate")
            } else {
                exchangeRate = priorExchangeRate!!
                Log.i("Currency", "Prior exchange rate: $priorExchangeRate")
            }
            exchangeRate
        } else {
            EXCHANGE_RATE_OF_1
        }
    }

    private fun userIsNotEditingExpense(priorExchangeRate: Float?) : Boolean {
        return priorExchangeRate == null
    }

    fun reverseFromBaseToExpenseCurrency(exchangeRate: Float, baseContribution: Float) : Float {
        return if (exchangeRate == EXCHANGE_RATE_OF_1) {
            baseContribution
        } else {
            baseContribution * exchangeRate
        }
    }

    fun quickExchange(exchangeRate: Float, value: Float) : Float {
        return if (exchangeRate == EXCHANGE_RATE_OF_1) {
            value
        } else {
            value / exchangeRate
        }
    }

    fun returnUiSymbol(countryCode: String) : String{
        for (currency in currencyArray) {
            if (countryCode == currency.countryCode) {
                return currency.currencyUiSymbol
            }
        }
        return "$"
    }

    fun saveRecentCurrencySharedPref(context: Context, currencyCode: String, currencySymbol: String) {
        val sharedPreferences = context.getSharedPreferences(SharedPrefManager.SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val edit = sharedPreferences.edit()
        edit.putString(SharedPrefManager.SHARED_PREF_GROUP_CURRENCY_CODE, currencyCode)
        edit.putString(SharedPrefManager.SHARED_PREF_GROUP_CURRENCY_SYMBOL, currencySymbol)
        edit.apply()
    }

    data class CurrencyDetails(var currencyCode: String, var currencySymbol: String, var exchangeRate: Float)

    val currencyArray = arrayOf<CurrencyUiData>(
        CurrencyUiData("AED", "United Arab Emirates Dirham"),
        CurrencyUiData("AFN", "Afghan Afghani"),
        CurrencyUiData("ALL", "Albanian Lek"),
        CurrencyUiData("AMD", "Armenian Dram"),
        CurrencyUiData("ANG", "Netherlands Antillean Guilder"),
        CurrencyUiData("AOA", "Angolan Kwanza"),
        CurrencyUiData("ARS", "Argentine Peso", "$", "ARS$"),
        CurrencyUiData("AUD","Australian Dollar", "$", "A$"),
        CurrencyUiData("AWG", "Aruban florin"),
        CurrencyUiData("AZN", "Azerbaijani manat"),
        CurrencyUiData("BAM", "Bosnia and Herzegovina convertible mark"),
        CurrencyUiData("BBD", "Barbados Dollar", "$", "BDS$"),
        CurrencyUiData("BDT", "Bangladeshi taka"),
        CurrencyUiData("BGN", "Bulgarian lev"),
        CurrencyUiData("BHD", "Bahraini dinar"),
        CurrencyUiData("BIF", "Burundian franc"),
        CurrencyUiData("BMD", "Bermuda Dollar", "S", "BMD$"),
        CurrencyUiData("BND", "Brunei dollar", "$", "B$"),
        CurrencyUiData("BOB", "Bolivian boliviano"),
        CurrencyUiData("BRL", "Brazilian real", "$", "R$"),
        CurrencyUiData("BSD", "Bahamian dollar", "$", "B$"),
        CurrencyUiData("BTC", "Bitcoin"),
        CurrencyUiData("BTN", "Bhutanese ngultrum"),
        CurrencyUiData("BWP", "Botswana pula", "P"),
        CurrencyUiData("BYN", "Belarusian ruble"),
        CurrencyUiData("BZD", "Belize dollar", "$", "BZD$"),
        CurrencyUiData("CAD", "Canadian Dollar", "$", "CA$"),
        CurrencyUiData("CDF", "Congolese franc", "FC"),
        CurrencyUiData("CHF", "Swiss franc"),
        CurrencyUiData("CLF", "Chilean Unidad de Fomento"),
        CurrencyUiData("CLP", "Chilean peso", "$", "CLP$"),
        CurrencyUiData("CNY", "Renminbi"),
        CurrencyUiData("COP", "Colombian peso", "$", "COP$"),
        CurrencyUiData("CRC", "Costa Rican colón"),
        CurrencyUiData("CUC", "Cuban convertible peso", "$", "CUC$"),
        CurrencyUiData("CUP", "Cuban peso", "$", "CUP$"),
        CurrencyUiData("CVE", "Cape Verdean escudo"),
        CurrencyUiData("CZK", "Czech koruna"),
        CurrencyUiData("DJF", "Djiboutian franc", "Fdj"),
        CurrencyUiData("DKK", "Danish krone", "Kr"),
        CurrencyUiData("DOP", "Dominican peso", "$", "DOP$"),
        CurrencyUiData("DZD", "Algerian dinar"),
        CurrencyUiData("EGP", "Egyptian pound"),
        CurrencyUiData("ERN", "Eritrean nakfa"),
        CurrencyUiData("ETB", "Ethiopian birr"),
        CurrencyUiData("EUR", "Euro", "€"),
        CurrencyUiData("FJD", "Fijian dollar", "$", "FJ$"),
        CurrencyUiData("FKP", "Falkland Islands pound", "£"),
        CurrencyUiData( "GBP", "Great British Pound","£"),
        CurrencyUiData("GEL", "Georgian lari"),
        CurrencyUiData("GGP", "Guernsey Pound", "£"),
        CurrencyUiData("GHS", "Ghanaian cedi"),
        CurrencyUiData("GIP", "Gibraltar pound", "£"),
        CurrencyUiData("GMD", "Gambian dalasi", "D"),
        CurrencyUiData("GNF", "Guinean franc", "Fr"),
        CurrencyUiData("GTQ", "Guatemalan quetzal", "Q"),
        CurrencyUiData("GYD", "Guyanese dollar", "$", "GYD$"),
        CurrencyUiData("HKD", "Hong Kong dollar", "$", "HKD$"),
        CurrencyUiData("HNL", "Honduran lempira", "L"),
        CurrencyUiData("HRK", "Croatian kuna", "kn"),
        CurrencyUiData("HTG", "Haitian gourde", "G"),
        CurrencyUiData("HUF", "Hungarian forint", "Ft"),
        CurrencyUiData("IDR", "Indonesian rupiah", "Rp"),
        CurrencyUiData("ILS", "Israeli Shekel"),
        CurrencyUiData("IMP", "Manx pound", "£"),
        CurrencyUiData("INR", "Indian rupee"),
        CurrencyUiData("IQD", "Iraqi dinar"),
        CurrencyUiData("IRR", "Iranian rial"),
        CurrencyUiData("ISK", "Icelandic krona", "kr"),
        CurrencyUiData("JEP", "Jersey pound", "£"),
        CurrencyUiData("JMD", "Jamaican dollar", "$", "JMD$"),
        CurrencyUiData("JOD", "Jordanian dinar"),
        CurrencyUiData("JPY", "Japanese yen", "Y"),
        CurrencyUiData("KES", "Kenyan shilling", "Ksh"),
        CurrencyUiData("KGS", "Kyrgyzstani som"),
        CurrencyUiData("KHR", "Cambodian riel"),
        CurrencyUiData("KMF", "Comorian franc", "CF"),
        CurrencyUiData("KPW", "North Korean won", "W"),
        CurrencyUiData("KRW", "South Korean won", "W"),
        CurrencyUiData("KWD", "Kuwaiti dinar", "KD"),
        CurrencyUiData("KYD", "Cayman Islands dollar", "$", "KYD$"),
        CurrencyUiData("KZT", "Kazakhstani tenge", "T"),
        CurrencyUiData("LAK", "Lao kip", "K"),
        CurrencyUiData("LBP", "Lebanese pound"),
        CurrencyUiData("LKR", "Sri Lankan rupee", "Rs"),
        CurrencyUiData("LRD", "Liberian dollar", "$", "L$"),
        CurrencyUiData("LSL", "Lesotho loti"),
        CurrencyUiData("LTL", "Lithuanian litas"),
        CurrencyUiData("LVL", "Latvian lats"),
        CurrencyUiData("LYD", "Libyan dinar", "LD"),
        CurrencyUiData("MAD", "Moroccan dirham"),
        CurrencyUiData("MDL","Moldovan leu", "L"),
        CurrencyUiData("MGA", "Malagasy ariary", "Ar"),
        CurrencyUiData("MKD", "Macedonian denar"),
        CurrencyUiData("MMK", "Burmese kyat", "K"),
        CurrencyUiData("MNT", "Mongolian tögrög"),
        CurrencyUiData("MOP", "Macanese Pataca"),
        CurrencyUiData("MRO", "Mauritanian ouguiya"),
        CurrencyUiData("MUR", "Mauritian rupee", "Rs"),
        CurrencyUiData("MVR", "Maldivian rufiyaa"),
        CurrencyUiData("MWK", "Malawian kwacha", "MK"),
        CurrencyUiData("MXN", "Mexican peso", "Mex$"),
        CurrencyUiData("MYR", "Malaysian ringgit", "RM"),
        CurrencyUiData("MZN", "Mozambican metical", "MT"),
        CurrencyUiData("NAD", "Namibian dollar", "N$"),
        CurrencyUiData("NGN", "Nigerian naira", "N"),
        CurrencyUiData("NIO", "Nicaraguan córdoba", "$", "C$"),
        CurrencyUiData("NOK", "Norwegian krone", "kr"),
        CurrencyUiData("NPR", "Nepalese rupee"),
        CurrencyUiData("NZD", "New Zealand dollar", "S", "NZD$"),
        CurrencyUiData("OMR", "Omani rial"),
        CurrencyUiData("PAB", "Panamanian balboa", "B/."),
        CurrencyUiData("PEN", "Sol", "S/"),
        CurrencyUiData("PGK", "Papua New Guinean kina", "K"),
        CurrencyUiData("PHP", "Philippine peso", "P"),
        CurrencyUiData("PKR", "Pakistani rupee", "Rs"),
        CurrencyUiData("PLN", "Polish złoty", "zl"),
        CurrencyUiData("PYG", "Paraguayan guaraní", "G"),
        CurrencyUiData("QAR", "Qatari riyal"),
        CurrencyUiData("RON", "Romanian leu", "lei"),
        CurrencyUiData("RSD", "Serbian dinar", "din"),
        CurrencyUiData("RUB", " Russian ruble", "P"),
        CurrencyUiData("RWF", "Rwandan franc", "RF"),
        CurrencyUiData("SAR", "v", "SR"),
        CurrencyUiData("SBD", "Solomon Islands dollar", "$", "Si$"),
        CurrencyUiData("SCR", "Seychellois rupee", "SR"),
        CurrencyUiData("SDG", "Sudanese pound"),
        CurrencyUiData("SEK", "Swedish krona", "kr"),
        CurrencyUiData("SGD", "Singapore dollar", "S$"),
        CurrencyUiData("SHP", "St Helena Pound", "£"),
        CurrencyUiData("SLL", "Sierra Leonean leone", "Le"),
        CurrencyUiData("SOS", "Somali shilling", "Sh.so"),
        CurrencyUiData("SRD", "Suriname Dollar", "$", "SRD$"),
        CurrencyUiData("STD", "São Tomé and Príncipe dobra", "Db"),
        CurrencyUiData("SYP", "Syrian pound", "LS"),
        CurrencyUiData("SZL", "Swazi lilangeni", "E"),
        CurrencyUiData("THB", "Thai baht"),
        CurrencyUiData("TJS", "Tajikistani somoni", "SM"),
        CurrencyUiData("TMT", "Turkmenistan manat", "T"),
        CurrencyUiData("TND", "Tunisian dinar"),
        CurrencyUiData("TRY", "Turkish lira"),
        CurrencyUiData("TTD", "Trinidad and Tobago dollar", "$","TT$"),
        CurrencyUiData("TWD", "New Taiwan dollar", "$", "NTS"),
        CurrencyUiData("TZS", "Tanzanian shilling", "TSh"),
        CurrencyUiData("UAH", "Ukrainian hryvnia"),
        CurrencyUiData("UGX", "Ugandan shilling", "USh"),
        CurrencyUiData("USD", "US Dollar", "$"),
        CurrencyUiData("UYU", "Peso Uruguayo", "$", "UYU$"),
        CurrencyUiData("UZS", "Uzbekistani soʻm"),
        CurrencyUiData("VEF", "Venezuelan bolívar"),
        CurrencyUiData("VND", "Vietnamese dong"),
        CurrencyUiData("VUV", "Vanuatu vatu", "VT"),
        CurrencyUiData("WST", "Samoan tala", "SAT"),
        CurrencyUiData("XAF", "Central African CFA franc", "FCFA"),
        CurrencyUiData("XCD", "Eastern Caribbean dollar", "$", "XCD$"),
        CurrencyUiData("XOF", "West African CFA franc", "CFA"),
        CurrencyUiData("XPF", "CFP franc", "F"),
        CurrencyUiData("YER", "Yemeni rial"),
        CurrencyUiData("ZAR", "South African rand"),
        CurrencyUiData("ZMK","Zambian kwacha"),
        CurrencyUiData("ZWL", "Zimbabwean dollar", "$", "ZWL$")
    )
}