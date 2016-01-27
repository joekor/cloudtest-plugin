/*
 * Copyright (c) 2013, SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import hudson.AbortException;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;

public abstract class AbstractSCommandBuilder extends Builder {
    /**
     * URL of the server to use (deprecated).
     */
    private final String url;
    /**
     * ID of the server to use.
     * @see CloudTestServer
     */
    private final String cloudTestServerID;

    public AbstractSCommandBuilder(String url, String cloudTestServerID) {
        this.url = url;
        this.cloudTestServerID = cloudTestServerID;
    }

    public CloudTestServer getServer() {
        return CloudTestServer.getByID(cloudTestServerID);
    }

    public String getUrl() {
        return url;
    }

    public String getCloudTestServerID() {
        return cloudTestServerID;
    }

    protected ArgumentListBuilder getSCommandArgs(AbstractBuild<?, ?> build, BuildListener listener, Composition composition) throws IOException, InterruptedException {
      CloudTestServer s = getServer();
      if (s == null)
          throw new AbortException("No TouchTest server is configured in the system configuration.");
  
      LOGGER.info("A0");
      
      // Download SCommand, if needed.
      FilePath scommand = new SCommandInstaller(s).scommand(build.getBuiltOn(), listener);
      LOGGER.info("A1");
      
      // if composition is not null, and has a username and password, then use them. 
      // If not, use the one in the cloud test server
      String username = s.getUsername();
      if (composition!= null && composition.getUsername() != null && composition.getUsername().length() > 0) {
    	  username = composition.getUsername();
      }
      
      String password = s.getPassword().getPlainText();
      if (composition!= null && composition.getPassword() != null && composition.getPassword().length() > 0) {
    	  password = composition.getPassword();
      }
      
      String url = s.getUrl();
      if (composition!= null && composition.getUrl() != null && composition.getUrl().length() > 0) {
    	  url = composition.getUrl();
      }

      ArgumentListBuilder args = new ArgumentListBuilder();
      args.add(scommand)
          .add("url=" + url)
          .add("username="+username);
          
      if (s.getPassword() != null)
          args.addMasked("password=" + password);
      
      ProxyConfiguration proxyConfig = Jenkins.getInstance().proxy;

      if (proxyConfig != null && proxyConfig.name != null) {
          // Jenkins is configured to use a proxy server.

          // Extract the destination CloudTest host.
          String host = new URL(s.getUrl()).getHost();

          // Check if the proxy applies for this destination host.
          // This code is more or less copied from ProxyConfiguration.createProxy() :-(.
          boolean isNonProxyHost = false;
          for (Pattern p : proxyConfig.getNoProxyHostPatterns()) {
              if (p.matcher(host).matches()) {
                  // It's a match.
                  // Don't use the proxy.
                  isNonProxyHost = true;

                  // No need to continue checking the list.
                  break;
              }
          }

          if (!isNonProxyHost) {
              // Add the SCommand proxy parameters.
              args.add("httpproxyhost=" + proxyConfig.name)
                  .add("httpproxyport=" + proxyConfig.port);

              // If there are proxy credentials, add those too.
              if (proxyConfig.getUserName() != null) {
                  args.add("httpproxyusername=" + proxyConfig.getUserName())
                      .addMasked("httpproxypassword=" + proxyConfig.getPassword());
              }
          }
      }

      return args;
    }
    
	private static final Logger LOGGER = Logger.getLogger(AbstractSCommandBuilder.class.getName());

}
