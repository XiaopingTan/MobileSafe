package com.txp.mobilesafe.activity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Currency;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.txp.mobilesafe.R;
import com.txp.mobilesafe.R.id;
import com.txp.mobilesafe.R.layout;
import com.txp.mobilesafe.utils.StreamUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class SplashActivity extends Activity {

	protected static final int CODE_UPDATE_DIALOG = 0;
	protected static final int CODE_URL_ERROR = 1;
	protected static final int CODE_NET_ERROR = 2;
	protected static final int CODE_JSON_ERROR = 3;
	protected static final int CODE_ENTER_HOME = 4;

	private TextView tvVersion;     //显示版本号的文本框
	private TextView tvProgress;    //下载进度展示
	
	//服务器返回的信息
	private String mVersionName;    //版本名
	private int mVersionCode;       //版本号
	private String mDescription;    //版本描述
	private String mDownloadUrl;    //下载地址
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case CODE_UPDATE_DIALOG:
				showUpdateDialog();
				break;
			case CODE_URL_ERROR:
				Toast.makeText(SplashActivity.this, "url异常", Toast.LENGTH_SHORT).show();
				enterHome();
			case CODE_NET_ERROR:
				Toast.makeText(SplashActivity.this, "网络异常", Toast.LENGTH_SHORT).show();
				enterHome();
			case CODE_JSON_ERROR:
				Toast.makeText(SplashActivity.this, "数据解析异常", Toast.LENGTH_SHORT).show();
				enterHome();
			case CODE_ENTER_HOME:
				enterHome();
			default:
				break;
			}
		};
	};
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		tvVersion = (TextView) findViewById(R.id.tv_version);
		tvVersion.setText("版本号：" + getVersionName());
		
		tvProgress = (TextView) findViewById(R.id.tv_progress);   //默认隐藏
		checkVersion();   //检查版本
	}

	/**
	 * 获取本地版本名称
	 */
	private String getVersionName() {
		PackageManager packageManager = getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(
					"com.txp.mobilesafe", 0);
			String versionName = packageInfo.versionName;
			return versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 获取本地的版本号
	 * @return
	 */
	private int getVersionCode() {
		PackageManager packageManager = getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(
					"com.txp.mobilesafe", 0);
			int versionCode = packageInfo.versionCode;
			return versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	/**
	 * 从服务器获取版本信息
	 */
	private void checkVersion() {
		
		final long startTime = System.currentTimeMillis();   //开始访问时间
		
		//启动子线程异步加载数据
		new Thread() {
			Message msg = Message.obtain();
			private HttpURLConnection connection = null;
			@Override
			public void run() {
				try {
					//本机地址用Localhost,但是如果用模拟加载本机地址时，可以用ip(10.0.2.2)来替换
					URL url = new URL("http://10.0.2.2:8080/update.json");
						connection = (HttpURLConnection) url.openConnection();
						connection.setRequestMethod("GET");  //设置请求方法
						connection.setConnectTimeout(5000);  //设置连接超时
						connection.setReadTimeout(5000);     //设置读取超时
						connection.connect();      //连接服务器
						int responseCode = connection.getResponseCode();  //获取响应码
						if (responseCode == 200) {
							InputStream inputStream = connection.getInputStream();
							String result = StreamUtils.readFromStream(inputStream);
							//System.out.println("服务器返回：" + result);
							
							//解析json
							JSONObject jo = new JSONObject(result);
							
							mVersionName = jo.getString("versionName");
							mVersionCode = jo.getInt("versionCode");
							mDescription = jo.getString("description");
							mDownloadUrl = jo.getString("downloadURL");
							
							//System.out.println("版本描述：" + mDescription);
							
							//判断是否有更新
							if (mVersionCode > getVersionCode()) {
								//服务器的versionCode大于本地的versionCode
								//说明有更新，弹出升级对话框
								msg.what = CODE_UPDATE_DIALOG;
							} else {
								//没有更新，跳转到主页面
								msg.what = CODE_ENTER_HOME;
							}
						}
   						
					} catch(MalformedURLException e) {
						//url错误的异常
						msg.what = CODE_URL_ERROR;
						e.printStackTrace();
					} catch (IOException e) {
						// 网络错误异常
						msg.what = CODE_NET_ERROR;
						e.printStackTrace();
					} catch (JSONException e) {
						//json解析失败
						msg.what = CODE_JSON_ERROR;
						e.printStackTrace();
					} finally {
						long endTime = System.currentTimeMillis();  //访问服务器结束时的时间
						//如果访问总时间小于3秒，开启线程强制睡眠，保证闪屏页展示三秒钟
						if ((endTime-startTime) < 3000) {
							try {
								Thread.sleep(3000-(endTime-startTime));
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						
						mHandler.sendMessage(msg);
						
						//关闭网络连接
						if (connection != null) {
							connection.disconnect();
						}
					}
			}

		}.start();
	}

	/**
	 * 升级提示对话框
	 */
	protected void showUpdateDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("升级提示！");
		builder.setMessage("发现新版本：" + mVersionName + "\n" + "新版本特性：" + mDescription);
		builder.setPositiveButton("立即更新", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Toast.makeText(SplashActivity.this, "正在更新...", Toast.LENGTH_SHORT).show();
				download();
			}
		});
		
		builder.setNegativeButton("暂不更新", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				enterHome();
			}
		});
		
		//设置取消事件的监听，用户在提示框界面点击返回键时会触发
		builder.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				enterHome();
			}
		});
		
		builder.show();
	}

	/**
	 * 跳转到主页面
	 */
	private void enterHome(){
		Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
		startActivity(intent);
		//结束欢迎页面
		finish();
	}
	
	/**
	 * 下载新版本APK
	 */
	protected void download() {
		//先判断内存卡是否存在,如果存在就开始下载
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			
			tvProgress.setVisibility(View.VISIBLE);    //设置进度框为可见
			
			String target = Environment.getExternalStorageDirectory() + "/update.apk";
			HttpUtils utils = new HttpUtils();
			utils.download(mDownloadUrl, target, new RequestCallBack<File>() {
				
				//下载文件的进度
				@Override
				public void onLoading(long total, long current,
						boolean isUploading) {
					super.onLoading(total, current, isUploading);
					System.out.println("下载进度：" + current + "/" + total);
					tvProgress.setText("下载进度：" + current*100/total + "%");
				}
				
				//下载成功，该方法在主线程运行
				@Override
				public void onSuccess(ResponseInfo<File> arg0) {
					System.out.println("下载成功！");
				}
				
				//下载失败，该方法在主线程运行
				@Override
				public void onFailure(HttpException arg0, String arg1) {
					Toast.makeText(SplashActivity.this, "下载失败！", Toast.LENGTH_SHORT).show();
				}
			});
			
		} else {
			//如果不存在，弹出提示
			Toast.makeText(SplashActivity.this, "没有找到SD卡", Toast.LENGTH_SHORT).show();
		}
	}

}
