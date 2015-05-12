package com.youeclass.service;

import java.io.File;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.StatFs;
import android.util.Log;
import android.widget.Toast;

import com.youeclass.downloads.MultiThreadDownload;
import com.youeclass.entity.DowningCourse;
///**
// * service是看不见的Activity 下载服务,在服务里启动线程进行下载,同时注册广播,接收来自Activity的广播信息 在线程中发送
// * 特定的广播让activity进行接收,更新UI
// * 
// * @author Administrator
// * 
// */
/**
 * 课程视频下载服务。
 * @author jeasonyoung
 *
 */
public class DownloadService extends Service {
	 private static final String TAG = "DownloadService";
	 private static final long THREAD_SLEEP = 500;
	 
	 private ConnectivityManager connectivityManager;
	 private SharedPreferences settings;
	 
	 private ExecutorService pools;
	 
	 private BlockingQueue<DowningCourse> downloadQueue;
	 private Map<DowningCourse, MultiThreadDownload> downloadThreads;
	 private Map<DowningCourse, Integer> downloadPositions;
	 private Handler downloadHandler;
	 
	 private IBinder binder;
	 
	 private File downloadRootDir;
	 private boolean isStop;
	/**
	 * 构造函数。
	 */
	public DownloadService(){
		//单线程池
		this.pools = Executors.newSingleThreadExecutor();
		//下载队列(线程安全)
		this.downloadQueue = new LinkedBlockingQueue<DowningCourse>();
		//下载线程集合(线程安全)
		this.downloadThreads = new ConcurrentHashMap<DowningCourse, MultiThreadDownload>();
		//下载课程位置集合(线程安全)
		this.downloadPositions = new ConcurrentHashMap<DowningCourse, Integer>();
		//
		this.binder = new FileDownloadServiceBinder();
	}
	/*
	 * 重载服务绑定。
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "下载服务被绑定...");
		this.isStop = false;
		return this.binder;
	}
	/*
	 * 重载创建。
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		Log.d(TAG, "下载服务正在被创建...");
		//网络连接服务
		this.connectivityManager = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		//获取配置
		this.settings = this.getSharedPreferences("settingfile", 0);
		//获取下载存储根目录
		this.downloadRootDir = Environment.getExternalStorageDirectory();
		//执行文件下载管理线程。
		pools.execute(new FileDownloadManagerThread());
		super.onCreate();
	}
	/*
	 * 重载服务摧毁时方法。
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		Log.d(TAG, "下载服务将被摧毁...");
		this.isStop = true;
		//停止下载线程。
		pools.shutdown();
		super.onDestroy();
	}
	
	//检测网络
	private boolean checkNetwork(){
		Log.d(TAG, "开始检查网络...");
		NetworkInfo network = this.connectivityManager.getActiveNetworkInfo();
		if(network != null){
			State state = network.getState();
			switch(network.getType()){
				case ConnectivityManager.TYPE_WIFI:{//wifi
					if(state  == State.CONNECTED || state == State.CONNECTING){
						return true;
					}
					break;
				}
				case ConnectivityManager.TYPE_MOBILE:{
					if(state  == State.CONNECTED || state == State.CONNECTING){
						boolean isDownUse3G = this.settings.getBoolean("setDownIsUse3G", true);
						if(!isDownUse3G){
							Toast.makeText(this, "当前网络为2G/3G,要下载请修改设置或开启wifi", Toast.LENGTH_LONG).show();
						}else{
							Toast.makeText(this, "当前网络为2G/3G", Toast.LENGTH_LONG).show();
						}
						return isDownUse3G;
					}
					break;
				}
				default:break;
			}
		}
		Log.d(TAG, "当前网络类型:["+network.getTypeName()+"],网络状态:["+network.getState()+"]!");
		Toast.makeText(this, "请检查您的网络!",Toast.LENGTH_LONG).show();
		return false;
	}
	/**
	 * 创建文件下载目录。
	 * @param course
	 * @return
	 */
	private File createDownloadSaveFileDir(DowningCourse course) throws Exception{
		if(this.downloadRootDir == null || !this.downloadRootDir.exists()){
			Log.d(TAG, "SD卡不存在！");
			 throw new Exception("未检测到SD卡，请检查SD是否插好！");
		}
		return new File(this.downloadRootDir + File.separator + "eschool" + File.separator + course.getUserName() + File.separator + "video");
	}
	//检查存储容量
	private boolean checkStorageCapacity(long fileSIze) throws Exception{
		Log.d(TAG, "检查SD卡容量：" + fileSIze);
		boolean result = true;
		if(fileSIze > 0){
			StatFs statFs = new StatFs(this.downloadRootDir.getPath());
			result = (statFs.getAvailableBlocks() * statFs.getBlockSize()) > fileSIze;
			if(!result){
				throw new Exception("可用容量不足，不能下载！");
			}
		}
		return result;
	}
	/**
	 * 添加课程下载。
	 * @param course
	 * @param pos
	 */
	protected synchronized void addCourseDownload(final DowningCourse course, final int pos){
		if(course == null || pos < 0) return;
		//异步处理
		new AsyncTask<String, Integer, Integer>(){
			@Override
			protected Integer doInBackground(String... params) {
				Log.d(TAG, "添加下载课程["+pos+"."+course+"]到队列...");
				 
				boolean result = false;
				if(!downloadQueue.contains(course) && ((course.getState() != DowningCourse.STATE_PAUSE))){//如果队列中不存在则加入对尾
						result = downloadQueue.offer(course);
						Log.d(TAG, "压入到队尾:" + result); 
				}
				//设置位置集合
				downloadPositions.put(course, pos);
				//发送UIHandler
				String msg = result ? "已添加到下载队列，等候排队连接!" : "下载队列中已存在!";
				Log.d(TAG, msg);
				sendHandlerMessage(course, DowningCourse.STATE_INIT, msg);
				return null;
			}
		}.execute(null,null);
	}
	/**
	 * 取消课程下载。
	 * @param course
	 */
	protected synchronized void cancelCourseDownload(final DowningCourse course){
		if(course == null)return;
		//异步处理
		new AsyncTask<String, Integer, Integer>(){
			@Override
			protected Integer doInBackground(String... params) {
				Log.d(TAG, "取消课程["+course.getCourseName()+"]下载...");
				//如果在队列中排序则从队列中移除
				if(downloadQueue.contains(course)){
					boolean result = downloadQueue.remove(course);
					Log.d(TAG, "从队列中移除:" + result);
				}
				//移除相关缓存
				removeCourseCache(course);
				//发送UIHandler
				String msg = course.getCourseName() + " 已取消下载!";
				Log.d(TAG, msg);
				sendHandlerMessage(course, DowningCourse.STATE_CANCEL, msg);
				return null;
			}
		}.execute(null,null);
	}
	/**
	 * 暂停课程下载。
	 * @param course
	 */
	protected synchronized void pauseCourseDownload(final DowningCourse course){
		if(course == null)return;
		new AsyncTask<String, Integer, Integer>(){
			@Override
			protected Integer doInBackground(String... params) {
				Log.d(TAG, "暂停课程["+course.getCourseName()+"]下载...");
				//从下载线程集合中获取下载线程
				MultiThreadDownload threadDownload = downloadThreads.get(course);
				if(threadDownload != null){
					Log.d(TAG, "停止线程下载...");
					threadDownload.Stop();
				}
				//发送UIHandler
				String msg = course.getCourseName() + " 下载已暂停!";
				Log.d(TAG, msg);
				sendHandlerMessage(course, DowningCourse.STATE_PAUSE,  msg);
				return null;
			}
		}.execute(null,null);
	}
	/**
	 * 继续课程下载。
	 * @param course
	 */
	protected synchronized void continueCourseDownload(final DowningCourse course){
		if(course == null)return;
		new AsyncTask<String, Integer, Integer>(){
			@Override
			protected Integer doInBackground(String... params) {
				Log.d(TAG, "继续课程["+course.getCourseName()+"]下载...");
				if(!downloadQueue.contains(course)){//如果队列中不存在则加入对尾
					boolean result = downloadQueue.offer(course);
					Log.d(TAG, "压入到队尾:" + result);
				}
				//发送UIHandler
				String msg = course.getCourseName() + " 重新进入排队中...";
				Log.d(TAG, msg);
				sendHandlerMessage(course, DowningCourse.STATE_WAITTING,  msg);
				return null;
			}
		}.execute(null,null);
	}
	/**
	 * 移除课程相关缓存
	 * @param course
	 */
	private synchronized void removeCourseCache(DowningCourse course){
		Log.d(TAG, "移除课程["+course.getCourseName()+"]下载相关缓存...");
		//移除下载线程对象
		if(this.downloadThreads.size() > 0 && this.downloadThreads.containsKey(course)){
			MultiThreadDownload threadDownload = this.downloadThreads.get(course);
			//如果现在存在，就停止下载
			if(threadDownload != null){
				threadDownload.Stop();
			}
			//从集合中移除
			this.downloadThreads.remove(course);
			Log.d(TAG, "移除课程["+course.getCourseName()+"]的下载线程对象缓存!");
		}
		//移除下载列表位置。
		if(this.downloadPositions.size() > 0 && this.downloadPositions.containsKey(course)){
			this.downloadPositions.remove(course);
			Log.d(TAG, "移除课程["+course.getCourseName()+"]的下载列表位置缓存!");
		}
	}
	/**
	 * 获取下载线程。
	 * @param course
	 * @return
	 */
	private synchronized MultiThreadDownload getDownloadThread(DowningCourse course) {
		 Log.d(TAG, "获取下载课程[" + course.getCourseName() +"]线程...");
		 return  (this.downloadThreads.size() == 0) ? null : this.downloadThreads.get(course);
	}
	/**
	 * 添加下载线程。
	 * @param course
	 * @param thread
	 */
	private synchronized void addDownloadThread(DowningCourse course, MultiThreadDownload thread){
		Log.d(TAG, "添加课程["+course.getCourseName()+"]下载线程到缓存...");
		if(course != null && thread != null){
			//设置文件大小
			course.setFileSize(thread.getFileSize());
			Log.d(TAG, "设置文件大小=>"+thread.getFileSize()+"/" + course.getFileSize() + "...");
			//添加到下载线程集合
			this.downloadThreads.put(course, thread);
		}
	}	
	/**
	 * 发送处理消息
	 * @param course
	 * @param msgType
	 * @param msg
	 */
	private synchronized void sendHandlerMessage(DowningCourse course,int msgType,String msg){
		if(course == null)return;
		Log.d(TAG, "发送前台UI处理消息：" + msgType);
		if(this.downloadHandler == null){
			Log.d(TAG, "前台处理Handler为null.");
			return;
		}
		int pos = this.downloadPositions.size() == 0 ? -1 :  this.downloadPositions.get(course);
		Log.d(TAG, "发送课程["+pos+"."+course.getCourseName()+"]前台UI处理消息：type=>" + msgType +",msg=>" + msg);
		this.downloadHandler.sendMessage(this.downloadHandler.obtainMessage(msgType, pos, 0, msg));
	}
	/**
	 * 发送下载进度。
	 * @param course
	 * @param totalFileSize
	 */
	private synchronized void sendHandlerDownloadProgress(DowningCourse course, long totalFileSize){
		if(course == null)return;
		if(this.downloadHandler == null){
			Log.d(TAG, "前台处理Handler为null.");
			return;
		}
		//设置课程下载量
		course.setFinishSize(totalFileSize);
		int pos = this.downloadPositions.size() == 0 ? -1 :  this.downloadPositions.get(course);
		Log.d(TAG, "发送课程["+pos+"."+course.getCourseName()+"]下载进度:"+ totalFileSize);
		this.downloadHandler.sendMessage(this.downloadHandler.obtainMessage(DowningCourse.STATE_DOWNING, pos, 0, Long.valueOf(totalFileSize)));
	}
	
	//文件下载管理线程(负责轮询下载队列)。
	private final class FileDownloadManagerThread extends Thread{
		private static final String TAG = "FileDownloadManagerThread";
		//线程执行体
		@Override
		public void run() {
			Log.d(TAG, "下载服务队列轮询线程启动...");
			while(!isStop){
				try {
					//线程等待
					Thread.sleep(THREAD_SLEEP);
					//从队列中获取需要下载的课程
					DowningCourse course = downloadQueue.poll();
					if(course != null){
						Log.d(TAG, "开始下载课程：" + course.getCourseName() + "...");
						//检查网络
						if(!checkNetwork()){
							sendHandlerMessage(course, DowningCourse.STATE_NETFAIL, "网络不可用");
							continue;
						}
						//下载线程创建
						MultiThreadDownload threadDownload = getDownloadThread(course);
						if(threadDownload == null){
							try {
								File savePath = createDownloadSaveFileDir(course);
								threadDownload = new MultiThreadDownload(DownloadService.this, course.getUserName(), course.getFileUrl(), savePath);
								addDownloadThread(course, threadDownload);
							} catch (Exception e) {
								Log.e(TAG, "创建课程["+course.getUserName()+"]下载线程时发生异常:" + e.getMessage(), e);
								sendHandlerMessage(course, DowningCourse.STATE_NETFAIL, e.getMessage());
								continue;
							}
						}
						//检查磁盘容量
						try {
							checkStorageCapacity(threadDownload.getFileSize());
						} catch (Exception e) {
							Log.e(TAG, "检查磁盘容量：" + e.getMessage(), e);
							sendHandlerMessage(course, DowningCourse.STATE_PAUSE, e.getMessage());
							continue;
						}
						//开始下载数据。
						long totalSize = threadDownload.download(new OnDownloadProgressListener(course));
						if(totalSize == threadDownload.getFileSize()){//下载完成
							sendHandlerMessage(course, DowningCourse.STATE_FINISH, "下载完成!");
							//移除全部缓存
							removeCourseCache(course);
						}else{//暂停
							sendHandlerMessage(course, DowningCourse.STATE_PAUSE, "下载被停止!");
						}
					}					
				} catch (Exception e) {
					Log.d(TAG, "下载队列轮询线程发生异常:" + e.getMessage(),e);
				}
			}
		}
	}
	//下载进程更新
	private final class OnDownloadProgressListener implements MultiThreadDownload.OnDownloadProgressListener{
		private DowningCourse course;
		/**
		 * 构造函数。
		 * @param course
		 */
		public OnDownloadProgressListener(DowningCourse course) {
			 this.course = course;
		}
		/*
		 * 更新进度
		 * @see com.youeclass.downloads.MultiThreadDownload.OnDownloadProgressListener#onDownloadSize(long)
		 */
		@Override
		public void onDownloadSize(long size) {
			//发送下载进度
			sendHandlerDownloadProgress(this.course, size);
		}
	}
	/**
	 * 课程下载服务接口实现。
	 * @author jeasonyoung
	 *
	 */
	private final class FileDownloadServiceBinder extends Binder implements IFileDownloadService{
		private static final String TAG = "FileDownloadServiceBinder";
		/*
		 * 设置UI处理Handler。
		 * @see com.youeclass.service.DownloadService.IFileDownloadService#setHandler(android.os.Handler)
		 */
		@Override
		public void setHandler(Handler handler) {
			if(handler == null)return;
			 Log.d(TAG, "设置UI处理Handler...");
			 downloadHandler = handler;
		}
		/*
		 * 添加下载课程
		 * @see com.youeclass.service.DownloadService.IFileDownloadService#addDownload(com.youeclass.entity.DowningCourse, int, android.os.Handler)
		 */
		@Override
		public void addDownload(DowningCourse course, int position) {
			Log.d(TAG, "添加下载课程["+course+"]到队列...");
			addCourseDownload(course, position);
		}
		/*
		 * 取消下载
		 * @see com.youeclass.service.DownloadService.IFileDownloadService#cancelDownload(com.youeclass.entity.DowningCourse)
		 */
		@Override
		public void cancelDownload(DowningCourse course) {
			Log.d(TAG, "取消课程["+course.getCourseName()+"]下载...");
			cancelCourseDownload(course);
		}
		/*
		 * 暂停下载
		 * @see com.youeclass.service.DownloadService.IFileDownloadService#pauseDownload(com.youeclass.entity.DowningCourse)
		 */
		@Override
		public void pauseDownload(DowningCourse course) {
			Log.d(TAG, "暂停课程["+course.getCourseName()+"]下载...");
			pauseCourseDownload(course);
		}
		/*
		 * 继续下载
		 * @see com.youeclass.service.DownloadService.IFileDownloadService#continueDownload(com.youeclass.entity.DowningCourse)
		 */
		@Override
		public void continueDownload(DowningCourse course) {
			Log.d(TAG, "继续课程["+course.getCourseName()+"]下载...");
			continueCourseDownload(course);
		}
	}
	/**
	 * 文件下载服务接口。
	 * @author jeasonyoung
	 *
	 */
	public interface IFileDownloadService{
		/**
		 * 设置UI处理Handler。
		 * @param handler
		 */
		void setHandler(Handler handler);
		/**
		 * 添加下载。
		 * @param course
		 * @param position
		 */
		void addDownload(DowningCourse course, int position);
		/**
		 * 取消下载。
		 * @param course
		 */
		void cancelDownload(DowningCourse course);
		/**
		 * 暂停下载。
		 * @param course
		 */
		void pauseDownload(DowningCourse course);
		/**
		 * 继续下载。
		 * @param course
		 */
		void continueDownload(DowningCourse course);
	}
}