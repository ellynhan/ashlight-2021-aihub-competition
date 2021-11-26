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
    Distance distClass = new Distance();
    ImageTask it = new ImageTask();
    boolean isProcessing;
    ArrayList<Object[]> result = new ArrayList();

    ProcessImage(Bitmap b, Activity a, boolean p){
        this.bmp = getResizedBitmap(b,640);
        this.activity = a;
        this.isProcessing = p;
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private Interpreter getTfliteInterpreter(String modelPath){
        try{
            return new Interpreter(loadModelFile(activity, modelPath));
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public class ImageTask extends AsyncTask<Void, Void, Exception> {
        float resultValue=0;
        float resultClassIndex=0;
        int resultFilterIndex=0;

        @Override
        protected Exception doInBackground(Void... params) {
            processFinished();
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
        };

        //AI process start
        void processFinished(){
            float[][][][] input = new float[1][640][640][3];
            float[][][] output = new float[1][25200][13]; //앞4개 좌표, 5번째 확률, 나머지8개 클래스

            for(int y=0; y<640; y++){
                for(int x=0; x<640; x++){
                    int pixel = bmp.getPixel(x,y);
                    input[0][y][x][0] = Color.red(pixel)/255.0f;
                    input[0][y][x][1] = Color.green(pixel)/255.0f;
                    input[0][y][x][2] = Color.blue(pixel)/255.0f;
                }
            }

            Interpreter lite = getTfliteInterpreter("proto2.tflite");
            lite.run(input,output);

            for(int i=0; i<25200; i++){ //[0,1,2,3] = [x,y,w,h]
                output[0][i][0] *= 640;
                output[0][i][1] *= 640;
                output[0][i][2] *= 640;
                output[0][i][3] *= 640;
            }

            //confidence 한번 거르는 작업을 했음.
            ArrayList<Float> tmp = new ArrayList<>();
            ArrayList<Object[]> filter = new ArrayList<>();
            for(int i=0; i<25200; i++){
                if(output[0][i][4]>0.25){
                    for(int j=0; j<5; j++){
                        tmp.add(output[0][i][j]);
                    }
                    for(int j=5; j<13; j++){
                        tmp.add(output[0][i][j]*output[0][i][4]); //confidence * probability
                    }
                    filter.add(tmp.toArray());
                    tmp.clear();
                }
            }

            if(filter.size()==0){
                isProcessing = false;
                return;
            }

            //중심과 너비, 높이 값을 이용해서 x,y의 최대/최소 값을 구하는 것
            ArrayList<Object[]> box = new ArrayList<>();
            for(int i=0; i<filter.size(); i++){
                tmp.add((float)filter.get(i)[0]-((float)filter.get(i)[2])/2);
                tmp.add((float)filter.get(i)[1]-((float)filter.get(i)[3])/2);
                tmp.add((float)filter.get(i)[0]+((float)filter.get(i)[2])/2);
                tmp.add((float)filter.get(i)[1]+((float)filter.get(i)[3])/2);
                box.add(tmp.toArray());
                tmp.clear();
            }

            //결과값 1개 뽑기
            System.out.print("filter SIZE:  ");
            System.out.println(filter.size());
            for(int i=0; i<filter.size(); i++){
                resultValue=0;
                resultClassIndex=0;
                boolean isRightRange = true;
                //값 이상한거 거르고 안 이상하면 좌표 저장해두기
                for(int j=0; j<4; j++){
                    float value = (float) box.get(i)[j];
                    if(value<0.0||value>640.0){
                        isRightRange = false;
                        break;
                    }
                    tmp.add((float)box.get(i)[j]);
                }

                //값이 정상이었으면 진행
                if(isRightRange == false)continue;

                //클래스 중에서 어떤 애가 가장 확률이 높은가
                for(int j=0; j<8; j++){
                    float currValue = (float)filter.get(i)[5+j];
                    if(currValue > resultValue){
                        resultValue = currValue;
                        resultClassIndex = j;
                        resultFilterIndex= i;
                    }
                }
            }
            System.out.println("===================");
            System.out.println(resultValue);
            System.out.println(resultClassIndex);
            System.out.println(resultFilterIndex);
            System.out.println("===================");

            //result에는 확률값, 클래스 인덱스, 방위값 저장됨
            tmp.add(resultClassIndex);
            float xl=(float)box.get(resultFilterIndex)[0];
            float yb=(float)box.get(resultFilterIndex)[1];
            float xr=(float)box.get(resultFilterIndex)[2];
            float yt=(float)box.get(resultFilterIndex)[3];
//                float dist = distClass.getDistance(xl,yb,xr,yt);
            float direct = distClass.getDirection(xr,xl);
//                tmp.add(dist);
            tmp.add(direct);
//            if(direct<=14 && direct>=9){
                result.add(tmp.toArray());
//            }
            tmp.clear();
            isProcessing = false;
            System.out.print("RESULT SIZE:  ");
            System.out.println(result.size());
        }
    }
}

/*

public void processFinished(){
                    float[][][][] input = new float[1][320][320][3];
                    float[][][] output = new float[1][6300][85];
                    ArrayList<Float> classFilter = new ArrayList<>();
                    for(int y=0; y<320; y++){
                        for(int x=0; x<320; x++){
                            int pixel = bmp.getPixel(x,y);
                            input[0][y][x][0] = Color.red(pixel)/255.0f;
                            input[0][y][x][1] = Color.green(pixel)/255.0f;
                            input[0][y][x][2] = Color.blue(pixel)/255.0f;
                        }
                    }
                    Interpreter lite = getTfliteInterpreter("untrained_model.tflite");
                    lite.run(input,output);
                    for(int i=0; i<6300; i++){ //[0,1,2,3] = [x,y,w,h]
                        output[0][i][0] *= 320;
                        output[0][i][1] *= 320;
                        output[0][i][2] *= 320;
                        output[0][i][3] *= 320;
                    }
                    //confidence 한번 거르는 작업을 했음.
                    ArrayList<Float> tmp = new ArrayList<>();
                    ArrayList<Object[]> filter = new ArrayList<>();
                    for(int i=0; i<6300; i++){
                        if(output[0][i][4]>0.25){
                            for(int j=0; j<5; j++){
                                tmp.add(output[0][i][j]);
                            }
                            for(int j=5; j<85; j++){
                                tmp.add(output[0][i][j]*output[0][i][4]);

                            }
                            filter.add(tmp.toArray());
                            tmp.clear();
                        }
                    }
                    ArrayList<Object[]> box = new ArrayList<>();
                    for(int i=0; i<filter.size(); i++){
                        tmp.add((float)filter.get(i)[0]-((float)filter.get(i)[2])/2);
                        tmp.add((float)filter.get(i)[1]-((float)filter.get(i)[3])/2);
                        tmp.add((float)filter.get(i)[0]+((float)filter.get(i)[2])/2);
                        tmp.add((float)filter.get(i)[1]+((float)filter.get(i)[3])/2);
                        box.add(tmp.toArray());
                        tmp.clear();
                    }

                    ArrayList<Object[]> result = new ArrayList();
                    for(int i=0; i<filter.size(); i++){
                        float maxValue=0,index=0;
                        boolean isRightRange = true;
                        for(int j=0; j<4; j++){
                            float value = (float) box.get(i)[j];
                            if(value<0.0||value>320.0){
                                isRightRange = false;
                                break;
                            }
                            tmp.add((float)box.get(i)[j]);
                        }

                        if(isRightRange == false)continue;
                        for(int j=0; j<80; j++){
                            float currValue = (float)filter.get(i)[5+j];
                            if(currValue > maxValue){
                                maxValue = currValue;
                                index = j;
                            }
                        }
                        if(maxValue > 0.70){ //0.45에서 0.70로 올렸음 너무 이상한거 많이 나옴
                            tmp.add(maxValue);
                            tmp.add(index);
                            float dist = distClass.getDistance(tmp.get(0),tmp.get(1),tmp.get(2),tmp.get(3));
                            float direct = distClass.getDirection(tmp.get(0),tmp.get(2));
                            tmp.add(dist);
                            tmp.add(direct);
                            if(classFilter.contains(index)==false&&index<80.0&&dist<6.0&&direct<14&&direct>9){//5m정도로 거름
                                result.add(tmp.toArray());
                                classFilter.add(index);
                            }
                        }
                        tmp.clear();
                    }
                    runOnUiThread(new Runnable() { //UI바꾸는건 mainThread에서 해야하기 때문에 이 쓰레드에서 작업을 해주어야 오류가 안남.
                        @Override
                        public void run() {
                            for(int i=0; i<result.size(); i++){
                                if((float)result.get(i)[5]>80.0){
                                    continue;
                                }
                                float indexFloat = (float) result.get(i)[5];
                                int indexInt = (int)indexFloat;
                                if(indexInt>=10){
                                    return ;
                                }
                                if(indexInt == 0){
                                    distancePerson.setText(String.format("%.3f", result.get(i)[6])+"m");
                                    directionPerson.setText(""+Math.round((float)result.get(i)[7])+"시 방향");
                                }
                                //출력 된 놈들 중에 확률 제일 높은 놈이 배열의 맨 앞에 오도록 계산하기.
                                box0.setText(String.format("%.3f", result.get(i)[0]));
                                box1.setText(String.format("%.3f", result.get(i)[1]));
                                box2.setText(String.format("%.3f", result.get(i)[2]));
                                box3.setText(String.format("%.3f", result.get(i)[3]));
                                score.setText(String.format("%.3f", result.get(i)[4]));
                                classIndex.setText(indexToClass[indexInt]);

                                if(mediaPlayer!=null){
                                    return ;
                                }
                                if(indexInt<10){
//                                    if(indexInt==0){
//                                        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.thereisperson);
//                                    }else if(indexInt==1){
//                                        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.bicycle);
//                                    }else if(indexInt==2) {
//                                        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.car);
//                                    }else if(indexInt==3){
//                                        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.motorcycle);
//                                    }else if(indexInt==5){
//                                        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.bus);
//                                    }else if(indexInt==7){
//                                        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.truck);
//                                    }else if(indexInt==9){
//                                        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.trafficlight);
//                                    }else{
//                                        return ;
//                                    }
                                    mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(1.25f));
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
                                        }
                                    });
                                }
                            }
                        }
                    });
                    processing = false;
                }
 */