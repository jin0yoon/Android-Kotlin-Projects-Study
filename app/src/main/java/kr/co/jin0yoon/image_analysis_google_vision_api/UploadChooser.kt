package kr.co.jin0yoon.image_analysis_google_vision_api

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UploadChooser : BottomSheetDialogFragment(){  //상속
    //java에서는 상속은 extends, 구현은 implements 키워드 사용
    //kotlin에서는 상속일 경우에는 ()를 넣고 구현일 경우에는 ()를 뺀다

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.upload_chooser, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }


}