package ta;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import stm.StorageManager;
import conf.Interest;
import conf.Phrase;
import conf.Tweet;

public class Window implements Runnable {
	static final Logger logger = LogManager.getLogger(Window.class.getName());

	private static final int executerTimeOutHours = 5;
	private static final int workerPoolSize = 5;
	private static final int pollTimeout = 10;
	
	private static Date date = new Date() ;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss") ;
	private static File file = new File("src/main/resources/tweets/" + dateFormat.format(date) + ".txt") ;
	private static BufferedWriter writer;
	

	private Thread t_cw;

	private volatile ExecutorService executor;
	private volatile WindowStatistics statistics;
	private volatile BlockingQueue<Tweet> tweetBuffer;

	private volatile long startTime;
	private volatile long endTime;
	private volatile boolean open;
	private volatile boolean done;

	// top-k related
	private List<String> topEntities;
	long timestamp;
	int tweetCount;

	
	public Window() {
		done = false;
		tweetBuffer = new LinkedBlockingQueue<Tweet>();
		statistics = Acquisition.getInterest().getStatistics().addNewStat();
		executor = Executors.newFixedThreadPool(workerPoolSize);
		t_cw = new Thread(this);
		t_cw.setName("t_cw");
		topEntities = new LinkedList<String>();
		try {
			writer = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void open() {
		startTime = System.currentTimeMillis();
		open = true;
		t_cw.start();
	}

	public void close() {
		synchronized (this) {
			open = false;
			endTime = System.currentTimeMillis();
			notifyAll();
		}
		logger.info("Window Size (secs):	" + (double) getLength() / 1000);
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void finish() {
		synchronized (this) {
			done = true;
			finalizeStatistics();
			notifyAll();
		}
		logger.info("window has been finished.");
	}

	public void shutdown() {
		synchronized (this) {
			printReport();
			if (t_cw != null) {
				t_cw.interrupt();
				t_cw = null;
			}
			System.gc();
		}
		logger.info("window has been shutdown.");
	}

	public void addTweet(Tweet tweet) {
		int windowSize = Acquisition.getWindowSize();
			try {
				System.out.println(tweet.getText());
				writer.write(tweet.getText());
				writer.newLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		if (isOpen()
				&& (statistics.relevantTweetCount.get() >= windowSize)
				&& (getLength() > Acquisition.minWindowLength)) {
			close();
		}

		
		if (isOpen())
			tweetBuffer.add(tweet);
		else {
			getStatistics().deltaTweetCount.incrementAndGet();
			tweet.setRelevance(.5);
			tweet.setInterestId(Acquisition.getInterest().getId());
			StorageManager.addTweet(tweet);
		}
	}

	public void run() {
		for (int i = 0; i < workerPoolSize; i++) {
			WindowWorker worker = new WindowWorker(this, i);
			executor.execute(worker);
		}

		executor.shutdown();
		try {
			if (!executor
					.awaitTermination(executerTimeOutHours, TimeUnit.HOURS))
				logger.info("Threads didn't finish in " + executerTimeOutHours
						+ " hours!");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		finish();
	}

	public Tweet pollTweet() {
		try {
			return tweetBuffer.poll(pollTimeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	public boolean isOpen() {
		return open;
	}

	public int getBufferSize() {
		return tweetBuffer.size();
	}

	private void finalizeStatistics() {
		Collection<Phrase> interestPhrases = Acquisition.getInterest()
				.getPhrases();

		Interest interest = Acquisition.getInterest();
		getStatistics().finalize(interest.getStatistics(), getLength());

		for (Phrase phrase : interestPhrases) {
			TotalStatistics pstatistics = phrase.getStatistics();
			if (pstatistics != null) {
				WindowStatistics plastWindowStatistics = pstatistics
						.getLastWindowStatistics();
				if (plastWindowStatistics != null)
					plastWindowStatistics.finalize(pstatistics, getLength());
			}
		}

		// top-k related
		// window finished, report
		logger.info("window is full");
		tweetCount = 0;
	}

	public long getEndTime() {
		return endTime;
	}

	public long getLength() {
		return (endTime > 0 ? endTime : System.currentTimeMillis()) - startTime;
	}

	public long getStartTime() {
		return startTime;
	}

	public WindowStatistics getStatistics() {
		return statistics;
	}

	public boolean isDone() {
		return done;
	}

	private void printReport() {
		logger.info("WTC: " + statistics.totalTweetCount.get());
		int tetc = statistics.relevantTweetCount.get()
				+ statistics.irrelevantTweetCount.get();
		logger.info("WETC: " + tetc);
		logger.info("WRTC: " + statistics.relevantTweetCount);
		logger.info("WAvgRel: " + statistics.getAvgRelevance());
		logger.info("WMinRel: " + statistics.getMinRelevance());
		logger.info("WRHC: " + statistics.getRelevantHashtags().size());
		logger.info("WIHC: " + statistics.getIrrelevantHashtags().size());
		logger.info("TUC: " + Acquisition.getInterest().getUsers().size());
		logger.info("TTC: "
				+ Acquisition.getInterest().getStatistics()
						.getTotalTweetCount());
		logger.info("TRTC: "
				+ Acquisition.getInterest().getStatistics()
						.getRelevantTweetCount());
		logger.info("TDTC: "
				+ Acquisition.getInterest().getStatistics()
						.getDeltaTweetCount());
	}

	public List<String> getTopEntities() {
		return topEntities;
	}

	public void setTopEntities(List<String> topEntities) {
		this.topEntities = topEntities;
	}

}