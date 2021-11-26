package ai.hackathon.aeye;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import java.util.ArrayList;

public class TrafficSignColor {
    Bitmap crop;
    Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
    TrafficSignColor(Bitmap bmp, int xl,int yb,int xr,int yt){
        crop = Bitmap.createBitmap(xr-xl+1, yt-yb+1, conf); // this creates a MUTABLE bitmap
        for(int y = yb; y<=yt; y++){
            for(int x=xl; x<=xr; x++){
                crop.setPixel(x-xl,y-yb,bmp.getPixel(x,y));
            }
        }
    }
    public Boolean isRed(){
        Bitmap bmp = crop;
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int redCount = 0;
        int greenCount = 0;
        for(int i=0; i<h; i++){
            for(int j=0; j<w; j++){
                int c = bmp.getPixel(j, i);
                int red = Color.red(c);
                int green = Color.green(c);
                if(red>green && red-green>80){
                    redCount++;
                }
                if(red<green && green-red>80){
                    greenCount++;
                }
            }
        }
        if(redCount>greenCount){
            return true;
        }else{
            return false;
        }
    }

}
