package com.yxx.uploadfile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private ValueCallback<Uri>uploadMessage;
    private ValueCallback<Uri[]>uploadMessageAboveL;
    private final static int FILE_CHOOSER_RESULT_CODE=10000;
    //private DownCompleteReveiver reveiver;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView webView=findViewById(R.id.webview_upfile);
        WebSettings settings=webView.getSettings();
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptEnabled(true);
        //http和https混合问题
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        webView.setWebViewClient(new WebViewClient());

       /* webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                downloadBySystem(url,contentDisposition,mimetype);
                Toast.makeText(MainActivity.this,"正在下载中...",Toast.LENGTH_LONG);

            }
        });*/
       /* if (reveiver!=null){
            unregisterReceiver(reveiver);
        }
        reveiver=new DownCompleteReveiver();
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(reveiver,intentFilter);*/

        webView.setWebChromeClient(new WebChromeClient(){
/*
            //for Android<3.0
            public void openFileChooser(ValueCallback<Uri>valueCallback){
                uploadMessage=valueCallback;  
                openFileChooserActivity();

            //for Android>=3.0
            public void openFileChooser(ValueCallback valueCallback,String acceptType){
                uploadMessage=valueCallback;
                openFileChooserActivity();
            }

            //for Android>=4.1
            public void openFileChooser(ValueCallback valueCallback,String acceptType,String capture){
                uploadMessage=valueCallback;
                openFileChooserActivity();
            }*/

            //for Android>=5.0

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                uploadMessageAboveL=filePathCallback;
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    } else {
                        openFileChooserActivity();
                    }
                }else {
                    openFileChooserActivity();
                }
                return true;
            }
        });
        //加载网页
        String targetUrl="https://www31.eiisys.com/webCompany.php?arg=10250660&kf_sign=jU3ODMTY0MkwMDEwOTQwNDgyNjI3MDE0NzIyNTA2NjA%253D&style=1";
        webView.loadUrl(targetUrl);
    }

  /*  @Override
    protected void onDestroy() {
        super.onDestroy();
        if(reveiver!=null){
            unregisterReceiver(reveiver);
        }
    }*/

    //回调方法触发本地选择文件
    private void openFileChooserActivity() {
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent,"File chooser"),FILE_CHOOSER_RESULT_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    openFileChooserActivity();
                }else {
                    Toast.makeText(this,"你拒绝了权限，无法发送文件",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }
    //选择文件后处理
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==FILE_CHOOSER_RESULT_CODE){
            if(null==uploadMessage&&null==uploadMessageAboveL)return;
            Uri result=data==null||requestCode!=RESULT_OK?null:data.getData();
            if(uploadMessageAboveL!=null){
                onActivityResultAboveL(requestCode,resultCode,data);
            }else if(uploadMessage!=null){
                uploadMessage.onReceiveValue(result);
                uploadMessage=null;
            }
        }else {
            if(uploadMessage!=null){
                uploadMessage.onReceiveValue(null);
                uploadMessage=null;
            }else if(uploadMessageAboveL!=null){
                uploadMessageAboveL.onReceiveValue(null);
                uploadMessageAboveL=null;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode,int resultCode,Intent intent){
        if(requestCode!=FILE_CHOOSER_RESULT_CODE||uploadMessageAboveL==null)
            return;
        Uri[]results=null;
        if(resultCode==Activity.RESULT_OK){
            if(intent!=null){
                String dataString=intent.getDataString();
                ClipData clipData=intent.getClipData();
                if(clipData!=null){
                    results=new Uri[clipData.getItemCount()];
                    for (int i=0;i<clipData.getItemCount();i++){
                        ClipData.Item item=clipData.getItemAt(i);
                        results[i]=item.getUri();
                    }
                }
                if(dataString!=null)
                    results=new Uri[]{Uri.parse(dataString)};
            }
        }
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL=null;
    }

  //  private class DownCompleteReveiver extends BroadcastReceiver{
       // @Override
       // public void onReceive(Context context, Intent intent) {
           // Log.d("onReceive.intent:{}",intent!=null?intent.toUri(0):null);
           // if (intent!=null){
               // if(DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())){
                   // long downloadId=intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
                   // Log.d("downloadId：{}",downloadId+"");
                    //DownloadManager downloadManager=(DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                   // String type=downloadManager.getMimeTypeForDownloadedFile(downloadId);
                    //Log.d("MTForDownloadedFile:{}",type);
                    //if (TextUtils.isEmpty(type)){
                       // type="*/*";
                   // }
                  //  Uri uri=downloadManager.getUriForDownloadedFile(downloadId);
                   // File file =new File(uri.getPath());

                   // Log.d("UriForDownloadedFile:{}",uri+"");
                   //if (uri!=null){
                   //   Intent handlerIntent=new Intent(Intent.ACTION_VIEW);
                       // handlerIntent.setDataAndType(uri,type);
                       // handlerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        //context.startActivity(handlerIntent);
                   // }

               // }
           // }
       // }
   // }

   /* private void downloadBySystem(String url,String contentDisposition,String mimeType){
        //指定下载地址
        DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
        //允许媒体扫描，根据下载的文件类型被加入相册、音乐等媒体库
        request.allowScanningByMediaScanner();
        //设置通知的显示类型，下载完成后显示通知
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle("下载通知");
        request.setDescription("下载完成！");
        //允许在计费流量下下载
        request.setAllowedOverMetered(false);
        //允许该记录在下载管理界面可见
        request.setVisibleInDownloadsUi(false);
        //允许漫游时下载
        request.setAllowedOverRoaming(true);
        //允许下载的网络类型
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        //设置下载文件保存的路径和文件名
        String fileName= URLUtil.guessFileName(url,contentDisposition,mimeType);
        Log.d("filename:{}",fileName);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,fileName);
        final DownloadManager downloadManager=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        long downloadId=downloadManager.enqueue(request);
        Log.d("downloadId","downloadId:{}"+downloadId);
    }*/
}