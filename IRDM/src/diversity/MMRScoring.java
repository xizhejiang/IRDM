package diversity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import eval.*;


import org.terrier.matching.ResultSet;
import org.terrier.matching.models.basicmodel.In;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.utility.ApplicationSetup;
import utils.Utils;

public class MMRScoring {

	/* Terrier Index */
	Index index;

	/* Index structures*/
	/* list of terms in the index */
	Lexicon<String> term_lexicon = null;
	/* list of documents in the index */
	DocumentIndex doi = null;

	/* Collection statistics */
	long total_tokens;
	long total_documents;
	/**
	 * The cache to all cosine similarity caculated
	 */
	HashMap<Integer,HashMap<Integer,Double>> cache_map;
	/**
	 * The cache of the term frequency accessed before
	 */
	HashMap<Integer,HashMap<Integer,Integer>> tf_cache;
	ArrayList<String> file_vector ;




	/**
	 * Initialize MMR model with index. Use
	 * @param index_path : initialize index
	 * @param prefix : language prefix for index (default = 'en')
	 * with location of index created using bash script.
	 */
	public MMRScoring(String index_path, String prefix) {
		index = Index.createIndex(index_path,prefix);
		cache_map = new HashMap<>();
		tf_cache=new HashMap<>();
		file_vector = new ArrayList<>();

	}

	/**
	 * Check if the required correlation is in the cache. If it is, return the correlation.
	 * In order to decrease the time complexity of the calculation
	 * @param document1
	 * @param document2
     * @return
     */
	public Double check_cache(int document1,int document2){
		if(cache_map.get(document1)!=null){
			if(cache_map.get(document1).get(document2)!=null){
				return cache_map.get(document1).get(document2);
			}
		}
		if(cache_map.get(document2)!=null){
			if(cache_map.get(document2).get(document1)!=null){
				return cache_map.get(document2).get(document1);
			}
		}
		return null;

	}

	/**
	 * This is the precaculation. The calculation will previously calculate
	 * n*(n-1)/2 combinations of the set of 100 files. The is to decrease the repeat calculation
	 * and decrease the time complexity of caculation.
	 * @param set The set contains 100 top files for a particular topic
     */
	public void caculate(ResultSet set){

		int total =0;
		HashMap<Integer, Integer> map1=null;
		HashMap<Integer, Integer> map2=null;
		int doc_ids [] = set.getDocids();
		double doc_scores [] = set.getScores();
		double print = 0.0;
		HashMap <Integer, Double> scores = new HashMap<>();
		for (int i = 0 ; i < doc_scores.length;i++){
			scores.put(doc_ids[i], doc_scores[i]);
		}
		for(Map.Entry<Integer,Double> entry1:scores.entrySet()){
			for(Map.Entry<Integer,Double> entry2:scores.entrySet()){
				if(entry1.getKey().equals(entry2.getKey())||check_cache(entry1.getKey(),entry2.getKey())!=null){
					continue;
				}else{

					if(tf_cache.containsKey(entry1.getKey())){
						map1 = tf_cache.get(entry1.getKey());

					}else{
						map1 = Utils.docTF(entry1.getKey(), index);
						tf_cache.put(entry1.getKey(),map1);
					}

					if(tf_cache.containsKey(entry2.getKey())){
						map1 = tf_cache.get(entry2.getKey());
					}else{
						map2 = Utils.docTF(entry2.getKey(), index);
						tf_cache.put(entry2.getKey(),map2);
					}
					System.out.println("map 2 size->"+ map2.size()+" "+entry2.getKey());

					if(cache_map.containsKey(entry1.getKey())){
						cache_map.get(entry1.getKey()).put(entry2.getKey(),getSimilarity(map1,map2));
					}else{
						HashMap<Integer,Double> temp = new HashMap<>();

						temp.put(entry2.getKey(),getSimilarity(map1,map2));
						cache_map.put(entry1.getKey(),temp);
					}
						System.out.println(entry1.getKey()+" "+entry2.getKey()+" "+check_cache(entry1.getKey(),entry2.getKey()));

					total++;

				}
			}
		}
		System.out.println("done cache map for "+total+" times");

	}

	/**
	 * This is the caculation of consine similarity by two vectors. The value of the vector
	 * will be normalised before calculation.
	 * @param map1
	 * @param map2
     * @return
     */
	public double getSimilarity(HashMap<Integer, Integer> map1,HashMap<Integer, Integer> map2){
		double up=0.0;
		double left=0.0;
		double right=0.0;
		double temp1=0.0;
		double temp2=0.0;
		Set<Integer> set= new HashSet<>();
		set.addAll(map1.keySet());
		set.addAll(map2.keySet());
		for(Integer key:set){
			/*Normalization here*/
			if(map2.get(key)==null){
				temp2=0.0;
			}else{
				/* normalization here*/
				temp2= (map2.get(key)+0.0)/(map2.size()+0.0);
			}
			if(map1.get(key)==null){
				temp1=0.0;
			}else{
				/* normalization here*/
				temp1= (map1.get(key)+0.0)/(map1.size()+0.0);
			}
			up+=temp1*temp2;
			left+=Math.pow(temp1+0.0,2);
			right+=Math.pow(temp2+0.0,2);

		}
		System.out.println("up "+up);
		double result = up/((Math.sqrt(left)*Math.sqrt(right)));
		System.out.println("caculate cosine->"+ result);
		return result;


	}


	/**
	 * This method will firstly calculate a one to one mapping from Di to Dq in order to reach
	 * the max value of f2, then use these values to calculate f1-f2 to find the argument max
	 * of f1-f2 and add the element to the Dq. This will be kept iterated until there is
	 * nothing is Di. Then return the re-ordered result list.
	 * @param lambda pre-set value
	 * @param topic_no
	 * @param set The set contains 100 top files for a particular topic
     * @return
     */
	public ArrayList<Result> RankList(double lambda, String topic_no, ResultSet set){
		LinkedHashMap<Integer, Result> res_i = new LinkedHashMap<>();
		HashMap<Integer,Double> consines = new HashMap<>();
		//ArrayList<Result> res_i = new ArrayList<>();
		ArrayList<Result> res_q = new ArrayList<>();
		Result temp_add = null;
		int doc_ids [] = set.getDocids();
		if(doc_ids.length<100){
			return null;
		}
		double doc_scores [] = set.getScores();
		double max_doc_score = doc_scores[0];
		final String metaIndexDocumentKey = ApplicationSetup.getProperty(
				"trec.querying.outputformat.docno.meta.key", "filename");
		String doc_names [] = Utils.getDocnos(metaIndexDocumentKey, set, index);
		for(int i=0;i<doc_ids.length;i++){
			Result result = new Result(topic_no,doc_names[i],doc_scores[i]/max_doc_score,doc_ids[i]);
			res_i.put(doc_ids[i],result);
		}

		if(res_q.size()==0){
			Result temp=res_i.remove(doc_ids[0]);
			temp.setDocument_rank(res_q.size());
			res_q.add(temp);
		}
		int round=0;
			while(res_i.size()>0){
				for(Map.Entry<Integer,Result> entry1: res_i.entrySet()){
					double max=-1.0;
					Double recent_value= -1.0;
					for(Result r:res_q){
						if((recent_value=check_cache(entry1.getKey(),r.getDoc_id()))!=null){
							if(recent_value>max){
								max = recent_value;
							}
						}
					}
					consines.put(entry1.getKey(),max);
				}
				System.out.println("the q size is "+ res_q.size());
				double max_score = -1.0;
				int max_id = -1;
				double score;
				for(Map.Entry<Integer,Result> entry1: res_i.entrySet()){
					score = lambda*(entry1.getValue().getScore())- (1-lambda)*consines.get(entry1.getKey());

						if(score>max_score){
							max_id = entry1.getKey();
							max_score = score;
						}
				}
				temp_add = res_i.get(max_id);
				temp_add.setDocument_rank(res_q.size());
				temp_add.setScore(max_score);
				res_q.add(temp_add);
				System.out.println("add "+max_id+" to q");
				res_i.remove(max_id);
				consines.clear();
			}
		return res_q;
	}

	/**
	 * Use TF-IDF or BM25 to find the top 100 documents, storing in a Resultset.
	 * @param id
	 * @param query query of a topic
	 * @param k how many documents to return
     * @return
     */
	public ResultSet buildResultSet(int id,String query,int k)
	{
		Manager manager = new Manager(this.index);
		SearchRequest srq = manager.newSearchRequest(id+"", query);
		srq.addMatchingModel("Matching", "TF_IDF");
		manager.runPreProcessing(srq);
		manager.runMatching(srq);
		manager.runPostProcessing(srq);
		manager.runPostFilters(srq);
		ResultSet set = srq.getResultSet().getResultSet(0,k);
		return set;

	}

	public void closeIndex() {
		try {
			index.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
