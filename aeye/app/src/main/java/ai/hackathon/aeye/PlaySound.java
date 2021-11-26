package ai.hackathon.aeye;

import android.content.Context;
import android.media.MediaPlayer;

import ai.hackathon.aeye.R;

public class PlaySound {
    MediaPlayer mediaPlayer;
    int classIndex;
    int direct;
    Context context;
    boolean isProcessing;
    PlaySound(int ci, int d, Context c, boolean p){
        this.classIndex = ci;
        this.direct = d;
        this.context = c;
        this.isProcessing = p;
    }
    //AI Class 값 목록
    /*
    'traffic_light', 'car', 'caution_zone->grating', 'person', 'motorcycle', 'caution_zone->manhole', 'bicycle', 'caution_zone->repair_zone'
     */
    String[] indexToClass= {"신호등", "자동차", "창살배수구", "사람", "오토바이", "맨홀", "자전", "손상된 길"};
    public void play(){
        mediaPlayer = MediaPlayer.create(context, R.raw.work);
        mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(5f)); //1.25f
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.stop();
                mp.reset();
                if(mediaPlayer!=null){
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
                isProcessing = false;
                System.out.println("SONG IS FINISHED");
            }
        });
    }
}
