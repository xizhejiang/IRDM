package diversity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import eval.Result;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.SearchRequest;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.utility.ApplicationSetup;
import utils.Utils;

public class PortfolioScoring {


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

	HashMap<Integer,HashMap<Integer,Double>> cache_map;
	HashMap<Integer,HashMap<Integer,Integer>> tf_cache;
	ArrayList<String> file_vector ;
	HashMap<Integer,Integer> rank_cache;




	/* Initialize MMR model with index. Use
	 * @param index_path : initialize index
	 * @param prefix : language prefix for index (default = 'en')
	 * with location of index created using bash script.
	 */
	public PortfolioScoring(String index_path, String prefix) {
		index = Index.createIndex(index_path,prefix);
		cache_map = new HashMap<>();
		tf_cache=new HashMap<>();
		file_vector = new ArrayList<>();

	}

	/**
	 * Check if the required pearson correlation is in the cache. If it is, return the correlation.
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
	 * This is the pre-caculation. The calculation will previously calculate
	 * n*(n-1)/2 peason correlation of the combinations of the set of 100 files. The is to decrease the repeat calculation
	 * and decrease the time complexity of caculation.
	 * @param set The set contains 100 top files for a particular topic
	 */
	public void caculate(ResultSet set){

		int total =0;
		HashMap<Integer, Integer> map1=null;
		HashMap<Integer, Integer> map2=null;
		int doc_ids [] = set.getDocids();
		double doc_scores [] = set.getScores();
		HashMap <Integer, Double> scores = new HashMap<>();
		for (int i = 0 ; i < doc_scores.length;i++){
			scores.put(doc_ids[i], doc_scores[i]);
		}
		for(Map.Entry<Integer,Double> entry1:scores.entrySet()){
			for(Map.Entry<Integer,Double> entry2:scores.entrySet()){
				/**
				 * If the required combination is in the cache, no more calculation is needed
				 * for this combination.
				 */
				if(entry1.getKey().equals(entry2.getKey())||check_cache(entry1.getKey(),entry2.getKey())!=null){
					continue;
				}else{
					/**
					 * check if the term frequency is in the cache, if it is, directly get it
					 * from the cache, if not, get it by calling Utils.docTF and put it in the
					 * cache
					 */
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
						cache_map.get(entry1.getKey()).put(entry2.getKey(),pearson(map1,map2));
					}else{
						HashMap<Integer,Double> temp = new HashMap<>();
						temp.put(entry2.getKey(),pearson(map1,map2));
						cache_map.put(entry1.getKey(),temp);
					}
					System.out.println(entry1.getKey()+" "+entry2.getKey()+" "+check_cache(entry1.getKey(),entry2.getKey()));
					total++;

				}
			}
		}
	}
	/**
	 * This is the caculation of pearson correlation by two vectors. The value of the vector
	 * will be normalised before calculation.
	 * @param map1 TF vector
	 * @param map2 TF vector
	 * @return
	 */
	public double pearson(HashMap<Integer, Integer> map1,HashMap<Integer, Integer> map2){
		double xy_total=0.0;
		double x_total=0.0;
		double y_total=0.0;
		double x_sqr_total=0.0;
		double y_sqr_total=0.0;
		double n= map1.size();
		double temp2 = 0.0;
		double temp1 = 0.0;
		/**
		 * find common term and put them in the hashset
		 */
		Set<Integer> set= new HashSet<>();
		set.addAll(map1.keySet());
		set.addAll(map2.keySet());
		for(Integer key:set) {
			/*Normalization here*/
			if (map2.get(key) == null) {
				temp2 = 0.0;
			} else {
				/* normalization here*/
				temp2 = (map2.get(key) + 0.0) / (map2.size() + 0.0);
			}
			if (map1.get(key) == null) {
				temp1 = 0.0;
			} else {
				/* normalization here*/
				temp1 = (map1.get(key) + 0.0) / (map1.size() + 0.0);
			}

			x_total+=temp1;
			y_total+=temp2;
			xy_total+=temp1*temp2;
			x_sqr_total+=Math.pow(temp1,2);
			y_sqr_total+=Math.pow(temp2,2);
		}




		double result = (n*xy_total-x_total*y_total)/(Math.sqrt(n*x_sqr_total-Math.pow(x_total,2))
				*Math.sqrt(n*y_sqr_total-Math.pow(y_total,2)));
		if(Double.isNaN(result)){
			for(Map.Entry<Integer, Integer> entry:map1.entrySet()){
				System.out.println("map1->"+entry.getValue());
			}
			for(Map.Entry<Integer, Integer> entry:map2.entrySet()){
				System.out.println("map2->"+entry.getValue());
			}
		}
		return result;
	}

	/**
	 * The method is going to calculate the max MVA with the document ID and put document from Di into Dj
	 * The function will be kept iterated until there is no document in Di
	 * @param topic_no topic number
	 * @param set The set contains 100 top files for a particular topic
	 * @param b parameter to be set
     * @return
     */

	public ArrayList<Result> RankList(String topic_no, ResultSet set, int b){
		LinkedHashMap<Integer, Result> res_i = new LinkedHashMap<>();
		HashMap<Integer,Double> right_sum = new HashMap<>();
		//ArrayList<Result> res_i = new ArrayList<>();
		ArrayList<Result> res_q = new ArrayList<>();
		int doc_ids [] = set.getDocids();
		double doc_scores [] = set.getScores();
		Result temp_add = null;
		final String metaIndexDocumentKey = ApplicationSetup.getProperty(
				"trec.querying.outputformat.docno.meta.key", "filename");
		String doc_names [] = Utils.getDocnos(metaIndexDocumentKey, set, index);
		for(int i=0;i<doc_ids.length;i++){
			Result result = new Result(topic_no,doc_names[i],doc_scores[i],doc_ids[i],i);
			res_i.put(doc_ids[i],result);
		}

		if(res_q.size()==0){
			Result temp=res_i.remove(doc_ids[0]);
			temp.setDocument_rank(res_q.size());
			res_q.add(temp);
		}
		double right_total = 0.0;

		while(res_i.size()>0){
			for(Map.Entry<Integer,Result> entry1: res_i.entrySet()){
				Double recent_value= -1.0;
				right_total=0.0;
				for(Result r:res_q){
					if((recent_value=check_cache(entry1.getKey(),r.getDoc_id()))!=null){
						right_total+=Math.pow(1/2,r.getDocument_rank())*recent_value;
					}
				}
				right_sum.put(entry1.getKey(),right_total);
			}
			System.out.println("the q size is "+ res_q.size());
			double max_score = -1.0;
			int max_id = -1;
			double score;
			for(Map.Entry<Integer,Result> entry1: res_i.entrySet()){
				score = entry1.getValue().getScore()-b*(Math.pow(1/2,entry1.getValue().getDocument_rank()))
						-2*b*right_sum.get(entry1.getKey());
				if(max_score<0.0){
					max_score=entry1.getValue().getScore();
					max_id = entry1.getKey();
				}else{
					if(score>max_score){
						max_id = entry1.getKey();
						max_score = entry1.getValue().getScore();
					}
				}
			}
			System.out.println("-----------------------res_size->->"+res_i.size()+"<-<------------------------");
			temp_add=res_i.get(max_id);
			temp_add.setDocument_rank(res_q.size());
			temp_add.setScore(max_score);
			System.out.println("max_sore->"+max_score);
			res_q.add(temp_add);
			System.out.println("add "+max_id+" to q");
			res_i.remove(max_id);
			right_sum.clear();
		}

		return res_q;
	}

	//public double scoreDocument(int document_id, String query, double lambda)
	{
		//return 0;
	}
	/**
	 * Use TF-IDF or BM25 to find the top 100 documents, storing in a Resultset.
	 * @param id
	 * @param query a certain topic
	 * @param k how many documents will return
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
