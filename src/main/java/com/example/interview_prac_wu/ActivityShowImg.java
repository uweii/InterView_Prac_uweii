package com.example.interview_prac_wu;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class ActivityShowImg extends AppCompatActivity {
    private ImageView mImageView;
    private String BASE_URL = "http://p5vw3wtwp.bkt.clouddn.com/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_img);
        Intent intent = getIntent();
        String picName = intent.getStringExtra("name");
        String url = BASE_URL + picName;
        mImageView = findViewById(R.id.iv_uploaded_img);
        Picasso.with(this).load(Uri.parse(url)).fit().centerCrop().into(mImageView);
    }
}
