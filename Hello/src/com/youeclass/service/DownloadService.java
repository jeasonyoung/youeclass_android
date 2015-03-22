package com.youeclass.service;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.youeclass.downloader.SmartDownloadProgressListener;
import com.youeclass.downloader.SmartFileDownloader;

/**
 * service是看不见的Activity 下载服务,在服务里启动线程进行下载,同时注册广播,接收来自Activity的广播信息 在线程中发送
 * 特定的广播让activity进行接收,更新UI
 * 
 * @author Administrator
 * 
 */
public class DownloadService extends Service {
	// private List<SmartFileDownloader> downloaderList;
	private static int downloaderCount;
	private static Handler mHandler;
	private Map<String, SmartFileDownloader> downloaderMap;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		cmdReceiver = new CommandReceiver();
		mHandler = new MyHandler(this);
		// 注册广播
		IntentFilter filter = new IntentFilter();
		filter.addAction("commandFromActivity");// 定义接收什么样的广播
		registerReceiver(cmdReceiver, filter);
		this.downloaderMap = new HashMap<String, SmartFileDownloader>();
		super.onCreate();
	}

	// service可以重复调用
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		downloaderCount++;
		if (downloaderCount >= 3) {
			Toast.makeText(this, "最多允许两个任务同时下载", Toast.LENGTH_LONG).show();
			return super.onStartCommand(intent, flags, startId);
		}
		String url = intent.getStringExtra("url");
		String dir = intent.getStringExtra("dir");
		String username = intent.getStringExtra("username");
		dojob(url, dir, username); // 启动线程去下载
		return super.onStartCommand(intent, flags, startId);
	}

	private void dojob(String id, String dir, String username) {
		new MyThread(id, new File(dir + "/" + username), username).start();
	}

	// 定一个线程启动下载器
	private class MyThread extends Thread {
		private String url;
		private File dir;
		private String username;

		public MyThread(String id, File dir, String username) {
			// TODO Auto-generated constructor stub
			this.url = id;
			this.dir = dir;
			this.username = username;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			// 定一个下载器,下载器在初始化时会进行一些耗时的操作
			SmartFileDownloader loader = new SmartFileDownloader(
					DownloadService.this, url, dir, 2, username,
					DownloadService.mHandler);
			downloaderMap.put(url, loader);
			try {
				// 启动下载并且监听下载进度
				System.out
						.println("!!!!!!!!!!!!!start the download listener!!!!!!!!!!!!");
				loader.download(new SmartDownloadProgressListener() {
					@Override
					public void onDownloadSize(int size) {
						// TODO Auto-generated method stub
						if (size > 0) {
							Intent intent = new Intent();// 创建Intent对象
							intent.setAction("updateUI"); // 定义广播类型
							intent.putExtra("url", url); // 绑定数据
							intent.putExtra("data", size);
							sendBroadcast(intent);// 发送广播
							System.out
									.println("!!!!!downloadService set broadcase!!!!!!");
						}
					}
				});
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				// 下载失败
			}
		}
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		// 注销广播
		unregisterReceiver(cmdReceiver);
		downloaderMap.clear();
		super.onDestroy();
	}

	// 定义一个广播接收者
	private class CommandReceiver extends BroadcastReceiver {
		// 接收来自Activity的广播,从而控制各个下载线程
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			int cmd = intent.getIntExtra("cmd", -1);
			final String url = intent.getStringExtra("url");
			final String path = intent.getStringExtra("path");
			if (cmd == 0) // 表示暂停
			{
				// 设置下载器里的标识
				SmartFileDownloader.flagMap.put(url, false);// 停止线程
				// downloaderMap.remove(url);
				downloaderCount--;
			} else if (cmd == 2) {
				// 设置下载器里的标识
				if (SmartFileDownloader.flagMap.get(url)) {
					SmartFileDownloader.flagMap.put(url, false);// 停止线程
					new Thread() {
						public void run() {
							SmartFileDownloader l = downloaderMap.get(url);
							while (!l.isStop()) {
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							Log.i("DownloadService", "该下载任务已经停止");
							File file = l.getSaveFile();
							file.delete();
							Log.i("DownloadService", file.exists() + "文件还存在么?");
							mHandler.sendEmptyMessage(3);
							downloaderMap.remove(url);
							downloaderCount--;
						};
					}.start();
				} else {
					SmartFileDownloader l = downloaderMap.get(url);
					File f = null;
					if (l == null) {
						f = new File(path);
					} else {
						f = l.getSaveFile();
						downloaderMap.remove(url);
					}
					if (f.exists()) {
						boolean flag = f.delete();
						Toast.makeText(DownloadService.this,
								"删除文件" + (flag ? "成功" : "失败"),
								Toast.LENGTH_SHORT).show();
					}
				}
			} else {
				// 结束所有进程同时停止服务
				stopAll();
				downloaderMap.clear();
				downloaderCount = 0;
				// 停止服务
				stopSelf();

			}
		}
	}

	private void stopAll() {
		Map<String, Boolean> map = SmartFileDownloader.flagMap;
		for (String s : map.keySet()) {
			map.put(s, false);
		}
		downloaderCount = 0;
	}

	// 必须持有一个接收者的引用,为注册这个广播定义接收器
	private CommandReceiver cmdReceiver;

	private static class MyHandler extends Handler {
		private WeakReference<DownloadService> weak;

		public MyHandler(DownloadService context) {
			// TODO Auto-generated constructor stub
			weak = new WeakReference<DownloadService>(context);
		}

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch (msg.what) {
			case -1:
				Toast.makeText(weak.get(), "服务器没有响应", Toast.LENGTH_SHORT)
						.show();
				break;
			case -2:
				Toast.makeText(weak.get(), "无法连接到下载地址", Toast.LENGTH_SHORT)
						.show();
				break;
			case -3:
				Toast.makeText(weak.get(), "未知文件大小", Toast.LENGTH_SHORT).show();
				break;
			case 3:
				Toast.makeText(weak.get(), "删除成功", Toast.LENGTH_SHORT).show();
				break;
			}
		}
	}
}
