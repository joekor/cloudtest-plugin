package com.soasta.jenkins;


import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.soasta.jenkins.xstream.CompositionResponse;

import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.VersionNumber;
import jenkins.model.Jenkins;

public class Composition extends AbstractDescribableImpl<Composition>  {


    private final String id;
	private final String name;
    private final String username;
    private final Secret password;
    private final String url;
    private CompositionResponse response;
   
    	
    private transient boolean generatedIdOrName;
    
    /**
     * filled by the jenkins plugin, when the user enters data in the UI 
     */
    @DataBoundConstructor
    public Composition(String name, String url, String username, Secret password, String id ) throws MalformedURLException {
		this.name = name;
		this.username    = username;
		this.password    = password;
		
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

    }

    public String getUrl() {
        return url;
    }
    
    public String getName() {
		return name;
	}

    public String getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public Secret getPassword() {
		return password;
	}
	
    public CompositionResponse getResponse() {
		return response;
	}

    public void setResponse(CompositionResponse response) {
		this.response = response;
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
        // we create a new Composition object, which will include an
        // auto-generated name and ID, and return that instead.

        // When Jenkins is finished loading everything, we'll go back
        // and write the auto-generated values to disk, so this logic
        // should only execute once.  See DescriptorImpl constructor.
        LOGGER.info("Re-creating object to generate a new server ID and name.");
        return new Composition(name, url, username, password, id );
    }

    public FormValidation validate() throws IOException {
    	
        HttpClient hc = createClient();

        PostMethod post = new PostMethod(url + "Login");
        post.addParameter("userName",getUsername());
        
        if (getPassword() != null) {
//          post.addParameter("password",getPassword());
          post.addParameter("password",getPassword().getPlainText());
        } else {
          post.addParameter("password","");
        }

        hc.executeMethod(post);

        // if the login succeeds, we'll see a redirect
        Header loc = post.getResponseHeader("Location");
        if (loc!=null && loc.getValue().endsWith("/Central"))
            return FormValidation.ok("Success!");

        if (!post.getResponseBodyAsString().contains("SOASTA"))
            return FormValidation.error(getUrl()+" doesn't look like a CloudTest server");

        // if it fails, the server responds with 200!
        return FormValidation.error("Invalid credentials.");
    }

    /**
     * Retrieves the build number of this CloudTest server.
     * Postcondition: The build number returned is never null.
     */
    public VersionNumber getBuildNumber() throws IOException {
        if (url == null) {
            // User didn't enter a value in the Configure Jenkins page.
            // Nothing we can do.
            throw new IllegalStateException("No URL has been configured for this CloudTest server.");
        }

        final String[] v = new String[1];
        try {
            HttpClient hc = createClient();
            
            GetMethod get = new GetMethod(url);
            hc.executeMethod(get);
            
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

    
    @Extension
    public static class DescriptorImpl extends Descriptor<Composition> {


        @Override
        public String getDisplayName() {
            return "";
        }

  
        public FormValidation doValidate(@QueryParameter String url, @QueryParameter String username, @QueryParameter String password, @QueryParameter String id, @QueryParameter String name) throws IOException {
            return new Composition(name, url, username, Secret.fromString(password), id ).validate();
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

        public FormValidation doCheckId(@QueryParameter String value) {
            return FormValidation.ok();
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

    
    private static final Logger LOGGER = Logger.getLogger(Composition.class.getName());

}