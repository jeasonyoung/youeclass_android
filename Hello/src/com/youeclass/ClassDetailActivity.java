package com.youeclass;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.youeclass.adapter.CourseDetailListAdapter;
import com.youeclass.adapter.MyCourseListAdapter;
import com.youeclass.util.Constant;
import com.youeclass.util.HttpConnectUtil;

public class ClassDetailActivity extends Activity implements OnClickListener{
	private ImageButton rbtn;
	private TextView title;
	private ListView list;
	private LinearLayout courseCenter,myCourse,playrecord;
	private List<String> adapterList = new ArrayList<String>();
	private List<String> urls;
	private ProgressDialog dialog;
	private Handler handler;
	private String gid;
	private LinearLayout nodata;
	private String username;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_coursedetaillist);
		rbtn = (ImageButton) this.findViewById(R.id.returnbtn);
		title = (TextView) this.findViewById(R.id.TopTitle1);
		list = (ListView) this.findViewById(R.id.courserList);
		dialog = ProgressDialog.show(ClassDetailActivity.this,null,"努力加载中请稍候",true,true);
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		initLayoutBtn();
		Intent intent = this.getIntent();
		title.setText(intent.getStringExtra("name"));
		this.gid = intent.getStringExtra("gid");
		this.username = intent.getStringExtra("username");
		list.setAdapter(new MyCourseListAdapter(this,adapterList,urls));
		//设置缓存颜色为透明
		list.setCacheColorHint(Color.TRANSPARENT);
		list.setAlwaysDrawnWithCacheEnabled(true); 
		rbtn.setOnClickListener(this);
		this.handler = new MyHandler(this);
		new GetMyLessonThread().start();
		this.list.setOnItemClickListener(new ItemClickListener());
	}
	private void initLayoutBtn()
	{
		this.nodata = (LinearLayout) this.findViewById(R.id.nodataLayout);
		this.courseCenter = (LinearLayout) this.findViewById(R.id.CourseCenter_layout_btn);
		this.myCourse = (LinearLayout) this.findViewById(R.id.MyCourse_layout_btn);
		this.playrecord = (LinearLayout) this.findViewById(R.id.LearningRecord_layout_btn);
		this.courseCenter.setOnClickListener(this);
		this.playrecord.setOnClickListener(this);
		this.myCourse.setOnClickListener(this);
	}
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
		{
		case R.id.returnbtn:
			this.finish();
			break;
		case R.id.CourseCenter_layout_btn://课程中心
			break;
		case R.id.MyCourse_layout_btn://我的课程
			Intent intent = new Intent(this,MyCourseActivity.class);
			intent.putExtra("username", username);
			this.startActivity(intent);
			break;
		case R.id.LearningRecord_layout_btn://学习记录
			Toast.makeText(this, "免费体验不提供该功能", Toast.LENGTH_SHORT).show();
			break;
		}
	}
	private class GetMyLessonThread extends Thread
	{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try{
				String result = HttpConnectUtil.httpGetRequest(ClassDetailActivity.this, Constant.DOMAIN_URL+"mobile/findFreeClass?gid="+gid);
				if(result!=null&&!result.equals("null"))
            	{
            		//解析json字符串,配置ListView的adapter
            		try
            		{
            			JSONObject json = new JSONObject(result);
            			int ok = json.optInt("ok");
            			if(ok==1)
            			{
            				JSONArray details = json.getJSONArray("data");
            				urls = new ArrayList<String>();
            				for(int i=0;i<details.length();i++)
                			{
                				JSONObject obj = details.getJSONObject(i);
                				adapterList.add(obj.getString("classTitle"));
                				urls.add(obj.getString("classHdUrl"));
                			}if(adapterList.size()==0)
                			{
                				handler.sendEmptyMessage(0);
                			}else
                			{
                				handler.sendEmptyMessage(1);
                			}
            			}else
            			{
            				handler.sendEmptyMessage(0);
            			}
            			
            		}catch(Exception e)
            		{
            			e.printStackTrace();
            			handler.sendEmptyMessage(0);
            		}
            	}else
            	{
            		handler.sendEmptyMessage(0);
            	}
			}catch(Exception e)
			{
				Message msg = handler.obtainMessage();
				msg.what = -1;
				handler.sendMessage(msg);
			}
		}
	}
	static class MyHandler extends Handler {
        WeakReference<ClassDetailActivity> mActivity;
        MyHandler(ClassDetailActivity activity) {
                mActivity = new WeakReference<ClassDetailActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
        	ClassDetailActivity theActivity = mActivity.get();
                switch (msg.what) {
                case 1:
            		theActivity.dialog.dismiss();
                    mActivity.get().list.setAdapter(new CourseDetailListAdapter(theActivity,theActivity.adapterList));
                    break;
                case 0:
                	theActivity.dialog.dismiss();
            		theActivity.nodata.setVisibility(View.VISIBLE);//无数据显示
                	break;
                case -1:
                	//连不上,
                	theActivity.dialog.dismiss();
            		theActivity.nodata.setVisibility(View.VISIBLE);//无数据显示
            		Toast.makeText(theActivity, "暂时连不上服务器,请稍候", Toast.LENGTH_SHORT).show();//提示
                }
        }
	}
	private class ItemClickListener implements OnItemClickListener
	{
		public ItemClickListener() {
			// TODO Auto-generated constructor stub
		}
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			MobclickAgent.onEvent(ClassDetailActivity.this,"free_video_listen");
			Intent intent = new Intent(ClassDetailActivity.this,VideoActivity3.class);
			intent.putExtra("name", ((TextView)arg1.findViewById(R.id.text4)).getText().toString());
			intent.putExtra("url",urls.get(arg2));
			intent.putExtra("username",username);
			intent.putExtra("playType","free");
			ClassDetailActivity.this.startActivity(intent);
		} 
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if(dialog!=null)
		{
			dialog.dismiss();	
		}
		super.onDestroy();
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
