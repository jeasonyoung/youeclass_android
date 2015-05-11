package com.youeclass;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.VideoView;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.os.MessageQueue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;
import com.youeclass.dao.CourseDao;
import com.youeclass.dao.PlayrecordDao;
import com.youeclass.entity.Playrecord;
import com.youeclass.player.VitamioVideoPlayer;
import com.youeclass.util.Constant;
import com.youeclass.util.StringUtils;

/**
 * 视频播放Activity基础类
 * @author jeasonyoung
 *
 */
@SuppressLint("ClickableViewAccessibility")
public class VideoPlayActivity extends Activity implements OnTouchListener, OnGestureListener {
	private PopupWindow title,toolbar;
	private SeekBar seekBar,volumnBar;
	private TextView tvTitle,tvCurrentTime,tvTotalTime,tvVolumnSize;
	private ImageButton btnReturn,btnPrev,btnNext,btnPlay;
	private VitamioVideoPlayer player;
	private VideoView videoView;
	
	private RelativeLayout videoLoadingLayout;
	//private Handler handler;
	private GestureDetector gestureDetector;
	private AudioManager audioManager;
	
	private Playrecord playRecord;
	private PlayrecordDao playRecordDao;
	
	private int height,width,volumnMax;
	//private boolean endFlag = false;
//	private long touchStartTime = System.currentTimeMillis();
	private String name,username,playType,url,courseId,httpUrl;
	/*
	 * 重载创建
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//检查播放器依赖库
		if(!LibsChecker.checkVitamioLibs(this))return;
		//设置不要标题栏，必须在setContentView之前
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		//设置全屏
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//设置屏幕常亮
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		//设置内容布局
		this.setContentView(R.layout.videoview);
		//设置视频View
		this.videoView = (VideoView)this.findViewById(R.id.surface_view);
		//设置audioManager
		this.audioManager = (AudioManager)this.getSystemService(AUDIO_SERVICE);
		//加载传递数据
		Uri uri = this.loadIntentData();
		//
		LayoutInflater inflater = LayoutInflater.from(this);
		View titleView = inflater.inflate(R.layout.title, null);
		titleView.getBackground().setAlpha(0);
		View barView = inflater.inflate(R.layout.videocontrol, null);
		
		this.videoLoadingLayout = (RelativeLayout)this.findViewById(R.id.videoloadingLayout);
		this.videoLoadingLayout.setVisibility(View.VISIBLE);
		
		WindowManager wManager = (WindowManager)this.getSystemService(WINDOW_SERVICE);
		Display display = wManager.getDefaultDisplay();//.getSize(outSizePoint);
		this.width = display.getWidth();//屏幕宽度
		this.height =display.getHeight();//屏幕高度
		
		this.title = new PopupWindow(titleView, 400, 50);
		this.title.setAnimationStyle(R.style.AnimationFade);
		this.toolbar = new PopupWindow(barView);
		this.toolbar.setAnimationStyle(R.style.AnimationFade);
		
		//加载popupwindow
		this.loadPopupWindows();
		//
		this.btnReturn =  (ImageButton)titleView.findViewById(R.id.imageBack);
		this.btnReturn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				VideoPlayActivity.this.backRecord();
				VideoPlayActivity.this.finish();
			}
		});
		//
		this.tvTitle = (TextView)titleView.findViewById(R.id.videoName);
		this.tvTitle.setText(this.name);
		
		//视频进度条
		this.seekBar = (SeekBar)barView.findViewById(R.id.seekBar);
		//创建音量拖拽条
		this.createVolumnBar(titleView);
		
		this.btnPlay = (ImageButton)barView.findViewById(R.id.imagePlay);
		this.btnPlay.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//点击播放或者暂停
				if(player.isPlaying()){
					//暂停
					player.pause();
					//更新数据库
					if(!"free".equalsIgnoreCase(playType) && playRecord != null && playRecordDao != null){
						playRecord.setCurrentTime((int)player.getCurrentTime());
						playRecordDao.saveOrUpdate(playRecord);
					}
					//更换图片
					btnPlay.setBackgroundResource(R.drawable.play_button);
					btnPlay.setImageResource(R.drawable.player_play);
					return;
				}
				//
				player.play();
				btnPlay.setBackgroundResource(R.drawable.pause_button);
				btnPlay.setImageResource(R.drawable.player_pause);
			}
		});
		this.btnPrev = (ImageButton)barView.findViewById(R.id.imagePrevious);
		this.btnPrev.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(player != null) player.setBack();
			}
		});
		this.btnNext = (ImageButton)barView.findViewById(R.id.imageNext);
		this.btnNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(player != null) player.setForward();
			}
		});
		
		this.tvTotalTime = (TextView)barView.findViewById(R.id.totalTime);
		this.tvCurrentTime = (TextView)barView.findViewById(R.id.playTime);
		
		if(!StringUtils.isEmpty(this.url)  && (this.url.indexOf(this.username) == -1) && (this.url.indexOf(Constant.NGINX_URL) == -1)){
			this.url = Constant.NGINX_URL + this.url;
			uri = Uri.parse(this.url);
		}
		
		int recordTime = 0;
		if(!"free".equalsIgnoreCase(this.playType)){
			if(this.playRecordDao == null){
				this.playRecordDao = new PlayrecordDao(this);
			}
			this.playRecord = this.playRecordDao.findRecord(this.courseId, this.username);
			if(this.playRecord == null){
				this.playRecord = new Playrecord();
				this.playRecord.setCourseId(this.courseId);
				this.playRecord.setUsername(this.username);
				this.playRecordDao.save(this.playRecord);
			}
			recordTime = this.playRecord.getCurrentTime();
		}
		this.player = new VitamioVideoPlayer(this, this.videoView, this.seekBar, this.tvCurrentTime, this.tvTotalTime, recordTime, this.videoLoadingLayout, uri);
		
		this.gestureDetector = new GestureDetector(this, this);
		this.videoView.setOnTouchListener(this);
		this.videoView.setLongClickable(true);
		this.videoView.setFocusable(true);
		this.videoView.setClickable(true);
		this.gestureDetector.setIsLongpressEnabled(true);
	}
	//加载传递数据
	private Uri loadIntentData(){
		Intent intent = this.getIntent();
		this.name = intent.getStringExtra("name");
		this.url = intent.getStringExtra("url");
		this.username = intent.getStringExtra("username");
		this.courseId = intent.getStringExtra("courseid");
		this.httpUrl = intent.getStringExtra("httpUrl");
		this.playType = intent.getStringExtra("playType");
		//
		if(!StringUtils.isEmail(this.url) && this.url.indexOf(this.username) > -1){
			File file = new File(this.url);
			if(file.exists()){
				return Uri.fromFile(file);
			}
			if(!"free".equalsIgnoreCase(this.playType)){
				Toast.makeText(this, "本地文件已经被删除", Toast.LENGTH_SHORT).show();
				//修改courseTab中的记录
				new CourseDao(this).updateState(this.username, this.httpUrl, 0);//.updateState(this.httpUrl, 0, this.username);
				if("local".equalsIgnoreCase(intent.getStringExtra("loginType"))){
					this.finish();
					return null;
				}
			}
		}
		return StringUtils.isEmpty(this.httpUrl) ? null : Uri.parse(this.httpUrl);
	}
	//加载popupwindow
	private void loadPopupWindows(){
		Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
			@Override
			public boolean queueIdle() {
				//显示两个popupwindow
				if(title != null && videoView.isShown()){
					title.showAtLocation(videoView, Gravity.TOP, 0, 0);
					title.update(height - 50, 0, width, 50);
				}
				if(toolbar != null && videoView.isShown()){
					toolbar.showAtLocation(videoView, Gravity.BOTTOM, 0, 0);
					toolbar.update(0, 0, width, 120);
				}
				//显示正在加载
				return false;
			}
		});
	}
	//设置音量拖拽条
	private void createVolumnBar(View titleView){
		if(titleView == null)return;
		//音量拖拽条
		this.volumnBar = (SeekBar)titleView.findViewById(R.id.seekBar1);
		//音量百分数
		this.tvVolumnSize = (TextView)titleView.findViewById(R.id.volumnSize);
		//音量最大值
		this.volumnMax = this.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		//设置音量
		this.volumnBar.setMax(this.volumnMax);
		
		//当前音量
		int current = this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		//设置进度
		this.volumnBar.setProgress(current);
		this.tvVolumnSize.setText((current * 100 / volumnMax) + "%");
		//
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		//设置拖拽事件
		this.volumnBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			    int vol = seekBar.getProgress();
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
				tvVolumnSize.setText((vol * 100 / seekBar.getMax()) + "%");
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			
			}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
				
			}
		});
	}

	//返回按钮的监听方式实现
	private void backRecord(){
		if(this.playRecord == null || this.playRecordDao == null || this.player == null)return;
		if(!"free".equalsIgnoreCase(this.playType)){
			this.playRecord.setCurrentTime((int)this.player.getCurrentTime());
			this.playRecordDao.saveOrUpdate(this.playRecord);
		}
		try {
			this.player.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void onCompletion(MediaPlayer player){
		if(this.toolbar != null && !this.toolbar.isShowing() && this.title != null){
			this.title.showAtLocation(this.videoView, Gravity.TOP, 0, 0);
			this.title.update(height - 50, 0, width, 50);
			this.toolbar.showAtLocation(this.videoView, Gravity.BOTTOM, 0, 0);
			this.toolbar.update(0, 0, width, 120);
		}
		if(this.btnPlay != null){
			//换图片
			this.btnPlay.setBackgroundResource(R.drawable.play_button);
			this.btnPlay.setImageResource(R.drawable.player_play);
			if(this.seekBar != null)this.seekBar.setProgress(0);
			if(this.tvCurrentTime != null)this.tvCurrentTime.setText("00:00");
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(this.player != null)this.player.play();
		MobclickAgent.onResume(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(this.player != null)this.player.pause();
		MobclickAgent.onPause(this);
	}
	
	@Override
	protected void onStop() {
		this.backRecord();
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		//this.endFlag = true;
		if(this.title != null && this.title.isShowing()){
			this.title.dismiss();
		}
		if(this.toolbar != null && this.toolbar.isShowing()){
			this.toolbar.dismiss();
		}
		super.onDestroy();
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		//轻触屏幕，控制条出现或者消失
		if(this.toolbar != null && this.title != null &&  this.title.isShowing()){
			this.title.dismiss();
			this.toolbar.dismiss();
			return true;
		}
		if(this.toolbar != null && !this.toolbar.isShowing()){
			this.title.showAtLocation(this.videoView, Gravity.TOP, 0, 0);
			this.title.update(this.height - 50, 0, width, 50);
			this.toolbar.showAtLocation(this.videoView, Gravity.BOTTOM, 0, 0);
			this.toolbar.update(0, 0, width, 120);
		}
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,float velocityY) {
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		//this.touchStartTime = System.currentTimeMillis();
		return this.gestureDetector.onTouchEvent(event);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(this.audioManager == null) return true;
		int currentVolume = this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		switch(keyCode){
			case KeyEvent.KEYCODE_VOLUME_UP:{//音量增大
				this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume + 1, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
				if(this.title != null && this.title.isShowing()){
					if(this.volumnBar != null)this.volumnBar.setProgress(currentVolume + 1);
					if(currentVolume < this.volumnMax && this.tvVolumnSize != null){
						this.tvVolumnSize.setText(((currentVolume + 1) * 100 / this.volumnMax) + "%");
					}
				}
				break;
			}
			case KeyEvent.KEYCODE_VOLUME_DOWN:{//音量减小
				this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume - 1, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
				if(currentVolume > 0 && this.title != null && this.title.isShowing()){
					if(this.volumnBar != null)this.volumnBar.setProgress(currentVolume - 1);
					if(this.tvVolumnSize != null)this.tvVolumnSize.setText(((currentVolume - 1)*100/this.volumnMax) + "%");
				}
				break;
			}
			case KeyEvent.KEYCODE_BACK:{
				return super.onKeyDown(keyCode, event);
			}
			default:break;
		}
		return true;
	}
}