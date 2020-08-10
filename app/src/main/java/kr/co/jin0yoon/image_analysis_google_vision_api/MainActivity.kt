package kr.co.jin0yoon.image_analysis_google_vision_api

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_analyze_view.*
import java.io.File

class MainActivity : AppCompatActivity() {

    //구별하기 위한 식별자
    private val CAMERA_PERMISION_REQUEST = 1000
    private val GALLERY_PERMISION_REQUEST = 1001

    private val FILE_NAME = "picture.jpg"

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
        if(PermissionUtil().requestPermission(
            this, CAMERA_PERMISION_REQUEST, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            openCamera()
        }
    }
    private fun checkGalleryPermission(){
        //유틸리티 사용
        PermissionUtil().requestPermission(
            this, GALLERY_PERMISION_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun openCamera(){
        //사진을 저장할 Uri(경로)
        val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", createCameraFile())

        //camera를 여는 부분
        startActivityForResult(
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, CAMERA_PERMISION_REQUEST
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            CAMERA_PERMISION_REQUEST -> {
                //RESULT_OK는 작업이 잘 되었는지 안되었는지 -> 작업이 잘 되었다는 것이 RESULT_OK)
                if (resultCode != Activity.RESULT_OK) return  //작업이 잘 되지 않았다면 return해서 나가도록

                val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", createCameraFile())
                uploadImage(photoUri)
            }
        }
    }

    private fun uploadImage(imageUri:Uri){
        //사진 type은 Bitmap
        val bitmap : Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)   //imageUri의 이미지의 Bitmap을 가져옴
        //java에서는 getContentResolver로 가져와야 함
        //kotlin에서는 getter, setter 구분이 없으므로 그냥 contentResolver로 가져오면 됨

        //bitmap을 ImageView에 넣어주면 됨
        uploaded_image.setImageBitmap(bitmap)
    }

    //사진 file을 만드는 함수
    private fun createCameraFile():File{
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(dir, FILE_NAME)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            GALLERY_PERMISION_REQUEST -> {

            }
            CAMERA_PERMISION_REQUEST -> {
                if (PermissionUtil().permissionGranted(requestCode, CAMERA_PERMISION_REQUEST, grantResults)){
                    openCamera()
                }
            }
        }
    }
}