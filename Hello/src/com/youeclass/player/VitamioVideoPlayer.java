package com.youeclass.player;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.widget.VideoView;

import java.util.Timer;
import java.util.TimerTask;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.youeclass.VideoActivity3;


public class VitamioVideoPlayer implements OnBufferingUpdateListener,
		OnCompletionListener, OnPreparedListener,
		OnInfoListener {
	
	private static final String TAG = "VitamioVideoView";
	private VideoView mVideoView;
	private SurfaceHolder surfaceHolder;
	private SeekBar skbProgress;
	private TextView currentTime;
	private TextView totalTime;
	private RelativeLayout loadLayout;
	private long duration;
	private int recordTime;
	private VideoActivity3 v3;
	private Timer mTimer ;
	
	public VitamioVideoPlayer(VideoActivity3 v3, VideoView videoView,
			SeekBar skbProgress, TextView currentTime, TextView totalTime,
			int recordTime, RelativeLayout loadLayout,String url) {
		// TODO Auto-generated constructor stub
		this.v3 = v3;
		this.mVideoView = videoView;
		this.skbProgress = skbProgress;
		this.currentTime = currentTime;
		this.totalTime = totalTime;
		this.recordTime = recordTime;
		this.loadLayout = loadLayout;
//		String url2 = "http://www.youeclass.com:8090/2013yjssssjj2-1.flv";
		System.out.println("最终的播放地址为:"+url);
		mVideoView.setVideoURI(Uri.parse(url));
		mVideoView.requestFocus();
		mVideoView.setOnBufferingUpdateListener(this);
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnPreparedListener(this);
	}
	
	TimerTask mTimerTask = new TimerTask() {
		@Override
		public void run() {
			try {
				if (mVideoView.isPlaying() && skbProgress.isPressed() == false) {
					handleProgress.sendEmptyMessage(0);
				}
			} catch (NullPointerException e) {
				Log.e(TAG, "播放器还没有创建");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	Handler handleProgress = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				try {
					int position = (int) mVideoView.getCurrentPosition();
					// int duration = mediaPlayer.getDuration();
					if (duration > 0) {
						long pos = skbProgress.getMax() * position / duration;
						skbProgress.setProgress((int) pos);
						currentTime.setText(getTime(position / 1000));
					}
					if (duration == position) {

					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		};
	};
	private String getTime(long count) {
		long m = count / 60;
		long s = count % 60;
		String ms = m < 10 ? "0" + m : m + "";
		return ms + ":" + (s < 10 ? "0" + s : s + "");
	}
	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// TODO Auto-generated method stub
		skbProgress.setSecondaryProgress(percent);
	}
	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		Log.e("mediaPlayer", "onCompletion 播放完毕");
		skbProgress.setProgress(0);
		currentTime.setText("00:00");
		mVideoView.seekTo(0);
		mVideoView.pause();
		mTimer.cancel();
		mTimer = null;
		v3.onCompletion(mp);
	}
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		return true;
	};
	public void onPrepared(MediaPlayer mp) {
		Log.e(TAG,"videoview OnPrepared");
		loadLayout.setVisibility(View.GONE);
		skbProgress.setOnSeekBarChangeListener(new SeekBarChangeEvent());
		duration = mp.getDuration();
		totalTime.setText(getTime(duration / 1000));
		if (recordTime > 0 && recordTime< duration-10) {
			skbProgress
					.setProgress((int) (recordTime * skbProgress.getMax() / duration));
			mp.seekTo(recordTime);
		}
		mp.setPlaybackSpeed(1.0f);
		if(!mp.isPlaying())
		{
			mp.start();
		}
	};
	class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener {
		int progress;

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// 原本是(progress/seekBar.getMax())*player.mediaPlayer.getDuration()
			this.progress = (int) (progress * mVideoView.getDuration() / seekBar
					.getMax());
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// seekTo()的参数是相对与影片时间的数字，而不是与seekBar.getMax()相对的数字
			mVideoView.seekTo(progress);
			// 更改时间
			currentTime.setText(getTime(progress / 1000));
		}
	}

	public void play()
	{
		Log.e(TAG,"videoview播放");
		mVideoView.start();
		if(mTimer == null)
		{
			mTimer = new Timer();
			mTimer.schedule(mTimerTask, 0, 1000);
		}
	}
	public void pause()
	{
		Log.e(TAG,"videoview暂停");
		if(mVideoView.isPlaying())
		{
			mVideoView.pause();
		}
	}
	public void stop()
	{
		Log.e(TAG,"videoView 停止播放");
		mVideoView.stopPlayback();
	}
	public long getCurrentTime()
	{
		return mVideoView.getCurrentPosition();
	}
	public void setBack() {
		long ct = mVideoView.getCurrentPosition();
		if (ct - 5000 > 0) {
			ct = ct - 5000;
			mVideoView.seekTo(ct);
			skbProgress
					.setProgress((int) (ct * skbProgress.getMax() / duration));
			currentTime.setText(getTime(ct / 1000));
		}
	}
	public void setForward() {
		long ct = mVideoView.getCurrentPosition();
		if (ct + 5000 < mVideoView.getDuration()) {
			ct = ct + 5000;
			mVideoView.seekTo(ct);
			skbProgress
					.setProgress((int) (ct * skbProgress.getMax() / duration));
			currentTime.setText(getTime(ct / 1000));
		}
	}
	public boolean isPlaying() {
		return mVideoView.isPlaying();
	}

}
