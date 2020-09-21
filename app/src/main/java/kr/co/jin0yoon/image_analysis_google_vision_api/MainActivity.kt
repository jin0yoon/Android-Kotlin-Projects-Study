package kr.co.jin0yoon.image_analysis_google_vision_api

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_analyze_view.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.*

class MainActivity : AppCompatActivity() {

    //구별하기 위한 식별자
    private val CAMERA_PERMISION_REQUEST = 1000
    private val GALLERY_PERMISION_REQUEST = 1001
    private val FILE_NAME = "picture.jpg"
    private var uploadChooser : UploadChooser? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpListener()
    }

    private fun setUpListener(){
        upload_image.setOnClickListener {
            uploadChooser = UploadChooser().apply {
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
            }
            uploadChooser!!.show(supportFragmentManager, "")
            //uploadChooser라는 변수를 통해서 show를 함
            //!!는 uploadChooser가 무조건 null이 아니므로
        }
    }

    private fun checkCameraPermission(){
        //유틸리티 사용
        if(PermissionUtil().requestPermission(
            this, CAMERA_PERMISION_REQUEST, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ){
            openCamera()
        }
    }
    private fun checkGalleryPermission(){
        //유틸리티 사용
        if(PermissionUtil().requestPermission(
            this, GALLERY_PERMISION_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            openGallery()
        }
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

    private fun openGallery(){
        val intent = Intent().apply {
            setType("image/*")
            setAction(Intent.ACTION_GET_CONTENT)
        }
        startActivityForResult(Intent.createChooser(intent, "Select a photo"), GALLERY_PERMISION_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            CAMERA_PERMISION_REQUEST -> {
                //RESULT_OK는 작업이 잘 되었는지 안되었는지 -> 작업이 잘 되었다는 것이 RESULT_OK)
                if (resultCode != Activity.RESULT_OK) return  //작업이 잘 되지 않았다면 return해서 나가도록

                val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", createCameraFile())
                uploadImage(photoUri)

                //여기에 찍은 사진을 갤러리에 저장하는 코드 삽입 필요
                galleryAddPic(photoUri)
            }
            GALLERY_PERMISION_REQUEST -> {
                //data가 null이 아니면 let이하를 실행하겠다
//                if (data != null){
//
//                }
                //java에서의 위 코드와 동일
                data?.let {
                    //이 안에서는 data가 it으로 사용됨
                    it.data?.let { it1 -> uploadImage(it1) }
                }
            }
        }
    }

    private fun uploadImage(imageUri:Uri){
        val bitmap : Bitmap

        //.getBitmap이 28이상부터 deprecated되므로 두가지로 구분하여 코드 작성 필
        //사진 type은 Bitmap
        if (Build.VERSION.SDK_INT < 28) {
            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)  //imageUri의 이미지의 Bitmap을 가져옴
            //java에서는 getContentResolver로 가져와야 함
            //kotlin에서는 getter, setter 구분이 없으므로 그냥 contentResolver로 가져오면 됨

            //bitmap을 ImageView에 넣어주면 됨
            uploaded_image.setImageBitmap(bitmap)
        }
        else{
            val decode = ImageDecoder.createSource(this.contentResolver, imageUri)
            bitmap = ImageDecoder.decodeBitmap(decode)

            //bitmap을 ImageView에 넣어주면 됨
            uploaded_image.setImageBitmap(bitmap)
        }

        uploadChooser?.dismiss()

        //업로드 할 사진이 준비되어 있는 상황이므로 여기에서 google vision api로 사진을 전송한다.
        //AsyncTask를 사용
       requestCloudVisionApi(bitmap)
    }

    //리펙토링 -> 여기 있던 google api 관련 코드들을 LabelDetectionTask.kt 파일로 옮김

    //사진 file을 만드는 함수
    private fun createCameraFile():File{
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(dir, FILE_NAME)
    }


    //찍은 사진을 갤러리에 저장하는 함수
    private fun galleryAddPic(photoUri: Uri) {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            mediaScanIntent.data = photoUri
            sendBroadcast(mediaScanIntent)
            Toast.makeText(this, "사진이 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            GALLERY_PERMISION_REQUEST -> {
                if (PermissionUtil().permissionGranted(requestCode, GALLERY_PERMISION_REQUEST, grantResults)){
                    openGallery()
                }
            }
            CAMERA_PERMISION_REQUEST -> {
                if (PermissionUtil().permissionGranted(requestCode, CAMERA_PERMISION_REQUEST, grantResults)){
                    openCamera()
                }
            }
        }
    }
}