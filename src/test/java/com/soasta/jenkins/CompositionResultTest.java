package com.soasta.jenkins;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import com.soasta.jenkins.xstream.CompositionError;
import com.soasta.jenkins.xstream.CompositionResponse;
import com.soasta.jenkins.xstream.Composition;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import hudson.util.VersionNumber;

/**
 * <Composition>
  <Name>/Compositions/SkyGo/Validation/SkyGo_Web_DLP_Flat_2tpm</Name>
  <InstanceID>1-98543FB2-90DF-44C4-8A69-B69113A7D307</InstanceID>
  <Status>Completed</Status>
  <TotalTime>216426</TotalTime>
  <TotalErrors>0</TotalErrors>
  <Messages />
  <ResultID>4</ResultID>
  <ResultName>Result from Mon Jan 25 08:22:02 PST 2016</ResultName>
</Composition>

 * @author home
 *
 */
public class CompositionResultTest {

	String str = "<?xml version='1.0' encoding='UTF-8'?>\n" +
			"<Composition>\n" + 
			"  <Name>/Compositions/SkyGo/Validation/SkyGo_Web_DLP_Flat_2tpm</Name>\n" + 
			"  <InstanceID>1-98543FB2-90DF-44C4-8A69-B69113A7D307</InstanceID>\n" + 
			"  <Status>Completed</Status>\n" + 
			"  <TotalTime>216426</TotalTime>\n" + 
			"  <TotalErrors>0</TotalErrors>\n" + 
			"  <Messages />\n" + 
			"  <ResultID>4</ResultID>\n" + 
			"  <ResultName>Result from Mon Jan 25 08:22:02 PST 2016</ResultName>\n" + 
			"</Composition>";
	
	private String comp2 = "<?xml version='1.0' encoding='UTF-8'?>\n" + 
			"<Composition>\n" + 
			"  <Name>/Compositions/SkyGo/Validation/SkyGo_Web_DLP_Flat_2tpm</Name>\n" + 
			"  <InstanceID>1-E66D76B6-82C3-4A64-8AB2-BDC4B0F08C35</InstanceID>\n" + 
			"</Composition>";
	
	
	private String errorstr = "<?xml version='1.0' encoding='UTF-8'?>\n" +
			"<Error>\n" + 
			"  <Message>The composition '/fail' does not exist.</Message>\n" + 
			"</Error>";
	
	@Test 
    public void testParseXML() throws IOException {
    	XStream xstream = new XStream(new StaxDriver());
    	xstream.processAnnotations(CompositionResponse.class);     // inform XStream to parse annotations in Data class
    	CompositionResponse data = (CompositionResponse) xstream.fromXML(str); // parse
    	assertNotNull(data);
    	assertEquals("/Compositions/SkyGo/Validation/SkyGo_Web_DLP_Flat_2tpm", data.getName());
    	assertEquals("1-98543FB2-90DF-44C4-8A69-B69113A7D307", data.getInstanceID());
    	assertEquals("Completed", data.getStatus());
    	assertEquals("216426", data.getTotalTime());
    	assertEquals(0, data.getTotalErrors());
    	assertEquals(4, data.getResultID());
    	assertEquals("Result from Mon Jan 25 08:22:02 PST 2016", data.getResultName());
    	//assertEquals(0, data.getMessages().size());
    }

	@Test 
    public void testParseXML2() throws IOException {
    	XStream xstream = new XStream(new StaxDriver());
    	xstream.processAnnotations(Composition.class);     // inform XStream to parse annotations in Data class
    	Composition data = (Composition) xstream.fromXML(comp2); // parse
    	assertNotNull(data);
    	assertEquals("/Compositions/SkyGo/Validation/SkyGo_Web_DLP_Flat_2tpm", data.getName());
    }
    
	@Test 
    public void testParseErrorXML() throws IOException {
    	XStream xstream = new XStream(new StaxDriver());
    	xstream.processAnnotations(CompositionError.class);     // inform XStream to parse annotations in Data class
    	CompositionError data = (CompositionError) xstream.fromXML(errorstr); // parse
    	assertNotNull(data);
    	assertEquals("The composition '/fail' does not exist.", data.getMessage());
    }
    
	
}
