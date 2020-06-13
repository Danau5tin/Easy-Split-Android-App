package com.splitreceipt.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.splitreceipt.myapplication.NewReceiptCreationActivity.Companion.currencyCode
import com.splitreceipt.myapplication.NewReceiptCreationActivity.Companion.currencySymbol
import com.splitreceipt.myapplication.ReceiptOverviewActivity.Companion.roundToTwoDecimalPlace
import com.splitreceipt.myapplication.data.ScannedItemizedProductData
import com.splitreceipt.myapplication.databinding.FragmentSplitReceiptScanBinding
import kotlinx.android.synthetic.main.alert_dialog_scanned_item_confirmation.view.*
import kotlinx.android.synthetic.main.alert_dialog_scanned_product_edit.view.*
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class SplitReceiptScanFragment : Fragment(), NewScannedReceiptRecyclerAdapter.onScannedClick {

    private lateinit var contxt: Context
    private lateinit var binding: FragmentSplitReceiptScanBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var explicitTotalTaxIgnoreList: ArrayList<String>
    private val currencyIntent: Int = 50
    private lateinit var participantList: ArrayList<String>
    private lateinit var adapter: NewScannedReceiptRecyclerAdapter
    private var currentPhotoPath: String = ""
    private var numberOfItemsProvided = false
    private var numberOfItems: Int = 0

    companion object{
        private const val CAMERA_REQ_CODE = 1
        private const val takePictureIntent = 2
        lateinit var itemizedArrayList: ArrayList<ScannedItemizedProductData>
        const val ownershipEqualString = "Equal"
    }

    override fun onAttach(context: Context) {
        contxt = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSplitReceiptScanBinding.inflate(inflater, container, false)
        sharedPreferences = contxt.getSharedPreferences(CurrencySelectorActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE)
        itemizedArrayList = ArrayList()
        participantList = ArrayList()
        explicitTotalTaxIgnoreList = ArrayList()
        updateUICurrency()
        retrieveParticipants()
        adapter = NewScannedReceiptRecyclerAdapter(participantList, itemizedArrayList, this)
        binding.scannedRecy.layoutManager = LinearLayoutManager(contxt)
        binding.scannedRecy.adapter = adapter
        binding.scannedRecy.isNestedScrollingEnabled = false


        binding.currencyAmountScan.addTextChangedListener(object: TextWatcher {
            //TODO: This is copied from other fragment. Can we move this up into the activity and have the two fragments listen for changes from the main activity?
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                val correctNumber: CharSequence
                if (!text.isNullOrBlank()){
                    val allText = text.toString()
                    if (allText.contains(".")){
                        /* If there is a decimal place present: find the index and ensure the user
                         cannot type more than 2 decimal places from that point by taking a substring.
                         Reset the cursor to end of text with setSelection.
                         */
                        val dotIndex = allText.indexOf(".")
                        if (start > (dotIndex + 2)){
                            correctNumber = text.subSequence(0, dotIndex + 3)
                            val correctText = correctNumber.toString()
                            binding.currencyAmountScan.setText(correctText)
                            binding.currencyAmountScan.text?.length?.let {binding.currencyAmountScan.setSelection(it)}
                        }
                    }
                    SplitReceiptManuallyFragment.transactionTotal = text.toString()
                    calculateExplicitDeleteValues()
                }
                else{
                    SplitReceiptManuallyFragment.transactionTotal =
                        NewReceiptCreationActivity.zeroCurrency

                    }}})

        binding.addReceiptImageButton.setOnClickListener {
            checkPermissions()
        }

        binding.currencyButtonScan.setOnClickListener {
            val intent = Intent(contxt, CurrencySelectorActivity::class.java)
            startActivityForResult(intent, currencyIntent)
        }

        return binding.root
    }

    private fun calculateExplicitDeleteValues() {
        /*
        This function discovers the correct taxes for each country so that when the receipt is
         scanned, the algorithm can ignore the tax values and not add them as another product.
         */
        //TODO: Ensure the different taxes are calculated differently for different currencies
        //TODO: Ensure the "GBP" is obtained from a static variable
        val expenseTotalString = binding.currencyAmountScan.text.toString()
        explicitTotalTaxIgnoreList.add(expenseTotalString)
        val expenseTotal = binding.currencyAmountScan.text.toString().toFloat()
        if (currencyCode == "GBP") {
            //TODO: Improve the text recog algorithm for identifying taxes and balances 1) After VAT is seen then any number after this is added to the explicitly deleted list?
            val twentyPercent = roundToTwoDecimalPlace(expenseTotal * 0.20F).toString()
            explicitTotalTaxIgnoreList.add(twentyPercent)
        }
        else {
            //TODO: Add each countries taxes? Likely too manual, maybe the TODO above is a more efficient way. Or maybe just by identifying the number of items it is enough anyway?
        }
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(contxt, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity!!, arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_REQ_CODE)
        } else {
            getImageFromCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQ_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getImageFromCamera()
            }
        }
    }

    private fun getImageFromCamera() {
        //TODO: Provide the user with instructions on how to get the best quality and performance
        //TODO: Check if the user has filled in the receipt total
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(contxt.packageManager)

        val photoFile: File? = try {
            createImageFile()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val photoUri: Uri = FileProvider.getUriForFile(contxt,
            "com.splitreceipt.myapplication.fileprovider",
            photoFile!!)

        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(intent, takePictureIntent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_hhmmss").format(Date())
        val storageDirectory: File? = contxt.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        Log.i("TEXT", storageDirectory.toString())
        return File.createTempFile("image_$timeStamp", ".jpg", storageDirectory).apply {
            currentPhotoPath = this.absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == takePictureIntent) {
            if (resultCode == Activity.RESULT_OK) {
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                val bitmap = BitmapFactory.decodeFile(currentPhotoPath, options)

                val ei = ExifInterface(currentPhotoPath)
                val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                val rotatedBitmap: Bitmap?
                rotatedBitmap = when(orientation){
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90F)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180F)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270F)
                    else -> bitmap
                }

                binding.addReceiptImageButton.setImageBitmap(rotatedBitmap)
                runTextRecognition(rotatedBitmap!!)
            }
        } else if (requestCode == currencyIntent){
            if (resultCode == Activity.RESULT_OK) {
                updateUICurrency()
            }
        }
    }

    private fun updateUICurrency(adapterInitialised: Boolean = false) {
        currencySymbol = sharedPreferences.getString(
            CurrencySelectorActivity.SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL, "$").toString()
        currencyCode = sharedPreferences.getString(
            CurrencySelectorActivity.SHARED_PREF_ACCOUNT_CURRENCY_CODE, "US").toString()
        binding.currencyButtonScan.text = currencyCode
        if (adapterInitialised) {
            //TODO: Update the adapter with the correct currency code
//            binding.fragManualRecy.post(Runnable { adapter.notifyDataSetChanged() })
        }
    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val recog = FirebaseVision.getInstance().onDeviceTextRecognizer
        recog.processImage(image).addOnSuccessListener {
            val blocks = it.textBlocks
            val itemsArray: ArrayList<String> = ArrayList()
            val valuesArray: ArrayList<String> = ArrayList()
            val deletedItemArray: ArrayList<String> = ArrayList()
            val deletedValuesArrays: ArrayList<String> = ArrayList()
            val deletedLowerCaseArrays: ArrayList<String> = ArrayList()
            val explicitDeletedArray: ArrayList<String> = ArrayList()
            val explicitWantedArray: ArrayList<String> = ArrayList()
            val lonelyCharacters: ArrayList<String> = ArrayList()
            val lonelyDigits: ArrayList<String> = ArrayList()
            var runningTotal = 0.0F
            var runningSubTotal = 0.0F
            var runningTax = 0.0F
            val negWords = arrayOf(":", "visa", ".com", "mastercard", "master", "card", "waitrose","change", "vat",
                "rate", "net", "copy", "*", "/", "%", "&", "0.00", "balance", "due", "totals",
                "tota", "gross")
            val wantedWords = arrayOf("items", "itens", "ite")
            if (blocks.isEmpty()) {
                Log.i("RECOG", "Failed to get any text")
            } else {
                for (block in blocks) {
                    for (line in block.lines){
                        var added = false

                        if (line.text.isBlank()) {
                            deletedItemArray.add(line.text)
                            added = true
                        }

                        if (added){
                            break
                        }

                        val newLine = line.text.toLowerCase().trim()

                        for (negWord in negWords) {
                            //Step 1: Check if any of the negative words are in the string.
                            if (newLine.contains(negWord)) {
                                deletedItemArray.add(line.text)
                                added = true
                            }
                        }
                        if (added) {break}

                        if(newLine.contains(".")) {
                            // Step 2 (DIGIT): Check if string is an individual item price
                            try {
                                val valueToFloat = newLine.toFloat()

                                if (valueToFloat == runningTotal || valueToFloat == runningSubTotal || valueToFloat == runningTax) {
                                    // Step 3: Check if we are just trying to add the total instead of an item
                                    deletedValuesArrays.add(newLine)
                                    added = true
                                }

                                if (added) {break}

                                val totalToFloat = runningTotal
                                val newTotal = totalToFloat + valueToFloat
                                runningTax = newTotal * 0.2F
                                val newSubTotal = newTotal - runningTax
                                runningTotal = newTotal
                                runningSubTotal = newSubTotal
                                Log.i("RECOG", "Value: $valueToFloat, Total: $runningTotal, Sub: $runningSubTotal, Tax: $runningTax")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            valuesArray.add(newLine)
                        } else {
                            //Step 2 (Item): Check if string is an item
                            for (element in line.elements) {
                                if (element.text.length == 1) {
                                    //Step 3: Check if any element is just one character
                                    val num = element.text.toIntOrNull()
                                    if (num == null) {
                                        // Character is not a digit and should be deleted
                                        val item = line.text.removeRange(line.text.indexOf(element.text), line.text.indexOf(element.text) + 1)
                                        itemsArray.add(item)
                                        lonelyCharacters.add(element.text)
                                        added = true
                                    } else {
                                        // Character is a lonely digit
                                        lonelyDigits.add(element.text)
                                    }
                                }
                            }

                            for (word in wantedWords) {
                                //Step 4: Check if we can extract the number of receipt items
                                if (newLine.contains(word)) {
                                    explicitWantedArray.add(line.text)
                                    added = true
                                }
                            }

                            if (added) {
                                break
                            }

                            if (newLine.equals(line.text)) {
                                //Step 5: Check if the item is all lowercase indicating it is not an item
                                deletedLowerCaseArrays.add(line.text)
                                added = true
                            }

                            if (added) {
                                break
                            }

                            itemsArray.add(line.text)
                        }
                    }
                }

                for (value in valuesArray) {
                    val stringTotal = runningTotal.toString()
                    val stringSubTotal = runningSubTotal.toString()
                    val stringTax = runningTax.toString()
                    if (value.contains(stringTotal) || value.contains(stringSubTotal) || value.contains(stringTax)) {
                        // Check if the running total has worked and delete any items that match the running total
                        deletedValuesArrays.add(value)
                    }

                }

                for (value in explicitWantedArray) {
                    itemsArray.remove(value)
                }
            }
            Log.i("RECOG", "ENTIRE BLOCK $it.text}")
            Log.i("RECOG", "ITEMS ARRAY $itemsArray")
            Log.i("RECOG", "VALUES ARRAY $valuesArray")
            Log.i("RECOG", "DELETED ITEMS ARRAY $deletedItemArray")
            Log.i("RECOG", "DELETED VALUES ARRAY $deletedValuesArrays")
            Log.i("RECOG", "EXPLICITLY DELETED VALUES ARRAY $explicitDeletedArray")
            Log.i("RECOG", "EXPLICITLY WANTED VALUES ARRAY $explicitWantedArray")
            Log.i("RECOG", "TOTAL $runningTotal")
            Log.i("RECOG", "SUB-TOTAL $runningSubTotal")
            Log.i("RECOG", "Lonely Characters List: $lonelyCharacters")
            Log.i("RECOG", "Lonely Digits List: $lonelyDigits")
            Log.i("RECOG", "Lower case words List: $deletedLowerCaseArrays")
            Log.i("RECOG", "--------------------------------------------")

            val finalCheckedItems: ArrayList<String> = ArrayList()

            //Final checks
            for (item in itemsArray) {
                if (item.length < 5) {
                    break
                }
                else {
                    val trimmed = item.trim()
                    finalCheckedItems.add(trimmed)
                }
            }
            for (value in valuesArray) {
                if (value.contains(" ")){
                    value.replace(" ", "")
                }
                if (value.contains("o")) {
                    value.replace("o", "0")
                }
                if (value.contains("c")){
                    value.replace("c", "0")
                }

            }

            var correctedItems: MutableList<String>
            var correctedValues: MutableList<String>

            try {
                // If we can get the number of items then create a sublist to that effect
                numberOfItems = explicitWantedArray[0].filter { it.isDigit() }.toInt()
                correctedItems = finalCheckedItems.subList(0, numberOfItems)
                correctedValues = valuesArray.subList(0, numberOfItems)
                Log.i("RECOG", "Able to find number of items and correct")
            } catch (exception: IndexOutOfBoundsException) {
                try {
                    if (!numberOfItemsProvided) {
                        numberOfItems = howManyDialog()
                        numberOfItemsProvided = true
                    }
                    correctedItems = finalCheckedItems.subList(0, numberOfItems)
                    correctedValues = valuesArray.subList(0, numberOfItems)
                    Log.i("RECOG", "Not able to automatically find number of items and correct. Set explicitly by user to $numberOfItems")
                } catch (exception: java.lang.IndexOutOfBoundsException){
                    exception.printStackTrace()
                    Toast.makeText(contxt, "Unable to recognise all the items", Toast.LENGTH_SHORT).show()
                    correctedItems = mutableListOf("Please try again")
                    correctedValues = mutableListOf("0.00")
                }

            }
            Log.i("RECOG", "Corrected Items List: $correctedItems")
            Log.i("RECOG", "Corrected Values List: $correctedValues")

            initializeProductList(correctedItems, correctedValues)
            flagAndRefresh()
            }

        .addOnFailureListener {
            Log.i("RECOG", "Failed completely")
        }
    }

    private fun howManyDialog(): Int {
        // Presents a dialog to the user asking for them to identify the number of products on the receipt
        var numberOfItems: Int = 0
        val diagView = LayoutInflater.from(contxt).inflate(R.layout.alert_dialog_scanned_item_confirmation, null)
        val builder = AlertDialog.Builder(contxt).setTitle("How many items?").setView(diagView).show()
        diagView.scannedHowManyUpdateBut.setOnClickListener {
            val userInput = diagView.scannedHowManyNumber.text.toString()
            if (userInput.isBlank() || !userInput.matches("[0-9]*".toRegex())) {
                Toast.makeText(contxt, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
            numberOfItems = userInput.toInt()
            builder.cancel()
        }
        diagView.scannedHowManyCantFindBut.setOnClickListener {
            builder.cancel()
        }
        return numberOfItems
    }


    private fun retrieveParticipants() {
        participantList = NewReceiptCreationActivity.participantList
        participantList.add(0, ownershipEqualString)
    }

    private fun initializeProductList (correctedItems: MutableList<String>, correctedValues: MutableList<String>){
        /*
        Matches the product names with their corresponding values
         */
        itemizedArrayList.clear()
        for (x in 0 until correctedItems.size){
            val productName = correctedItems[x]
            val productValue = correctedValues[x]
            val defaultError = false
            itemizedArrayList.add(ScannedItemizedProductData(productName, productValue, defaultError))
        }
    }

    private fun flagAndRefresh(){
        /*
        Flags any potential issues with the current product list and refreshes adapter
         */
        for (product in itemizedArrayList){
            val itemName = product.itemName
            val itemValue = product.itemValue
            // Flag any potential errors in the items name
            product.potentialError = itemName.length < 7
            // Flag any potential errors in the value
            if (!product.potentialError){
                val regex = "[0-9]+\\.[0-9][0-9]".toRegex()
                if (itemValue.startsWith(".")){
                    product.potentialError = true
                } else if (itemValue.length <= 2) {
                    product.potentialError = true
                } else product.potentialError = !itemValue.matches(regex)
            }
        }
        adapter.notifyDataSetChanged()
    }

    override fun editProduct(position: Int) {
        //TODO: The product name and the product price should have delete buttons which clear the text and set the selector ready to go on the line to type the correct values
        val product = itemizedArrayList[position]
        val productName = product.itemName
        val productValue = product.itemValue
        val itemOwnership = product.ownership

        val diagView = LayoutInflater.from(contxt).inflate(R.layout.alert_dialog_scanned_product_edit, null)
        val builder = AlertDialog.Builder(contxt).setView(diagView).setTitle("Edit product").show()
        diagView.dialogProductName.setText(productName)
        diagView.dialogProductValue.setText(productValue)
        val spinnerAdapter = ArrayAdapter(contxt, R.layout.support_simple_spinner_dropdown_item, participantList)
        diagView.dialogSpinner.adapter = spinnerAdapter
        val spinPosition = spinnerAdapter.getPosition(itemOwnership)
        diagView.dialogSpinner.setSelection(spinPosition)

        diagView.dialogUpdateButton.setOnClickListener {
            product.itemName = diagView.dialogProductName.text.toString()
            product.itemValue = diagView.dialogProductValue.text.toString()
            product.ownership = diagView.dialogSpinner.selectedItem.toString()
            flagAndRefresh()
            builder.cancel()
        }
        diagView.dialogDeleteButton.setOnClickListener {
            itemizedArrayList.remove(product)
            flagAndRefresh()
        }
        diagView.dialogCancelButton.setOnClickListener {
            builder.cancel()
        }
    }

    override fun radioChecked(position: Int, group: RadioGroup, checkedId: Int) {
        val checkedRadioButton: RadioButton? = group.findViewById(checkedId)
        if (checkedRadioButton != null) {
            val isChecked = checkedRadioButton.isChecked
            if (isChecked) {
                val productOwner = checkedRadioButton.text.toString()
                itemizedArrayList[position].ownership = productOwner
            }
        }

    }

    private fun rotateImage(bitmap: Bitmap?, degrees: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap!!, 0,0,bitmap.width, bitmap.height, matrix, true)
    }
}
