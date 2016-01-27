package com.soasta.jenkins;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

public class JenkinsHttpClient {

    public static HttpClient createClient() {
    	HttpClientParams connectionParams = new HttpClientParams(); 
    	connectionParams.setConnectionManagerTimeout(1000);
        HttpClient hc = new HttpClient(connectionParams);
        hc.setConnectionTimeout(5*1000);
        Jenkins j = Jenkins.getInstance();
        ProxyConfiguration jpc = j!=null ? j.proxy : null;
        if(jpc != null) {
            hc.getHostConfiguration().setProxy(jpc.name, jpc.port);
            if(jpc.getUserName() != null)
                hc.getState().setProxyCredentials(AuthScope.ANY,new UsernamePasswordCredentials(jpc.getUserName(),jpc.getPassword()));
        }
        
        // CloudTest servers will reject the default Java user agent.
        hc.getParams().setParameter(HttpMethodParams.USER_AGENT, "Jenkins/" + Jenkins.getVersion().toString());
        
        return hc;
    }

    
}
