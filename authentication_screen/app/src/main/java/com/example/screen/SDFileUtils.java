package com.example.screen;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SDFileUtils {
    private String fileName;
    private static SDFileUtils logFile = null;
    private static final String TAG = "SDFileUtils";

    // SDCRAD文件访问的构造函数
    public SDFileUtils(String str) {
        //SDPATH = Environment.getExternalStorageDirectory().getAbsolutePath() ;
        this.fileName = str;
    }

    // 在SDCRAD上创建文件
    private File createFile() throws IOException {

        File file = new File(fileName);
        file.createNewFile();
        return file;
    }

    // 向文件中写入数据
    public void writeToSDFile(String msg) {
        File file = null;
        OutputStream outputStream = null;

        try {
            file = this.createFile();
            outputStream = new FileOutputStream(file, true);
            //Log.i(TAG, "writeToSDFile: " + (outputStream == null));
            outputStream.write(msg.getBytes());
            outputStream.flush();
        } catch (IOException e) {

            e.printStackTrace();
        } finally {
            //Log.i(TAG, "writeToSDFile: " + (outputStream == null));
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}