package eval;

public class Result {
	String document_id;
	int document_rank;
	double score;



	int doc_id;




	int topic_no;

	public Result(String topic_no,String document_id, double score,int doc_id,int document_rank) {
		this.topic_no = Integer.parseInt(topic_no);
		this.document_id = document_id;
		this.score = score;
		this.doc_id = doc_id;
		this.document_rank= document_rank;
	}

	public Result(String topic_no,String document_id, double score,int doc_id) {
		this.topic_no = Integer.parseInt(topic_no);
		this.document_id = document_id;
		this.score = score;
		this.doc_id = doc_id;
	}
	public Result(String topic_no,String document_id, String document_rank, String score) {
		this.topic_no = Integer.parseInt(topic_no);
		this.document_id = document_id;
		this.document_rank = Integer.parseInt(document_rank);
		this.score = Double.parseDouble(score);
	}

	public int getDocument_rank() {
		return document_rank;
	}

	public void setDocument_rank(int document_rank) {
		this.document_rank = document_rank;
	}

	public String getDocument_id() {
		return document_id;
	}

	public void setDocument_id(String document_id) {
		this.document_id = document_id;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public int getTopic_no() {
		return topic_no;
	}
	public int getDoc_id() {
		return doc_id;
	}

	public void setDoc_id(int doc_id) {
		this.doc_id = doc_id;
	}

	public void setTopic_no(int topic_no) {
		this.topic_no = topic_no;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Result result = (Result) o;

		if (doc_id != result.doc_id) return false;
		if (topic_no != result.topic_no) return false;
		return document_id.equals(result.document_id);

	}


// Add the constructors.

	  // Create getDocumentId and seDocumentId functions.
	  // Create getDocumentScore and seDocumentScore functions.
	  // Create getDocumentRank and seDocumentRank functions.
	}
