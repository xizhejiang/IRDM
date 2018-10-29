package eval;

import org.terrier.matching.models.basicmodel.In;
import utils.Utils;

import java.io.*;
import java.util.*;


public class NDCG {

	private NDCG() {}


	/**
	 * filter the whole result file. Keep the top 100 files for each topic
	 * @param r_list
	 * @param k
     * @return
     */
	public static LinkedHashMap<Integer, ArrayList<Result>> filter_res(ArrayList<Result> r_list,int k){
		LinkedHashMap<Integer,ArrayList<Result>> map = new LinkedHashMap<Integer, ArrayList<Result>>();
		Iterator<Result> iterator = r_list.iterator();
		Result temp;
		while(iterator.hasNext()){
			temp= iterator.next();
			if(map.get(temp.getTopic_no())!=null){
					if(map.get(temp.getTopic_no()).size()<k){
						map.get(temp.getTopic_no()).add(temp);
					}
			}else{
				ArrayList<Result> topic_arrayList= new ArrayList<Result>();
				topic_arrayList.add(temp);
				map.put(temp.getTopic_no(),topic_arrayList);
			}
		}
		return map;
	}


	/**
	 * A hashmap contains another hashmap will be used to cache the entries in the result.
	 * The hashmap was used to reduce the time complexity of calculations, O(1).
	 * @param cache_map
	 * @param topic
	 * @param document_id
     * @return
     */
	public static boolean check_id_match_topic(HashMap<String,HashMap<Integer,Integer>>cache_map,int topic,String document_id){
			if(cache_map.get(document_id)!=null&&cache_map.get(document_id).get(topic)!=null){return true;}
			return false;
		}

	/**
	 * The DCG of the 100 files in order will be calculated, and the 100 files will be then
	 * re-ordered to calculate idcg.
	 * @param all_cache
	 * @param retrieved_list
	 * @param qrel_list
	 * @param k
     * @return
     */
	public static double compute (HashMap<String,HashMap<Integer,Integer>> all_cache,
								  ArrayList <Result> retrieved_list, ArrayList <Qrel> qrel_list , int k ) {

		double dcg = 0;
		double idcg = 0;
		double total_NDCG=0;
		int relevant=0;
		int index =0;
		/**
		 * store top 100 files' records for each topic
		 */
		LinkedHashMap<Integer,ArrayList<Result>> results_map = filter_res(retrieved_list,k);
		int total_size = results_map.size();
		for(Map.Entry<Integer,ArrayList<Result>> entry: results_map.entrySet()){
			dcg=0;
			idcg=0;
			ArrayList<Result> result_idcg=entry.getValue();
			for(Result result_entry: entry.getValue()){
				if(check_id_match_topic(all_cache,result_entry.getTopic_no(),result_entry.getDocument_id())){
					relevant=all_cache.get(result_entry.getDocument_id()).get(result_entry.getTopic_no());
				}else{
					relevant=0;
				}
				if(result_entry.getDocument_rank()==0){
					dcg=relevant;
				}else{
					dcg+=relevant* (Math.log(2) / Math.log(result_entry.getDocument_rank()+1));
				}
			}
			/**
			 * sort the list by relevance.
			 */
			Collections.sort(result_idcg, new Comparator<Result>() {
				@Override
				public int compare(Result o1, Result o2) {
					int rel1,rel2;
					if(check_id_match_topic(all_cache,o1.getTopic_no(),o1.getDocument_id())){
						rel1=all_cache.get(o1.getDocument_id()).get(o1.getTopic_no());
					}else{
						rel1=0;
					}

					if(check_id_match_topic(all_cache,o2.getTopic_no(),o2.getDocument_id())){
						rel2=all_cache.get(o2.getDocument_id()).get(o2.getTopic_no());
					}else{
						rel2=0;
					}

					return rel2-rel1;
				}
			});
			index =0;
			for(Result result_entry: result_idcg){
				if(check_id_match_topic(all_cache,result_entry.getTopic_no(),result_entry.getDocument_id())){
					relevant=all_cache.get(result_entry.getDocument_id()).get(result_entry.getTopic_no());
				}else{
					relevant=0;
				}
				if(index==0){
					idcg=relevant;
				}else{
					idcg+=relevant* (Math.log(2) / Math.log(index+1));
				}
				index++;
				//result_count++;
			}
			if(idcg!=0){
				total_NDCG+=dcg/idcg;
			}
	}

		return total_NDCG/total_size;
		
	}


}
