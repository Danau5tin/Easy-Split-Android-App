package com.splitreceipt.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.splitreceipt.myapplication.SplitReceiptScanFragment.Companion.errorsCleared
import com.splitreceipt.myapplication.SplitReceiptScanFragment.Companion.ownershipEqualString
import com.splitreceipt.myapplication.data.ScannedItemizedProductData

class ScanTextRecogHelper(var contxt: Context, var itemProductList: ArrayList<ScannedItemizedProductData>,
                          var adapter: NewScannedReceiptRecyclerAdapter) {

    val negWords = arrayOf(
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

    fun runTextRecognition(bitmap: Bitmap, explicitTotal: String, numberOfItems: Int) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val recog = FirebaseVision.getInstance().onDeviceTextRecognizer
        recog.processImage(image).addOnSuccessListener {
            val blocks = it.textBlocks
            val itemsArray: ArrayList<String> = ArrayList()
            val valuesArray: ArrayList<String> = ArrayList()
            val deletedItemArray: ArrayList<String> = ArrayList()
            val deletedValuesArrays: ArrayList<String> = ArrayList()
            val deletedLowerCaseArrays: ArrayList<String> = ArrayList()
            val lonelyCharacters: ArrayList<String> = ArrayList()
            val lonelyDigits: ArrayList<String> = ArrayList()
            if (blocks.isEmpty()) {
                Log.i("RECOG", "Failed to get any text")
            } else {
                for (block in blocks) {
                    for (line in block.lines) {

                        if (line.text.isBlank()) {
                            deletedItemArray.add(line.text)
                        }
                        val newLine = line.text.toLowerCase()

                        for (negWord in negWords) {
                            //Step 1: Check if any of the negative words are in the string.
                            for (element in line.text){
                                if (element.toString() == negWord) {
                                    deletedItemArray.add(line.text)
                                }
                            }
                        }

                        if (newLine.contains(".")) {
                            // Step 2 (DIGIT): Check if string is an individual item price
                            if (newLine.length < 6) {
                                valuesArray.add(newLine)
                            }
                        }
                        else {
                            //Step 2 (Item): Check if string is an item
                            if (newLine.equals(line.text)) {
                                //Step 5: Check if the item is all lowercase indicating it is not an item
                                deletedLowerCaseArrays.add(line.text)
                            }

                            itemsArray.add(line.text)
                        }
                    }
                }
            }

            for (delete in deletedLowerCaseArrays){
                itemsArray.remove(delete)
            }
            for (delete in deletedItemArray) {
                itemsArray.remove(delete)
            }
            Log.i("RECOG", "ENTIRE BLOCK ${it.text}")
            Log.i("RECOG", "ITEMS ARRAY $itemsArray")
            Log.i("RECOG", "VALUES ARRAY $valuesArray")
            Log.i("RECOG", "DELETED ITEMS ARRAY $deletedItemArray")
            Log.i("RECOG", "DELETED VALUES ARRAY $deletedValuesArrays")
            Log.i("RECOG", "Lonely Characters List: $lonelyCharacters")
            Log.i("RECOG", "Lonely Digits List: $lonelyDigits")
            Log.i("RECOG", "Lower case words List: $deletedLowerCaseArrays")
            Log.i("RECOG", "--------------------------------------------")

            val finalCheckedItems: ArrayList<String> = ArrayList()
            val finalCheckedValues: ArrayList<String> = ArrayList()

            //Final checks
            for (item in itemsArray) {
                val trimmed = item.trim()
                if (trimmed.length > 4){
                    finalCheckedItems.add(trimmed)
                }

            }

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
                if (finalisedValue.contains("£")){
                    finalisedValue = finalisedValue.replace("£", "")
                }
                if (finalisedValue.contains("$")){
                    finalisedValue = finalisedValue.replace("$", "")
                }
                if (finalisedValue.contains("€")){
                    finalisedValue = finalisedValue.replace("€", "")
                }
                if (finalisedValue[0].equals("0")) {
                    finalisedValue = finalisedValue.removeRange(0,1)
                }

                if (!value.contains(explicitTotal)){
                    finalCheckedValues.add(finalisedValue)
                }
            }

            try {
                // If we can get the number of items then create a sublist to that effect
                Log.i("RECOG", "finalCheItemSize: ${finalCheckedItems.size}")
                Log.i("RECOG", "finalCheValSize: ${finalCheckedValues.size}")
                Log.i("RECOG", "numOfItems: ${numberOfItems}")
                val correctedItems = finalCheckedItems.subList(0, numberOfItems)
                val correctedValues = finalCheckedValues.subList(0, numberOfItems)
                Log.i("RECOG", "correctItems: ${correctedItems.size}")
                Log.i("RECOG", "correctVals: ${correctedValues.size}")
                initializeProductList(correctedItems, correctedValues)
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

    private fun initializeProductList (correctedItems: MutableList<String>, correctedValues: MutableList<String>){
        /*
        Matches the product names with their corresponding values after text recognition
         */
        itemProductList.clear()
        for (x in 0 until correctedItems.size){
            val productName = correctedItems[x]
            val productValue = correctedValues[x]
            val defaultError = false
            val defaultOwnership = ownershipEqualString
            val defaultSql = "-1"
            itemProductList.add(
                ScannedItemizedProductData(productName, productValue,
                defaultError, defaultOwnership, defaultSql)
            )
        }
    }

    fun flagAndRefresh(itemProductList: ArrayList<ScannedItemizedProductData>){
        /*
        Flags any potential issues with the current product list and refreshes adapter
         */
        errorsCleared = true
        for (product in itemProductList){
            val itemName = product.itemName
            val itemValue = product.itemValue
            // Flag any potential errors in the items name
            product.potentialError = itemName.length < 4
            // Flag any potential errors in the value
            if (!product.potentialError){
                val regex = "[0-9]+\\.[0-9][0-9]".toRegex()
                if (itemValue.startsWith(".")){
                    product.potentialError = true
                } else if (itemValue.length <= 2) {
                    product.potentialError = true
                } else product.potentialError = !itemValue.matches(regex)
            }
            if (product.potentialError){
                errorsCleared = false
            }
        }
        adapter.notifyDataSetChanged()
    }
}