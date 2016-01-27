package com.soasta.jenkins;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.soasta.jenkins.xstream.JUnitTestSuites;

public class CSVResultsProcessorTest {

	private String testStr = "Minute of Test,Time,DLP - Sign In and Watch,DownloadTransactionsBooked,DownloadTransactionsDeleted,Login,ParentalControl\n" + 
			"0,27-Jan-2016 14:58:00,595,30,173,347,45\n" + 
			"1,27-Jan-2016 14:59:00,585,29,171,352,32\n" + 
			"2,27-Jan-2016 15:00:00,452,29,152,243,27\n" + 
			"3,27-Jan-2016 15:01:00,538,30,168,310,30";
	
	@Test
	public void testParse() {
		List<TransactionThreshold> thresholds = new ArrayList<TransactionThreshold>();
		
		CSVResultsProcessor resultsProcessor = new CSVResultsProcessor(testStr, "", thresholds);
		resultsProcessor.parse();
				
		assertEquals(7, resultsProcessor.getHeaderMap().size());
		assertEquals("rows imported", 4, resultsProcessor.getRowCount());
		JUnitTestSuites junitTestSuites = resultsProcessor.getTestSuites();

	}
}
