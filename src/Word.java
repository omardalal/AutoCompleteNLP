//Word class that is used to store words and their counts
public class Word implements Comparable<Word> {

	//Attributes
	private String word;
	private int count;
	
	//Constructor
	public Word(String word, int count) {
		this.word = word;
		this.count = count;
	}

	//Setters and Getters
	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	public String stringVal(int total) {
		return word+"("+((double)(count*100)/total)+")_";
	}
	
	//Used to sort lists containing Word elements (Descending)
	@Override
	public int compareTo(Word w) {
		if (this.count>w.count) {
			return -1;
		} else if (this.count<w.count) {
			return 1;
		}
		return 0;
	}
}
