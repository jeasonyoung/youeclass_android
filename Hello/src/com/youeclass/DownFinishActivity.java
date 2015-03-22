package com.youeclass;

import java.io.File;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.umeng.analytics.MobclickAgent;
import com.youeclass.adapter.DownedListAdapter;
import com.youeclass.dao.CourseDao;
import com.youeclass.entity.Course;

public class DownFinishActivity extends BaseActivity {
	private ListView listview;
	private CourseDao dao;
	private List<Course> list;
	private LinearLayout nodata;
	private BaseAdapter mAdapter;
	private QuickActionPopupWindow actionbar;
	private ActionItem action_delete;
	private ActionButtonClickListener listener;
	private String username;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_downfinish);
		this.listview = (ListView) this.findViewById(R.id.videoListView);
		this.nodata = (LinearLayout) this.findViewById(R.id.down_nodataLayout);
		this.username = getIntent().getStringExtra("username");
		if (this.dao == null)
			this.dao = new CourseDao(this);
		this.list = this.dao.findAllDowned(username);
		if (list.size() == 0) {
			nodata.setVisibility(View.VISIBLE);
		}
		mAdapter = new DownedListAdapter(this, list);
		this.listview.setAdapter(mAdapter);
		this.listview.setOnItemClickListener(new ItemClickListener());
		this.listview.setOnItemLongClickListener(new ItemLongClickListener());
	}

	private class ItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			//
			MobclickAgent.onEvent(DownFinishActivity.this,"download_listen");
			//
			Course c = list.get(arg2);
			Intent intent = new Intent(DownFinishActivity.this,
					VideoActivity.class);
			intent.putExtra("name", c.getCourseName());
			intent.putExtra("url", c.getFilePath());
			intent.putExtra("courseid", c.getCourseId());
			intent.putExtra("username", username);
			DownFinishActivity.this.startActivity(intent);
		}
	}

	private class ItemLongClickListener implements OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			// TODO Auto-generated method stub
			showWindow(arg1,arg2);
			return false;
		}
	private void showWindow(View v, int location) {
		if(actionbar == null)
		{
			actionbar = new QuickActionPopupWindow(DownFinishActivity.this);
			action_delete = new ActionItem();
			action_delete.setTitle("删除");
			action_delete.setIcon(getResources().getDrawable(
					R.drawable.action_delete));
			
			actionbar.addActionItem(action_delete);
			// 设置动画风格
			actionbar.setAnimStyle(QuickActionPopupWindow.ANIM_AUTO);
		}
		if(listener == null)
		{
			listener = new ActionButtonClickListener();
		}
		listener.setIndex(location);
		action_delete.setClickListener(listener); 
		// 显示
		actionbar.show(v);
		}
	}
	private class ActionButtonClickListener implements OnClickListener
	{
		private int index;
		public void setIndex(int index) {
			this.index = index;
		}
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			//弹框是否确认删除
			AlertDialog dialog = new AlertDialog.Builder(DownFinishActivity.this)
			.setTitle("删除文件")
			.setMessage("是否确认删除该视频文件")
			.setPositiveButton("确定", new  DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.cancel();
					actionbar.dismiss();
					//to do something
					Course c = list.get(index);
					new File(c.getFilePath()).delete();
					Log.i("DownFinish","删除了文件");
					dao.updateState(c.getFileUrl(), 0, c.getUsername());
					list.remove(index);
					mAdapter.notifyDataSetChanged();
					}
			}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.cancel();
				}
			}).create();
			dialog.show();
		}
	}
	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	};
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		MobclickAgent.onResume(this);
		
	}
}
