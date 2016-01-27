package com.soasta.jenkins.xstream;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("testsuite")
public class Testsuite {

	// <testsuite name="JUnitXmlReporter" errors="0" tests="0" failures="0"
	// time="0" timestamp="2013-05-24T10:23:58" />

	@XStreamAsAttribute
	private String name;
	
	@XStreamAsAttribute
	private String timestamp;
	
	@XStreamAsAttribute
	private int errors;
	
	@XStreamAsAttribute
	private int tests;
	
	@XStreamAsAttribute
	private int failures;
	
	@XStreamAsAttribute
	private int time;

	@XStreamImplicit // define list as an implicit collection
	private List<Testcase> testcases = new ArrayList<Testcase>();

	public void addTestcase(Testcase testcase) {
		testcases.add(testcase);
	}

	public List<Testcase> getTestcases() {
		return testcases;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}

	public int getTests() {
		return tests;
	}

	public void setTests(int tests) {
		this.tests = tests;
	}

	public int getFailures() {
		return failures;
	}

	public void setFailures(int failures) {
		this.failures = failures;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

}
