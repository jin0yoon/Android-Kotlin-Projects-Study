package kr.co.jin0yoon.image_analysis_google_vision_api

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.upload_chooser.*

class UploadChooser : BottomSheetDialogFragment(){  //상속
    //java에서는 상속은 extends, 구현은 implements 키워드 사용
    //kotlin에서는 상속일 경우에는 ()를 넣고 구현일 경우에는 ()를 뺀다

    //interface로 카메라, 갤러리 버튼 이벤트 구현
    interface UploadChooserNotifierInterface{
        fun cameraOnClick()

        fun galleryOnClick()
    }

    var uploadChooserNotifierInterface : UploadChooserNotifierInterface? = null

    //MainActivity에서 호출할 것 이므로 private를 붙이지 않음
    fun addNotifier(listener: UploadChooserNotifierInterface){
        uploadChooserNotifierInterface = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.upload_chooser, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setUpListener()
    }

    private fun setUpListener(){
        upload_camera.setOnClickListener {
            //? -> null이 아니면 ?이후를 진행한다.
            //uploadChooserNotifierInterface가 null이 아니라면 cameraOnClick()를 호출한다.
            uploadChooserNotifierInterface?.cameraOnClick()

            /*
            !! -> 무조건 있고 !!이후를 진행한다.
            uploadChooserNotifierInterface가 무조건 있으므로 cameraOnClick()를 호출한다.
            uploadChooserNotifierInterface!!.cameraOnClick()
             */

            /*
            uploadChooserNotifierInterface의 기본 상태를 null로 지정했으므로 null이 있기 때문에
            !!가 아니라 ?를 써야함
             */
        }
        upload_gallery.setOnClickListener {
            //uploadChooserNotifierInterface가 null이 아니라면 galleryOnClick()를 호출한다.
            uploadChooserNotifierInterface?.galleryOnClick()
        }
    }


}