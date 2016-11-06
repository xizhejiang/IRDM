package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.sleepycat.je.tree.IN;
import org.apache.hadoop.util.hash.Hash;
import org.terrier.matching.ResultSet;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.DocumentIndexEntry;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.MetaIndex;
import org.terrier.structures.bit.DirectIndex;
import org.terrier.structures.postings.IterablePosting;

import eval.*;

public class Utils {

	public enum QREL {
		ADHOC, DIVERSITY
	}
	/* 
	 * Computes the hashmap with IDF of <term_id, term_idf>. 
	 * @param total_index_documents : It is total number of documents in the index and final 
	 * @param index_lexicon : It is the lexicon of the index. 
	 */
	public static HashMap<Integer, Float>  populateTermIDFMap(long total_index_documents, 
			Lexicon<String> index_lexicon) {
		HashMap <Integer, Float>term_idf = new HashMap<Integer, Float>();

		float idf = 0;
		long df = 0;

		int unique_terms = index_lexicon.numberOfEntries();

		for(int i=0; i<unique_terms; i++) {
			Map.Entry<String,LexiconEntry> term_lexicon_entry = index_lexicon.getLexiconEntry(i);

			if (term_lexicon_entry.getValue() != null)
				df = term_lexicon_entry.getValue().getDocumentFrequency();
			if(df!=0)
				idf = (float) Math.log10(((float)total_index_documents / (float) df));

			term_idf.put(i, idf);

		}
		return term_idf;
	}

	/* Return term idf. Calculates based on the formula
	 * log(N/df) where N = total documents in the collection.
	 * 
	 */
	public float getTermIDF(String term, long total_index_documents, Lexicon<String> index_lexicon) {
		float idf = 0;
		long df = 0;

		LexiconEntry le = index_lexicon.getLexiconEntry(term);
		if (le != null)
			df = le.getDocumentFrequency();
		if(df!=0)
			idf = (float) Math.log10(((float)total_index_documents / (float) df));

		return idf;
	}

	/*
	 * Returns the number of times a token appears in index. For example, term t1 has 
	 * following frequency in 3 documents: d1=3, d2=4, d3=5
	 * then total term frequency of t1 in index is 3+4+5 = 12.
	 */
	public static int getTermTFInIndex(String term, Lexicon<String> index_lexicon) {
		LexiconEntry term_le = index_lexicon.getLexiconEntry(term);
		if (term_le != null)
			return term_le.getFrequency();
		else
			return 0;
	}

	/*
	 * Returns the frequency of a token normalized by total number of tokens 
	 * in index. For example, an index has following term distribution:
	 * t1=12, t2=20, t3=50. 
	 * then normalized term frequency of t1 in index is 12/(12+20+50).
	 */
	public static float getNormTermTFInIndex(String term, double total_index_terms,
			Lexicon<String> index_lexicon) {
		LexiconEntry term_le = index_lexicon.getLexiconEntry(term);
		if (term_le != null)
			return ( (float) term_le.getFrequency() /(float) total_index_terms);
		else
			return 0;
	}


	public static ArrayList <Qrel> loadQrels(HashMap<String,HashMap<Integer,Integer>> map, String file_name, QREL qrel_type) {

		ArrayList <Qrel> qrels = new ArrayList<Qrel>();
		try {
			BufferedReader ifile = new BufferedReader(new FileReader(new File (file_name)));
			String [] split;
			String line;
			Qrel temp = null;
			while((line=ifile.readLine())!=null)
			{
				split = line.split(" ");

				int relevent = Integer.parseInt(split[3]);
				int topic = Integer.parseInt(split[0]);
//				if(relevent<0){
//					relevent=0;
//				}

					// Qrel structure: TOPIC ITERATION DOCUMENT# RELEVANCE
					temp = new Qrel (split[0], split[2], relevent+"");
					HashMap<Integer,Integer> innerMap= new HashMap<>();
					if(map.containsKey(split[2])){
						map.get(split[2]).put(Integer.parseInt(split[0]),relevent);
					}else{
						innerMap.put(Integer.parseInt(split[0]),relevent);
						map.put(split[2],innerMap);
					}



				if (temp!=null)
					qrels.add(temp);

			}
			ifile.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return qrels;

	}

	public static ArrayList <Qrel> loadDiversityQrels(HashMap<String,HashMap<Integer,HashMap<Integer,Integer>>> cache_map, HashMap<Integer,HashSet<Integer>> map_sets, String file_name, QREL qrel_type) {

		ArrayList <Qrel> qrels = new ArrayList<Qrel>();
		try {
			BufferedReader ifile = new BufferedReader(new FileReader(new File (file_name)));
			String [] split;
			String line;
			Qrel temp = null;
			while((line=ifile.readLine())!=null)
			{
				split = line.split("\\s+");

				int relevent = Integer.parseInt(split[3]);
				int topic = Integer.parseInt(split[0]);
//				if(relevent<0){
//					relevent=0;
//				}
				int split0 =Integer.parseInt(split[0]);
				int split1 = Integer.parseInt(split[1]);
					// Qrel structure: TOPIC INTENT DOCUMENT# RELEVANCE
					temp = new Qrel (split[0], split[1], split[2], relevent+"");
					if(map_sets.containsKey(split0)){
						map_sets.get(split0).add(split1);
					}else{
						HashSet<Integer> set = new HashSet<>();
						set.add(split1);
						map_sets.put(split0,set);
					}

					if(cache_map.containsKey(split[2])){
						if(cache_map.get(split[2]).containsKey(split0)){
								cache_map.get(split[2]).get(split0).put(split1,relevent);
						}else{
							HashMap<Integer,Integer> innerinnermap = new HashMap<>();
							innerinnermap.put(split1,relevent);
							cache_map.get(split[2]).put(split0,innerinnermap);
						}
					}else{
						HashMap<Integer,Integer> innerinnermap = new HashMap<>();
						HashMap<Integer,HashMap<Integer,Integer>> innermap = new HashMap<>();
						innerinnermap.put(split1,relevent);
						innermap.put(split0,innerinnermap);
						cache_map.put(split[2],innermap);


					}
					HashMap<Integer,Integer> innerinnerMap= new HashMap<>();
					innerinnerMap.put(Integer.parseInt(split[0]),relevent);
					//map.put(split[2],innerMap);

				if (temp!=null)
					qrels.add(temp);

			}
			ifile.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return qrels;

	}

	public static ArrayList <Result> loadResults(String file_name) {

		ArrayList <Result> res = new ArrayList<Result>();
		try {
			BufferedReader ifile = new BufferedReader(new FileReader(new File (file_name)));
			String [] split;
			String line;
			Result result;

			while((line=ifile.readLine())!=null)
			{
				split = line.split("\\s+");
				result = new Result(split[0],split[2].substring(split[2].lastIndexOf("/")+1,split[2].lastIndexOf(".")),split[3],split[4]);
				//result = new Result(split[0],split[2],split[3],split[4]);

				if(result!=null){
					res.add(result);
				}
			}
			ifile.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return res;

	}

	/** Generates a map of terms in the document and its within document term frequencies.
	 * 
	 * @param docid
	 * 
	 */
	public static HashMap <Integer, Integer> docTF(int docid, Index index ) 
	{
		
		HashMap <Integer, Integer>  map = new HashMap <Integer, Integer> ();
		Lexicon<String> lex = index.getLexicon();
		try{
			IterablePosting postings = index.getDirectIndex().getPostings(
					(BitIndexPointer)index.getDocumentIndex().getDocumentEntry(docid));
			while (postings.next() != IterablePosting.EOL) {
				Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(postings.getId());
				map.put(lee.getValue().getTermId(), postings.getFrequency());
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		return map;
	}
	
	/** method which extracts the docnos for the prescribed resultset */
	public static String[] getDocnos(final String metaIndexDocumentKey, final ResultSet set, final Index index) 	
	{
		String[] docnos= null;
		
		if (set.hasMetaItems(metaIndexDocumentKey)) {
			docnos = set.getMetaItems(metaIndexDocumentKey);
		} else {
			final MetaIndex metaIndex = index.getMetaIndex();
			try {
				docnos = metaIndex.getItems(metaIndexDocumentKey, set.getDocids());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return docnos;
	}

}
