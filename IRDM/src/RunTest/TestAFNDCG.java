package RunTest;

import eval.AlphaNDCG;
import eval.Qrel;
import eval.Result;
import utils.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

/**
 * Created by AlexJIANG on 4/3/16.
 */
public class TestAFNDCG {
    public static final String filename = "ws_mvr_ndcg-4.txt";
    public static final String lambda ="0.5";
    public static final String b="-4";
    public static void main(String args[]) {

        Properties ps = new Properties();
        try {
            ps.load(new FileInputStream(System.getProperty("user.dir") + File.separator + "Configure" + File.separator + "configuration.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String BM_Path = ps.getProperty("MMR.result");
        String di_hoc_Path = ps.getProperty("diversity_path");
        HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> all_cache = new HashMap<>();
        HashMap<Integer, HashSet<Integer>> map_sets = new HashMap<>();
        ArrayList<Qrel> qrels = Utils.loadDiversityQrels(all_cache, map_sets, di_hoc_Path, Utils.QREL.ADHOC);
        System.out.println(all_cache.size());
        ArrayList<Result> results = Utils.loadResults(BM_Path);
        int ks[] = {1, 5, 10, 20, 30, 40, 50};
        double alphas[]={0.1,0.5,0.9};
        File folder = new File(System.getProperty("user.dir") + File.separator + "file");
        if (!folder.exists() && !folder.isDirectory()) {
            try {
                folder.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("forder exits");
        }
        File file = new File(System.getProperty("user.dir") + File.separator + "file" + File.separator + filename);
        if (!file.exists()) {
            try {
                file.createNewFile();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
//        bw.write("bm25");
//        System.out.println("bm25");
//        bw.newLine();
//        bw.write("K        |       NDCG@k");
//        System.out.println("K        |       NDCG@k");
//        bw.newLine();
            bw.write("porfolio");
            bw.newLine();
            bw.write("lambda="+lambda);
            bw.newLine();
            bw.write("alpha  |  K  |  alpha-NDCG@K");
            bw.newLine();
            String temp;
                for(Double alpha:alphas){
                    for (Integer k : ks) {
                        bw.write((temp = alpha+"  |  "+k + "  |  " + Double.parseDouble(String.format("%.3f", AlphaNDCG.compute(all_cache, map_sets, results, qrels, k, alpha)))));
                        System.out.println(temp);
                        bw.newLine();
                    }

                }

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
