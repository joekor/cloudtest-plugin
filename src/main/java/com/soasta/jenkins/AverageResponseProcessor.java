package com.soasta.jenkins;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.soasta.jenkins.xstream.JUnitTestSuites;
import com.soasta.jenkins.xstream.Testcase;
import com.soasta.jenkins.xstream.Testsuite;

public class AverageResponseProcessor implements CSVResultsProcessor {

	private static final int MINUTE_OF_TEST_COLUMN = 0;
	private static final int TIMESTAMP_COLUMN = 1;
	private String csvResults;
	private List<TransactionThreshold> thresholds;
	private JUnitTestSuites jUnitTestSuites;
	private PrintStream jenkinsLogger;

	public AverageResponseProcessor(String csvResults, List<TransactionThreshold> thresholds, PrintStream jenkinsLogger) {
		this.csvResults = csvResults;
		this.thresholds = thresholds;	
		this.jenkinsLogger = jenkinsLogger;
	}
	
	/**
	 * 
Minute of Test,Time,DLP - Sign In and Watch,DownloadTransactionsBooked,DownloadTransactionsDeleted,Login,ParentalControl
0,27-Jan-2016 14:58:00,595,30,173,347,45
1,27-Jan-2016 14:59:00,585,29,171,352,32
2,27-Jan-2016 15:00:00,452,29,152,243,27
3,27-Jan-2016 15:01:00,538,30,168,310,30
	 * @return true if no exceptions
	 */
	
	private void log(String logString) {
		LOGGER.info(logString);
		jenkinsLogger.println(logString);
	}
	
	public boolean parse() {
		jUnitTestSuites = new JUnitTestSuites();
		CSVParser parser;
		try {
			// We have to loop through one row at a time, which is one row per minute.
			// So treat each row as a test suite, and each column, if there is a matching defined threshold, 
			// as a test. 
			parser = CSVParser.parse(csvResults, CSVFormat.EXCEL.withHeader());
			headerMap = parser.getHeaderMap();
			for (String key : headerMap.keySet()) {
				LOGGER.info(key + " : " + headerMap.get(key));
			}
			
			// loop through, and validate the thresholds we are interested in are a subset the headers received.
			// If not, log and remove that threshold.
			for (Iterator<TransactionThreshold> iterator = thresholds.iterator(); iterator.hasNext();) {
				TransactionThreshold threshold = iterator.next();
				if (!headerMap.containsKey(threshold.getTransactionname())) {
			        // Remove the current element from the iterator and the list.
			        iterator.remove();
			        log("WARNING: " + threshold.getTransactionname() + " not found in results from composition.");
			    }
			}
			
			
			for (CSVRecord csvRecord : parser) {
				rowCount++;
				LOGGER.info(csvRecord.getRecordNumber()+"");
				Testsuite testsuite = new Testsuite();
				jUnitTestSuites.addTestsuite(testsuite);
				// Loop through all the defined thresholds from the Jenkins Plugin UI.
				for (TransactionThreshold threshold : thresholds) {
					// If there is a matching transaction name defined, compared to the results from the test, then continue.
					String value = csvRecord.get(threshold.getTransactionname());
					if (value != null) {
						testsuite.incrementCount();
						testsuite.setTimestamp(csvRecord.get(TIMESTAMP_COLUMN));
						String testName = "Minute: " + csvRecord.get(MINUTE_OF_TEST_COLUMN);
						Testcase testcase = new Testcase(testName, threshold.getTransactionname(), 0 );
						if (!validateTest(csvRecord, threshold)) {
							testsuite.incrementFailures();
							testcase.setFailure(threshold.getThresholdname().trim() + " " + value.trim() + " not within defined bounds of " + threshold.getThresholdminvalue() + " - " + threshold.getThresholdmaxvalue());
						}
						testsuite.addTestcase(testcase);
						System.out.println(testcase);
						LOGGER.info(threshold.getTransactionname() + " : " + value);
						
					}
				}
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false; 
		}
		return true;
		 
		
	}

	private boolean validateTest(CSVRecord csvRecord, TransactionThreshold threshold) {
		// TODO Auto-generated method stub
		String value = csvRecord.get(threshold.getTransactionname());
		float floatval = Float.parseFloat(value);
		float min = Float.parseFloat(threshold.getThresholdminvalue());
		float max = Float.parseFloat(threshold.getThresholdmaxvalue());
		
		return floatval > min && floatval < max;
	}

	private static final Logger LOGGER = Logger.getLogger(AverageResponseProcessor.class.getName());
	private Map<String, Integer> headerMap;
	private int rowCount;


	public int getRowCount() {
		// TODO Auto-generated method stub
		return rowCount;
	}

	public JUnitTestSuites getTestSuites() {
		// TODO Auto-generated method stub
		return jUnitTestSuites;
	}

	public Map<String, Integer> getHeaderMap() {
		// TODO Auto-generated method stub
		return headerMap;
	}

}
