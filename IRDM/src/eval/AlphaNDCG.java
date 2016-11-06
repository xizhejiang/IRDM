package eval;

import java.util.*;

public class AlphaNDCG {
    private AlphaNDCG() {
    }

    /**
     * filter the whole result file. Keep the top 100 files for each topic
     * A linkedlist hashmap was used to improve performance of seaching, O(1)
     * @param r_list
     * @param k
     * @return
     */
    public static LinkedHashMap<Integer, ArrayList<Result>> filter_res(ArrayList<Result> r_list, int k) {
        LinkedHashMap<Integer, ArrayList<Result>> map = new LinkedHashMap<Integer, ArrayList<Result>>();
        Iterator<Result> iterator = r_list.iterator();
        Result temp;
        while (iterator.hasNext()) {
            temp = iterator.next();
            if (map.get(temp.getTopic_no()) != null) {
                if (map.get(temp.getTopic_no()).size() < k) {
                    map.get(temp.getTopic_no()).add(temp);
                }
            } else {
                ArrayList<Result> topic_arrayList = new ArrayList<Result>();
                topic_arrayList.add(temp);
                map.put(temp.getTopic_no(), topic_arrayList);
            }
        }
        return map;
    }

    /**
     * the total relevance of the previous documents occured of the same subtopic will be calculated,
     * and then the result will be used as an exponet. The 1-alpha to the power of this exponet will be returned.
     * @param arfa pre-set value
     * @param result_list k top files for a certain topic
     * @param cache_map the cache of the relevance value identified by the document id, topic no, and sub topic
     * @param result Result Object contains the information of a file entry
     * @param subtopic the subtopic number
     * @return
     */
    public static double cal_alfa_exp_sigma(double arfa, ArrayList<Result> result_list, HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> cache_map, Result result, int subtopic) {
        double total = 0.0;
        for (Result temp : result_list) {
            if (temp.equals(result)) {
                break;
            } else {
                total += get_rel(cache_map, temp.getDocument_id(), subtopic, temp.getTopic_no());
            }
        }
        return Math.pow(1 - arfa, total);
    }

    /**
     * All the documents occurs in different subtopics in one single topic will iterated.
     * This is very similar to calculate the DCG for each document, however the formula is
     * more complicated. It is the sum of R(d,sub)*cal_alfa_exp_sigma(d,sub)
     * @param arfa
     * @param result_list the return list
     * @param cache_map the cache of document relevances identified by document id
     *                  topic no, and subtopic no
     * @param result a single Result Object contained the file information
     * @param sets  all the sub topics in one topic
     * @return
     */

    public static double cal_dcg_per_doc(double arfa, ArrayList<Result> result_list, HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> cache_map,
                                         Result result, HashSet<Integer> sets) {
        double total = 0.0;
        int subtopic = 0;
        double right;
        double left;
        Iterator<Integer> iterator = sets.iterator();
        while (iterator.hasNext()) {
            subtopic = iterator.next();
            right = cal_alfa_exp_sigma(arfa, result_list, cache_map, result, subtopic);
            left = get_rel(cache_map, result.getDocument_id(), subtopic, result.getTopic_no());
            total += left * right;
        }
        return total;

    }

    /**
     * The method will return the relevance of a document identified by the document ID, subtopic and
     * topic.
     * Three levels of hashMap<hashmap<hashmap>> will be used to act as a cache to reduce the
     * time complexity of calculation. Because the same docuemnt could occured in different topics
     * or even different subtopic. That is the reason to use three levels of hashmaps to be the cache.
     * @param cache_map the cache of the relevance value identified by the document id, topic no, and sub topic
     * @param document_id the id of the document
     * @param subtopic subtopic number
     * @param topic topic number
     * @return
     */
    public static Integer get_rel(HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> cache_map, String document_id
            , int subtopic, int topic) {
        Integer result;
        if (cache_map.get(document_id) != null) {
            if (cache_map.get(document_id).get(topic) != null) {
                if ((result = cache_map.get(document_id).get(topic).get(subtopic)) != null) {
                    return result;
                }
            }
        }
        return 0;
    }

    /**
     * The mehthod works basically the same as to calculate DCG and idcg. There difference is to
     * use different formula to give the value to each entry and add them all together.
     * @param all_cache the cache of document relevances identified by document id
     *                  topic no, and subtopic no
     * @param map_sets all the subtopic for each topic
     * @param retrieved_list K top documents
     * @param qrel_list all entries in the qrel file
     * @param k
     * @param alfa
     * @return
     */
    public static double compute(HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> all_cache, HashMap<Integer, HashSet<Integer>> map_sets,
                                 ArrayList<Result> retrieved_list, ArrayList<Qrel> qrel_list, int k, double alfa) {

        double dcg = 0;
        double idcg = 0;
        double total_NDCG = 0;
        int relevent = 0;
        int index = 0;
        LinkedHashMap<Integer, ArrayList<Result>> results_map = filter_res(retrieved_list, k);
        int total_size = results_map.size();
        for (Map.Entry<Integer, ArrayList<Result>> entry : results_map.entrySet()) {
            dcg = 0;
            idcg = 0;
            double score = 0.0;

            index = 1;
            for (Result result_entry : entry.getValue()) {
                score = cal_dcg_per_doc(alfa, entry.getValue(), all_cache, result_entry, map_sets.get(result_entry.getTopic_no()))
                        / (Math.log(1 + index) / Math.log(2));
                result_entry.setScore(score);
                dcg += score;
                index++;
            }
            ArrayList<Result> result_idcg = entry.getValue();
            Collections.sort(result_idcg, new Comparator<Result>() {
                @Override
                public int compare(Result o1, Result o2) {
                    double score1, score2;
                    score1 = o1.getScore();
                    score2 = o2.getScore();
                    double result = score2 - score1;
                    if (result > 0.0) {
                        return 1;
                    } else if (result == 0.0) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            });
            index = 1;
            for (Result result_entry : result_idcg) {
                score = cal_dcg_per_doc(alfa, entry.getValue(), all_cache, result_entry, map_sets.get(result_entry.getTopic_no()))
                        / (Math.log(1 + index) / Math.log(2));
                idcg += score;
                index++;
            }
            if (idcg != 0) {
                total_NDCG += dcg / idcg;
            }
        }

        return total_NDCG / total_size;

    }
}
