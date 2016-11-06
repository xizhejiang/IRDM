package RunTest;

import diversity.MMRScoring;
import eval.Result;
import org.terrier.applications.batchquerying.TRECQuery;
import org.terrier.matching.ResultSet;
import utils.ResultWriteFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by AlexJIANG on 3/30/16.
 */
public class TestMMR {
    public static void main(String args[]){
        Properties ps = new Properties();
        try {
            ps.load(new FileInputStream("/Users/AlexJIANG/UCL/Coursework/Backup/IRDM/Configure"
                    +File.separator+"configuration.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.setProperty("terrier.home","/Users/AlexJIANG/UCL/MSCNCS/IRDM/terrier-core-4.1/");
        System.setProperty("terrier.home","/Users/AlexJIANG/Downloads/terrier-core-4.1");
        String topic_file_path = ps.getProperty("topic_file_path");
        TRECQuery trec_topics = new TRECQuery(topic_file_path);
        String index_path = ps.getProperty("index_path");
        MMRScoring mmr = new MMRScoring(index_path,"terrier_clueweb_index");
        int i = 0;
        ArrayList<Result> ress = new ArrayList<>();
        ArrayList<Result> temps;
       // mmr.create_file_vector(ps);
        while(trec_topics.hasNext())
        {
            String query = trec_topics.next();
            ResultSet set = mmr.buildResultSet(i, query,100);
            if(set.getExactResultSize()<100){
                System.out.println("Remove bad record");
                continue;
            }
            mmr.caculate(set);
            i=i++;
            temps=mmr.RankList(0.5,trec_topics.getQueryId(),set);
            if(temps!=null){
                for(Result result:temps){
                    ress.add(result);
                }
            }

        }
        ResultWriteFile rwf = new ResultWriteFile();
        rwf.write_file(ress,ps.getProperty("folder"),ps.getProperty("MMR.result"),"MMR");
        System.out.println("--------------------------------------------");
        mmr.closeIndex();
    }
}
