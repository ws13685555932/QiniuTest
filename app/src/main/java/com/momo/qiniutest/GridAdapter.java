package com.momo.qiniutest;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Administrator on 2017/5/19.
 */

public class GridAdapter extends BaseAdapter {

    private Context context;
    private List<Bitmap> listitem;

    public GridAdapter(Context context,List<Bitmap> listitem) {
        this.context = context;
        this.listitem = listitem;
    }

    @Override
    public int getCount() {
        return listitem.size();
    }

    @Override
    public Object getItem(int position) {
        return listitem.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.gridview_item, null);
        }

        ImageView imageView = (ImageView) convertView.findViewById(R.id.image);
        imageView.setImageBitmap((Bitmap) getItem(position));
        return convertView;
    }

}
