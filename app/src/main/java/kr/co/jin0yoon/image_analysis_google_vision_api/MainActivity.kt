package kr.co.jin0yoon.image_analysis_google_vision_api

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpListener()
    }

    private fun setUpListener(){
        upload_image.setOnClickListener {
            UploadChooser().show(supportFragmentManager, "")
        }
    }
}