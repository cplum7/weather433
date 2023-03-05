package w433;


import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/*
This program starts rtl_fm, a program
which connects to a RTL-SDR device and
outputs integer samples of signals received
from transmissions from a Wittime WT2039A
temperature and humidity wireless sensor.
Transmission frequency is 433 MHz and
temperature measuring range is
-40°F (-40°C) to 158°F (70°C)

The encoded signal consists of 23 duplicate
data packets each preceded by a sync signal.
A sync as shown below is repeated four times
and has a relative width of 3 high and 3 low.

 ______   --> sample values > 15000
|      |      
|      |      
|      |______

A one bit is 2 high and 1 low as follows:
 ____
|    |  
|    |  
|    |__

A zero bit is 1 high and 2 low as follows:
 __
|  |    
|  |    
|  |____

If the rtl_fm program sampling rate is set
to 160,000 samples per second a one bit,
a zero bit and each high and low sync
have a width of approximately 135 samples.
As show above, samples with an arbitrarily
set value greater than 15000 determine
a high signal for sync and data bits.
In other words, if most of a syncs 135 samples
are greater than 15000 it is considered a high sync
otherwise it is a low sync.

Each data packets consists of 41 bits as
in the following example:

temp=13.1°C=55.6°F humidity=76%   hex=0x78 4C 10 83 43 
 0111 1000  0100 1100 00 01 0000 1000 0011 0100 0011 0
|sensor ID?|humidity | ?|CN|  °C × 10     |    CRC? |unused separator?| 

minus centigrade temperatures are coded as follows:
0xFFF = -0.1°C  0xFFE = -0.2°C and so forth to 0xE70 = -40.0°C

CN (channel number) 0=channel 1  1=channel 2  2=channel 3
channel number is set on the sensor

The Cyclic Redundancy Check (CRC) algorithm has yet to be
determined and is ignored.

Ideally a received transmission consists of 23 identical
and valid data packets.
However, if the transmission is corrupted for any reason
and the received data packets are not all identical
the program compares valid data packets and selects the one
with the most matches.

This program provides the code and option to update a
database with temperature data.  The properties file,
weather433.props, provides the means to control this
DB option without having to change and recompile this
program.

Each row of the DB table contains the maximum and minium
temperature during each hour of the day and the time when
they occurred.  Times in the database are the number of seconds
that have elapsed since January 1, 1970 (midnight UTC/GMT).

Temperature values are entered in the DB as (°C X 10) + 400;
so for example 13.1°C would be entered as 13.1 X 10 + 400 = 531
The plus 400 is to eliminate any minus values.

A separate thread is run when accessing the DB
Another thread uses humidity values during the
last 24 hours to control a sprinkler system.

*/

public class Weather433 {
	static Properties props = null;
	static final String propertyFile = "./weather433.props";
	static final String logfilename = "./Weather433.log";
	static final Logger LOGGER = Logger.getLogger(Weather433.class.getName());
	static Handler logHndlr = null;
	static final String versionnum = "1.4.8";
	boolean dbg = true;
	long dbgTime = 0;
	boolean writetoDB = false;
	boolean controlSprinkler = false;
	String httpStrRemote;
	String smartPlugAddr;
	int channelNum = 1;
	int bitLen = 40;  // bit 41 not used; separator bit?
	int bitIndx_Channel = 18;
	int bitIndx_Temp = 20;
	int bitIndx_Humidity = 8;
	int bitIndx_CRC_calc = 0; // index to start of data bits used in crc calculation
	int bitIndx_CRC_sent = 32;
	int crcBits = 32; // number of bits used in crc calculation
	String signalSampler = "/opt/local/bin/rtl_fm";
	String signalSamplerOpts = "-M am -f 433.92M -s 160k";
	private int highCnt = 0;
	private long sampleCnt = 0;
	private long startHCnt = 0;
	private int hSample = 15000;
	int matchCnt = 0;
	int hSync = 0;
	boolean findSync  = true;
	BitSet dataBitSet = new BitSet(bitLen);
	int bitSetIdx = 0;
	BitSet [] uniqDataSets = new BitSet[23];
	int uniqDataSetsIndx = 0;
	int [] bitSetMatchCnt = new int[23];
	private int syncLen = 135;
	private int syncLenMin = (int) (syncLen * .9);
	private int syncLenMax = (int) (syncLen * 1.1);
	String bitStr="";
	int dataSetCnt = 0;
	private boolean dataSetEnd;
	private int lowCnt;
	String matchCntStr = "";
	String notBestMatchDataSetsStr = "";
	int maxTemp = 0;
	int minTemp = 9999;
	int maxTime = 0;
	int minTime = 0;
	int currentHr = 0;
	int lastHourlyTime = 0;
	static final int SECS_PER_HR = 3600;
	AccessDBThread curTmpThrd = null;
	AccessDBThread writeHrlyThrd = null;
	private int tempLog;
	private int timeLog;
	private boolean doSampleCnt = false;
	int high24HrHumidity = 0;
	private boolean dbgProps;
	Process process = null;
	
	public Weather433(String showUnusedCodes) {
		setLogging();
		addMenuExtra();
		loadProps();
		new AccessTempDB433(httpStrRemote, dbg).setStartTime();
		checkLastHrlyDBEntry();
		if (controlSprinkler) {
			SprinklerThread sprnklrThrd = new SprinklerThread(smartPlugAddr);
			sprnklrThrd.start();
		}
		List<String> command = setSamplerArgs();
		buildProcess(command);
	}
	
	public void setLogging() {
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"%1$tF %1$tT v" + versionnum + " %4$s %5$s%6$s%n");
		try {
			logHndlr = new FileHandler(logfilename, Integer.MAX_VALUE, 1, true);
			logHndlr.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(logHndlr);
		} catch (SecurityException | IOException e) {
			String logpath = new File(logfilename).getAbsolutePath();
			System.err.println("Error creating log file:" + logpath + " error:" + e.toString());
			exitWeather433();
		}
	}
	
	// add icon to system tray to exit program
	// On mac icon goes on menu bar in upper right of screen
	public void addMenuExtra() {
		TrayIcon trayIcon = null;
		SystemTray tray = SystemTray.getSystemTray();
		Image image = Toolkit.getDefaultToolkit().getImage("./w433.png");
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LOGGER.info("addMenuExtra listener: do exit");
				exitWeather433();
			}
		};	
		PopupMenu popup = new PopupMenu();
		MenuItem defaultItem = new MenuItem("Exit");
		defaultItem.addActionListener(listener);
		popup.add(defaultItem);
		trayIcon = new TrayIcon(image, "Tray Icon", popup);
		trayIcon.addActionListener(listener);		
		try {
		    tray.add(trayIcon);
		} catch (AWTException e) {
			LOGGER.severe("AWTException"+ " msg:" + e.getMessage());
		}
	}
	
	public void loadProps() {
		InputStream inStrm = null;
		props = new Properties();

		try {
			inStrm = new FileInputStream(propertyFile);
			if (inStrm != null) {
				props.load(inStrm);
				inStrm.close();
			}
		} catch (IOException e) {
			LOGGER.severe("IOException: error reading property file: : " + propertyFile + " msg:" + e.getMessage());
		}

		hSample = Integer.parseInt(props.getProperty("hSample", "15000"));
		controlSprinkler = props.getProperty("controlSprinkler", "false").equals("false") ? false : true;
		writetoDB = props.getProperty("writetoDB", "false").equals("false") ? false : true;
		httpStrRemote = props.getProperty("httpStrRemote");
		smartPlugAddr = props.getProperty("smartPlugAddr");
		channelNum = Integer.parseInt(props.getProperty("channelNum", "1"));
		signalSampler = props.getProperty("signalSampler", "/opt/local/bin/rtl_fm");
		dbgProps = props.getProperty("debug", "false").equals("false") ? false : true;
		dbg = dbgProps;
	}
	
	public void checkLastHrlyDBEntry() {
		int[] hourlyVals = new AccessTempDB433(httpStrRemote, dbg).getLastHourlyTime();

		if (hourlyVals[0] > 0) {
			//int secsSincedbStartTime = (int) (System.currentTimeMillis() / 1000) - dbStartTime;
			//int secsSinceCurrentHr = (int) (System.currentTimeMillis() / 1000) % SECS_PER_HR;
			int now = (int) (System.currentTimeMillis() / 1000);
			int secsSinceCurrentHr = now % SECS_PER_HR;
			currentHr = now - secsSinceCurrentHr;

			// include previous db updates if within current hour
			if (hourlyVals[0] >= currentHr) {
				maxTime = hourlyVals[0];
				maxTemp = hourlyVals[1];
				minTime = hourlyVals[2];
				minTemp = hourlyVals[3];
			}

			LOGGER.info("maxTime=" + maxTime + " maxTemp=" + maxTemp + " minTime=" + minTime + " minTemp=" + minTemp
					+ " currentHr=" + currentHr);
		} else {
			LOGGER.severe("no hourlyVals[0]   exiting");
			exitWeather433();
		}
	}
	
	public List<String> setSamplerArgs() {
		List<String> command = new ArrayList<String>();
		command.clear();
		command.add(signalSampler);
		// -M $mode -f $frequency -s $srate
		String[] signalSamplerOptsArray = signalSamplerOpts.split(" ");
		for (String option : signalSamplerOptsArray)
			command.add(option);
		return command;
	}
	
	public void buildProcess(List<String> command) {
		String msg = "";
		ProcessBuilder processb = new ProcessBuilder(command);
		try {			
			short sample = 0;
			long now = System.currentTimeMillis();
			dbgTime = now;
			long dbgStartTime = now;
			process = processb.start();
			InputStream is = process.getInputStream();
			InputStream es = process.getErrorStream();
			DataInputStream dis = new DataInputStream(is);
			while (process.isAlive()) {
				if (es.available() > 0) {
					Character txt = Character.valueOf((char) es.read());
					msg = msg + txt.toString();
				} else if (!msg.isEmpty()) {
					LOGGER.info(msg);
					msg = "";
				}
				while (dis.available() > 0) {
					sample = dis.readShort();
					sample = fmtEndian(sample);
					processSignal(sample);
					if (doSampleCnt)
						sampleCnt++;
				}
				now = System.currentTimeMillis();
				if (!dbg && (now - dbgTime) > (1000 * 60 * 10)) {
					resetFindSync();
					dbg = true;
					LOGGER.severe("buildProcess: turning on dbg; no valid temp for 10 minutes  sampleCnt="
							+ sampleCnt);
					dbgStartTime = now;
				} else if (dbgStartTime > 0 && dbg && (now - dbgStartTime) > (1000 * 60 * 3)) {
					dbg = dbgProps;
					dbgStartTime = 0;
					boolean foundValidTemp = ((now - dbgTime) < (1000 * 60 * 10)) ? true: false;						
					LOGGER.info("resetting dbg  foundValidTemp=" + (foundValidTemp ? "true" : "false"));
				}
			}
			// rtl_fm process terminated
			LOGGER.info("rtl_fm process terminated");
			exitWeather433();
		} catch (IOException e) {
			LOGGER.severe("buildProcess: IOException errmsg: " + e.getMessage());
		}
	}
	
	public void processSignal(short sample) {
		if (sample > hSample) {
			if (!doSampleCnt)
				doSampleCnt = true;
			if (highCnt == 0)
				startHCnt = sampleCnt;
			highCnt++;
			
			if (dataSetEnd && ((lowCnt + highCnt) > syncLenMin)) {
				lowCnt = 0;
				dataSetEnd = false;
				startHCnt = sampleCnt;
				highCnt = 1;
			}
			
			if (findSync && (sampleCnt - startHCnt) > (syncLen + syncLenMin)) {
				if (highCnt > syncLenMin)
					hSync++;
				else
					startHCnt = 0;
				highCnt = 0;
				
				if (hSync == 4) {
					findSync = false;
					startHCnt = 0;
					hSync = 0;
					bitSetIdx = 0;
					dataBitSet = new BitSet(bitLen);
				}
			}
			
			// found 1 bit or 0 bit
			if (!findSync && bitSetIdx < bitLen && highCnt > 0 && (sampleCnt - startHCnt) > syncLenMin) {
				if (highCnt < syncLenMin) {
					String tmp = (bitStr.length()%5 == 0) ? " " : "";
					if (highCnt > (syncLenMin/2)) {			
						bitStr = bitStr + tmp + "1";
						if (bitSetIdx < bitLen)
							dataBitSet.set(bitSetIdx);
					} else
						bitStr = bitStr + tmp + "0";
					bitSetIdx++;
					highCnt = 0;
				} else {  // insufficient bits find sync
					findSync = true;
					bitSetIdx = 0;
					bitStr =  "";
					dataBitSet = new BitSet(bitLen);
				}
			}
			
			if (!findSync && bitSetIdx >= bitLen) {
				highCnt = 0;
				findBestValidDataSet();
				dataSetCnt++;
				if (dbg)
					logDbgInfo(1);
				bitStr =  "";
				dataBitSet = new BitSet(bitLen);
				findSync = true;
				bitSetIdx = 0;
				dataSetEnd = true;
				lowCnt = 0;
			}
		} else if (dataSetEnd) {
			lowCnt++;
			// if signal end
			if (lowCnt > syncLenMax)
				processData();
		}
	}
	
	// if dataset valid, count matching datasets
	public void findBestValidDataSet() {
		boolean match = false;
		if (isDataSetValid()) {
			for (int i = 0; i < uniqDataSetsIndx; i++) {
				if (dataBitSet.equals(uniqDataSets[i])) {
					bitSetMatchCnt[i]++;
					match = true;
				}
			}
			// match = false for uniqDataSetsIndx = 0
			if (!match)
				uniqDataSets[uniqDataSetsIndx++] = dataBitSet;
		}
	}
	
	public boolean isDataSetValid() {
		boolean isValid = true;
		int data = getDataBitSetDecVal(dataBitSet, 0, 8);
		isValid = (data == 0x78);
		int channel = getDataBitSetDecVal(dataBitSet, bitIndx_Channel, 2);
		isValid = isValid && (channel == (channelNum - 1));
		int temp = getDataBitSetDecVal(dataBitSet, bitIndx_Temp, 12);
		isValid = isValid && ((temp <= 0x2BC && temp >= 0) || (temp >= 0xE70 && temp <= 0xFFF));
		int humidity = getDataBitSetDecVal(dataBitSet, bitIndx_Humidity, 8);
		isValid = isValid && (humidity >= 0x14 && humidity <= 0x5F);
		int crc = getDataBitSetDecVal(dataBitSet, bitIndx_CRC_sent, 8);
		int lastIndx = bitIndx_CRC_calc + (crcBits);
		int crcCalc = getCRC(0x31, 8, 0, 0, dataBitSet.get(bitIndx_CRC_calc, lastIndx));
		//isValid = isValid && (crc == crcCalc);
		return isValid;
	}
	
	public void processData() {
		int bestMatchIdx = 0;
		int bestMatchCnt = 0;
		matchCntStr = "";
		for (int i = 0; i<uniqDataSetsIndx;i++) {
			matchCntStr = matchCntStr + " " + i + "=" + bitSetMatchCnt[i];
			if (bitSetMatchCnt[i] > bestMatchCnt) {
				bestMatchCnt = bitSetMatchCnt[i];
				bestMatchIdx = i;
			}
		}
		notBestMatchDataSetsStr = "";
		for (int i = 0; i<uniqDataSetsIndx;i++) {
			if (i != bestMatchIdx) {
				notBestMatchDataSetsStr = notBestMatchDataSetsStr + "\nnot best match: indx=" + i + " cnt=" +  bitSetMatchCnt[i] + " hex=0x";
				for (int j = 0; j < bitLen; j += 8)
					notBestMatchDataSetsStr = notBestMatchDataSetsStr + String.format("%02X ", getDataBitSetDecVal(uniqDataSets[i], j, 8));
			}
		}
		
		dataBitSet = uniqDataSets[bestMatchIdx];
		if (dataBitSet != null) {
			decodeDataBitSet();
			logDbgInfo(2);
		}
		resetFindSync();
	}
	
	public void resetFindSync() {
		hSync = 0;
		highCnt = 0;
		lowCnt = 0;
		findSync = true;
		dataSetEnd = false;
		bitSetIdx = 0;
		startHCnt = 0;
		dataSetCnt = 0;
		uniqDataSetsIndx = 0;
		uniqDataSets = new BitSet[23];
		bitSetMatchCnt = new int[23];
		sampleCnt = 0;
		doSampleCnt = false;		
	}
		
	public void decodeDataBitSet() {
		dbgTime = System.currentTimeMillis();
		boolean doWriteHourlyTemps = false;
		int temp400 = getTemp400();
		int time = (int) (System.currentTimeMillis() / 1000);
		int humidity = getDataBitSetDecVal(dataBitSet, bitIndx_Humidity, 8);
		if (humidity > high24HrHumidity)
			high24HrHumidity = humidity;
		if (curTmpThrd != null)
			curTmpThrd.interrupt();
		curTmpThrd = new AccessDBThread(time, temp400);
		curTmpThrd.start();
		lastHourlyTime = maxTime;
		while ((time - currentHr) >= SECS_PER_HR) {
			maxTemp = 0;
			minTemp = 9999;
			currentHr += SECS_PER_HR;
			lastHourlyTime = 0;
			doWriteHourlyTemps = true;
		}

		if (temp400 > maxTemp) {
			maxTemp = temp400;
			maxTime = time;
			doWriteHourlyTemps = true;
		}

		if (temp400 < minTemp) {
			minTemp = temp400;
			minTime = time;
			doWriteHourlyTemps = true;
		}

		if (doWriteHourlyTemps) {
			if (writeHrlyThrd != null)
				writeHrlyThrd.interrupt();
			writeHrlyThrd = new AccessDBThread(0, 0);
			writeHrlyThrd.start();
		}
		tempLog = temp400;
		timeLog = time;
	}
	
	public int getTemp400() {
		int temp400 = 0;
		int temp = getDataBitSetDecVal(dataBitSet, bitIndx_Temp, 12);
		if (temp <= 0x2BC && temp >= 0)
			temp400 = temp + 400;
		else if ((temp >= 0xE70 && temp <= 0xFFF))
			temp400 = 399 - ((temp ^ 0xFFF));
		return temp400;	
	}
	
	
	public int getDataBitSetDecVal(BitSet inBitSet, int startingoffset, int cnt) {
		int power = cnt - 1;
		int num = 0;
		for (int i = 0; i < cnt; i++) {
			num += (inBitSet.get(i + startingoffset) ? 1 : 0) * Math.pow(2, power);
			power--;
		}
		return num;
	}
	
	public short fmtEndian(short sample) {
		short sampleRL;
		short sampleRR;
		sampleRL = (short) (sample << 8);
		sampleRR = (short) ((sample >> 8) & 0xff);
		return (short) (sampleRL | sampleRR);
	}
	// getCRC(0x31, 8, 0, 0, dataBitSet.get(bitIndx_CRC_calc, lastIndx));
	public int getCRC(int poly, int deg, int start, int finalXor, BitSet crcBitSet) {
		String crcbits = "";
		int oneBits = (int) (Math.pow(2, deg) - 1);
		int crc = start;
		for (int i = 0; i < crcBits; i++) {
			int bit = crcBitSet.get(i) ? 1 : 0;
			crcbits = crcbits + new Integer(bit).toString();
			int msb = (crc >> (deg - 1)) & 1;
			crc = (crc << 1) & oneBits;
			int val = msb ^ bit;
			if (val != 0) {
				crc = crc ^ poly;
			}
		}
		//LOGGER.info(crcbits);
		return crc ^ finalXor;
	}
	
	public String getTempStr(int temp) {
		Double tmpF = new Float((temp < 400) ? 400 - temp : temp - 400) / 10.;
		tmpF = ((tmpF * 9) / 5);
		tmpF = (temp < 400) ? 32 - tmpF : tmpF + 32; 
		return String.format("%.1f", new Float(tmpF));
	}
	
	public void logDbgInfo(int logId) {
		StringBuilder sb = new StringBuilder();
		int humidity = getDataBitSetDecVal(dataBitSet, bitIndx_Humidity, 8);
		switch (logId) {
		case 1:
			sb.append("temp=" + tempLog + "=" + getTempStr(tempLog) + " time=" + timeLog);
			sb.append(" humidity=" + humidity);
			sb.append(" hex=0x");
			for (int i = 0; i < bitLen; i += 8)
				sb.append(String.format("%02X ", getDataBitSetDecVal(dataBitSet, i, 8)));
			sb.append("  sampleCnt=" + sampleCnt + " dataSetCnt=" + dataSetCnt);
			LOGGER.info(sb.toString());
			break;
		case 2:
			if (dbg) {
				sb.append("matchCnt" + matchCntStr);
				if (!notBestMatchDataSetsStr.isEmpty())
					sb.append(notBestMatchDataSetsStr);
				sb.append("\nsignal len=" + sampleCnt);
				sb.append("\nbits=");
				String tmp = "";
				for (int i = 0; i < bitLen; i++) {
					tmp = (i%4 == 0) ? " " : "";
					if (dataBitSet.get(i))
						sb.append(tmp + "1");
					else
						sb.append(tmp +"0");
				}
				sb.append("\n");
			}
			String humidityStr = Integer.toString(humidity);
			sb.append("temp=" + tempLog + "=" + getTempStr(tempLog) + "  humidity=" + humidityStr);
			sb.append("   hex=0x");
			for (int i = 0; i < bitLen; i += 8)
				sb.append(String.format("%02X ", getDataBitSetDecVal(dataBitSet, i, 8)));
			LOGGER.info(sb.toString());
			break;
		}
	}
	
	public class SprinklerThread extends Thread {
		AccessTempDB433 accessSmartPlug433;

		public SprinklerThread(String smartPlugAddr) {
			accessSmartPlug433 = new AccessTempDB433(smartPlugAddr, dbg);
		}

		public void run() {
			while (true) {
				long timeon = 10L; //10 minutes
				long sleepTime = 10L; //10 minutes
				if (LocalDateTime.now().getHour() == 11) {
					LOGGER.info("sprinkler control: high24HrHumidity=" + high24HrHumidity);
					if (high24HrHumidity < 80)
						accessSmartPlug433.turnOnSmartPlug();
					if (high24HrHumidity < 60)
						timeon = 20L; //20 minutes
					try {
						Thread.sleep(1000L * (timeon * 60L));
					} catch (InterruptedException e) {
						LOGGER.severe("SprinklerThread: InterruptedException: " + e.getMessage());
					}
					accessSmartPlug433.turnOffSmartPlug();
					high24HrHumidity = 0;
					sleepTime = 60L * 23L; // 23 hrs
				} 
				
				try {
					LOGGER.info("sprinkler control: sleepTime=" + sleepTime);
					Thread.sleep(1000L * (sleepTime * 60L)); 
				} catch (InterruptedException e) {
					LOGGER.severe("SprinklerThread: InterruptedException: " + e.getMessage());
				}
			}
		}
	}
	
	public class AccessDBThread extends Thread {
		int curTmp;
		int curTime;
		AccessTempDB433 accessTempDB433;

		public AccessDBThread(int curTime, int curTmp) {
			this.curTmp = curTmp;
			this.curTime = curTime;
			accessTempDB433 = new AccessTempDB433(httpStrRemote, dbg);
		}

		public void run() {
			if (writetoDB) {
				if (curTmp > 0)
					accessTempDB433.updateCurrentTemp(curTime, curTmp);
				else
					accessTempDB433.writeHourlyTemps(lastHourlyTime, maxTime, maxTemp, minTime, minTemp);
				if (dbg)
					LOGGER.info("accessTempDB433 curTime=" + curTime + "  curTmp=" + curTmp);
			}
		}
	}
	
	public void exitWeather433() {
		if (process != null)
			process.destroyForcibly();
		LOGGER.info("exiting Weather433");
		System.exit(0);
	}

	public static void main(String[] args) {
		new Weather433((args.length > 0) ? args[0] : "");
	}

}

