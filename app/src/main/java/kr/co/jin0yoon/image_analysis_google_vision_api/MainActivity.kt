package kr.co.jin0yoon.image_analysis_google_vision_api

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    //구별하기 위한 식별자
    private val CAMERA_PERMISION_REQUEST = 1000
    private val GALLERY_PERMISION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpListener()
    }

    private fun setUpListener(){
        upload_image.setOnClickListener {
            UploadChooser().apply {
                //apply는 apply앞의 부분의 초기 설정을 할 때 유용하게 사용
                //UploadChooser를 만들고 그 값들을 초기화하겠다.
                //UploadChooser안의 함수들을 사용할 수 있음
                addNotifier(object : UploadChooser.UploadChooserNotifierInterface{
                    override fun cameraOnClick() {
                        Log.d("upload", "cameraOnClick")
                        //카메라 권한
                        checkCameraPermission()
                    }

                    override fun galleryOnClick() {
                        Log.d("upload", "galleryOnClick")
                        //사진첩 권한 (외부 저장소 접근)
                        checkGalleryPermission()
                    }
                })
            }.show(supportFragmentManager, "")
        }
    }

    private fun checkCameraPermission(){
        //유틸리티 사용
        PermissionUtil().requestPermission(
            this, CAMERA_PERMISION_REQUEST, Manifest.permission.CAMERA
        )
    }
    private fun checkGalleryPermission(){
        //유틸리티 사용
        PermissionUtil().requestPermission(
            this, GALLERY_PERMISION_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA
        )
    }
}