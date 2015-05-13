package com.youeclass;

import java.util.List;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
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
	private DowningListAdapter mAdapter;
	private DownloadServiceConnection serviceConnection = new DownloadServiceConnection();
	private DownloadService.IFileDownloadService fileDownloadService;
	private CourseDao courseDao = new CourseDao(this);
	private String username;
	/*
	 * 重载创建。
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//绑定文件下载服务
		this.getApplicationContext().bindService(new Intent(this, DownloadService.class), this.serviceConnection, BIND_AUTO_CREATE);
		//加载布局
		this.setContentView(R.layout.activity_downing);
		this.listView = (ListView) this.findViewById(R.id.videoListView);
		//长按弹出取消下载的PopupWindow
		this.listView.setOnItemLongClickListener(new OnItemLongClickListener());
		this.nodata = (LinearLayout) this.findViewById(R.id.down_nodataLayout);
		
		Intent intent = this.getIntent();
		this.username = intent.getStringExtra("username");
		//异步加载数据
		new AsyncLoadData(intent.getStringExtra("name"), intent.getStringExtra("url"));
	}
	
	/**
	 * 异步加载后台数据
	 * @author jeasonyoung
	 *
	 */
	private final class AsyncLoadData extends AsyncTask<String, Integer, List<DowningCourse>>{
		private static final String TAG = "AsyncLoadData";
		/**
		 * 构造函数。
		 * @param courseName 课程名称
		 * @param courseUrl 课程URL
		 */
		public AsyncLoadData(String courseName, String courseUrl){
			Log.d(TAG, "初始化异步加载...");
			this.execute(courseName, courseUrl);
		}
		/*
		 * 重载后台执行
		 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
		 */
		@Override
		protected List<DowningCourse> doInBackground(String... params) {
			Log.d(TAG, "正在异步加载的课程... ");
			String name = params[1],url = params[2];
			List<DowningCourse> list = courseDao.findAllDowning(params[0]);
			//初始化从课程列表中点击的要下载项
			if(!StringUtils.isEmpty(name) && !StringUtils.isEmpty(url))
			{
				DowningCourse downing = new DowningCourse();
				downing.setCourseName(name);
				downing.setFileUrl(url);
				downing.setState(DowningCourse.STATE_INIT);
				downing.setUserName(params[0]);
				if(!list.contains(downing)){
					Log.d(TAG, "添加须下载课程["+name+"=>"+url+"]到集合...");
					list.add(downing);
					//更新数据库中状态为下载状态
					courseDao.updateState(downing.getUserName(), downing.getFileUrl(), DowningCourse.STATE_DOWNING);
				}
			}
			return list;
		}
		/*
		 * UI处理
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(List<DowningCourse> result) {
			mAdapter = new DowningListAdapter(getApplicationContext(), result);
			listView.setAdapter(mAdapter);
			if(result.size() == 0){
				nodata.setVisibility(View.VISIBLE);
			}
		};
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
		this.getApplicationContext().unbindService(this.serviceConnection);
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
			if(position > -1 && position < mAdapter.getCount()){
				this.course = (DowningCourse)mAdapter.getItem(position);
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
					 courseDao.deleteDowing(username, this.course.getFileUrl());
					 //从数据集合中移除
					 mAdapter.removeCourse(this.course);
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