package kr.co.jin0yoon.image_analysis_google_vision_api

import android.app.Activity
import java.lang.StringBuilder

class PermissionUtil{

    fun requestPermission(activity: Activity, requestCode: Int, vararg permissions:String) : Boolean {

        var granted = true
        val permissionNeeded = ArrayList<String>()

        permissions.forEach {

        }
    }
}