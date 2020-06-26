package com.splitreceipt.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.splitreceipt.myapplication.databinding.ActivityScanCropImageBinding


class ScanCropImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanCropImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanCropImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var bmp: Bitmap? = null
        val filename = intent.getStringExtra("bitmap")
        try {
            val input = openFileInput(filename)
            bmp = BitmapFactory.decodeStream(input)
            input.close()
            binding.cropImageView.setImageBitmap(bmp)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Crop photo"
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.vector_x_white)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuNext -> {
                val croppedBmp = binding.cropImageView.croppedImage
                try {
                    val filename = "bitmap.png"
                    val stream = this.openFileOutput(filename, Context.MODE_PRIVATE);
                    croppedBmp!!.compress(Bitmap.CompressFormat.PNG, 100, stream);

                    stream.close();
                    croppedBmp.recycle();

                    intent.putExtra("bitmap", filename)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                    return true
                } catch (e : Exception) {
                    e.printStackTrace();
                }
            }
            else -> return false
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.next_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        return true
    }
}
