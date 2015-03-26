package com.youeclass.adapter;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.youeclass.R;
import com.youeclass.customview.DownloadButton;
import com.youeclass.downloader.SmartFileDownloader;
import com.youeclass.entity.DowningCourse;
import com.youeclass.service.DownloadService;
import com.youeclass.util.FileUtil;

public class DowningListAdapter extends BaseAdapter {
	private Context context;
	private List<DowningCourse> list;
	private String username;
	private SharedPreferences settingfile;
	public DowningListAdapter(Context context, List<DowningCourse> list,String username) {
		super();
		this.context = context;
		this.list = list;
		this.username = username;
		this.settingfile = context.getSharedPreferences("settingfile", 0);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub.Boolean flag =
		// SmartFileDownloader.flagMap.get(url);
		//if (convertView == null) {
		/*
		 * 初始化下载项的问题
		 * 一开始数据库还没有下载项,都是新的,文件大小不定
		 * 有数据的情况
		 */
			DowningListItem item = null;
			if(convertView==null)	//converView还不存在
			{
				LayoutInflater inflater = LayoutInflater.from(context);
				convertView = inflater.inflate(R.layout.list_downing_layout, null);
				item = new DowningListItem();
				// 文件名
				item.filenameLab = (TextView) convertView
						.findViewById(R.id.filenameLab);
				// 进度条
				item.finishProgress = (ProgressBar) convertView
						.findViewById(R.id.finishProgress);
				item.finishProgress.setMax(100);
				// 下载中
				item.downing = (TextView) convertView
						.findViewById(R.id.fileDownText);
				// 百分数
				item.percent = (TextView) convertView
						.findViewById(R.id.fileFininshProgressLab);
				// 连接中
				item.connecting = (TextView) convertView
						.findViewById(R.id.finishSizeTextView);
				// 暂停或继续按钮
				item.pauseBtn = (DownloadButton) convertView
						.findViewById(R.id.pauseBtn);
				convertView.setTag(item);
			}else
			{
				item = (DowningListItem) convertView.getTag();
			}
			DowningCourse dc = list.get(position);
			System.out.println(dc.getFileurl()+" 下载状态 : "+dc.getStatus());
			item.filenameLab.setText(dc.getCourseName());
			//获取下载状态
			Boolean flag = SmartFileDownloader.flagMap.get(dc.getFileurl());
			//为空表示尚未下载过,为假表示暂停了
			if(flag == null || !flag)
			{
				//初始化,--暂停,
				if(dc.getStatus()==0)	//以前没有下载完的状态玛都为0;显示继续
				{
					int percentNum = (int) (dc.getFinishsize()*100.0/dc.getFilesize());
					item.percent.setText(percentNum	+ "%");
					item.finishProgress.setProgress(percentNum);
					// 已有下载但是没有启动下载
					item.pauseBtn.setImageResource(R.drawable.continuedown);// 显示继续按钮
					item.pauseBtn.setText(R.string.continueDown);// 显示继续
					item.downing.setText("暂停中");
				}else if (dc.getStatus()== -1) {	//表示刚刚加入进来,还没有开始下载
					item.connecting.setText("连接中...");
					item.downing.setText("");// 还没开始下载
					// 禁用按钮或者设置取消
					//item.pauseBtn.setEnabled(false);
				}else if(dc.getStatus() == 4)
				{
					item.downing.setText("等待中");
					item.pauseBtn.setImageResource(R.drawable.waitdown);// 显示等待按钮
					item.pauseBtn.setText(R.string.waitDown);// 显示等待
//					item.pauseBtn.setEnabled(false);//禁用按钮
				}else if(dc.getStatus() == -2)
				{
					item.connecting.setText("连接失败!");
					item.pauseBtn.setImageResource(R.drawable.retry);
					item.pauseBtn.setText(R.string.retry);
				}
			}else	//正在下载
			{
				if(dc.getStatus()==1)	//刚刚加入进来的开始下载了
				{
					item.connecting.setText("");
					item.pauseBtn.setEnabled(true);
					//item.finishProgress.setProgress(0);
					//dc.setStatus(0);
				}
				dc.setStatus(1);
				//正在下载  
				// 更新进度数值
				int percentNum = (int) (dc.getFinishsize()*100.0/dc.getFilesize());
				item.percent.setText(percentNum	+ "%");
				//更新进度条
				item.finishProgress.setProgress(percentNum);
				item.downing.setText("下载中");
				item.pauseBtn.setImageResource(R.drawable.pausedown);// 显示继续按钮
				item.pauseBtn.setText(R.string.pauseDown);// 显示继续
				System.out.println("!!!!!!! update the progress !!!!!!!!!");
			}
			// 设置按钮事件
			item.pauseBtn.setOnClickListener(new PauseClickEvent(position, list,
					item.downing));
		return convertView;
	}

	// 判断下载与暂停的标识是什么
	/*
	 * map里取不到 url对应的值,表示没有开始下载 取出为false,暂停了下载 取出为true,正在下载
	 */
	private class PauseClickEvent implements OnClickListener {
		private TextView textView;
		private DowningCourse dc;

		public PauseClickEvent(int position, List<DowningCourse> l, TextView t) {
			// TODO Auto-generated constructor stub
			this.textView = t;
			this.dc = l.get(position);
		}

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(dc.getStatus()==4||dc.getStatus()==-1) return;
			String url  = dc.getFileurl();
			Boolean flag = SmartFileDownloader.flagMap.get(url);
			// 停止了下载
			if (flag == null || !flag) {
				// 启动下载
				if(SmartFileDownloader.getDowningCount()>=2)
				{
					dc.setStatus(4);  //等待状态
					((DownloadButton) v).setImageResource(R.drawable.waitdown);// 显示暂停按钮
					((DownloadButton) v).setText(R.string.waitDown);
					return;
				}
				Boolean wifiState = checkWifiNetworkInfo();
				Boolean isDownUse3G = settingfile.getBoolean("setDownIsUse3G", true);
				if(wifiState==null)
				{
					//print("请检查您的网络");
					Toast.makeText(context, "请检查您的网络", Toast.LENGTH_SHORT).show();
					return;
				}
				if(wifiState==false&&isDownUse3G==false)//没有wifi,又不允许3G下载
				{
					//print("当前网络为2G/3G,要下载请修改设置或开启wifi");
					Toast.makeText(context, "当前网络为2G/3G,要下载请修改设置或开启wifi", Toast.LENGTH_SHORT).show();
					return;
				}
				if(!FileUtil.checkSDCard(dc.getFilesize()))
				{
					Toast.makeText(context, "没有SD卡或者空间不够", Toast.LENGTH_SHORT).show();
					return;
				}
				SmartFileDownloader.flagMap.put(url, true);
				Intent intent = new Intent(context, DownloadService.class);
				intent.putExtra("url", url);
				File dir = Environment.getExternalStorageDirectory();
				intent.putExtra("dir", dir.getPath() + "/eschool");
				intent.putExtra("username", username);
				context.startService(intent);
				textView.setText("下载中");
				dc.setStatus(1);
				((DownloadButton) v).setImageResource(R.drawable.pausedown);// 显示暂停按钮
				((DownloadButton) v).setText(R.string.pauseDown);
			} else {
				// 发广播通知后台service暂停
				SmartFileDownloader.flagMap.put(url, false);
				Intent myIntent = new Intent();// 创建Intent对象
				myIntent.setAction("commandFromActivity");
				myIntent.putExtra("cmd", 0);
				myIntent.putExtra("url", url);
				context.sendBroadcast(myIntent);// 发送广播
				textView.setText("暂停中");
				//下载状态码为0
				dc.setStatus(0);
				((DownloadButton) v).setImageResource(R.drawable.continuedown);// 显示继续按钮
				((DownloadButton) v).setText(R.string.continueDown);// 显示继续
				//同时查找处于暂停状态的任务
				DowningCourse waiting = getFirstWait();
				if(waiting!=null)
				{
					SmartFileDownloader.flagMap.put(waiting.getFileurl(), true);
					Intent intent = new Intent(context, DownloadService.class);
					intent.putExtra("url", waiting.getFileurl());
					File dir = Environment.getExternalStorageDirectory();
					intent.putExtra("dir", dir.getPath() + "/eschool");
					intent.putExtra("username", username);
					context.startService(intent);
					waiting.setStatus(1);
					DowningListAdapter.this.notifyDataSetChanged();
				}
			}
		}
	}
	static class DowningListItem
	{
		TextView filenameLab,downing,percent,connecting;
		DownloadButton pauseBtn;
		ProgressBar finishProgress;
	}
	//获得列表中第一个等待的任务
	private DowningCourse getFirstWait()
	{
		for(DowningCourse dc:list)
		{
			if(dc.getStatus()==4)
			{
				return dc;
			}
		}
		return null;
	}
	private Boolean checkWifiNetworkInfo()
	{
		ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		 //mobile 3G Data Network
        State mobile = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
        //wifi
        State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if(wifi==State.CONNECTED||wifi==State.CONNECTING)
        {
        	return true;
        }
        if(mobile==State.CONNECTED||mobile==State.CONNECTING)
        {
        	return false;
        }
		return null;
	}
}
