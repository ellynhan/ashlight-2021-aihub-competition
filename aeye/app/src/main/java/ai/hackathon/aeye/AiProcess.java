package ai.hackathon.aeye;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class AiProcess {
    Bitmap bmp;
    Activity activity;
    Distance distClass = new Distance();
    TrafficSignColor tsc;
    boolean isProcessing;
    float resultValue=0;
    float resultClassIndex=0;
    int resultFilterIndex=0;
    int inputSize;
    int outputSize;
    int classSize;
    String aiPathName;
    ArrayList<Object> result = new ArrayList();

    AiProcess(int is, int os,int cs,String path, Bitmap b, Activity a, boolean p){
        this.inputSize = is;
        this.outputSize = os;
        this.classSize = cs;
        this.aiPathName = path;
        this.bmp = getResizedBitmap(b,is);
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

    public void runProcess(){
        System.out.println("====PROCESS RUNNING====");
        float[][][][] input = new float[1][inputSize][inputSize][3];
        float[][][] output = new float[1][outputSize][5+classSize]; //앞4개 좌표, 5번째 확률, 나머지8개 클래스

        for(int y=0; y<inputSize; y++){
            for(int x=0; x<inputSize; x++){
                int pixel = bmp.getPixel(x,y);
                input[0][y][x][0] = Color.red(pixel)/255.0f;
                input[0][y][x][1] = Color.green(pixel)/255.0f;
                input[0][y][x][2] = Color.blue(pixel)/255.0f;
            }
        }

        Interpreter lite = getTfliteInterpreter(aiPathName);
        lite.run(input,output);

        for(int i=0; i<outputSize; i++){ //[0,1,2,3] = [x,y,w,h]
            output[0][i][0] *= inputSize;
            output[0][i][1] *= inputSize;
            output[0][i][2] *= inputSize;
            output[0][i][3] *= inputSize;
        }

        //confidence 한번 거르는 작업을 했음.
        ArrayList<Float> tmp = new ArrayList<>();
        ArrayList<Object[]> filter = new ArrayList<>();
        for(int i=0; i<outputSize; i++){
            if(output[0][i][4]>0.25){
                for(int j=0; j<5; j++){
                    tmp.add(output[0][i][j]);
                }
                for(int j=5; j<5+classSize; j++){
                    tmp.add(output[0][i][j]*output[0][i][4]); //confidence * probability
                }
                filter.add(tmp.toArray());
                tmp.clear();
            }
        }

        //걸려진놈이 없다면 종료
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
        for(int i=0; i<filter.size(); i++){
            resultValue=0;
            resultClassIndex=0;
            boolean isRightRange = true;
            //값 이상한거 거르고 안 이상하면 좌표 저장해두기
            for(int j=0; j<4; j++){
                float value = (float) box.get(i)[j];
                if(value<0.0||value>(float)inputSize){
                    isRightRange = false;
                    break;
                }
                tmp.add((float)box.get(i)[j]);
            }

            //값이 정상이었으면 진행
            if(isRightRange == false)continue;

            //클래스 중에서 어떤 애가 가장 확률이 높은가
            for(int j=0; j<classSize; j++){
                float currValue = (float)filter.get(i)[5+j];
                if(currValue > resultValue){
                    resultValue = currValue;
                    resultClassIndex = j;
                    resultFilterIndex= i;
                }
            }
        }

        //결과값 낮으면 종료
        if(resultValue<0.45){
            isProcessing = false;
            return;
        }

        //debugging
        System.out.println("===================");
        System.out.println(resultValue);
        System.out.println(resultClassIndex);
        System.out.println(resultFilterIndex);
        System.out.println("===================");

        //result에는 확률값, 클래스 인덱스, 방위값 저장됨

        float xl=(float)box.get(resultFilterIndex)[0];
        float yb=(float)box.get(resultFilterIndex)[1];
        float xr=(float)box.get(resultFilterIndex)[2];
        float yt=(float)box.get(resultFilterIndex)[3];
        float direct = distClass.getDirection(xr,xl);
        float dist = distClass.getDistance(xl,yb,xr,yt);
        result.add(resultValue);
        if(resultClassIndex == 9){
            tsc = new TrafficSignColor(bmp,(int)xl,(int)yb,(int)xr,(int)yt);
            if(tsc.isRed()){ //빨간불 100
                result.add(100);
            }else{ //초록불 101
                result.add(101);
            }
        }else{
            result.add((int)resultClassIndex);
        }
        result.add((int)direct);
        result.add(dist);
        isProcessing = false;
    }
}
