package lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ItalianClassifier implements LanguageClassifier {
	private static ItalianClassifier instance = null;
	private static ArrayList<String> dictionary;
	private static double threshold;

	public static ItalianClassifier getInstance(double th) {
		if (instance == null)
			instance = new ItalianClassifier(th);
		return instance;
	}

		
	public ItalianClassifier(double th) {
		dictionary = new ArrayList<String>();
		threshold = th;

		try {
			InputStream in = ItalianClassifier.class
					.getResourceAsStream("/profiles/it");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(in));
			String line = null;

			while ((line = bufferedReader.readLine()) != null)
				dictionary.add(line.toLowerCase().trim());

			bufferedReader.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean isItalian(List<String> words) {
		int numberOfItalianWords = 0;
		int numberOfWords = words.size();

		for (String word : words) {
			if (dictionary.contains(word.toLowerCase()) || isItalianName(word))
				numberOfItalianWords++;
		}

		double prob = (double) numberOfItalianWords / (double) numberOfWords;

		if (prob >= threshold)
			return true;
		else
			return false;
	}

	public boolean satisfy(List<String> words) {
		return isItalian(words);
	}

	private static boolean isItalianName(String word) {
		return word.matches("[A-Z][a-zA-Z0-9]+");
	}
}
