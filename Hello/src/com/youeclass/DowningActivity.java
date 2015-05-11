package com.youeclass;

import java.util.List;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.umeng.analytics.MobclickAgent;
import com.youeclass.adapter.DowningListAdapter;
import com.youeclass.dao.CourseDao;
import com.youeclass.entity.DowningCourse;
import com.youeclass.service.DownloadService;
import com.youeclass.util.StringUtils;

/**
 *  视频下载UI。
 * @author jeasonyoung
 *
 */
public class DowningActivity extends BaseActivity{
	private static final String TAG = "DowningActivity";
	private ListView listView;
	private LinearLayout nodata;
	private List<DowningCourse> list;
	private DowningListAdapter mAdapter;
	private CourseDao dao;
	private String username;
	
	private DownloadServiceConnection connection;
	private DownloadService.IFileDownloadService fileDownloadService;
	
	/**
	 * 构造函数。
	 */
	public DowningActivity(){
		super();
		this.dao = new CourseDao(this);
		this.connection = new DownloadServiceConnection();
	}
	
	/*
	 * 重载创建。
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_downing);
		Intent intent = getIntent();
		String name=intent.getStringExtra("name");
		String url = intent.getStringExtra("url");
		this.username = intent.getStringExtra("username");
		this.listView = (ListView) this.findViewById(R.id.videoListView);
		this.nodata = (LinearLayout) this.findViewById(R.id.down_nodataLayout);
		
		//绑定服务
		Intent serviceIntent = new Intent(this, DownloadService.class);
		this.getApplicationContext().bindService(serviceIntent, connection, BIND_AUTO_CREATE);
		
		//找出数据库中所有正在下载的课程
		this.list = this.dao.findAllDowning(this.username);
		//System.out.println("list size = "+list.size()+"!!!!!!!!!");
		Log.d(TAG, "正在下载的课程 list size = " + list.size());
		//初始化从课程列表中点击的要下载项
		if(!StringUtils.isEmpty(name) && !StringUtils.isEmpty(url))
		{
			DowningCourse downing = new DowningCourse();
			downing.setCourseName(name);
			downing.setFileUrl(url);
			downing.setState(DowningCourse.STATE_INIT);
			downing.setUserName(this.username);
			if(!list.contains(downing)){
				list.add(downing);
			}
		}
		//配置数据源
		this.mAdapter = new DowningListAdapter(this, this.list);
		this.listView.setAdapter(this.mAdapter);
		if(list.size() == 0)
		{
			//数据库中没有正在下载中的数据
			this.nodata.setVisibility(View.VISIBLE);
		}
		//长按弹出取消下载的PopupWindow
		this.listView.setOnItemLongClickListener(new OnItemLongClickListener());
	}
	/*
	 * 重载恢复。
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}
	/*
	 * 重载暂停。
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	};
	
	@Override
	protected void onDestroy() {
		//取消绑定服务
		this.unbindService(this.connection);
		super.onDestroy();
	}
	/**
	 * 下载服务连接器。
	 * @author jeasonyoung
	 *
	 */
	private final class DownloadServiceConnection implements ServiceConnection{
		/*
		 * 连接服务。
		 * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName, android.os.IBinder)
		 */
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "连接下载服务...");
			fileDownloadService = (DownloadService.IFileDownloadService)service;
			mAdapter.setDownloadService(fileDownloadService);
		}
		/*
		 * 断开服务。
		 * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
		 */
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "断开下载服务...");
			 fileDownloadService = null;
		}
	}
	/**
	 * 长按取消下载事件处理。
	 * @author jeasonyoung
	 *
	 */
	private class OnItemLongClickListener implements AdapterView.OnItemLongClickListener, View.OnClickListener,DialogInterface.OnClickListener{
		private QuickActionPopupWindow actionbar;
		private ActionItem actionDelete;
		private AlertDialog dialog;
		private DowningCourse course;
		 /*
		  * 重载长按处理。
		  * @see android.widget.AdapterView.OnItemLongClickListener#onItemLongClick(android.widget.AdapterView, android.view.View, int, long)
		  */
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			if(list.size() < position){
				this.course = list.get(position);
				this.showPopupWindow(view);
			}
			return false;
		}
		//显示弹出框
		private void showPopupWindow(View v){
			this.actionbar = new QuickActionPopupWindow(DowningActivity.this);
			this.actionDelete = new ActionItem();
			this.actionDelete.setIcon(getResources().getDrawable(R.drawable.action_delete));
			this.actionDelete.setTitle("取消");
			this.actionDelete.setClickListener(this);
			this.actionbar.addActionItem(this.actionDelete);
			//设置动画风格
			this.actionbar.setAnimStyle(QuickActionPopupWindow.ANIM_AUTO);
			//显示
			this.actionbar.show(v);
		}
		/*
		 * 取消事件处理。
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		@Override
		public void onClick(View v) {
			 //弹框是否确认删除
			this.dialog = new AlertDialog.Builder(DowningActivity.this)
			.setTitle("删除文件")
			.setMessage("是否确认取消下载并删除文件?")
			.setPositiveButton("确定",this)
			.setNegativeButton("取消", this).create();
			//显示
			this.dialog.show();
		}
		/*
		 * 弹框按钮事件处理
		 * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
		 */
		@Override
		public void onClick(DialogInterface dialog, int which) {
			 switch(which){
				 case DialogInterface.BUTTON_POSITIVE:{//确认
					 dialog.cancel();
					 this.actionbar.dismiss();
					 //发送广播意图：停止下载,并删除文件
					 if(fileDownloadService != null){
						 Log.d(TAG, "课程["+this.course.getCourseName()+"]取消下载...");
						 fileDownloadService.cancelDownload(this.course);
					 }
					 //删除数据库记录
					 dao.deleteDowing(username, this.course.getFileUrl());
					 //从数据集合中移除
					 list.remove(this.course);
					 //更新UI
					 mAdapter.notifyDataSetChanged();
					 break;
				 }
				 case DialogInterface.BUTTON_NEGATIVE:{//取消
					 dialog.cancel();
					 this.actionbar.dismiss();
					 break;
				 }
			 }
		}
	}
}