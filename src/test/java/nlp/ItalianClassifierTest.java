package nlp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import lang.ItalianClassifier;

public class ItalianClassifierTest {

	@Test
	public void test() {
		ItalianClassifier i = new ItalianClassifier(.8);
		List trueList = new ArrayList();
		trueList.add("Ciao");
		trueList.add("Buongiorno");
		assertTrue(i.isItalian(trueList));
		
		List falseList = new ArrayList();
		falseList.add("يَآرَپ پشارة");
		falseList.add("Good Morning");
		assertFalse(i.isItalian(falseList));
	}

}
