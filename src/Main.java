import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

	public static void main(String[] args) {
		//The following function is called once to create the bigram and trigram files
//		BuildNGrams.createNGram(); //Uncomment this line to build the bigram and trigram for the first time
		launch();
	}
	
	//Box CSS Style
	private final String BOX_STYLE = "-fx-background-color:#fff; -fx-background-radius:10px;-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.16), 25px, 0, 0, 0);";
	
	//Start Method
	@Override
	public void start(Stage pS) throws Exception {
		VBox root = getRoot();
		root.setStyle("-fx-font-size:18px");
		Scene scene = new Scene(root, 800, 600);
		pS.setScene(scene);
		pS.setTitle("Auto Complete");
		pS.setResizable(false);
		pS.show();
	}
	
	//Initialize Root VBox
	private VBox getRoot() {
		VBox root = new VBox(25);
		root.setAlignment(Pos.CENTER);
		
		VBox typeBox = getTypeBox();
		
		HBox suggBox = getSuggBox();
		
		root.getChildren().addAll(typeBox, suggBox);
		return root;
	}
	
	//Initialize VBox that contains the text area
	private TextArea txtArea;
	private VBox getTypeBox() {
		VBox root = new VBox(25);
		root.setStyle(BOX_STYLE);
		root.setPadding(new Insets(15));
		root.setMaxWidth(700);
		root.setAlignment(Pos.CENTER);
		
		HBox typeHBox = new HBox(25);
		root.setAlignment(Pos.CENTER);
		
		Label titleLbl = new Label("Auto Complete Form");
		titleLbl.setStyle("-fx-font-weight:bold; -fx-font-size: 22px");
		
		txtArea = getTextArea();
		
		Label lbl = new Label("اكتب هنا");
		lbl.setStyle("-fx-font-weight:bold");
		
		typeHBox.getChildren().addAll(txtArea, lbl);
		
		root.getChildren().addAll(titleLbl, typeHBox);
		return root;
	}
	
	//Initialize text area
	private TextArea getTextArea() {
		TextArea textArea = new TextArea();
		textArea.setFocusTraversable(false);
		textArea.setMaxWidth(550);
		textArea.setPromptText("...ابدأ الكتابة");
		textArea.setWrapText(true);
		
		//Every time the value of the text in textArea changes
		textArea.textProperty().addListener((obs, oldVal, newVal) -> {
			ArrayList<String> wordsList = new ArrayList<>();
			ArrayList<String> nextWords = new ArrayList<>();
			//If last character was space -> predict next word
			if (newVal.trim().length()>0&&newVal.charAt(newVal.length()-1)==' ') {
				String[] val = newVal.split(" ");
				wordsList.add("<s>");
				wordsList.add("<s>");
				for (int i=0; i<val.length; i++) {
					wordsList.add(val[i]);
				}
				nextWords = findNext(wordsList.get(wordsList.size()-2)+" "+wordsList.get(wordsList.size()-1));
			//If the last character was not a space -> predict a completion for the current word
			} else if (newVal.trim().length()>0&&newVal.charAt(newVal.length()-1)!=' ') {
				int index = newVal.lastIndexOf(' ');
				index = index==-1?0:index;
				nextWords = completeWord(newVal.substring(index).trim());
			//If the new value is empty then predict most used words at the beggining of the sentence
			} else if (newVal.trim().length()==0) {
				nextWords = findNext("<s> <s>");
			}
			//Show suggestions in the suggestion pane (maximum suggestions = 5)
			try {
				if (nextWords.size()>0) {
					suggPane.getChildren().clear();
					for (int i=0; i<5; i++) {
						if (nextWords.get(i).length()<2) {
							continue;
						}
						suggPane.getChildren().add(getSuggBtn(nextWords.get(i)));
					}
				}
			} catch (IndexOutOfBoundsException ex) {}
		});
		
		return textArea;
	}
	
	//Initialize Suggestions Box
	private FlowPane suggPane;
	private HBox getSuggBox() {
		HBox root = new HBox(25);
		root.setStyle(BOX_STYLE);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(15));
		root.setMaxWidth(700);
		
		suggPane = new FlowPane(7.5, 7.5);
		suggPane.setAlignment(Pos.CENTER);
		
		ArrayList<String> startWords = findNext("<s> <s>");
		try {
			for (int i=0; i<5; i++) {
				suggPane.getChildren().add(getSuggBtn(startWords.get(i)));
			}
		} catch (IndexOutOfBoundsException ex) {}
		
		
		Label lbl = new Label("الاقتراحات");
		lbl.setStyle("-fx-font-weight:bold");
		
		root.getChildren().addAll(suggPane, lbl);
		
		return root;
	}
	
	//Initialize suggestion button
	private Button getSuggBtn(String word) {
		Button btn = new Button(word);
		btn.setTextFill(Color.WHITE);
		btn.setPadding(new Insets(7.5, 10, 7.5, 10));
		final String DEFAULT = "-fx-background-radius:12.5px;";
		btn.setStyle("-fx-background-color:#666;"+DEFAULT);
		btn.setOnMouseEntered(e-> {
			btn.setStyle("-fx-background-color:#888;"+DEFAULT);
		});
		btn.setOnMouseExited(e-> {
			btn.setStyle("-fx-background-color:#666;"+DEFAULT);
		});
		btn.setOnAction(e-> {
			String txt = txtArea.getText();
			//Check if the program should append a new word or complete an existing one
			if (!txt.isEmpty()&&txt.charAt(txt.length()-1)!=' ') {
				int index = txt.lastIndexOf(' ');
				index = index==-1?0:index;
				txtArea.setText(txt.substring(0, index==0?0:index+1));
			}
			txtArea.appendText(btn.getText()+" ");
		});
		return btn;
	}
	
	//Returns suggestions to complete a word
	private ArrayList<String> completeWord (String word) {
		ArrayList<String> nextWords = new ArrayList<>();
		try {
			int index = BuildNGrams.getFileNum(word.charAt(0));
			Scanner in = new Scanner(new File("./Data/bigram"+index+".txt"));
			while (in.hasNextLine()&&nextWords.size()<5) {
				String line = in.nextLine();
				if (line.startsWith(word)) {
					nextWords.add(line.split("_")[0].replaceAll("[0-9)(.]", ""));
				}
			}
			in.close();
		} catch (FileNotFoundException ex) {
			System.out.println("File Not Found");
		}
		return nextWords;
	}
	
	//Find next possible words
	private ArrayList<String> findNext(String word) {
		ArrayList<String> nextWords = new ArrayList<>();
		getWordsFromFile("trigram", word, nextWords);
		//If one or less results were found in the trigram
		//Search in Bigram
		if (nextWords.size()<=1) {
			getWordsFromFile("bigram", word.split(" ")[1], nextWords);
		}
		return nextWords;
	}
	
	//Get possible words from trigram or bigram
	private void getWordsFromFile(String fileName, String word, ArrayList<String> nextWords) {
		try {
			//Get the number of the file where the words are stored
			int fileNum = BuildNGrams.getFileNum(word.charAt(0));
			Scanner in = new Scanner(new File("./Data/"+fileName+fileNum+".txt"));
			while (in.hasNextLine()&&nextWords.size()<=1) {
				String[] words = in.nextLine().split("_");
				if (words.length>1&&words[0].trim().replaceAll("[0-9)(.]", "").equals(word)) {
					for (int i=1; i<words.length; i++) {
						String wd = words[i].replaceAll("[0-9)(.]", "");
						if (!wd.equals("<s>")&&!wd.equals("</s>")) {
							if (!nextWords.contains(wd)) {
								nextWords.add(wd);
							}
						}
					}
				}
			}
			in.close();
		} catch (FileNotFoundException ex) {
			System.out.println("File Not Found ");
		}
	}
}