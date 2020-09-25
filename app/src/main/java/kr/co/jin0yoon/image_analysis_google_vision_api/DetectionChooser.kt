package kr.co.jin0yoon.image_analysis_google_vision_api

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.detection_chooser.*

class DetectionChooser : DialogFragment(){

    private var detectionChooserNotifierInterface: DetectionChooserNotifierInterface? = null

    //interface를 사용하여 click event를 MainActivity로 전달
    interface DetectionChooserNotifierInterface{
        fun detectLabel()
        fun detectLandmark()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //layout과 연결
        return inflater.inflate(R.layout.detection_chooser, container, false)

    }

    //detectionChooserNotifierInterface를 등록할 수 있는 함수
    //외부에서 사용할 것이므로 private는 뺌
    fun addDetectionChooserNotifierInterface(listener: DetectionChooserNotifierInterface){
        detectionChooserNotifierInterface = listener
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setUpListener()
    }

    private fun setUpListener(){
        detect_label.setOnClickListener {
            detectionChooserNotifierInterface?.detectLabel()
            dismiss()
        }

        detect_landmark.setOnClickListener {
            detectionChooserNotifierInterface?.detectLandmark()
            dismiss()
        }

        detect_cancel.setOnClickListener {
            dismiss()
        }
    }
}