package com.momo.qiniutest;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qiniu.android.common.Zone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "mytag";

    @BindView(R.id.btn_choose)
    Button btnChoose;

    @BindView(R.id.progressbar)
    ProgressBar progressbar;
    @BindView(R.id.btn_upload)
    Button btnUpload;
    @BindView(R.id.tv_info)
    TextView tvInfo;
    @BindView(R.id.grid)
    GridView grid;
    GridAdapter adapter;

    ArrayList<Bitmap> bitmapList = new ArrayList<>();
    ArrayList<String> imagePaths = new ArrayList<>();

    String token = "QX_NRcyn7o0htLCNF64xDaw1H8vmVGkHDYBlGTQ8:FuALDZfq8Udjfn1wLnDzudOTHDM=:eyJzY29wZSI6InRlc3QtZGVtbyIsImRlYWRsaW5lIjoxNDk1MjA4ODE1fQ==";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        adapter = new GridAdapter(this,bitmapList);
        grid.setAdapter(adapter);

        progressbar.setMax(100);
        progressbar.setProgress(0);
    }

    Configuration config = new Configuration.Builder()
            .zone(Zone.zone2)
            .build();
    UploadManager uploadManager = new UploadManager(config);
    int[] i = {0};//循环变量，表示现在正在上传第几张图片


    @OnClick({R.id.btn_choose, R.id.btn_upload})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_choose:
                Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
                // 如果限制上传到服务器的图片类型时可以直接写如："image/jpeg 、 image/png等的类型"
                pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(pickIntent, 1);
                break;
            case R.id.btn_upload:
                //递归上传两张图片

                if(imagePaths.size() == 1) {
                    uploadFile(imagePaths.get(0), new UploadListener() {
                        @Override
                        public void onUploadSuccess(String url) {
                            tvInfo.setText("上传成功\n" + "图片地址为：" + url);
                        }

                        @Override
                        public void onUploadFail(Error error) {
                            tvInfo.setText("上传失败");
                        }
                    });
                }else{
                    //循环上传
//                    loopUpload();
                    //递归上传
                    recurseUpload();
                }
                break;
        }
    }
    //循环上传图片
    private void loopUpload() {
        tvInfo.setText("");
        progressbar.setProgress(0);
        for(i[0]=0;i[0]<imagePaths.size();i[0]++) {
            uploadFile(imagePaths.get(i[0]), new UploadListener() {
                @Override
                public void onUploadSuccess(String url) {
                    tvInfo.setText(tvInfo.getText()+"\n第" + i[0] + "张上传成功\n" + "图片地址为：" + url);
                }

                @Override
                public void onUploadFail(Error error) {
                    tvInfo.setText("上传失败！");
                }
            });
        }
    }

    //递归上传图片
    private void recurseUpload() {
        tvInfo.setText("");
        progressbar.setProgress(0);
        uploadMutliFiles(imagePaths, new UploadMutliListener() {
            @Override
            public void onUploadMutliSuccess() {
                tvInfo.setText(tvInfo.getText().toString() + "\n全部上传成功！");
            }

            @Override
            public void onUploadMutliFail(Error error) {
                tvInfo.setText(tvInfo.getText().toString() + "\n上传失败！");
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null) {
                //将content类型uri转为bitmap，用于gridview显示图片
                Uri uri = data.getData();
                Bitmap bit = null;
                try {
                    bit = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                bitmapList.add(bit);
                adapter.notifyDataSetChanged();

                //将content类型uri转为file类型uri
                String path = getFilePathFromContentUri(uri, getContentResolver());
                if (!imagePaths.contains(path)) {
                    imagePaths.add(path);
                }
            }
        }

    }

    public String getFilePathFromContentUri(Uri selectedVideoUri,
                                            ContentResolver contentResolver) {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn, null, null, null);
//      也可用下面的方法拿到cursor
//      Cursor cursor = this.context.managedQuery(selectedVideoUri, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }



    //上传多张图片
    public void uploadMutliFiles(final List<String> filesUrls, final UploadMutliListener uploadMutliListener) {
        if (filesUrls != null && filesUrls.size() > 0) {
            final String url = filesUrls.get(i[0]);
            uploadFile(url, new UploadListener() {
                @Override
                public void onUploadSuccess(String url) {
                    final UploadListener uploadListener = this;
                    Log.d(TAG, "第" + (i[0]+1) + "张:" + url + "\t上传成功!");
                    tvInfo.setText(tvInfo.getText().toString() + "\n第" + (i[0]+1) + "张上传成功\n" + "图片地址为：" + url);
                    i[0]++;
                    //递归边界条件
                    if (i[0] < filesUrls.size()) {
                        //七牛后台对上传的文件名是以时间戳来命名，以秒为单位，如果文件上传过快，两张图片就会重名而上传失败，所以间隔1秒，保证上传成功
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                uploadFile(filesUrls.get(i[0]), uploadListener);
                            }
                        }, 1000);
                    } else {
                        uploadMutliListener.onUploadMutliSuccess();
                    }
                }

                @Override
                public void onUploadFail(Error error) {
                    print("第" + (i[0]+1) + "张上传失败!" + filesUrls.get(i[0]));
                    uploadMutliListener.onUploadMutliFail(error);
                }
            });

        }
    }

    //上传单个文件
    public void uploadFile(final String filePath, final UploadListener uploadListener) {
        if (filePath == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (uploadManager == null) {
                    uploadManager = new UploadManager();
                }
                uploadManager.put(filePath, null, token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo respInfo,
                                                 JSONObject jsonData) {

                                if (respInfo.isOK()) {
                                    print(jsonData.toString());
                                    String url = "";
                                    try {
                                        //图片的外链地址domain + key
                                        url = "http://oq543v9g0.bkt.clouddn.com/" + jsonData.getString("key");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    uploadListener.onUploadSuccess(url);

                                } else {
                                    print(respInfo.error);
                                    uploadListener.onUploadFail(new Error("上传失败" + respInfo.error));
                                }
                            }



                        }, new UploadOptions(null, null, false,
                                new UpProgressHandler(){
                                    public void progress(String key, double percent){
                                        progressbar.setProgress((int) (percent * 100));
                                    }
                                }, null));
            }
        }).start();
    }

    //上传回调
    public interface UploadListener {
        void onUploadSuccess(String url);

        void onUploadFail(Error error);
    }

    //上传多张文件回调
    public interface UploadMutliListener {
        void onUploadMutliSuccess();

        void onUploadMutliFail(Error error);
    }


    public void print(String s) {
        Log.d("mytag", s);
    }


}
