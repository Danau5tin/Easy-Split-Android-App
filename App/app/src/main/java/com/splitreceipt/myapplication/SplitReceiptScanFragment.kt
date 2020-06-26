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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.splitreceipt.myapplication.NewExpenseCreationActivity.Companion.currencyCode
import com.splitreceipt.myapplication.NewExpenseCreationActivity.Companion.currencySymbol
import com.splitreceipt.myapplication.NewExpenseCreationActivity.Companion.editSqlRowId
import com.splitreceipt.myapplication.data.ScannedItemizedProductData
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_CODE
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL
import com.splitreceipt.myapplication.data.SharedPrefManager.SHARED_PREF_NAME
import com.splitreceipt.myapplication.data.SqlDbHelper
import com.splitreceipt.myapplication.databinding.FragmentSplitReceiptScanBinding
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.alert_dialog_scanned_product_edit.view.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class SplitReceiptScanFragment : Fragment(), NewScannedReceiptRecyclerAdapter.onScannedClick {

    private lateinit var contxt: Context
    private lateinit var binding: FragmentSplitReceiptScanBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var scanTextHelper: ScanTextRecogHelper
    private var explicitTotal = ""
    private val currencyIntent: Int = 50
    private val cropImageIntent: Int = 60
    private lateinit var participantList: ArrayList<String>
    private lateinit var participantAdapterList: MutableList<String>
    private lateinit var adapter: NewScannedReceiptRecyclerAdapter
    private var currentPhotoPath: String = ""
    private var numberOfItemsProvided = false
    private var numberOfItems: Int = 0
    private lateinit var photoUri: Uri

    companion object{
        private const val CAMERA_REQ_CODE = 1
        private const val takePictureIntent = 2
        lateinit var itemizedArrayList: ArrayList<ScannedItemizedProductData>
        const val ownershipEqualString = "Equal"
        var errorsCleared = true
    }

    override fun onAttach(context: Context) {
        contxt = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSplitReceiptScanBinding.inflate(inflater, container, false)
        numberOfItemsProvided = false
        sharedPreferences = contxt.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        itemizedArrayList = ArrayList()
        participantList = ArrayList()
        updateUICurrency()
        retrieveParticipants()
        adapter = NewScannedReceiptRecyclerAdapter(participantAdapterList, itemizedArrayList, this)
        binding.scannedRecy.layoutManager = LinearLayoutManager(contxt)
        binding.scannedRecy.adapter = adapter
        binding.scannedRecy.isNestedScrollingEnabled = false


        binding.currencyAmountScan.addTextChangedListener(object: TextWatcher {
            //TODO: This is copied from other fragment. Can we move this up into the activity and have the two fragments listen for changes from the main activity?
            override fun afterTextChanged(s: Editable?) {
                setTotal()
            }
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
                    SplitExpenseManuallyFragment.transactionTotal = text.toString()
                }
                else{
                    SplitExpenseManuallyFragment.transactionTotal =
                        NewExpenseCreationActivity.zeroCurrency

                    }}})

        binding.addReceiptImageButton.setOnClickListener {
            val okayToProceed = okayToProceed()
            if (okayToProceed){
                setTotal()
                checkPermissions()
            }
        }

        binding.currencyButtonScan.setOnClickListener {
            val intent = Intent(contxt, CurrencySelectorActivity::class.java)
            startActivityForResult(intent, currencyIntent)
        }

        if (NewExpenseCreationActivity.isScanned) {
            binding.currencyAmountScan.setText(NewExpenseCreationActivity.editTotal)
            val dbHelper = SqlDbHelper(contxt)
            itemizedArrayList = dbHelper.getReceiptProductDetails(editSqlRowId, itemizedArrayList)
        }

        scanTextHelper = ScanTextRecogHelper(contxt, itemizedArrayList, adapter)
        return binding.root
    }


    private fun okayToProceed(): Boolean {
        if(binding.currencyAmountScan.text.toString().isBlank()){
            Toast.makeText(contxt, "Please enter receipt total", Toast.LENGTH_SHORT).show()
            return false
        } else if (binding.numberScanItemsText.text.toString().isBlank()){
            Toast.makeText(contxt, "Please enter number of items on receipt", Toast.LENGTH_LONG).show()
            return false
        } else {
            val num = binding.numberScanItemsText.text.toString().toIntOrNull()
            if (num != null){
                numberOfItems = binding.numberScanItemsText.text.toString().toInt()
                Log.i("RECOG", "Number of items set explicitly by user to $numberOfItems")
                return true
            } else {
                Toast.makeText(contxt, "Please enter a valid number", Toast.LENGTH_LONG).show()
                return false
            }
        }

    }

    private fun setTotal() {
        val expenseTotalString = binding.currencyAmountScan.text.toString()
        explicitTotal = expenseTotalString
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
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(contxt.packageManager)

        val photoFile: File? = try {
            createImageFile()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        photoUri = FileProvider.getUriForFile(contxt,
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
                //Here is where the user is asked to crop the image.
                try {
                    val filename = "bitmap.png"
                    val stream = contxt.openFileOutput(filename, Context.MODE_PRIVATE);
                    rotatedBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    stream.close();
                    rotatedBitmap.recycle();

                    val intent = Intent(contxt, ScanCropImageActivity::class.java)
                    intent.putExtra("bitmap", filename)
                    startActivityForResult(intent, cropImageIntent)
                } catch (e : Exception) {
                    e.printStackTrace();
                }
            }
        }
        else if (requestCode == cropImageIntent) {
            if (resultCode == Activity.RESULT_OK) {
                var croppedBmp: Bitmap? = null
                val filename = data?.getStringExtra("bitmap")
                try {
                    val input = contxt.openFileInput(filename)
                    croppedBmp = BitmapFactory.decodeStream(input)
                    input.close()
                    binding.addReceiptImageButton.setImageBitmap(croppedBmp)
                    scanTextHelper.runTextRecognition(croppedBmp, explicitTotal, numberOfItems)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        else if (requestCode == currencyIntent){
            if (resultCode == Activity.RESULT_OK) {
                updateUICurrency()
            }
        }
    }

    private fun updateUICurrency(adapterInitialised: Boolean = false) {
        currencySymbol = sharedPreferences.getString(SHARED_PREF_ACCOUNT_CURRENCY_SYMBOL, "$").toString()
        currencyCode = sharedPreferences.getString(SHARED_PREF_ACCOUNT_CURRENCY_CODE, "US").toString()
        binding.currencyButtonScan.text = currencyCode
        if (adapterInitialised) {
            //TODO: Update the adapter with the correct currency code
//            binding.fragManualRecy.post(Runnable { adapter.notifyDataSetChanged() })
        }
    }

    private fun retrieveParticipants() {
        participantList = NewExpenseCreationActivity.participantList
        participantAdapterList = NewExpenseCreationActivity.participantList.toMutableList()
        participantAdapterList.add(0, ownershipEqualString)
    }

    override fun editProduct(position: Int) {
        /*
        Opens up a dialog to edit the product in further detail
         */
        //TODO: The product name and the product price should have delete buttons which clear the text and set the selector ready to go on the line to type the correct values
        val product = itemizedArrayList[position]
        val productName = product.itemName
        val productValue = product.itemValue
        val itemOwnership = product.ownership

        val diagView = LayoutInflater.from(contxt).inflate(R.layout.alert_dialog_scanned_product_edit, null)
        val builder = AlertDialog.Builder(contxt).setView(diagView).setTitle("Edit product").show()
        diagView.dialogProductName.setText(productName)
        diagView.dialogProductValue.setText(productValue)
        val spinnerAdapter = ArrayAdapter(contxt, R.layout.support_simple_spinner_dropdown_item, participantAdapterList)
        diagView.dialogSpinner.adapter = spinnerAdapter
        val spinPosition = spinnerAdapter.getPosition(itemOwnership)
        diagView.dialogSpinner.setSelection(spinPosition)

        diagView.dialogUpdateButton.setOnClickListener {
            product.itemName = diagView.dialogProductName.text.toString()
            product.itemValue = diagView.dialogProductValue.text.toString()
            product.ownership = diagView.dialogSpinner.selectedItem.toString()
            scanTextHelper.flagAndRefresh(itemizedArrayList)
            builder.cancel()
        }
        diagView.dialogDeleteButton.setOnClickListener {
            itemizedArrayList.remove(product)
            SqlDbHelper(contxt).deleteReceiptProduct(product.sqlRowId)
            scanTextHelper.flagAndRefresh(itemizedArrayList)
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
