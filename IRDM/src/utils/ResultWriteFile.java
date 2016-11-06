package utils;

import eval.Result;

import java.io.*;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by AlexJIANG on 4/1/16.
 */
public class ResultWriteFile {
    private static final String config_path="/Users/AlexJIANG/UCL/Coursework/Backup/IRDM/Configure/configuration.properties";
    public void write_file(ArrayList<Result> results,String folder_path,String file_path,String model){
        File folder = new File(folder_path);
        if(!folder.exists()&&!folder.isDirectory()){
            try{
                folder.mkdir();
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            System.out.println("forder exits");
        }
        File file = new File(file_path);
        if(!file.exists()){
            try {
                file.createNewFile();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            for(Result res:results){
                bw.write(res.getTopic_no()+" "+"Q0"+" "+res.getDocument_id()+" "+res.getDocument_rank()
                +" "+res.getScore()+" "+model);
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


}
