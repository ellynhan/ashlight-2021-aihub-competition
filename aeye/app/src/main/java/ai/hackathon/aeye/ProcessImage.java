package ai.hackathon.aeye;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class ProcessImage {
    Bitmap bmp;
    Activity activity;
    ImageTask it = new ImageTask();
    AiProcess ap;
    boolean manhole=false;
    boolean isProcessing;
    ArrayList<Object> result = new ArrayList();

    ProcessImage(Bitmap b, Activity a, boolean p){
        this.bmp = b;
        this.activity = a;
        this.isProcessing = p;
    }

    public class ImageTask extends AsyncTask<Void, Void, Exception> {

        @Override
        protected Exception doInBackground(Void... params) {
            processStart();
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
        };

        //AI process start
        void processStart(){
            System.out.println("====PROCESS START====");
            ap = new AiProcess(640,25200,17,"manhole.tflite",bmp,activity,true);
            ap.runProcess();
            if(ap.result.size() == 0 || ap.result.size()==3&&Float.parseFloat(ap.result.get(0).toString())<0.73){//else
                ap = new AiProcess(320,6300,80,"untrained_model.tflite",bmp,activity,true);
                ap.runProcess();
                if(ap.result.size()==3){
                    int classIdx = Integer.parseInt(ap.result.get(1).toString());
                    if(classIdx>3 && classIdx!=100 && classIdx!=101){
                        ap.result.clear();
                    }
                }
            }else{//manhole
                manhole = true;
            }
            System.out.println("=========PROCESS RESULT ========");
            System.out.println(ap.result.size());
            result = ap.result;
            isProcessing = false;
        }
    }
}