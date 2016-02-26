package com.soasta.jenkins;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.soasta.jenkins.xstream.JUnitTestSuites;
import com.thoughtworks.xstream.XStream;

import groovy.ui.SystemOutputInterceptor;

public class CSVResultsProcessorTest {

	private String testStr = "Minute of Test,Time,DLP - Sign In and Watch,DownloadTransactionsBooked,DownloadTransactionsDeleted,Login,ParentalControl\n" + 
			"0,27-Jan-2016 14:58:00,595,30,173,347,45\n" + 
			"1,27-Jan-2016 14:59:00,585,29,171,352,32\n" + 
			"2,27-Jan-2016 15:00:00,452,29,152,243,27\n" + 
			"3,27-Jan-2016 15:01:00,538,30,168,310,30";
	
	private String smallTestStr = "Minute of Test,Time,PC\n" + 
			"0,27-Jan-2016 14:58:00,45";
	
	private String junitPassXML = "<testsuites>\n" + 
			"  <testsuite timestamp=\"27-Jan-2016 14:58:00\" errors=\"0\" tests=\"1\" failures=\"0\" time=\"0\">\n" + 
			"    <testcase name=\"Minute: 0\" classname=\"PC\" time=\"0\"/>\n" + 
			"  </testsuite>\n" + 
			"</testsuites>";
	
	private String junitFailXML = "<testsuites>\n" + 
			"  <testsuite timestamp=\"27-Jan-2016 14:58:00\" errors=\"0\" tests=\"1\" failures=\"1\" time=\"0\">\n" + 
			"    <testcase name=\"Minute: 0\" classname=\"PC\" time=\"0\">\n" + 
			"      <failure>Average Response 45 not within defined bounds of 10 - 20</failure>\n" + 
			"    </testcase>\n" + 
			"  </testsuite>\n" + 
			"</testsuites>";
	@Test
	public void testParse() {
		List<TransactionThreshold> thresholds = new ArrayList<TransactionThreshold>();
		
		AverageResponseProcessor resultsProcessor = new AverageResponseProcessor(testStr, thresholds, System.out);
		resultsProcessor.parse();
				
		assertEquals(7, resultsProcessor.getHeaderMap().size());
		assertEquals("rows imported", 4, resultsProcessor.getRowCount());
		JUnitTestSuites junitTestSuites = resultsProcessor.getTestSuites();
		assertEquals(junitTestSuites.getTestsuites().size(),resultsProcessor.getRowCount()); 
	}

	@Test
	public void testParseWithPassingThresholds() {
		List<TransactionThreshold> thresholds = new ArrayList<TransactionThreshold>();
		thresholds.add(new TransactionThreshold("DownloadTransactionsBooked", "Average Response", "10", "50", ""));
		AverageResponseProcessor resultsProcessor = new AverageResponseProcessor(testStr, thresholds, System.out);
		resultsProcessor.parse();
				
		assertEquals(7, resultsProcessor.getHeaderMap().size());
		assertEquals("rows imported", 4, resultsProcessor.getRowCount());
		JUnitTestSuites junitTestSuites = resultsProcessor.getTestSuites();
		assertEquals(junitTestSuites.getTestsuites().size(),resultsProcessor.getRowCount()); 
		assertEquals(1, junitTestSuites.getTestsuites().get(0).getTestcases().size());
		assertEquals(null, junitTestSuites.getTestsuites().get(0).getTestcases().get(0).getFailure());

	}
	
	@Test
	public void testParseWithFailingThresholds() {
		List<TransactionThreshold> thresholds = new ArrayList<TransactionThreshold>();
		thresholds.add(new TransactionThreshold("DLP - Sign In and Watch", "Average Response", "10", "50", ""));
		AverageResponseProcessor resultsProcessor = new AverageResponseProcessor(testStr, thresholds, System.out);
		resultsProcessor.parse();
				
		assertEquals(7, resultsProcessor.getHeaderMap().size());
		assertEquals("rows imported", 4, resultsProcessor.getRowCount());
		JUnitTestSuites junitTestSuites = resultsProcessor.getTestSuites();
		assertEquals(junitTestSuites.getTestsuites().size(),resultsProcessor.getRowCount()); 
		assertEquals(1, junitTestSuites.getTestsuites().get(0).getTestcases().size());
		assertEquals("Average Response 595 not within defined bounds of 10 - 50", junitTestSuites.getTestsuites().get(0).getTestcases().get(0).getFailure());

	}
	
	@Test
	public void testParseWithPassingThresholdsXML() {
		List<TransactionThreshold> thresholds = new ArrayList<TransactionThreshold>();
		thresholds.add(new TransactionThreshold("PC", "Average Response", "40", "50", ""));
		AverageResponseProcessor resultsProcessor = new AverageResponseProcessor(smallTestStr, thresholds, System.out);
		resultsProcessor.parse();
				
		assertEquals(3, resultsProcessor.getHeaderMap().size());
		assertEquals("rows imported", 1, resultsProcessor.getRowCount());
		JUnitTestSuites junitTestSuites = resultsProcessor.getTestSuites();
		assertEquals(junitTestSuites.getTestsuites().size(),resultsProcessor.getRowCount()); 
		assertEquals(1, junitTestSuites.getTestsuites().get(0).getTestcases().size());
		assertEquals(null, junitTestSuites.getTestsuites().get(0).getTestcases().get(0).getFailure());

		XStream xstream = jenkins.model.Jenkins.XSTREAM;
		xstream.processAnnotations(JUnitTestSuites.class);

		// Object to XML Conversion
		String xml = xstream.toXML(junitTestSuites);
		assertEquals(junitPassXML, xml);
	

	}
	
	@Test
	public void testParseWithFailingThresholdsXML() {
		List<TransactionThreshold> thresholds = new ArrayList<TransactionThreshold>();
		thresholds.add(new TransactionThreshold("PC", "Average Response", "10", "20", ""));
		AverageResponseProcessor resultsProcessor = new AverageResponseProcessor(smallTestStr, thresholds, System.out);
		resultsProcessor.parse();
				
		assertEquals(3, resultsProcessor.getHeaderMap().size());
		assertEquals("rows imported", 1, resultsProcessor.getRowCount());
		JUnitTestSuites junitTestSuites = resultsProcessor.getTestSuites();
		assertEquals(junitTestSuites.getTestsuites().size(),resultsProcessor.getRowCount()); 
		assertEquals(1, junitTestSuites.getTestsuites().get(0).getTestcases().size());
		assertEquals("Average Response 45 not within defined bounds of 10 - 20", junitTestSuites.getTestsuites().get(0).getTestcases().get(0).getFailure());
		XStream xstream = jenkins.model.Jenkins.XSTREAM;
		xstream.processAnnotations(JUnitTestSuites.class);

		// Object to XML Conversion
		String xml = xstream.toXML(junitTestSuites);
		assertEquals(junitFailXML, xml);
	}	
	
}
