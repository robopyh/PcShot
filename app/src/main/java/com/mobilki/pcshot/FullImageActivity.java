//Activity shows fullscreen images.
//PhotoView (https://github.com/chrisbanes/PhotoView) class provides users with zooming images.
//Glide (https://github.com/bumptech/glide) simplifies the process of loading images and display on application.

package com.mobilki.pcshot;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;


public class FullImageActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.full_image);

        int position = getIntent().getExtras().getInt("id");

        PhotoView photoView = (PhotoView) findViewById(R.id.photo_view);
        File[] images = new ImageAdapter(this).getImages();
        Uri imageUri = Uri.fromFile(images[position]);

        Glide.with(this).load(imageUri).into(photoView);
    }
}
