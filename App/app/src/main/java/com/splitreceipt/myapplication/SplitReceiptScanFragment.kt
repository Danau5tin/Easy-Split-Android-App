package com.splitreceipt.myapplication

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat

/**
 * A simple [Fragment] subclass.
 */
class SplitReceiptScanFragment : Fragment() {

//    private lateinit var contxt: Context

    companion object{
        private const val CAMERA_REQ_CODE = 1
    }

//    override fun onAttach(context: Context) {
//        contxt = context
//        super.onAttach(context)
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_split_receipt_scan, container, false)
    }

//    private fun checkPermissions() {
//        if (ActivityCompat.checkSelfPermission(contxt, android.Manifest.permission.CAMERA)
//            != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.CAMERA),
//                CAMERA_REQ_CODE)
//        } else {
//            getImageFromCamera()
//        }
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun getImageFromCamera() {
        TODO("Not yet implemented")
    }

}
