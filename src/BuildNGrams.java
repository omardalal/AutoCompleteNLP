import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

//Class used to build the bigram and trigram for the first use
public class BuildNGrams {
	
	//Number of words in the corpus
	private static int wordCount = 0;
	public static void createNGram() {
		System.out.println("Processing...");
		String corpus = readFiles();
		buildCountTable(corpus);
		System.out.println("Trigram and Bigram Generated Successfully!");
	}
	
	//Read all 14 input files to create a corpus
	public static String readFiles() {
		try {
			FileInputStream[] files = new FileInputStream[14];
			for (int i=1; i<=14; i++) {
				files[i-1] = new FileInputStream("./Data/"+i+".txt");
			}
			String corpus = "";
			for (FileInputStream fis: files) {
				InputStreamReader reader = new InputStreamReader(fis, "UTF-8");
				int c = 0;
				while ((c=reader.read())!=-1) {
					if ((char)c==' ') {
						wordCount++;
					}
					corpus+=(char)c;
				}
				corpus = corpus.replaceAll("[^ء-ي :;،,.\\n]", "");
				reader.close();
				fis.close();
			}
			return corpus;
		} catch (FileNotFoundException ex) {
			System.out.println("File not Found!");
		} catch (IOException ex) {
			System.out.println("Input/Output Error!");
		}
		return "";
	}
	
	//Look for a word in an ArrayList
	public static Word getWord(ArrayList<Word> list, String word) {
		for (int i=0; i<list.size(); i++) {
			Word w = list.get(i);
			if (w.getWord().equals(word)) {
				return w;
			}
		}
		return null;
	}
	
	//Build bigram and trigram count tables
	public static void buildCountTable(String corpus) {
		String[] sentencesArr = corpus.split("[.,،:;\\n]");
		
		//Find all unique words for bigram
		ArrayList<Word> uniqueWords = new ArrayList<>();
		for (int i=0; i<sentencesArr.length; i++) {
			String[] words = ("<s> <s> "+sentencesArr[i].trim()+" </s>").split(" ");
			for (String word: words) {
				if (word.trim().length()<=1) {
					continue;
				}
				Word w = getWord(uniqueWords, word.trim());
				if (word.trim().length()>1&&w==null) {
					uniqueWords.add(new Word(word.trim(), 1));
				} else if (w!=null) {
					w.setCount(w.getCount()+1);
				}
				wordCount++;
			}
		}
		//Sort words by their probabilities
		Collections.sort(uniqueWords);
		buildProbabilityTable(uniqueWords, "bigram", sentencesArr);
		
		//Find all possible tokens for trigram
		ArrayList<Word> uniqueTokens = new ArrayList<>();
		for (int i=0; i<sentencesArr.length; i++) {
			String[] words = ("<s> <s> "+sentencesArr[i].trim()+" </s>").split(" ");
			for (int j=0; j<words.length; j++) {
				String word = words[j].trim();
				if (word.trim().length()<=1) {
					continue;
				}
				if (j+1<words.length) {
					if (words[j+1].trim().length()>1) {
						word+=" "+words[j+1];
					} else {
						if (j+2<words[j].length()&&words[j+2].trim().length()>1) {
							word+=" "+words[j+2];
						} else {
							continue;
						}
					}
				} else {
					continue;
				}
				Word w = getWord(uniqueTokens, word.trim());
				if (w==null) {
					uniqueTokens.add(new Word(word.trim(), 1));
				} else if (w!=null) {
					w.setCount(w.getCount()+1);
				}
			}
		}
		//Sort token according to their probabilities
		Collections.sort(uniqueTokens);
		buildProbabilityTable(uniqueTokens, "trigram", sentencesArr);
	}
	
	//Build probability tables for bigrams and trigrams
	//Bigrams and trigram are split into 9 files each to speed up the process of looking for a suggestion every time
	public static void buildProbabilityTable(ArrayList<Word> list, String fileName, String[] sentencesArr) {
		try {
			FileOutputStream[] outFile = new FileOutputStream[9];
			OutputStreamWriter[] writer = new OutputStreamWriter[9];
			for (int i=0; i<9; i++) {
				outFile[i] = new FileOutputStream("./Data/"+fileName+i+".txt");
				writer[i] = new OutputStreamWriter(outFile[i], "UTF-8");
			}
			for (int i=0; i<list.size(); i++) {
				Word w = list.get(i);
				String token = w.getWord();
				ArrayList<String> possibleWords = new ArrayList<>();
				//Look for all occurrences of the given word
				for (int j=0; j<sentencesArr.length; j++) {
					String prefix = fileName.equals("trigram")?"<s> <s> ":"<s> ";
					String sentence = prefix+sentencesArr[j].trim()+" </s>";
					while (sentence.contains(token)) {
						int index = sentence.indexOf(token)+token.length();
						int k = index+1;
						String nextWord = "";
						while (k<sentence.length()&&(Character.isLetter(sentence.charAt(k))||sentence.charAt(k)=='<'||sentence.charAt(k)=='>'||sentence.charAt(k)=='/')) {
							nextWord+=sentence.charAt(k++);
						}
						if (!nextWord.isEmpty()) {
							possibleWords.add(nextWord);
						}
						sentence = sentence.substring(index);
					}
				}
				int fileNum = getFileNum(token.charAt(0));
				//Calculate the probability of each word using its count
				double probability = ((double)w.getCount()*100/wordCount);
				writer[fileNum].write(token+probability+"_"+listToString(possibleWords)+"\n");
			}
			for (int i=0; i<writer.length; i++) {
				writer[i].close();
			}
		} catch (FileNotFoundException ex) {
			System.out.println("File not Found!");
		} catch (IOException ex) {
			System.out.println("Input/Output Error!");
		}
	}
	
	//Convert list of words into a string that will be printed to the bigram/trigram files
	public static String listToString(ArrayList<String> list) {
		//Calculate the count of each word
		HashMap<String, Integer> wCount = new HashMap<>();
		for (String str: list) {
			Integer count = wCount.get(str);
			if (count!=null) {
				wCount.put(str, wCount.get(str)+1);
			} else {
				wCount.put(str, 1);
			}
		}
		ArrayList<Word> countList = new ArrayList<>(); 
		wCount.forEach((k, v) -> {
			countList.add(new Word(k, v));
		});
		//Sort words according to probability
		Collections.sort(countList);
		String output = "";
		for (Word str: countList) {
			output+=str.stringVal(list.size());
		}
		if (output.length()>0) {
			output = output.substring(0, output.length()-1);
		}
		return output;
	}
	
	//Returns the number of the bigram/trigram file the token is going to be stored in based on the first letter
	//Bigrams and trigrams are split into multiple files to speed up suggestions
	public static int getFileNum(char ch) {
		int c = (int)ch;
		if (c>=1569&&c<1575) {
			return 0;
		} else if (c>=1575&&c<1580) {
			return 1;
		} else if (c>=1580&&c<1585) {
			return 2;
		} else if (c>=1585&&c<1590) {
			return 3;
		} else if (c>=1590&&c<1595) {
			return 4;
		} else if (c>=1595&&c<1600) {
			return 5;
		} else if (c>=1600&&c<1605) {
			return 6;
		} else if (c>=1600&&c<=1610) {
			return 7;
		}
		return 8;
	}
}
