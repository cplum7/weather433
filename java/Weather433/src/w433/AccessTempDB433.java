package w433;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AccessTempDB433 {
	static Logger LOGGER = Weather433.LOGGER;
	static final int MAX_TIME_COL = 0;
	static final int MAX_TEMP_COL = 1;
	static final int MIN_TIME_COL = 2;
	static final int MIN_TEMP_COL = 3;
	int lastHourlyTime = 0;
	String httpStrRemote;
	InputStream inStrmURLConnect = null;
	boolean sendingSavedToDB = false;
	List<int[]> savedData = new ArrayList<int[]>();
	boolean dbg = false;

	public AccessTempDB433(String httpStrRemote, boolean dbg) {
		this.httpStrRemote = httpStrRemote;
		this.dbg = dbg;
	}

	public HttpURLConnection urlConnect(String httpSuffix) {
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(httpStrRemote + httpSuffix);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(1000 * 60 * 3);
			urlConnection.setReadTimeout(1000 * 60 * 3);
			urlConnection.connect();
			inStrmURLConnect = urlConnection.getInputStream();
		} catch (Exception e) {
			LOGGER.severe("urlConnect Exception:" + e.getMessage() + " " + httpSuffix);
			urlConnection = null;
		}

		return urlConnection;
	}

	public void urlDisconnect(HttpURLConnection urlConnection) {
		if (urlConnection != null)
			urlConnection.disconnect();
	}
	
	public void setStartTime() {
		String suffix = "setStartTime.php";
		urlConnect(suffix);
	}

	public int[] getLastHourlyTime() {
		String rtnStr = "";
		String[] strVals = null;
		int[] hrlyVals = new int[4];
		String suffix = "getLastHourlyTime.php";
		HttpURLConnection urlConnection = urlConnect(suffix);
		if (urlConnection != null) {
			InputStreamReader isr = new InputStreamReader(inStrmURLConnect);
			BufferedReader br = new BufferedReader(isr);
			try {
				while ((rtnStr = br.readLine()) != null)
					strVals = rtnStr.split(" |<");
				hrlyVals[MAX_TIME_COL] = Integer.valueOf(strVals[MAX_TIME_COL]);
				hrlyVals[MAX_TEMP_COL] = Integer.valueOf(strVals[MAX_TEMP_COL]);
				hrlyVals[MIN_TIME_COL] = Integer.valueOf(strVals[MIN_TIME_COL]);
				hrlyVals[MIN_TEMP_COL] = Integer.valueOf(strVals[MIN_TEMP_COL]);
			} catch (IOException e) {
				LOGGER.severe("getLastHourlyTime: IOException: " + e.getMessage());
			} finally {
				urlDisconnect(urlConnection);
			}
		} else {
			LOGGER.severe("getLastHourlyTime: IOException: connect error: ");
		}

		return hrlyVals;
	}

	public void updateCurrentTemp(int time, int temp) {
		String suffix = "setCurrentTimeTemp.php/?time=" + time + "&temp=" + temp;
		if ((urlConnect(suffix) != null) && !savedData.isEmpty())
			sendSavedToDB();
	}

	public void writeHourlyTemps(int lastHourlyTime, int maxTime, int maxTemp, int minTime, int minTemp) {
		String suffix = "setHourlyTemp.php/?lastHourlyTime=" + lastHourlyTime + "&maxTime=" + maxTime + "&maxTemp="
				+ maxTemp + "&minTime=" + minTime + "&minTemp=" + minTemp;
		HttpURLConnection urlConnection = urlConnect(suffix);
		if (urlConnection == null && !sendingSavedToDB) {
			int[] hrlyVals = new int[5];
			hrlyVals[0] = lastHourlyTime;
			hrlyVals[1] = maxTime;
			hrlyVals[2] = maxTemp;
			hrlyVals[3] = minTime;
			hrlyVals[4] = minTemp;
			savedData.add(hrlyVals);
		}
		urlDisconnect(urlConnection);
		if (dbg)
			LOGGER.info(suffix);
	}

	public void sendSavedToDB() {
		sendingSavedToDB = true;
		int[] hrlyVals = new int[5];
		for (int i = 0; i < savedData.size(); i++) {
			hrlyVals = savedData.get(i);
			writeHourlyTemps(hrlyVals[0], hrlyVals[1], hrlyVals[2], hrlyVals[3], hrlyVals[4]);
		}
		LOGGER.info("sendSavedToDB sent " + savedData.size() + "rows to DB  last row: " + hrlyVals[0] + " "
				+ hrlyVals[1] + " " + hrlyVals[2] + " " + hrlyVals[3] + " " + hrlyVals[4]);
		savedData.clear();
		sendingSavedToDB = false;
	}
	
	public void turnOnSmartPlug() {
		urlConnect("on");
	}
	
	public void turnOffSmartPlug() {
		urlConnect("off");
	}
}
