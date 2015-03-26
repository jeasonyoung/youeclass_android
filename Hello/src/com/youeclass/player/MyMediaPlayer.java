package com.youeclass.player;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MyMediaPlayer extends MediaPlayerStateWrapper{
	private int videoWidth;
	private int videoHeight;
	private SeekBar skbProgress;
	private TextView currentTime;
	private TextView totalTime;
	private RelativeLayout loadLayout;
	private int duration;
	private int recordTime;
	private Timer mTimer = new Timer();
	public MyMediaPlayer(SeekBar skbProgress,
		TextView currentTime, TextView totalTime,int recordTime,RelativeLayout loadLayout) {
		this.skbProgress = skbProgress;
		this.currentTime = currentTime;
		this.totalTime = totalTime;
		this.recordTime = recordTime;
		this.loadLayout = loadLayout;
		mTimer.schedule(mTimerTask, 0, 1000);
	}
	/*******************************************************
	 * 通过定时器和Handler来更新拖动条
	 ******************************************************/
	TimerTask mTimerTask = new TimerTask() {
		@Override
		public void run() {
			if (isPlaying() && skbProgress.isPressed() == false) {
				handleProgress.sendEmptyMessage(0);
			}
		}
	};

	Handler handleProgress = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				try{
					int position = getCurrentPosition();
					//int duration = mediaPlayer.getDuration();
					if (duration > 0) {
						long pos = skbProgress.getMax() * position / duration;
						skbProgress.setProgress((int) pos);
						currentTime.setText(getTime(position / 1000));
					}
					if (duration == position) {
					
					}
				}catch(Exception e)
				{
					e.printStackTrace();
				}
			}

		};
	};
	@Override
	public void onCompletion(android.media.MediaPlayer mp) {
		currentTime.setText(totalTime.getText());
		//退出来
		skbProgress.setProgress(0);
		currentTime.setText("00:00");
	};
	private String getTime(int count) {
		int m = count / 60;
		int s = count % 60;
		String ms = m < 10 ? "0" + m : m + "";
		return ms + ":" + (s < 10 ? "0" + s : s + "");
	}
	@Override
	public void onPrepared(android.media.MediaPlayer mp) {
		// TODO Auto-generated method stub
		loadLayout.setVisibility(View.GONE);
		videoWidth = getVideoWidth();
		videoHeight = getVideoHeight();
		/*if (videoHeight != 0 && videoWidth != 0) {
			start();
		}*/
		skbProgress.setOnSeekBarChangeListener(new SeekBarChangeEvent());
		duration = getDuration();
		totalTime.setText(getTime(duration / 1000));
		if(recordTime>0)
		{
			skbProgress.setProgress(recordTime*skbProgress.getMax()/duration);
			seekTo(recordTime);
		}
		Log.e("mediaPlayer", "onPrepared");
	}
	@Override
	public void onBufferingUpdate(android.media.MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		skbProgress.setSecondaryProgress(percent);
	}
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		mTimer.cancel();//取消这个定时任务
		super.stop();
	}
	//seekbar监听事件
	class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {
		int progress;
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// 原本是(progress/seekBar.getMax())*player.mediaPlayer.getDuration()
			this.progress = progress * getDuration()
					/ seekBar.getMax();
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// seekTo()的参数是相对与影片时间的数字，而不是与seekBar.getMax()相对的数字
			seekTo(progress);
			//更改时间
			currentTime.setText(getTime(progress/1000));
		}
	}
	//快进
	public void setForward()
	{
		int ct = this.getCurrentPosition();
		if(ct+5000<this.getDuration())
		{
			ct = ct+5000;
			seekTo(ct);
			skbProgress.setProgress(ct*skbProgress.getMax()/duration);
			currentTime.setText(getTime(ct/1000));
		}
	}
	//快退
	public void setBack()
	{
		int ct = this.getCurrentPosition();
		if(ct-5000>0)
		{
			ct = ct-5000;
			seekTo(ct);
			skbProgress.setProgress(ct*skbProgress.getMax()/duration);
			currentTime.setText(getTime(ct/1000));
		}
	}
}
