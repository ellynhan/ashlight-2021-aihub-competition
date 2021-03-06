package ai.hackathon.aeye;

import android.content.Context;
import android.media.MediaPlayer;

import ai.hackathon.aeye.R;

public class PlaySound {
    MediaPlayer mediaPlayer;
    int classIndex;
    int direct;
    float dist;
    Context context;
    boolean isProcessing;
    boolean isManhole;
    PlaySound(int ci, int direct, float dist, Context c, boolean p, boolean manhole){
        this.classIndex = ci;
        this.direct = direct;
        this.dist = dist;
        this.context = c;
        this.isProcessing = p;
        this.isManhole = manhole;
    }
    //AI Class 값 목록
    //manhole index 16
    String[] indexToClassName= {"사람", "자전거", "자동차", "오토바이", "비행기", "버스", "기차", "트럭", "boat", "신호등",
            "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
            "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
            "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
            "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"};
    //사람 0 자전거 1 자동차 2 오토바이 3 신호등 9

    public void setObject(){
        if(isManhole){
            mediaPlayer = MediaPlayer.create(context, R.raw.manhole);
        }else{
            if(classIndex == 0){
                mediaPlayer = MediaPlayer.create(context, R.raw.people);
            }else if(classIndex == 1){
                mediaPlayer = MediaPlayer.create(context, R.raw.bicycle);
            }else if(classIndex == 2){
                mediaPlayer = MediaPlayer.create(context, R.raw.car);
            }else if(classIndex == 3){
                mediaPlayer = MediaPlayer.create(context, R.raw.motorcycle);
            }else if(classIndex == 100){
                mediaPlayer = MediaPlayer.create(context, R.raw.redlight);
            }else if(classIndex == 101){
                mediaPlayer = MediaPlayer.create(context, R.raw.greenlight);
            }
        }
    }

    public void setDistance(){
        if(dist<2){
            mediaPlayer = MediaPlayer.create(context, R.raw.m1);
        }else if(dist<3){
            mediaPlayer = MediaPlayer.create(context, R.raw.m2);
        }else if(dist<4){
            mediaPlayer = MediaPlayer.create(context, R.raw.m3);
        }else if(dist<5){
            mediaPlayer = MediaPlayer.create(context, R.raw.m4);
        }else if(dist<6){
            mediaPlayer = MediaPlayer.create(context, R.raw.m5);
        }
    }

    public void setDirection(){
        if(direct>9 && direct<=10){
            mediaPlayer = MediaPlayer.create(context, R.raw.clock10);
        }else if(direct==11){
            mediaPlayer = MediaPlayer.create(context, R.raw.clock11);
        }else if(direct==12){
            mediaPlayer = MediaPlayer.create(context, R.raw.clock12);
        }else if(direct==13){
            mediaPlayer = MediaPlayer.create(context, R.raw.clock13);
        }else if(direct<15){
            mediaPlayer = MediaPlayer.create(context, R.raw.clock14);
        }
    }

    public void playSoundProcess(){
        System.out.println("======SOUND PLAY======");
        setDirection();
        play();
        setDistance();
        play();
        setObject();
        play();
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
                System.out.println("=====PLAY FINISHED=====");
            }
        });
    }

    public void play(){
        mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(1.25f)); //1.25f
        mediaPlayer.start();
        while(mediaPlayer.isPlaying()){}
        mediaPlayer.reset();
        mediaPlayer.release();
    }
}
