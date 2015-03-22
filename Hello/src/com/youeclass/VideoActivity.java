package com.youeclass;

import java.io.File;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.youeclass.dao.CourseDao;
import com.youeclass.dao.PlayrecordDao;
import com.youeclass.entity.Playrecord;
import com.youeclass.player.Player;

public class VideoActivity extends Activity implements OnTouchListener,OnGestureListener{
	private PopupWindow title;
	private PopupWindow toolbar;
	private SurfaceView view;
	private int height;
	private int width;
	private SeekBar seekbar,volumnBar;
	private TextView titleTxt,totalTime,currentTime,volumnSize;
	private ImageButton returnBtn,prev,next,playbtn,playerList;
	private Player player;
	private RelativeLayout videoloadingLayout;
	private String name,url,courseid,httpUrl;
	private Handler handler;
	private GestureDetector mGestureDetector;
	private boolean endFlag = false;
	private long touchStartTime = System.currentTimeMillis();
	private AudioManager mAudioManager;
	private int volumnMax;
	private PlayrecordDao dao;
	private CourseDao courseDao;
	private Playrecord record;
	private String username,playType;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);//不要标题栏,必须在setContentview之前
		//设置全屏
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.setContentView(R.layout.activity_video);
		this.mAudioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//屏幕常亮
		Intent intent = this.getIntent();
		this.name = intent.getStringExtra("name");
		this.url = intent.getStringExtra("url");
		this.username = intent.getStringExtra("username");
		this.courseid = intent.getStringExtra("courseid");
		this.httpUrl = intent.getStringExtra("httpUrl");
		this.playType = intent.getStringExtra("playType");
		if(!"free".equals(playType))
		{
		if(this.url.indexOf(username)!=-1)
		{
			//表示从文件里读取,看文件是否存在
			File file = new File(url);
			if(!file.exists())
			{
				Toast.makeText(this, "本地文件已经被删除", Toast.LENGTH_SHORT).show();
				//修改courseTab中的记录,结束
				new CourseDao(this).updateState(httpUrl, 0, username);
				if("local".equals(intent.getStringExtra("loginType")))
				{
					this.finish();
					return;
				}
			}
		}}
		LayoutInflater inflater = LayoutInflater.from(this);
		View t = inflater.inflate(R.layout.title, null);
		t.getBackground().setAlpha(0);
		View bar = inflater.inflate(R.layout.videocontrol, null);
		this.view = (SurfaceView) this.findViewById(R.id.surfaceView);
		this.videoloadingLayout = (RelativeLayout) this.findViewById(R.id.videoloadingLayout);
		WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
		width = wm.getDefaultDisplay().getWidth();//屏幕宽度
		height = wm.getDefaultDisplay().getHeight();//屏幕高度
		this.title = new PopupWindow(t,400,50);
		this.title.setAnimationStyle(R.style.AnimationFade);
		this.toolbar = new PopupWindow(bar);
		this.toolbar.setAnimationStyle(R.style.AnimationFade);             
		videoloadingLayout.setVisibility(View.VISIBLE);
		Looper.myQueue().addIdleHandler(new IdleHandler()
	      {
	             @Override
	             public boolean queueIdle() 
	             {
	                 //显示两个popupwindow
	            	 if(title!=null&&view.isShown())
	            	 {
	            		 title.showAtLocation(view, Gravity.TOP, 0, 0);
	            		 title.update(height-50, 0, width, 50);
	            	 }
	            	 if(toolbar!=null&&view.isShown())
	            	 {
	            		 toolbar.showAtLocation(view, Gravity.BOTTOM, 0, 0);
	            		 toolbar.update(0, 0, width, 120);
	            	 }
	            	//显示正在加载
	                return false;  
	             }
	      });
		returnBtn = (ImageButton) t.findViewById(R.id.imageBack);
		titleTxt = (TextView) t.findViewById(R.id.videoName);
		titleTxt.setText(name);
		seekbar = (SeekBar) bar.findViewById(R.id.seekBar);//视频进度条 
		volumnBar = (SeekBar) t.findViewById(R.id.seekBar1);//音量拖拽条
		volumnSize = (TextView) t.findViewById(R.id.volumnSize);//音量百分数
		volumnMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		volumnBar.setMax(volumnMax);
		volumnBar.setProgress(current);
		volumnSize.setText(current*100/volumnMax+"%");
		playbtn = (ImageButton) bar.findViewById(R.id.imagePlay);
		prev = (ImageButton) bar.findViewById(R.id.imagePrevious);
		next = (ImageButton) bar.findViewById(R.id.imageNext);
		totalTime = (TextView) bar.findViewById(R.id.totalTime);
		currentTime = (TextView) bar.findViewById(R.id.playTime);
		ClickEvent c = new ClickEvent();
		returnBtn.setOnClickListener(c);
		playbtn.setOnClickListener(c);
		prev.setOnClickListener(c);
		next.setOnClickListener(c);
		volumnBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seekBar.getProgress(), 8);
				volumnSize.setText(seekBar.getProgress()*100/seekBar.getMax()+"%");
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				
			}
		});
		OnErrorListener listener = new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				Toast.makeText(VideoActivity.this, "未知媒体错误", Toast.LENGTH_SHORT).show();
				VideoActivity.this.finish();
				return false;
			}
		};
		OnCompletionListener listener2 = new OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				if(toolbar!=null&&!toolbar.isShowing())
				{
					title.showAtLocation(view, Gravity.TOP, 0, 0);
		   		 	title.update(height-50, 0, width, 50);
		   		 	toolbar.showAtLocation(view, Gravity.BOTTOM, 0, 0);
		   		 	toolbar.update(0, 0, width, 120);
				}
				if(playbtn!=null){
				//换图片
				playbtn.setBackgroundResource(R.drawable.play_button);
				playbtn.setImageResource(R.drawable.player_play);
				seekbar.setProgress(0);
				currentTime.setText("00:00");
			}}
		};
		if(!"free".equals(playType))
		{
			initRecord();
			player = new Player(view,seekbar,currentTime,totalTime,record.getCurrentTime(),videoloadingLayout,listener,listener2);
		}else
		{
			player = new Player(view,seekbar,currentTime,totalTime,0,videoloadingLayout,listener,listener2);
		}
		
		
		//监听播完事件
		
		
		//开一个线程等准备好就开始播放
		new Thread(){
			public void run() {
				while(true)
					{
						if(player.isCreated())
						{
							String url="http://192.168.1.246:8080/struts01/video/daomeixiong001.3gp";
							String url2 = "http://www.youeclass.com:8090/test_video.mp4"; //网络视频地址
							try{
							player.playUrl(url2);
							handler.sendEmptyMessage(1);
							break;
							}catch(Exception e)
							{
								handler.sendEmptyMessage(-1);
								break;
							}
						}
					}
			};
		}.start();
		handler = new MyHandler(this);
		this.mGestureDetector = new GestureDetector(this,this);
		this.view.setOnTouchListener(this);
		this.view.setLongClickable(true);
		this.view.setFocusable(true);   
		this.view.setClickable(true);  
		mGestureDetector.setIsLongpressEnabled(true);   
	}
	static class MyHandler extends Handler {
        WeakReference<VideoActivity> mActivity;
        MyHandler(VideoActivity activity) {
                mActivity = new WeakReference<VideoActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
        	VideoActivity theActivity = mActivity.get();
        	switch(msg.what)
        	{
        	case 1:
        		//theActivity.videoloadingLayout.setVisibility(View.GONE);
        		break;
        	case 0:
        		if(theActivity.title!=null&&theActivity.title.isShowing())
        		{
        			theActivity.title.dismiss();
        			theActivity.toolbar.dismiss();
        		}
        		break;
        	case -1:
        		Toast.makeText(theActivity, "未知媒体错误", Toast.LENGTH_SHORT).show();
        		break;
        	}
        	
        }
	}
	class ClickEvent implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			switch(arg0.getId())
			{
			case R.id.imageBack:
				recordAndBack();
				VideoActivity.this.finish();
				break;
			case R.id.imagePlay:
				playOrPause();
				break;
			case R.id.imagePrevious:
				back();
				break;
			case R.id.imageNext:
				forward();
				break;
			}
		}
	}
	private void initRecord()
	{
		if(dao==null)
		{
			dao = new PlayrecordDao(this);
		}
		record = dao.findRecord(courseid, username);
		if(record==null)
		{
			record = new Playrecord();
			record.setCourseId(courseid);
			record.setUsername(username);
			dao.save(record);
		}
	}
	private void recordAndBack()	//返回按钮的监听方式实现
	{
		//to do something
		if(!"free".equals(playType))
		{
			record.setCurrentTime(player.getCurrentTime());
			dao.saveOrUpdate(record);
		}
		if(player!=null)
		{
			try{
				player.stop();
			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		//开一个线程(应该要开一个线程)
		super.onStart();
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		MobclickAgent.onResume(this);
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		MobclickAgent.onPause(this);
	}
	private void playOrPause()	//点击播放或者暂停
	{
		//暂停视频播放
		if(player.isPlaying())
		{
			player.pause();//暂停
			//更新数据库
			if(!"free".equals(playType))
			{
			record.setCurrentTime(player.getCurrentTime());
			dao.saveOrUpdate(record);
			}
			//换图片
			playbtn.setBackgroundResource(R.drawable.play_button);
			playbtn.setImageResource(R.drawable.player_play);
			return;
		}
		player.play();
		playbtn.setBackgroundResource(R.drawable.pause_button);
		playbtn.setImageResource(R.drawable.player_pause);
	}
	private void forward()
	{
		if(player!=null)
		{
			player.setForward();
		}
	}
	private void back()
	{
		if(player!=null)
		{
			player.setBack();
		}
	}
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		if(!player.isNull())
		{
			recordAndBack();//停止记时
		}
		super.onStop();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		endFlag = true;
		if(title!=null&&title.isShowing())
		{
			title.dismiss();
		}
		if(toolbar!=null&&toolbar.isShowing())
		{
			toolbar.dismiss();
		}
		super.onDestroy();
	}
	//轻触屏幕,控制条出现或者消失
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		if(toolbar!=null&&title.isShowing())
		{
			title.dismiss();
			toolbar.dismiss();
			return true;
		}
		if(toolbar!=null&&!toolbar.isShowing())
		{
			title.showAtLocation(view, Gravity.TOP, 0, 0);
   		 	title.update(height-50, 0, width, 50);
   		 	toolbar.showAtLocation(view, Gravity.BOTTOM, 0, 0);
   		 	toolbar.update(0, 0, width, 120);
		}
		return true;
	}
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		touchStartTime = System.currentTimeMillis();
		//new ShowThread().start();
		return mGestureDetector.onTouchEvent(event);
	}
	private class ShowThread extends Thread
	{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(!endFlag)
			{
				long endTime = System.currentTimeMillis();
				if(endTime-touchStartTime>6000)
				{
					Message msg = handler.obtainMessage();
					msg.what = 0;
					handler.sendMessage(msg);
					break;
				}
			}
		}
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		   switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:// 音量增大
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume+1, 8);
		if(title!=null&&title.isShowing())
		{
			volumnBar.setProgress(currentVolume+1);
			volumnSize.setText((currentVolume+1)*100/volumnMax+"%");
		}
		break;
		case KeyEvent.KEYCODE_VOLUME_DOWN:// 音量减小
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume-1, 8);
		if(title!=null&&title.isShowing())
		{
			if(currentVolume!=0)
			{
				volumnBar.setProgress(currentVolume-1);
				volumnSize.setText((currentVolume-1)*100/volumnMax+"%");
			}
		}
		break;
		case KeyEvent.KEYCODE_BACK:// 返回键
		//jniOnCallCppEvent();
		    return super.onKeyDown(keyCode, event); 
		default:
		break;
		}
		   
		   return true;
	}
}
