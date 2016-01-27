package com.soasta.jenkins.xstream;


import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * <?xml version='1.0' encoding='UTF-8'?>
<Composition>
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
@XStreamAlias("Message") // maps Composition element in XML to this class
public class CompositionResponseMessage {

	
}


