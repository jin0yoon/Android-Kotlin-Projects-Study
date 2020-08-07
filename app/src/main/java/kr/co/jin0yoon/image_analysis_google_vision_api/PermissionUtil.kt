package kr.co.jin0yoon.image_analysis_google_vision_api

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.StringBuilder

class PermissionUtil{

    //세번째 파라미터는 가변인수(여러개가 들어갈 수 있는 인수) -> vararg
    //return은 Boolean -> 요청한 권한이 전부 있으면 true를 return, 하나라도 없는 것이 있으면 false를 return
    fun requestPermission(activity: Activity, requestCode: Int, vararg permissions:String) : Boolean {

        var granted = true

        //요청받은 권한 중에서 permission을 얻어야 하는 것들을 담아두기 위한 변수
        val permissionNeeded = ArrayList<String>()

        //forEach를 통해 반복문
        //index가 필요한 경우에는 for문을 사용하고, index는 필요없고 객체 하나하나만 필요하면 forEach를 사용
//        permissions.forEachIndexed { index, s ->  } -> for문과 동일하게 사용가능
        permissions.forEach {
            //it은 forEach를 통해서 넘어온 하나
            val permissionCheck = ContextCompat.checkSelfPermission(activity, it)
            val hasPermission = permissionCheck == PackageManager.PERMISSION_GRANTED
            granted = granted and hasPermission  //둘다 true인 경우에만 true

            if (!hasPermission){
                permissionNeeded.add(it)
            }

            if (granted){   //요청 권한이 모두 있는 경우
                return true
            }else{
                //권한 요청
                //arrayList를 string 배열로 바꿔주기 위해서 toTypedArray()를 해줌
                ActivityCompat.requestPermissions(activity, permissionNeeded.toTypedArray(), requestCode)
                return false
            }
        }
    }
}