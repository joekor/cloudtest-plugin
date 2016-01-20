/*
 * Copyright (c) 2012-2013, CloudBees, Inc., SOASTA, Inc.
 * All Rights Reserved.
 */
package com.soasta.jenkins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.LocalLauncher;
import hudson.model.AbstractBuild;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestDataPublisher;
import hudson.util.ArgumentListBuilder;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import hudson.util.QuotedStringTokenizer;
import jenkins.model.Jenkins;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestCompositionRunner extends AbstractSCommandBuilder {
	/**
	 * Composition to execute.
	 */
	private List<Composition> compositions;
	private final boolean deleteOldResults;
	private final int maxDaysOfResults;
	private final String additionalOptions;
	private final List<TransactionThreshold> thresholds;
	private final boolean generatePlotCSV;

	@DataBoundConstructor
	public TestCompositionRunner(String url, String cloudTestServerID, List<Composition> compositions,
			DeleteOldResultsSettings deleteOldResults, String additionalOptions, List<TransactionThreshold> thresholds,
			boolean generatePlotCSV) {
		super(url, cloudTestServerID);
		this.compositions = compositions;
		this.deleteOldResults = (deleteOldResults != null);
		this.maxDaysOfResults = (deleteOldResults == null ? 0 : deleteOldResults.maxDaysOfResults);
		this.additionalOptions = additionalOptions;
		this.thresholds = thresholds;
		this.generatePlotCSV = generatePlotCSV;
	}

	public List<TransactionThreshold> getThresholds() {
		return thresholds;
	}

	public List<Composition> getCompositions() {
		return compositions;
	}

	public boolean getDeleteOldResults() {
		return deleteOldResults;
	}

	public int getMaxDaysOfResults() {
		return maxDaysOfResults;
	}

	public String getAdditionalOptions() {
		return additionalOptions;
	}

	public boolean getGeneratePlotCSV() {
		return generatePlotCSV;
	}

	public Object readResolve() throws IOException {
		if (getCloudTestServerID() != null)
			return this;

		// We don't have a server ID.
		// This means the builder config is based an older version the plug-in.

		// Look up the server by URL instead.
		// We'll use the ID going forward.
		CloudTestServer s = CloudTestServer.getByURL(getUrl());

		LOGGER.info("Matched server URL " + getUrl() + " to ID: " + s.getId() + "; re-creating.");

		return new TestCompositionRunner(getUrl(), s.getId(), compositions,
				deleteOldResults ? new DeleteOldResultsSettings(maxDaysOfResults) : null, additionalOptions, thresholds,
				generatePlotCSV);
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		
		LOGGER.info("in perform");
		LOGGER.info("compositions size : " + compositions.size());
		

		// set up for running compositions in parallel
		ExecutorService executor = Executors.newWorkStealingPool();
		List<Callable<Integer>> callables = new ArrayList<Callable<Integer>>();

		// Create a unique sub-directory to store all test results.
		String resultsDir = "." + getClass().getName();

		// Split by newline.
		EnvVars envs = build.getEnvironment(listener);
		// String[] compositions =
		// envs.expand(this.composition).split("[\r\n]+");
		String additionalOptionsExpanded = additionalOptions == null ? null : envs.expand(additionalOptions);
		String[] options = additionalOptionsExpanded == null ? null
				: new QuotedStringTokenizer(additionalOptionsExpanded).toArray();

		LOGGER.info("0");
		for (Composition composition : compositions) {
			ArgumentListBuilder args = getSCommandArgs(build, listener);
			LOGGER.info("1");
			
			// if thresholds are included in this post-build action, add them to
			// scommand arguments
			if (thresholds != null) {
				displayTransactionThreholds(listener.getLogger());

				for (TransactionThreshold threshold : thresholds) {
					args.add("validation=" + threshold.toScommandString());
				}
			}
			LOGGER.info("2");


			// Add the additional options to the composition if there are any.
			if (options != null) {
				args.add(options);
			}

			if (generatePlotCSV) {
				args.add("outputthresholdcsvdir=" + build.getWorkspace());
			}

			LOGGER.info("adding new callable to list");
			
			callables.add(callable(build, resultsDir, composition, launcher, listener, args));

		}

		LOGGER.info("about to invoke all");

		executor.invokeAll(callables).stream().map(future -> {
			try {
				return future.get();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}).forEach(System.out::println);

		// Now that we've finished running all the compositions, pass
		// the results directory off to the JUnit archiver.
		String resultsPattern = resultsDir + "/**/*.xml";
		JUnitResultArchiver archiver = new JUnitResultArchiver(resultsPattern, true,
				new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(Saveable.NOOP,
						Collections.singleton(new JunitResultPublisher(null))));
		return archiver.perform(build, launcher, listener);
	}

	Callable<Integer> callable(AbstractBuild<?, ?> build, String resultsDir, Composition composition, Launcher launcher,
			BuildListener listener, ArgumentListBuilder args) {
		return () -> {

			LOGGER.info("in callable thing");

			args.add("cmd=play", "wait", "format=junitxml").add("name=" + composition.getName());

			String fileName = composition + ".xml";

			// Strip off any leading slash characters (composition names
			// will typically be the full CloudTest folder path).
			if (fileName.startsWith("/")) {
				fileName = fileName.substring(1);
			}

			// Put the file in the test results directory.
			fileName = resultsDir + File.separator + fileName;

			FilePath xml = new FilePath(build.getWorkspace(), fileName);

			// Make sure the directory exists.
			xml.getParent().mkdirs();

			// Run it!
			int exitCode = launcher.launch().cmds(args).pwd(build.getWorkspace()).stdout(xml.write())
					.stderr(listener.getLogger()).join();

			if (xml.length() == 0) {
				// SCommand did not produce any output.
				// This should never happen, but just in case...
				return -1;
			}

			// if (deleteOldResults) {
			// // Run SCommand again to clean up the old results.
			// args = getSCommandArgs(build, listener);
			//
			// args.add("cmd=delete", "type=result")
			// .add("path=" + composition)
			// .add("maxage=" + maxDaysOfResults);
			//
			// launcher
			// .launch()
			// .cmds(args)
			// .pwd(build.getWorkspace())
			// .stdout(listener)
			// .stderr(listener.getLogger())
			// .join();
			// }

			return exitCode;
		};
	}

	private void displayTransactionThreholds(PrintStream jenkinsLogger) {
		String THRESHOLD_TABLE_FORMAT = "%-15s %-20s %7s";
		jenkinsLogger.println("~");
		jenkinsLogger.println("Custom Transaction Threholds:");
		for (TransactionThreshold threshold : thresholds) {
			String formattedString = String.format(THRESHOLD_TABLE_FORMAT, threshold.getTransactionname(),
					threshold.getThresholdname(), threshold.getThresholdvalue());
			jenkinsLogger.println(formattedString);
		}
		jenkinsLogger.println("~");
	}

	@Extension
	public static class DescriptorImpl extends AbstractCloudTestBuilderDescriptor {
		@Override
		public String getDisplayName() {
			return "Play Composition(s)";
		}

		/**
		 * Called automatically by Jenkins whenever the "maxDaysOfResults" field
		 * is modified by the user.
		 * 
		 * @param value
		 *            the new maximum age, in days.
		 */
		public FormValidation doCheckMaxDaysOfResults(@QueryParameter String value) {
			if (value == null || value.trim().isEmpty()) {
				return FormValidation.error("Days to keep results is required.");
			} else {
				try {
					int maxDays = Integer.parseInt(value);

					if (maxDays <= 0) {
						return FormValidation.error("Value must be > 0.");
					} else {
						return FormValidation.ok();
					}
				} catch (NumberFormatException e) {
					return FormValidation.error("Value must be numeric.");
				}
			}
		}

		public AutoCompletionCandidates doAutoCompleteComposition(@QueryParameter String cloudTestServerID)
				throws IOException, InterruptedException {
			CloudTestServer s = CloudTestServer.getByID(cloudTestServerID);

			ArgumentListBuilder args = new ArgumentListBuilder();
			args.add(install(s)).add("list", "type=composition").add("url=" + s.getUrl())
					.add("username=" + s.getUsername());

			if (s.getPassword() != null)
				args.addMasked("password=" + s.getPassword());

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int exit = new LocalLauncher(TaskListener.NULL).launch().cmds(args).stdout(out).join();
			if (exit == 0) {
				BufferedReader r = new BufferedReader(new StringReader(out.toString()));
				AutoCompletionCandidates a = new AutoCompletionCandidates();
				String line;
				while ((line = r.readLine()) != null) {
					if (line.endsWith("object(s) found."))
						continue;
					a.add(line);
				}
				return a;
			}
			return new AutoCompletionCandidates(); // no candidate
		}

		private synchronized FilePath install(CloudTestServer s) throws IOException, InterruptedException {
			SCommandInstaller sCommandInstaller = new SCommandInstaller(s);
			return sCommandInstaller.scommand(Jenkins.getInstance(), TaskListener.NULL);
		}
	}

	public static class DeleteOldResultsSettings {
		private final int maxDaysOfResults;

		@DataBoundConstructor
		public DeleteOldResultsSettings(int maxDaysOfResults) {
			this.maxDaysOfResults = maxDaysOfResults;
		}

		public int getMaxDaysOfResults() {
			return maxDaysOfResults;
		}
	}

	private static final Logger LOGGER = Logger.getLogger(TestCompositionRunner.class.getName());
}
