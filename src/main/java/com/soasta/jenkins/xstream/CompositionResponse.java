package com.soasta.jenkins.xstream;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 *
 */
@XStreamAlias("Composition") // maps Composition element in XML to this class
public class CompositionResponse {

	@XStreamAlias("Name")
	private String name;
	
	@XStreamAlias("InstanceID")
	private String instanceID;

	@XStreamAlias("Status")
	private String status;

	@XStreamAlias("TotalTime")
	private String totalTime;

	@XStreamAlias("TotalErrors")
	private int totalErrors;
	
    //@XStreamImplicit(itemFieldName="Messages")
	//@XStreamOmitField
	
	@XStreamAlias("Messages")	
    private List<CompositionResponseMessage> messages = new ArrayList<CompositionResponseMessage>();
    

	//@XStreamAlias("Messages")
    //private List messages = new ArrayList();
    
	@XStreamAlias("ResultID")
	private int resultID;

	@XStreamAlias("ResultName")
	private String resultName;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInstanceID() {
		return instanceID;
	}

	public void setInstanceID(String instanceID) {
		this.instanceID = instanceID;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(String totalTime) {
		this.totalTime = totalTime;
	}

	public int getTotalErrors() {
		return totalErrors;
	}

	public void setTotalErrors(int totalErrors) {
		this.totalErrors = totalErrors;
	}


	public List<CompositionResponseMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<CompositionResponseMessage> messages) {
		this.messages = messages;
	}

	public int getResultID() {
		return resultID;
	}

	public void setResultID(int resultID) {
		this.resultID = resultID;
	}

	public String getResultName() {
		return resultName;
	}

	public void setResultName(String resultName) {
		this.resultName = resultName;
	}
	
	
}
//
//class Messages {
//
//
//	}

