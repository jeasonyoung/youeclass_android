package com.youeclass;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.umeng.analytics.MobclickAgent;
import com.youeclass.adapter.QuestionCommonAdapter;
import com.youeclass.dao.PaperDao;
import com.youeclass.entity.ExamQuestion;
import com.youeclass.entity.QuestionAdapterData;

public class QuestionCommonFirstActivity extends Activity{
	private ImageButton returnbtn;
	private TextView title;
	private LinearLayout loadingLayout;
	private LinearLayout contentLayout;
	private ListView notebookListView;
	private LinearLayout nodataLayout;
	private ArrayList<QuestionAdapterData> data;
	private String actionName,username;
	private Class c;
	private int stringResId;
	private Gson gson;
	private PaperDao dao;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_question_doproblem_record);
		Intent mIntent = this.getIntent();
		actionName = mIntent.getStringExtra("actionName");
		username = mIntent.getStringExtra("username");
		c = "myNotes".equals(actionName)?QuestionMyNotebookActivity.class:
			"myErrors".equals(actionName)?QuestionDoExamActivity2.class:
				QuestionDoExamActivity2.class;
		stringResId ="myNotes".equals(actionName)?R.string.my_notebookStr:
			"myErrors".equals(actionName)?R.string.errorQuesitionStr:
				R.string.my_favoriteStr;
		gson = new Gson();
		dao = new PaperDao(this);
		initView();
	}
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		initData();
		this.loadingLayout.setVisibility(View.GONE);
		if(data==null||data.size()==0)
		{
			this.contentLayout.setVisibility(View.GONE);
			this.nodataLayout.setVisibility(View.VISIBLE);
		}else
		{
			this.notebookListView.setAdapter(new QuestionCommonAdapter(this, data));
			this.notebookListView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
						long arg3) {
					// TODO Auto-generated method stub
					QuestionAdapterData qad = data.get(arg2);
					Intent mIntent = new Intent(QuestionCommonFirstActivity.this,c);//!!!修改class
					//绑数据
					mIntent.putExtra("paperId", qad.getPaperId());
					mIntent.putExtra("username", username);
					mIntent.putExtra("title", qad.getTitle());
					mIntent.putExtra("action",actionName);
					mIntent.putExtra("questionListJson", getQuestionListJson(qad.getPaperId(), username, actionName));
					QuestionCommonFirstActivity.this.startActivity(mIntent);
					
				}
			});
		}
		super.onStart();
	}
	private String getQuestionListJson(String paperid,String username,String actionName)
	{
		String json = null;
		if("myErrors".equals(actionName))
		{
			ArrayList<ExamQuestion> list = (ArrayList<ExamQuestion>) dao.findQuestionFromErrors(username, paperid);
			json = gson.toJson(list);
		}else if("myFavors".equals(actionName))
		{
			ArrayList<ExamQuestion> list = (ArrayList<ExamQuestion>) dao.findQuestionFromFavors(username, paperid);
			json = gson.toJson(list);
		}
		return json;
	}
	private void initData()
	{
		data = (ArrayList<QuestionAdapterData>) dao.findAdapterData(actionName, username);
	}
	private void initView()
	{
		this.returnbtn = (ImageButton) this.findViewById(R.id.returnbtn);
		this.title = (TextView) this.findViewById(R.id.TopTitle1);
		this.loadingLayout = (LinearLayout) this.findViewById(R.id.loadingLayout);
		this.nodataLayout = (LinearLayout) this.findViewById(R.id.nodataLayout);
		this.contentLayout = (LinearLayout) this.findViewById(R.id.questionContentLayout);
		this.notebookListView = (ListView) this.findViewById(R.id.question_record_ListView);
		this.returnbtn.setOnClickListener(new ReturnBtnClickListener(this));
		this.title.setText(stringResId);
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
