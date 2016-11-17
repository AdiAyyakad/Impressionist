package com.cmsc434.adi.impressionist;

import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class ImageActivity extends AppCompatActivity {

    ImpressionistView impressionistView;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
    }

    @Override
    protected void onResume() {
        imageView = (ImageView) findViewById(R.id.imageView);
        impressionistView = (ImpressionistView) findViewById(R.id.impressionistView);
        impressionistView.sourceView = imageView;
        impressionistView.sourceBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        super.onResume();
    }
}
