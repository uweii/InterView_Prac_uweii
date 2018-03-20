package com.example.interview_prac_wu;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.service.autofill.FillContext;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.http.Response;
import com.qiniu.util.Auth;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {
    private ImageView mShowCaptureImg;
    private Button mBtnCapture;
    private Button mBtnUpload;
    private boolean mHasCaptrue = false;
    private String mImgSavedir;  //图片保存的目录
    private static int RESQUEST_CODE_CAPTURE = 0x123;
    private static int RESQUEST_CODE_PERMISSION = 0x124;
    private String savePath;
    private String TAG = "MaimActivity";
    private String AK = "dmcJpi_O57UchvNHNP1ibR93A1txAA4ch7iOHZFW";
    private String SK = "_b4LUp5c1mpjk_Q3e2GtkMhfIlcMTgx3gYUAk6GB";
    private String BUCKETNAME = "myspace";
    private ProgressBar mProgressBar;  //进度条
    private String mPicName;
    private boolean isUploading = false;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what==0x120){
                double progress = (Double) msg.obj;
                mProgressBar.setProgress((int)progress);
                if(progress==100){
                    isUploading = false;
                    Intent intent = new Intent(MainActivity.this,ActivityShowImg.class);
                    intent.putExtra("name",mPicName);
                    startActivity(intent);
                }
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        setImgCategory();//设置图片保存的目录
        mShowCaptureImg = findViewById(R.id.iv_capture);
        mBtnCapture = findViewById(R.id.btn_capture);
        mBtnUpload = findViewById(R.id.btn_upload);
        mProgressBar = findViewById(R.id.progressbar);
        mBtnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Util.canWriteSdcard(MainActivity.this)) {   //检测是否能够写sd卡
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RESQUEST_CODE_PERMISSION);
                    }
                }
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                long tag = System.currentTimeMillis();
                mPicName = tag + ".jpg";
                savePath = mImgSavedir + mPicName;
               // Log.d(TAG,"savePath is ====> " + savePath);
                Uri uri = Uri.fromFile(new File(savePath));
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(intent, RESQUEST_CODE_CAPTURE);
            }
        });
        mBtnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mHasCaptrue){
                    Toast.makeText(MainActivity.this,"请先拍摄照片再上传",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!Util.hasNetAccess(MainActivity.this)){
                    Toast.makeText(MainActivity.this,"请检查是否开启网络",Toast.LENGTH_SHORT).show();
                    return;
                }
                isUploading = true;
                UploadManager uploadManager = new UploadManager();
                Auth auth = Auth.create(AK, SK);
                String token = auth.uploadToken(BUCKETNAME);
                try {
                    Bitmap bitmap = getSmallBitmap(new File(savePath));
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);
                    uploadManager.put(outputStream.toByteArray(), mPicName, token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject res) {
                                //res包含hash、key等信息，具体字段取决于上传策略的设置
                                if(info.isOK()) {
                                    //Log.i("qiniu", "Upload Success");
                                    Toast.makeText(MainActivity.this,"上传成功",Toast.LENGTH_SHORT).show();
                                } else {
                                    //Log.i("qiniu", "Upload Fail");
                                    Toast.makeText(MainActivity.this,"上传失败！",Toast.LENGTH_SHORT).show();
                                }
                            }
                        }, new UploadOptions(null, null, false,
                                new UpProgressHandler(){
                                    public void progress(String key, double percent){
                                        Log.i("my Progress", key + ": " + percent);
                                        Message msg = new Message();
                                        msg.what = 0x120;
                                        msg.obj = percent*100;
                                        mHandler.sendMessage(msg);
                                    }
                                }, null)  ) ;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setImgCategory() {
        String sdPath = System.getenv("EXTERNAL_STORAGE");
        mImgSavedir = sdPath + "/拍摄照片/";
        File file = new File(mImgSavedir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == RESQUEST_CODE_CAPTURE) {
            File file = new File(savePath);
            try {
                isUploading = false;
                mShowCaptureImg.setImageBitmap(getSmallBitmap(file));
                mProgressBar.setProgress(0);
                mHasCaptrue = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap getSmallBitmap(File file) throws FileNotFoundException {  //获取压缩后的图片
        Uri uri = Uri.fromFile(file);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
        int w = options.outWidth;
        int h = options.outHeight;
        float ww = 240f;
        float hh = 400f;
        int scale = Math.max(Math.round(w / ww), Math.round(h / hh));
        options.inJustDecodeBounds = false;
        options.inSampleSize = scale;
//        bitmap.recycle();
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RESQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //获取到权限
            } else {
                Toast.makeText(this, "您拒绝了写文件权限，无法保存图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            if(isUploading){
                Toast.makeText(this,"正在上传图片!!!",Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
