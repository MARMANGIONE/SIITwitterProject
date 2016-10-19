package conf;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Transient;

import twitter4j.Status;
import twitter4j.URLEntity;
import txt.TextNormalizer;

@Entity("tweet")
public class Tweet {

	@Id
	private long id;

	// @Serializeds
	private Status status;
	@Indexed
	private long timestamp;

	private String interest;
	private double relevance;

	@Embedded("entities")
	private HashMap<String, String> entities;

	@Transient
	private HashSet<String> terms;

	public Tweet() {
	}

	public Tweet(Status s) {
		entities = new HashMap<String, String>(); // Mention-Entity
		relevance = 0;
		status = s;
		if (getStatus() != null) {
			id = getStatus().getId();
			timestamp = getStatus().getCreatedAt().getTime();
		}
	}

	public Map<String, String> getEntities() {
		return entities;
	}

	public boolean containsPhrase(String p) {
		String[] pterms = p.split(" ");

		for (String pt : pterms) {
			if (!getTerms().contains(pt))
				return false;
		}

		return true;
	}

	public HashSet<String> getTerms() {
		if (terms == null) {
			terms = new HashSet<String>();
			String text = getStatus().getText();

			for (URLEntity url : getStatus().getURLEntities()) {
				String urltext = url.getExpandedURL().replace("http", "")
						.replace("https", "").replace("www", "");
				String[] urlterms = urltext.split("[^A-Za-z]");
				for (String ut : urlterms)
					if (ut.length() > 3)
						text += " " + ut;
			}

			if (getStatus().getRetweetedStatus() != null
					&& !getStatus().getText().equals(
							getStatus().getRetweetedStatus().getText()))
				text += " " + getStatus().getRetweetedStatus().getText();

			text = text.toLowerCase();
			terms.addAll(TextNormalizer.normalize(text));
		}

		return terms;
	}

	public String getText() {
		return getStatus().getText();
	}

	public Date getTime() {
		return getStatus().getCreatedAt();
	}

	public long getUserID() {
		if (status != null)
			return getStatus().getUser().getId();
		return -1;
	}

	public long getId() {
		return id;
	}

	public Status getStatus() {
		return status;
	}

	public double getRelevance() {
		return relevance;
	}

	public String getInterestId() {
		return interest;
	}

	public void setRelevance(double relevance) {
		this.relevance = relevance;
	}

	public void setInterestId(String interestId) {
		this.interest = interestId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void addEntity(String mention, String ents) {
		entities.put(mention, ents);
	}
}
