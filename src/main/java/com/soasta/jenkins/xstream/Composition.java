package com.soasta.jenkins.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 *
 */
@XStreamAlias("Composition") // maps Composition element in XML to this class
public class Composition {

	@XStreamAlias("Name")
	private String Name;

	@XStreamAlias("InstanceID")
	private String InstanceID;

	
	
	public Composition(String name, String instanceID) {
		super();
		this.Name = name;
		this.InstanceID = instanceID;
	}



	public String getName() {
		return Name;
	}



	public void setName(String name) {
		Name = name;
	}



	public String getInstanceID() {
		return InstanceID;
	}



	public void setInstanceID(String instanceID) {
		InstanceID = instanceID;
	}

	

}
