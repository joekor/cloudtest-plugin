package com.soasta.jenkins;

import java.io.PrintStream;
import java.util.List;

import com.soasta.jenkins.xstream.JUnitTestSuites;

public interface CSVResultsProcessor {


	public void setCsvResults(String csvResults);
	public void setThresholds(List<TransactionThreshold> thresholds);
	public void setJenkinsLogger(PrintStream jenkinsLogger) ;

	public boolean parse();
	public JUnitTestSuites getTestSuites();
}
