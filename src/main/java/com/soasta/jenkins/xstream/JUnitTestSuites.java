package com.soasta.jenkins.xstream;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("testsuites") // maps Composition element in XML to this class
public class JUnitTestSuites {

	@XStreamImplicit // define list as an implicit collection
	private List<Testsuite> testsuites = new ArrayList<Testsuite>();

	public void addTestsuite(Testsuite testsuite) {
		testsuites.add(testsuite);
	}

	public List<Testsuite> getTestsuites() {
		return testsuites;
	}
}




//<testcase classname="JUnitXmlReporter.constructor" name="should default path to an empty string" time="0.006">
//<failure message="test failure">Assertion failed</failure>
//</testcase>

