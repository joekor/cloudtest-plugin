/*
 * Copyright (c) 2012-2013, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.Secret;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import com.soasta.jenkins.*;
/**
 * Information about a specific CloudTest Server and access credential.
 *
 * @author Kohsuke Kawaguchi
 */
public class CloudTestServer extends AbstractDescribableImpl<CloudTestServer> {
    /**
     * URL like "http://touchtestlite.soasta.com/concerto/"
     */
    private final String url;

    private final String username;
    private final Secret password;

    private final String id;
    private final String name;

    private transient boolean generatedIdOrName;

    @DataBoundConstructor
    public CloudTestServer(String url, String username, Secret password, String id, String name) throws MalformedURLException {
        if (url == null || url.isEmpty()) {
            // This is not really a valid case, but we have to store something.
            this.url = null;
        }
        else {
            // normalization
            // TODO: can the service be running outside the /concerto/ URL?
            if (!url.endsWith("/")) url+='/';
            if (!url.endsWith("/concerto/"))
                url+="concerto/";
            this.url = url;
        }

        if (username == null || username.isEmpty()) {
          this.username = "";
        }
        else {
          this.username = username;
        }
        
        if (password == null || password.getPlainText() == null || password.getPlainText().isEmpty()) {
          this.password = null;
        }
        else {
          this.password = password;
        }

        // If the ID is empty, auto-generate one.
        if (id == null || id.isEmpty()) {
            this.id = UUID.randomUUID().toString();

            // This is probably a configuration created using
            // an older version of the plug-in (before ID and name
            // existed).  Set a flag so we can write the new
            // values after initialization (see DescriptorImpl).
            generatedIdOrName = true;
        }
        else {
            this.id = id;
        }

        // If the name is empty, default to URL + user name.
        if (name == null || name.isEmpty()) {
          if (this.url == null) {
            this.name = "";
          }
          else {
            this.name = url + " (" + username + ")";

            // This is probably a configuration created using
            // an older version of the plug-in (before ID and name
            // existed).  Set a flag so we can write the new
            // values after initialization (see DescriptorImpl).
            generatedIdOrName = true;
          }
        }
        else {
            this.name = name;
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public Secret getPassword() {
        return password;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Object readResolve() throws IOException {
        if (id != null &&
            id.trim().length() > 0 &&
            name != null &&
            name.trim().length() > 0)
            return this;

        // Either the name or ID is missing.
        // This means the config is based an older version the plug-in.

        // The constructor handles this, but XStream doesn't go
        // through the same code path (as far as I can tell).  Instead,
        // we create a new CloudTestServer object, which will include an
        // auto-generated name and ID, and return that instead.

        // When Jenkins is finished loading everything, we'll go back
        // and write the auto-generated values to disk, so this logic
        // should only execute once.  See DescriptorImpl constructor.
        LOGGER.info("Re-creating object to generate a new server ID and name.");
        return new CloudTestServer(url, username, password, id, name);
    }

    public FormValidation validate(Composition composition) throws IOException {
    	
    	return CloudTestServer.validate(this, composition);

    }

    /**
     * Retrieves the build number of this CloudTest server.
     * Postcondition: The build number returned is never null.
     */
    public VersionNumber getBuildNumber() throws IOException {
    	LOGGER.info("in getbuildnumber" );
        if (url == null) {
            // User didn't enter a value in the Configure Jenkins page.
            // Nothing we can do.
            throw new IllegalStateException("No URL has been configured for this CloudTest server.");
        }
        FormValidation validate = this.validate(null);
        if (!validate.kind.equals(Kind.OK)) {
        	throw new IllegalStateException("Unable to get Build Number from Cloud Server. " + validate.getMessage());
        }

        final String[] v = new String[1];
        try {
            HttpClient hc = createClient();
            LOGGER.info("D0" );
            GetMethod get = new GetMethod(url);
            LOGGER.info("D1" );
            hc.executeMethod(get);
            LOGGER.info("D2" );
            
            if (get.getStatusCode() != 200) {
                throw new IOException(get.getStatusLine().toString());
            }

            SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
            sp.parse(get.getResponseBodyAsStream(), new DefaultHandler() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
                    if (systemId.endsWith(".dtd"))
                        return new InputSource(new StringReader(""));
                    return null;
                }

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (qName.equals("meta")) {
                        if ("buildnumber".equals(attributes.getValue("name"))) {
                            v[0] = attributes.getValue("content");
                            throw new SAXException("found");
                        }
                    }
                }
            });
            LOGGER.warning("Build number not found in " + url);
        } catch (SAXException e) {
            if (v[0] != null)
                return new VersionNumber(v[0]);

            LOGGER.log(Level.WARNING, "Failed to load " + url, e);
        } catch (ParserConfigurationException e) {
            throw new Error(e);
        }

        // If we reach this point, then we failed to extract the build number.
        throw new IOException("Failed to extract build number from \'" +
          this.getDescriptor().getDisplayName() + "\': <" + url + ">.");
    }

    private HttpClient createClient() {
        HttpClient hc = JenkinsHttpClient.createClient();
        
        return hc;
    }

    public static CloudTestServer getByURL(String url) {
        List<CloudTestServer> servers = Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class).getServers();
        for (CloudTestServer s : servers) {
            if (s.getUrl().equals(url))
                return s;
        }
        // if we can't find any, fall back to the default one
        if (!servers.isEmpty())
            return servers.get(0);
        return null;
    }

    public static CloudTestServer getByID(String id) {
        List<CloudTestServer> servers = Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class).getServers();
        for (CloudTestServer s : servers) {
            if (s.getId().equals(id))
                return s;
        }
        // if we can't find any, fall back to the default one
        if (!servers.isEmpty())
            return servers.get(0);
        return null;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<CloudTestServer> {

        @CopyOnWrite
        private volatile List<CloudTestServer> servers;

        public DescriptorImpl() {
            load();
            if (servers == null) {
                servers = new ArrayList<CloudTestServer>();
            } else {
                // If any of the servers that we loaded was
                // missing a name or ID, and had to auto-generate
                // it, then persist the auto-generated values now.
                for (CloudTestServer s : servers) {
                    if (s.generatedIdOrName) {
                        LOGGER.info("Persisting generated server IDs and/or names.");
                        save();

                        // Calling save() once covers all servers,
                        // so we can stop looping.
                        break;
                    }
                }
            }
        }

        @Override
        public String getDisplayName() {
            return "CloudTest Server";
        }

        public List<CloudTestServer> getServers() {
            return servers;
        }

        public void setServers(Collection<? extends CloudTestServer> servers) {
            this.servers = new ArrayList<CloudTestServer>(servers);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            setServers(req.bindJSONToList(CloudTestServer.class,json.get("servers")));
            save();
            return true;
        }

        public FormValidation doValidate(@QueryParameter String url, @QueryParameter String username, @QueryParameter String password, @QueryParameter String id, @QueryParameter String name) throws IOException {
            return new CloudTestServer(url,username,Secret.fromString(password), id, name).validate(null);
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Required.");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Required.");
            } else if (!isValidURL(value)) {
                return FormValidation.error("Invalid URL syntax (did you mean http://" + value + " ?");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckUsername(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Required.");
            } else {
                return FormValidation.ok();
            }
        }

        public FormValidation doCheckPassword(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Required.");
            } else {
                return FormValidation.ok();
            }
        }

        private static boolean isValidURL(String url) {
            try {
                new URL(url);
                return true;
            }
            catch (MalformedURLException e) {
                return false;
            }
        }
    }
    

    public static FormValidation validate(CloudTestServer s, Composition composition) throws IOException {
    	
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
        
    	if (!ping(url, 5000)) {
    		return FormValidation.error("Unable to ping " + url);  
    	}
        HttpClient hc = JenkinsHttpClient.createClient();
        
        hc.setTimeout(5*1000);
        LOGGER.info("D4");
        PostMethod post = new PostMethod(url + "Login");
        post.addParameter("userName", username);
        
        if (password != null) {
          post.addParameter("password", password);
        } else {
          post.addParameter("password","");
        }

        hc.executeMethod(post);
        LOGGER.info("D5");
        // if the login succeeds, we'll see a redirect
        Header loc = post.getResponseHeader("Location");
        if (loc!=null && loc.getValue().endsWith("/Central"))
            return FormValidation.ok("Success!");

        if (!post.getResponseBodyAsString().contains("SOASTA"))
            return FormValidation.error(url+" doesn't look like a CloudTest server");

        // if it fails, the server responds with 200!
        return FormValidation.error("Invalid credentials.");
    }
    
    /**
     * Pings a HTTP URL. This effectively sends a HEAD request and returns <code>true</code> if the response code is in 
     * the 200-399 range.
     * @param url The HTTP URL to be pinged.
     * @param timeout The timeout in millis for both the connection timeout and the response read timeout. Note that
     * the total timeout is effectively two times the given timeout.
     * @return <code>true</code> if the given HTTP URL has returned response code 200-399 on a HEAD request within the
     * given timeout, otherwise <code>false</code>.
     */
    public static boolean ping(String url, int timeout) {
        url = url.replaceFirst("^https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            LOGGER.info("D6 : " + responseCode);
            
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException exception) {
        	LOGGER.info("D5 : " + url);
        	LOGGER.info(exception.getMessage());
            
            return false;
        }
    }    

    private static final Logger LOGGER = Logger.getLogger(CloudTestServer.class.getName());
}
