package com.soasta.jenkins.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

@XStreamAlias("testcase")
public class Testcase {
	@XStreamAsAttribute
	private String name;
	
	@XStreamAsAttribute
	private String classname;
	
	
	@XStreamAsAttribute
	private int time;
	
	private String failure;

	
	public Testcase(String name, String classname, int time) {
		super();
		this.name = name;
		this.classname = classname;
		this.time = time;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClassname() {
		return classname;
	}

	public void setClassname(String classname) {
		this.classname = classname;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public String getFailure() {
		return failure;
	}

	public void setFailure(String failure) {
		this.failure = failure;
	}

	
}
