package com.mobilki.pcshot;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;


class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private File[] images;

    ImageAdapter(Context c) {
        mContext = c;
        loadImages();
    }

    //called to display newly loaded image
    void refresh() {
        loadImages();
        notifyDataSetChanged();
    }

    public int getCount() {
        return images == null ? 0 : images.length;
    }

    public Object getItem(int position) {
        return images == null ? null : images[position];
    }

    public long getItemId(int position) {
        return position;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(300, 300));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        Uri imageUri = Uri.fromFile(images[position]);
        Glide.with(mContext).load(imageUri).into(imageView);
        return imageView;
    }

    //load files into list and sort it with date
    private void loadImages() {
        images = new File(MainActivity.DIR).listFiles();
        Arrays.sort(images, new Comparator<File>(){
            public int compare(File f1, File f2)
            {
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            } });
    }

    public File[] getImages() {
        return images;
    }
}
