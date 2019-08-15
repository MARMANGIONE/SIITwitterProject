package stm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import ta.Acquisition;
import ta.UserStatistics;

import com.mongodb.MongoClient;

import conf.ConfigMgr;
import conf.Interest;
import conf.Report;
import conf.Tweet;
import conf.User;

public class StorageManager implements Runnable {
	private static StorageManager instance = null;

	private volatile static BlockingQueue<Tweet> tweets;
	private volatile Morphia morphia = new Morphia();

	private volatile static boolean storeUserInfo = false;
	private volatile static Datastore datastore;
	private volatile static Datastore simulationdatastore;

	private static boolean running;
	private static Thread t_sm;

	public StorageManager() {
		this(ConfigMgr.readConfigurationParameter("MongoDBDatabase"));
	}

	public StorageManager(String database) {
		storeUserInfo = Boolean.valueOf(ConfigMgr
				.readConfigurationParameter("MongoDBStoreUserInfo"));
		String host = ConfigMgr
				.readConfigurationParameter("MongoDBDatabaseHost");
		int port = Integer.parseInt(ConfigMgr
				.readConfigurationParameter("MongoDBDatabasePort"));

		MongoClient m = null;
		m = new MongoClient(host, port);

		morphia.map(Tweet.class).map(User.class).map(Interest.class)
				.map(Report.class);

		datastore = morphia.createDatastore(m, database);
		tweets = new LinkedBlockingQueue<Tweet>();

		boolean simulate = Boolean.valueOf(ConfigMgr
				.readConfigurationParameter("UseSimulator"));
		String simulationDatabase = ConfigMgr
				.readConfigurationParameter("SimulationDBName");
		if (simulate) {
			simulationdatastore = morphia
					.createDatastore(m, simulationDatabase);
			simulationdatastore.ensureIndexes();
		}
	}

	public synchronized static StorageManager getInstance() {
		if (instance == null) {
			instance = new StorageManager();
			StorageManager.t_sm = new Thread(instance);
			StorageManager.t_sm.setName("t_sm");
		}
		return instance;
	}

	public synchronized static StorageManager getInstance(String database) {
		if (instance == null) {
			instance = new StorageManager(database);
			StorageManager.t_sm = new Thread(instance);
			StorageManager.t_sm.setName("t_sm");
		}
		return instance;
	}

	public static void start() {
		running = true;
		t_sm.start();
	}

	public static void stop() {
		running = false;
	}

	public void run() {
		while (running) {
			Tweet tweet = null;
			try {
				tweet = tweets.take();
				if (tweet == null)
					continue;
				storeTweet(tweet);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized static org.mongodb.morphia.query.Query<Tweet> getSimulationQuery() {
		if (simulationdatastore != null) {
			org.mongodb.morphia.query.Query<Tweet> q = simulationdatastore
					.createQuery(Tweet.class).disableValidation()
					.order("timestamp");
			return q;
		}

		return null;
	}


	public synchronized static Report getLatestReport(String interestId) {
		if (datastore != null) {
			List<Report> reports = datastore.createQuery(Report.class)
					.disableValidation().filter("interestId =", interestId)
					.order("-timestamp").limit(2).asList();

			if (reports.size() > 0)
				reports.get(0);
		}

		return null;
	}

	public synchronized static void addTweet(Tweet tweet) {
		tweets.add(tweet);
	}

	public static void storeInterest(Interest interest) {
		datastore.save(interest);
	}

	public static void deleteInterest(String interestId) {
		datastore.delete(Interest.class, interestId);
	}

	public static void storeUser(User user) {
		datastore.delete(datastore.createQuery(User.class).filter("id",
				user.getId()));
		datastore.save(user);
	}

	public static void storeReport(Report report) {
		datastore.save(report);
	}

	public static void resetUserWindowCounts() {
		if (storeUserInfo) {
			datastore.update(
					datastore.createQuery(User.class),
					datastore.createUpdateOperations(User.class).set(
							"windowNumTweets", 0));
		}
	}

	public static List<Interest> getInterests() {
		List<Interest> interests = datastore.find(Interest.class)
				.field("active").equal(true).asList();
		return interests;
	}

	public static void main(String[] args) throws Exception {
		StorageManager.getInstance();
		clearAll();

		Report report = getLatestReport("45");
		System.out.println(report.getStartTime());
	}

	public static void clearReports() {
		datastore.delete(datastore.createQuery(Report.class));
	}

	public static void clearAll() {
		datastore.delete(datastore.createQuery(Tweet.class));
		datastore.delete(datastore.createQuery(User.class));
		datastore.delete(datastore.createQuery(Report.class));
	}

	public static void close() {
		running = false;
		for (Tweet tweet : tweets)
			storeTweet(tweet);

		synchronized (t_sm) {
			if (t_sm != null) {
				t_sm.interrupt();
				t_sm = null;
			}
		}
	}

	public static void storeTweet(Tweet tweet) {
		Tweet t = tweet;
		datastore.save(t);

		if (!storeUserInfo)
			return;

		if (Acquisition.getInterest() == null)
			return;
		User user = null;
		long userID = tweet.getUserID();
		UserStatistics userStatistics = Acquisition.getInterest().getUsers()
				.get(userID);

		if (userStatistics != null) {
			user = new User(tweet.getStatus().getUser(), userStatistics);
			storeUser(user);
		}
	}

	public synchronized static int getQueueSize() {
		return tweets.size();
	}

}
