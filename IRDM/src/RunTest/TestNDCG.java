package RunTest;

import eval.NDCG;
import eval.Qrel;
import eval.Result;
import utils.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by AlexJIANG on 3/30/16.
 */
public class TestNDCG {


    public static void main(String args[]){
        Properties ps = new Properties();
        try {
            ps.load(new FileInputStream(System.getProperty("user.dir")+File.separator+"Configure"+File.separator+"configuration.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String BM_Path=ps.getProperty("BMRes_path");
        String ad_hoc_Path=ps.getProperty("ad_hoc_path");
        HashMap<String, HashMap<Integer,Integer>> all_cache =new HashMap<>();
        ArrayList<Qrel> qrels = Utils.loadQrels(all_cache, ad_hoc_Path, Utils.QREL.ADHOC);
        ArrayList<Result> results = Utils.loadResults(BM_Path);
        int ks[]={1,5,10,20,30,40,50};
        File folder = new File(System.getProperty("user.dir")+File.separator+"file");
        if(!folder.exists()&&!folder.isDirectory()){
            try{
                folder.mkdir();
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            System.out.println("forder exits");
        }
        File file = new File(System.getProperty("user.dir")+File.separator+"file"+File.separator+"bm25_ndcg.txt");
        if(!file.exists()){
            try {
                file.createNewFile();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write("bm25");
            System.out.println("bm25");
            bw.newLine();
            bw.write("K        |       NDCG@k");
            System.out.println("K        |       NDCG@k");
            bw.newLine();
            String temp;
            for(Integer k:ks){
                if(k>9){
                    bw.write(temp=(k+"       |       "+Double.parseDouble(String.format("%.3f",NDCG.compute(all_cache,results,qrels,k)))));
                    System.out.println(temp);
                }else{
                    bw.write((temp=k+"        |       "+Double.parseDouble(String.format("%.3f",NDCG.compute(all_cache,results,qrels,k)))));
                    System.out.println(temp);
                }

                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
