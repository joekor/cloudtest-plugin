package com.soasta.jenkins.xstream;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
<Error>
  <Message>The composition '/joel' does not exist.</Message>
</Error>
 *
 */
@XStreamAlias("Error") // maps Composition element in XML to this class
public class CompositionError {

	@XStreamAlias("Message")
	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}

