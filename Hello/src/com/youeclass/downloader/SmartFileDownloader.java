package com.youeclass.downloader;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.youeclass.dao.DownloadDao;
import com.youeclass.entity.DownloadItem;

/**
 * 文件下载器
 * 
 */
public class SmartFileDownloader {
	private static final String TAG = "SmartFileDownloadItem";
	private Context context;
	private String username;
	private DownloadDao DownloadDao;
	/* 已下载文件长度 */
	private int downloadSize = 0;
	/* 原始文件长度 */
	private int fileSize = 0;
	/* 线程数 */
	private SmartDownloadThread[] threads;
	/* 每条线程下载的长度 */
	private int piece;
	/* 本地保存文件 */
	private File saveFile;
	/* 下载路径 */
	private String downloadUrl;
	/* 下载 线程数据的集合 */
	private List<DownloadItem> list;
	/* 是否没下完 */
	private boolean isExist = false;
	private boolean isOver = false;
	/* 各个任务的暂停标识 */
	public static Map<String,Boolean> flagMap = new HashMap<String,Boolean>();
	/*handler*/
	private Handler mHandler;
	/**
	 * 获取线程数
	 */
	public int getThreadSize() {
		return threads.length;
	}
	/**
	 * 获取文件大小
	 * 
	 * @return
	 */
	public int getFileSize() {
		return fileSize;
	}

	/**
	 * 累计已下载大小
	 * 
	 * @param size
	 */
	protected synchronized void append(int size) {
		downloadSize += size;
	}

	/**
	 * 更新指定线程最后下载的位置
	 */ 
	protected synchronized void update(DownloadItem loader) {
		this.DownloadDao.update(loader);
	}
	/**
	 * 构建文件下载器
	 * 
	 * @param downloadUrl
	 *            下载路径
	 * @param fileSaveDir
	 *            文件保存目录
	 * @param threadNum
	 *            下载线程数
	 */
	public SmartFileDownloader(Context context, String downloadUrl,
			File fileSaveDir, int threadNum,String username,Handler handler) {
		try {
			System.out.println("!!!! init the downloader !!!!");
			this.context = context;
			this.downloadUrl = downloadUrl;
			this.username = username;
			this.mHandler = handler;
			DownloadDao = new DownloadDao(this.context);
			URL url = new URL(this.downloadUrl);
			if (!fileSaveDir.exists()) {
				fileSaveDir.mkdirs();
			}
			this.threads = new SmartDownloadThread[threadNum];
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5 * 1000);
			conn.setRequestMethod("GET");
			conn.setRequestProperty(
					"Accept",
					"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
			conn.setRequestProperty("Accept-Language", "zh-CN");
			conn.setRequestProperty("Referer", downloadUrl);
			conn.setRequestProperty("Charset", "UTF-8");
			conn.setRequestProperty(
					"User-Agent",
					"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.connect();
			//printResponseHeader(conn);
			if (conn.getResponseCode() == 200) {
				this.fileSize = conn.getContentLength();// 根据响应获取文件大小
				if (this.fileSize <= 0)
				{
					//throw new RuntimeException("Unkown file size ");
					mHandler.sendEmptyMessage(-3);
				}
				String filename = getFileName(conn);
				this.saveFile = new File(fileSaveDir, filename);/* 保存文件 */
				list = DownloadDao.findByUrl(downloadUrl,username);
				if (this.saveFile.exists()) {
					System.out.println(list.size());
					if (list.size() > 0) {
						// 表示还有没有下载完成的
						if (list.size() == this.threads.length) {
							for (int i = 0; i < list.size(); i++) {
								// 已经下载的长度
								this.downloadSize += list.get(i)
										.getCompleteSize();
							}
							print("已经下载的长度" + this.downloadSize);
						}
						isExist = true;
					} else {
						print("文件已经下载");
						//删除文件,重新下载
						this.saveFile.delete();
					}
				} 
				// 文件还不存在
				if(!this.saveFile.exists()){
					// 将文件分块,如果不能整除,余数放到最后一块
					this.piece = this.fileSize / this.threads.length; // 每一块的大小
					//第一块 0-(piece-1)
					//第二块 piece*1-piece*2-1
					if (!isExist) {	
						DownloadDao.deleteAll(this.downloadUrl,username);//文件存在记录存在没有意义
						list.clear();
						DownloadItem loader = new DownloadItem();
						loader.setStartPos(0);
						loader.setThreadId(1);
						loader.setEndPos(this.piece-1);
						loader.setCompleteSize(0);
						loader.setUrl(downloadUrl);
						loader.setUsername(username);
						list.add(loader);
						for (int i = 1; i < threads.length - 1; i++) {
							DownloadItem loader1 = new DownloadItem();
							loader1.setStartPos(this.piece*i);
							loader1.setThreadId(i + 1);
							loader1.setEndPos(this.piece*(i+1)-1);
							loader1.setCompleteSize(0);
							loader1.setUrl(downloadUrl);
							loader1.setUsername(username);
							list.add(loader1);
						}
						if (threadNum > 1) {
							DownloadItem loader2 = new DownloadItem();
							loader2.setStartPos(list.get(threadNum - 2)
									.getEndPos()+1);
							loader2.setEndPos(fileSize-1);
							loader2.setThreadId(threadNum);
							loader2.setCompleteSize(0);
							loader2.setUrl(downloadUrl);
							loader2.setUsername(username);
							list.add(loader2);
						}
						//将安排的线程保存进数据库
						DownloadDao.save(list);
					}
				}
			} else {
				//throw new RuntimeException("server no response ");
				//没有响应
				//to do something !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				handler.sendEmptyMessage(-1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			print(e.toString());
			//throw new RuntimeException("don't connection this url");
			//无法连接
			//to do something !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			handler.sendEmptyMessage(-1);
		}
	}
	/**
	 * 获取文件名
	 */
	private String getFileName(HttpURLConnection conn) {
		String filename = this.downloadUrl.substring(this.downloadUrl
				.lastIndexOf('/') + 1);
		if (filename == null || "".equals(filename.trim())) {// 如果获取不到文件名称
			for (int i = 0;; i++) {
				String mine = conn.getHeaderField(i);
				if (mine == null)
					break;
				if ("content-disposition".equals(conn.getHeaderFieldKey(i)
						.toLowerCase())) {
					Matcher m = Pattern.compile(".*filename=(.*)").matcher(
							mine.toLowerCase());
					if (m.find())
						return m.group(1);
				}
			}
			filename = UUID.randomUUID() + ".tmp";// 默认取一个文件名
		}
		return filename;
	}

	/**
	 * 开始下载文件
	 * 
	 * @param listener
	 *            监听下载数量的变化,如果不需要了解实时下载的数量,可以设置为null
	 * @return 已下载文件大小
	 * @throws Exception
	 */
	public int download(SmartDownloadProgressListener listener)
			throws Exception {
		try {
			RandomAccessFile randOut = new RandomAccessFile(this.saveFile, "rw");
			if (this.fileSize > 0)
				randOut.setLength(this.fileSize);
			randOut.close();
			URL url = new URL(this.downloadUrl);
			for (int i = 0; i < this.list.size(); i++) {
				DownloadItem l = list.get(i);
				int downLength = l.getCompleteSize();
				int needLength = l.getEndPos() - l.getStartPos();
				if (downLength < needLength
						&& this.downloadSize < this.fileSize) {
					this.threads[i] = new SmartDownloadThread(this, url,
							list.get(i), this.saveFile);
					this.threads[i].setPriority(7);
					this.threads[i].start();
				} else {
					this.threads[i] = null;
				}
			}
			boolean notFinish = true;// 下载未完成
			while (notFinish&&SmartFileDownloader.flagMap.get(downloadUrl)) {// 循环判断是否下载完毕
				Thread.sleep(900);
				notFinish = false;// 假定下载完成
				for (int i = 0; i < this.threads.length; i++) {
					if (this.threads[i] != null && !this.threads[i].isFinish()) {
						notFinish = true;// 下载没有完成
						if (this.threads[i].getDownLength() == -1) {// 如果下载失败,再重新下载
							this.threads[i] = new SmartDownloadThread(this,
									url, list.get(i), this.saveFile);
							this.threads[i].setPriority(7);
							this.threads[i].start();
						}
					}
				}
				if (listener != null)
					System.out.println(this.downloadSize);
					listener.onDownloadSize(this.downloadSize);
			}
			if(!notFinish)	//已经下载完
			{
				DownloadDao.deleteAll(this.downloadUrl,this.downloadSize,this.saveFile.getPath(),username);
			}else
			{
				DownloadDao.updateCourse(this.downloadUrl,this.downloadSize,this.saveFile.getPath(),username);
			}
			isOver = true;
		} catch (Exception e) {
			e.printStackTrace();
			print(e.toString());
			throw new Exception("file download fail");
		}
		return this.downloadSize;
	}

	/**
	 * 获取Http响应头字段
	 * 
	 * @param http
	 * @return
	 */
	public static Map<String, String> getHttpResponseHeader(
			HttpURLConnection http) {
		Map<String, String> header = new LinkedHashMap<String, String>();
		for (int i = 0;; i++) {
			String mine = http.getHeaderField(i);
			if (mine == null)
				break;
			header.put(http.getHeaderFieldKey(i), mine);
		}
		return header;
	}

	/**
	 * 打印Http头字段
	 * 
	 * @param http
	 */
	public static void printResponseHeader(HttpURLConnection http) {
		Map<String, String> header = getHttpResponseHeader(http);
		for (Map.Entry<String, String> entry : header.entrySet()) {
			String key = entry.getKey() != null ? entry.getKey() + ":" : "";
			print(key + entry.getValue());
		}
	}

	// 打印日志
	private static void print(String msg) {
		Log.i(TAG, msg);
	}

	public int getCompleteSize() {
		// TODO Auto-generated method stub
		int size = 0;
		for (DownloadItem l : list) {
			size += l.getCompleteSize();
		}
		return size;
	}
	
	public boolean isStop()
	{
		boolean flag = true ;
		for(SmartDownloadThread t:threads)
		{
			//isAlive为真表示为活动线程
			flag = flag&&!t.isAlive();
		}
		return flag&&isOver;
	}
	
	public File getSaveFile()
	{
		return this.saveFile;
	}
	public static int getDowningCount()
	{
		int count=0;
		for(Boolean b : flagMap.values())
		{
			if(b) count++;
		}
		return count;
	}
 }
