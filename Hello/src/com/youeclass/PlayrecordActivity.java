package com.youeclass;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.youeclass.dao.PlayrecordDao;
import com.youeclass.entity.Playrecord;

public class PlayrecordActivity extends ListActivity{
	private ImageButton returnBtn;
	private ArrayList<Playrecord> recordList;
	private String username;
	private String loginType;
	private PlayrecordDao dao = new PlayrecordDao(this);
	private RecordListAdapter mAdapter;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_playrecord);
		Intent intent = this.getIntent();
		this.username = intent.getStringExtra("username");
		this.loginType = intent.getStringExtra("loginType");
		this.returnBtn = (ImageButton) this.findViewById(R.id.returnbtn);
		this.returnBtn.setOnClickListener(new ReturnBtnClickListener(this));
		
	}
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		if(recordList==null)
		{
			this.recordList = (ArrayList<Playrecord>) this.dao.getRecordList(username);
		}else
		{
			this.recordList.clear();
			this.recordList.addAll(this.dao.getRecordList(username));
		}
		if(mAdapter==null)
		{
			mAdapter = new RecordListAdapter();
			this.setListAdapter(mAdapter);
		}else
		{
			mAdapter.notifyDataSetChanged();
		}
		super.onStart();
	}
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		Playrecord r = this.recordList.get(position);
		if("local".equals(loginType))
		{
			if(r.getCourseFilePath()==null)
			{
				Toast.makeText(this, "您没有下载该视频", Toast.LENGTH_SHORT).show();
				return;
			}
		}
		//
		MobclickAgent.onEvent(this,"record_listen");
		//
		Intent intent = new Intent(this,VideoActivity3.class);
		intent.putExtra("username", username);
		intent.putExtra("name", r.getCourseName());
		intent.putExtra("url", r.getCourseFilePath()==null?r.getCourseUrl():r.getCourseFilePath());
		intent.putExtra("httpUrl", r.getCourseUrl());
		intent.putExtra("loginType", loginType);
		intent.putExtra("courseid", r.getCourseId());
		this.startActivity(intent);	
	}
	private class RecordListAdapter extends BaseAdapter
	{
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			try{
				return recordList.size();
			}catch(Exception e)
			{
				return 0;
			}
		}
		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			try{
				return recordList.get(position);
			}catch(Exception e)
			{
				return null;
			}
		}
		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			LayoutInflater mInflater = LayoutInflater.from(PlayrecordActivity.this);
			Playrecord  r = recordList.get(position);
			convertView = mInflater.inflate(R.layout.list_playrecord, null);
			TextView coursename = (TextView) convertView.findViewById(R.id.coursenamelab);
			TextView currentTime = (TextView) convertView.findViewById(R.id.currentTimeLab);
			TextView playTime = (TextView) convertView.findViewById(R.id.palytimelab);
			coursename.setText(r.getCourseName());
			currentTime.setText("已学习到:"+r.getFormatCurrentTime());
			playTime.setText("学习时间:"+r.getPlayTime());
			return convertView;
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
