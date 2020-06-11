package com.splitreceipt.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.splitreceipt.myapplication.data.ScannedItemizedProductData
import com.splitreceipt.myapplication.databinding.FragmentSplitReceiptScanBinding
import kotlinx.android.synthetic.main.alert_dialog_scanned_product_edit.view.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class SplitReceiptScanFragment : Fragment(), NewScannedReceiptRecyclerAdapter.onScannedClick {

    private lateinit var contxt: Context
    private lateinit var binding: FragmentSplitReceiptScanBinding
    private lateinit var itemizedArrayList: ArrayList<ScannedItemizedProductData>
    private lateinit var participantList: ArrayList<String>
    private lateinit var adapter: NewScannedReceiptRecyclerAdapter
    private var currentPhotoPath: String = ""

    companion object{
        private const val CAMERA_REQ_CODE = 1
        private const val TAKE_PICTURE = 2
    }

    override fun onAttach(context: Context) {
        contxt = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSplitReceiptScanBinding.inflate(inflater, container, false)
        itemizedArrayList = ArrayList()
        participantList = ArrayList()
        adapter = NewScannedReceiptRecyclerAdapter(participantList, itemizedArrayList, this)
        binding.scannedRecy.layoutManager = LinearLayoutManager(contxt)
        binding.scannedRecy.adapter = adapter
        binding.scannedRecy.isNestedScrollingEnabled = false


        binding.addReceiptImageButton.setOnClickListener {
            checkPermissions()
        }

        return binding.root
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
        startActivityForResult(intent, TAKE_PICTURE)
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
        if (requestCode == TAKE_PICTURE) {
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

                    if (value.contains("26.6") || value.contains("17.95") ||
                        value.contains("8.65") || value.contains("1.44") ||
                        value.contains("25.16")){
                        // Check if user given totals are in the values. (this will only be used if the running totals do not work with more testing)
                        explicitDeletedArray.add(value)
                    }
                }
                for (value in explicitDeletedArray) {
                    valuesArray.remove(value)
                }

                for (value in explicitWantedArray) {
                    itemsArray.remove(value)
                }
            }
            Log.i("RECOG", "ENTIRE BLOCK ${it.text}")
            Log.i("RECOG", "ITEMS ARRAY ${itemsArray}")
            Log.i("RECOG", "VALUES ARRAY ${valuesArray}")
            Log.i("RECOG", "DELETED ITEMS ARRAY ${deletedItemArray}")
            Log.i("RECOG", "DELETED VALUES ARRAY ${deletedValuesArrays}")
            Log.i("RECOG", "EXPLICITLY DELETED VALUES ARRAY ${explicitDeletedArray}")
            Log.i("RECOG", "EXPLICITLY WANTED VALUES ARRAY $explicitWantedArray")
            Log.i("RECOG", "TOTAL $runningTotal")
            Log.i("RECOG", "SUB-TOTAL $runningSubTotal")
            Log.i("RECOG", "Lonely Characters List: ${lonelyCharacters}")
            Log.i("RECOG", "Lonely Digits List: ${lonelyDigits}")
            Log.i("RECOG", "Lower case words List: ${deletedLowerCaseArrays}")
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

            var correctedItems: MutableList<String>
            var correctedValues: MutableList<String>

            try {
                // If we can get the number of items then create a sublist to that effect
                val numbItems = explicitWantedArray[0].filter { it.isDigit() }.toInt()
                correctedItems = finalCheckedItems.subList(0, numbItems)
                correctedValues = valuesArray.subList(0, numbItems)
                Log.i("RECOG", "Able to find number of items and correct")
            } catch (exception: IndexOutOfBoundsException) {
                correctedItems = itemsArray
                correctedValues = valuesArray
                Log.i("RECOG", "Not able to find number of items and correct")
            }

            Log.i("RECOG", "Corrected Items List: ${correctedItems}")
            Log.i("RECOG", "Corrected Values List: ${correctedValues}")

            retrieveParticipants()
            initializeProductList(correctedItems, correctedValues)
            flagAndRefresh()
            }

        .addOnFailureListener {
            Log.i("RECOG", "Failed completely")
        }
    }

    fun updateOwnerships(){
        //TODO: Create function. When radioButtons pressed or when save button pressed in receipt creation activity?
        //TODO: This function will update the itemized products list with the correct ownership details ready for the sql insertion and participant balancing
    }

    private fun retrieveParticipants() {
        participantList = NewReceiptCreationActivity.participantList
        participantList.add(0, "Equal")
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
        val product = itemizedArrayList[position]
        val productName = product.itemName
        val productValue = product.itemValue
        val itemOwnership = product.ownership //TODO: Find the ownership from the currently selected radio button

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

    private fun rotateImage(bitmap: Bitmap?, degrees: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap!!, 0,0,bitmap.width, bitmap.height, matrix, true)
    }
}
