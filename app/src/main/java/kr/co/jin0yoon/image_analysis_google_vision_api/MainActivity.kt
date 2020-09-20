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
    private val CLOUD_VISION_API_KEY = "AIzaSyAsRG3GL6duexyGSzO6l70UHejNUJJQniQ"
    private val ANDROID_PACKAGE_HEADER = "X-Android-Package"
    private val ANDROID_CERT_HEADER = "X-Android-Cert"
    private val MAX_LABEL_RESULTS = 10

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

    private fun requestCloudVisionApi(bitmap: Bitmap){
        //AsyncTask를 만들어서 사용
        val visionTask = ImageRequestTask(this, prepareImageRequest(bitmap))  //Vision.Images.Annotate를 만들어주어야 하는데, 이걸 만들어주는 prepareImageRequest() 함수를 만들었음.
        visionTask.execute()    //visionTask를 실행
    }

    //여기부터 google api로 보낼 request를 만드는 부분
    private fun prepareImageRequest(bitmap: Bitmap): Vision.Images.Annotate{   //return type : Vision.Images.Annotate
        //여기부터 api에 보낼 request의 Header 부분
        val httpTransport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        //requestInitializer라는 것 -> 서버로 요청을 보내는 것을 request를 보낸대고 하는데, request에는 여러가지 것들을 할 수 있는데, request에서 Header라는 것이 있음
        //Header를 설정해주는 부분
        //Header가 어떻게 생겼는지는 실제로 요청을 보낸 다음에 프로파일러를 통해서 지금 설정한 것이 어떻게 반영돼서 request가 나가는 지 확인할 수 있음
        //request를 보낼 때 Header라는 곳에 특정 key, value값들을 넣어줄 수 있음
        val requestInitializer = object : VisionRequestInitializer(CLOUD_VISION_API_KEY){   //key를 넣어줘야 함. google api에서 생성한 key
            override fun initializeVisionRequest(request: VisionRequest<*>?) {  //request는 nullable
                super.initializeVisionRequest(request)

                val packageName = packageName
                request?.requestHeaders?.set(ANDROID_PACKAGE_HEADER, packageName)

                //Header에다가 ANDROID_PACKAGE_HEADER말고 다른 Header를 붙여줘야 함
                //그 Header를 만들어주는 유틸리티 -> PackageManagerUtil
                val sig = PackageManagerUtil().getSignature(packageManager, packageName)
                request?.requestHeaders?.set(ANDROID_CERT_HEADER, sig)
            }
        }//여기까지 Header 부분


        val builder = Vision.Builder(httpTransport, jsonFactory, null)
        builder.setVisionRequestInitializer(requestInitializer)
        val vision = builder.build()

        val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
        batchAnnotateImagesRequest.requests = object : ArrayList<AnnotateImageRequest>(){
            init {
                val annotateImageRequest = AnnotateImageRequest()

                //request를 보낼 때 사진첩이나 카메라에서 가져온 이미지를 전송해야 하는데
                //이미지를 전송하는 방법 중 하나가 이미지를 전부 byteArray로 바꿈
                //이미지라는 것이 우리 눈에는 색상이 있어보이지만 사실상 0,1로 된 byte로 된 숫자의 나열일 뿐
                //그래서 이미지를 전송하기 위해서 이미지를 byteArray로 바꿔서 전송해줌
                //즉, 사진첩이나 카메라로 부터 가져온 이미지를 request에 실어보내기 위해서 이미지를 일정한 format으로 만들어주고, 그 다음 byte로 바꿔주는 작업을 하는 것
                //여기부터 이미지를 byteArray로 뱌꿔주는 부분
                val base64EncodedImage = Image() //base64로 encoding 된 image인 base64EncodedImage 변수
                val byteArrayOutputStream = ByteArrayOutputStream() // byteArrayOutputStream 변수
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)  //bitmap 압축. format은 JPEG, 퀄리티는 90으로 압축  //압축시킨 것을 byteArrayOutputStream에 넣어줌
                val imageBytes = byteArrayOutputStream.toByteArray()  //byteArrayOutputStream을 byteArray로 바꿈

                base64EncodedImage.encodeContent(imageBytes)
                annotateImageRequest.image = base64EncodedImage
                //여기까지 이미지를 byteArray로 뱌꿔주는 부분

                //google vision api에 요청할 것은 이 이미지를 전달하여 그 이미지의 특징들을 요청
                //그 특징을 labelDetection이라고 함
                //보낼 요청의 설정을 해주는 부분
                //labelDetection을 할 것이고, 최대 10개만 받겠다.
                annotateImageRequest.features = object : ArrayList<Feature>(){
                    init {
                        val labelDetection = Feature()   //labelDetection이라는 feature를 만듬
                        labelDetection.type = "LABEL_DETECTION"  //feature의 type을 "LABEL_DETECTION"으로 해줌
                        labelDetection.maxResults = MAX_LABEL_RESULTS  //google cloud vision으로 부터 어떤 결과를 받는데, 그 결과값을 10개만 받겠다고 지정
                        add(labelDetection)
                    }
                }
                add(annotateImageRequest)
                //여기까지 요청 설정해주는 부분
            }
        }
        val annotateRequest = vision.Images().annotate(batchAnnotateImagesRequest)
        annotateRequest.setDisableGZipContent(true)
        return annotateRequest
    }
    //여기까지 google api로 보낼 request를 만드는 부분

    //AsyncTack
    //inner class로 만듬
    inner class ImageRequestTask constructor(
        activity: MainActivity,
        val request: Vision.Images.Annotate  //request는 Vision.Images.Annotate type

        //kotlin에는 constructor가 2가지 종류가 있음
        //primary constructor, secondary constructor
        //primary constructor -> class를 적어주는 라인에서 작성하는 것
        //secondary constructor -> class 내부에 작성하는 것
        //primary는 생략을 해줘도 상관 없음

        //constructor에서 받은 변수들 앞에 val, var을 붙일 수 있음
        //변수 사용 목적에 따라서 설정을 해줘야 할 때도 있고 하지 말아야 할 때도 있음
        //class가 가지고 있는 함수 내에서도 변수를 사용하고 싶으면 val, var를 붙여줘야 함
        //val를 붙여주지 않으면 doInBackground에서 activity 변수를 사용할 수 없음  * 붙이지 않아도 init{}에서는 변수를 사용할 수 있음
//        val activity: MainActivity,
//        var request: Vision.Images.Annotate

    ) : AsyncTask<Any, Void, String>(){   //return type은 AsyncTask

        private val weakReference : WeakReference<MainActivity> //weakReference<MainActivity> type의 weakReference 선언

        //class 내부의 변수를 선언 할 때, 자바에서는 타입, 변수명만 적어주면 되지만,
        //kotlin의 경우에는 변수의 초기값들을 반드시 initialize 해줘야 함
        //init{}을 통해 초기값을 initialize 해준다.
       init {
            weakReference = WeakReference(activity)
        }

        //통신 작업을 doInBackground에서 해줌
        //doInBackground(), onPostExecute()를 override 해준다.
        override fun doInBackground(vararg p0: Any?): String {  //return type은 String
            try {
                //execute는 google api에 요청한다는 것
                val response = request.execute()  //request.excute()로 request를 보내면 response를 받아줌
                return convertResponseToString(response)   //결과인 이미지 분석 결과를 string으로 표시를 할 것이므로 response를 string으로 변환을 해줘야 함
            }catch (e: Exception){     //예외처리
                e.printStackTrace()
            }
            return "분석 실패"
        }

        //doInBackground에서 처리한 결과값을 표기하는 작업
        override fun onPostExecute(result: String?) {
            //네트워크에서 해주면 좋은 방어코드
            //새로운 스레드를 만들어서 그 곳에서 작업
            //스레드가 돌아가는 동안에도 mainactivity도 작동하기 때문에
            //response가 오기 전에 사용자가 activity를 나가게 되면 그 이후 response가 와서 setTextview를 하게되면 오류가 발생
            //이러한 경우를 대처하기 위한 방어코드
            val activity = weakReference.get()
            if (activity != null && !activity.isFinishing){  //!activity.isFinishing -> activity가 종료되는 중이 아니라면
                uploaded_image_result.text = result
            }
        }

    }

    //response를 string으로 바꿔주는 함수
    //google vision api에게 이미지를 전달하게 되면, response로 BatchAnnotateImagesResponse의 형태로 보내줌
    //BatchAnnotateImagesResponse 형태로 받아진 response를 우리가 원하는 형태인 string으로 바꿔주면 됨
    private fun convertResponseToString(response: BatchAnnotateImagesResponse) : String {
        val message = StringBuilder("분석 결과\n")
        val labels = response.responses[0].labelAnnotations
        labels?.let {
            it.forEach {
                //String.format -> string을 정해준 형태로 변환해줌
                //google vision api는 기본적으로 영어로 주기 때문에 언어는 영어인 Locale.US로 지정
                //%.3f -> 소수점 네자리까지만
                //string은 %s
                //'3.3333: 설명' 의 형태
                message.append(String.format(Locale.US, "%.3f: %s", it.score, it.description))
                message.append("\n")  //줄바꿈
            }
            return message.toString()
        }
        return "분석 실패"
    }

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