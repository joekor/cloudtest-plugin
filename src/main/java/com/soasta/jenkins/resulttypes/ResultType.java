package com.soasta.jenkins.resulttypes;

import com.soasta.jenkins.AverageResponseProcessor;

public enum ResultType {

	AverageResponse("Average Response Time", AverageResponseProcessor.class, "resultTransactionAverageDurations");

	private String uitext;
	private Class<?> processorClass;
	private String resultType;

	private ResultType(String uitext, Class<?> processorClass, String resultType) {
		this.uitext = uitext;
		this.processorClass = processorClass;
		// both the name of the result type to get from scommand, but also a unique identifier in the Jenkins UI
		this.resultType = resultType;
	}

	public String getUIText() {
		return this.uitext;
	}

	public String getUitext() {
		return uitext;
	}

	public Class<?> getProcessorClass() {
		return processorClass;
	}

	public String getResultType() {
		return resultType;
	}

	public static ResultType getByResultType(String resultType) {
	    for(ResultType e : values()) {
	        if(e.resultType.equals(resultType)) return e;
	    }
	    return null;
	 }

}
