package com.soasta.jenkins;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.soasta.jenkins.xstream.JUnitTestSuites;
import com.soasta.jenkins.xstream.Testsuite;

public class CSVResultsProcessor {

	private String csvResults;
	private String resultsDir;
	private List<TransactionThreshold> thresholds;
	private JUnitTestSuites jUnitTestSuites;
	private HashMap<String, TransactionThreshold> thresholdsMap = new HashMap<String, TransactionThreshold>();

	public CSVResultsProcessor(String csvResults, String resultsDir, List<TransactionThreshold> thresholds) {
		// TODO Auto-generated constructor stub
		this.csvResults = csvResults;
		this.resultsDir = resultsDir;
		this.thresholds = thresholds;	
		
		for (TransactionThreshold threshold : thresholds) {
			thresholdsMap.put(threshold.getTransactionname(), threshold);
		}
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
			
			for (CSVRecord csvRecord : parser) {
				rowCount++;
				LOGGER.info(csvRecord.getRecordNumber()+"");
				Testsuite testsuite = new Testsuite();
				jUnitTestSuites.addTestsuite(testsuite);
				// Loop through all the defined thresholds from the Jenkins Plugin UI.
				for (TransactionThreshold threshold : thresholds) {
					// If there is a matching transaction defined, compared to the results from the test, then continue.
					
					String value = csvRecord.get(threshold.getTransactionname());
					if (value != null) {
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

	private static final Logger LOGGER = Logger.getLogger(CSVResultsProcessor.class.getName());
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
