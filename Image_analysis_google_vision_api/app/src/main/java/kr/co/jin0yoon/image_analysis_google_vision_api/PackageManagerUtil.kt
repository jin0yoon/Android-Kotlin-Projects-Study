package kr.co.jin0yoon.image_analysis_google_vision_api

import android.content.pm.PackageManager
import android.content.pm.Signature
import com.google.common.io.BaseEncoding
import java.lang.Exception
import java.security.MessageDigest

class PackageManagerUtil{
    //signature를 받아오는 함수
    fun getSignature(pm: PackageManager, pakageName: String): String? {
        try {
            val packageInfo = pm.getPackageInfo(pakageName, PackageManager.GET_SIGNATURES)
            return if (packageInfo == null
                    || packageInfo.signatures == null
                    || packageInfo.signatures.size == 0
                    || packageInfo.signatures[0] == null
            ){
                null
            }else{
                signatureDigest(packageInfo.signatures[0])
            }
        }catch (e: Exception){
            return null
        }
    }

    private fun signatureDigest(sig: Signature): String? {
        val signature = sig.toByteArray()
        try {
            val md = MessageDigest.getInstance("SHA1")
            val digest = md.digest(signature)
            return BaseEncoding.base16().lowerCase().encode(digest)
        }catch (e: Exception){
            return null
        }
    }
}