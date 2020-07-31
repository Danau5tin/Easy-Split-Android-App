package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.splitreceipt.myapplication.SplitReceiptScanFragment.Companion.errorsCleared
import com.splitreceipt.myapplication.adapters.NewScannedReceiptRecyclerAdapter
import com.splitreceipt.myapplication.data.ScannedItemizedProductData
import java.util.*
import kotlin.collections.ArrayList

class ScanTextRecogHelper(var contxt: Context, var itemProductList: ArrayList<ScannedItemizedProductData>,
                          var adapter: NewScannedReceiptRecyclerAdapter) {

    private val negWords = arrayOf(
        ":",
        "visa",
        ".com",
        ".co",
        "www",
        "mastercard",
        "master",
        "card",
        "change",
        "vat",
        "rate",
        "net",
        "copy",
        "*",
        "0.00",
        "balance",
        "due",
        "totals",
        "tota",
        "gross"
    )

    private val flaggedItems: ArrayList<String> = ArrayList()
    private val finalCheckedItems: ArrayList<String> = ArrayList()
    private val finalCheckedValues: ArrayList<String> = ArrayList()

    fun runTextRecognition(bitmap: Bitmap, explicitTotal: String, numberOfItems: Int) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val recog = FirebaseVision.getInstance().onDeviceTextRecognizer
        recog.processImage(image).addOnSuccessListener {
            val blocks = it.textBlocks
            val itemsArray: ArrayList<String> = ArrayList()
            val valuesArray: ArrayList<String> = ArrayList()
            if (blocks.isEmpty()) {
                Log.i("RECOG", "Failed to get any text")
            } else {
                for (block in blocks) {
                    for (line in block.lines) {
                        if (line.text.isBlank()) {
                            flaggedItems.add(line.text)
                            itemsArray.add(line.text)
                            continue
                        }
                        val newLine = line.text.toLowerCase(Locale.ROOT)
                        flagAnyNegWords(line)

                        if (textIsLikelyAValue(newLine)) {
                            if (newLine.length < 6) {
                                valuesArray.add(newLine)
                            }
                        }
                        else {
                            if (newLine.equals(line.text)) {
                                //If the text is all lowercase indicating it is not an item
                                flaggedItems.add(line.text)
                            }
                            itemsArray.add(line.text)
                        }
                    }
                }
            }

            for (delete in flaggedItems) {
                itemsArray.remove(delete)
            }
            Log.i("RECOG", "ENTIRE BLOCK ${it.text}")
            Log.i("RECOG", "ITEMS ARRAY $itemsArray")
            Log.i("RECOG", "VALUES ARRAY $valuesArray")
            Log.i("RECOG", "DELETED ITEMS ARRAY $flaggedItems")

            addItemsOverFourChars(itemsArray)
            addValuesAfterKnownStringErrors(valuesArray, explicitTotal)

            try {
                Log.i("RECOG", "finalCheItemSize: ${finalCheckedItems.size}")
                Log.i("RECOG", "finalCheValSize: ${finalCheckedValues.size}")
                Log.i("RECOG", "numOfItems: $numberOfItems")
                val correctedItems = finalCheckedItems.subList(0, numberOfItems)
                val correctedValues = finalCheckedValues.subList(0, numberOfItems)
                Log.i("RECOG", "correctItems: ${correctedItems.size}")
                Log.i("RECOG", "correctVals: ${correctedValues.size}")
                updateProductList(correctedItems, correctedValues)
                flagAndRefresh(itemProductList)
            } catch (exception: IndexOutOfBoundsException) {
                Log.i("RECOG", "Unable to identify all items")
                Toast.makeText(contxt, "FAILED TO RECOGNISE- TRY AGAIN!!", Toast.LENGTH_LONG).show()
            }
        }
            .addOnFailureListener {
                Log.i("RECOG", "Failed completely")
            }
    }

    private fun flagAnyNegWords(line: FirebaseVisionText.Line) {
        for (negWord in negWords) {
            for (element in line.text) {
                if (element.toString() == negWord) {
                    flaggedItems.add(line.text)
                }
            }
        }
    }

    private fun textIsLikelyAValue(newLine: String) = newLine.contains(".")

    private fun addItemsOverFourChars(itemsArray: ArrayList<String>) {
        for (item in itemsArray) {
            val trimmed = item.trim()
            if (trimmed.length > 4) {
                finalCheckedItems.add(trimmed)
            }
        }
    }

    private fun addValuesAfterKnownStringErrors(valuesArray: ArrayList<String>, explicitTotal: String) {
        for (value in valuesArray) {
            var finalisedValue = value
            if (finalisedValue.contains(" ")) {
                finalisedValue = finalisedValue.replace(" ", "")
            }
            if (finalisedValue.contains("o")) {
                finalisedValue = finalisedValue.replace("o", "0")
            }
            if (finalisedValue.contains("c")) {
                finalisedValue = finalisedValue.replace("c", "0")
            }
            if (finalisedValue.contains("£")) {
                finalisedValue = finalisedValue.replace("£", "")
            }
            if (finalisedValue.contains("$")) {
                finalisedValue = finalisedValue.replace("$", "")
            }
            if (finalisedValue.contains("€")) {
                finalisedValue = finalisedValue.replace("€", "")
            }
            if (finalisedValue[0].toString() == "0") {
                finalisedValue = finalisedValue.removeRange(0, 1)
            }

            if (!value.contains(explicitTotal)) {
                finalCheckedValues.add(finalisedValue)
            }
        }
    }


    private fun updateProductList (correctedItems: MutableList<String>, correctedValues: MutableList<String>){
        itemProductList.clear()
        for (x in 0 until correctedItems.size){
            val productName = correctedItems[x]
            val productValue = correctedValues[x]
            itemProductList.add(
                ScannedItemizedProductData(productName, productValue))
        }
    }

    fun flagAndRefresh(itemProductList: ArrayList<ScannedItemizedProductData>){
        errorsCleared = true
        for (product in itemProductList){
            val itemName = product.itemName
            val itemValue = product.itemValue
            flagPotErrorItem(product, itemName)
            flagPotErrorValue(product, itemValue)
            if (product.potentialError){
                errorsCleared = false
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun flagPotErrorItem(product: ScannedItemizedProductData, itemName: String) {
        product.potentialError = itemName.length < 4
    }

    private fun flagPotErrorValue(product: ScannedItemizedProductData, itemValue: String) {
        if (!product.potentialError) {
            val regex = "[0-9]+\\.[0-9][0-9]".toRegex()
            when {
                itemValue.startsWith(".") -> {
                    product.potentialError = true
                }
                itemValue.length <= 2 -> {
                    product.potentialError = true
                }
                else -> product.potentialError = !itemValue.matches(regex)
            }
        }
    }
}