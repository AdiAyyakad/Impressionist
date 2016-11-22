package com.cmsc434.adi.impressionist;

import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class ImageActivity extends AppCompatActivity {

    ImpressionistView impressionistView;
    ImpressionistImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
    }

    @Override
    protected void onResume() {
        imageView = (ImpressionistImageView) findViewById(R.id.imageView);
        impressionistView = (ImpressionistView) findViewById(R.id.impressionistView);
        impressionistView.setImageView(imageView);

        super.onResume();
    }
}
