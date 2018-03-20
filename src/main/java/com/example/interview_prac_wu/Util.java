package com.example.interview_prac_wu;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.support.v4.content.ContextCompat;

/**
 * Created by uwei on 2018/3/20.
 */

public class Util {
    public static boolean canWriteSdcard(Context context)
    {
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasNetAccess(Context context)
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm.getActiveNetworkInfo()!=null&&cm.getActiveNetworkInfo().isConnected()){
            return true;
        }
        return false;
    }
}
