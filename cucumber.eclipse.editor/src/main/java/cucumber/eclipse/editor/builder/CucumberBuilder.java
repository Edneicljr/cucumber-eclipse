package cucumber.eclipse.editor.builder;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import cucumber.eclipse.editor.steps.ExtensionRegistryStepProvider;
import cucumber.eclipse.steps.integration.StepPreferences;
import cucumber.eclipse.steps.integration.marker.MarkerFactory;

/**
 * Incremental builder of step definitions.
 * 
 * This is a generic builder independent of the language used to create the step
 * definitions.
 * 
 * The step definitions detection is delegated to IStepDefinition provides by
 * external plug-ins.
 * 
 * This builder DOES NOT MAINTAIN glue, see {#CucumberGherkinBuilder} for this
 * job.
 * 
 * @author qvdk
 *
 */
public class CucumberBuilder extends IncrementalProjectBuilder {

	public static final String ID = "cucumber.eclipse.builder.stepdefinition";

	private final ExtensionRegistryStepProvider stepDefinitionsProvider = ExtensionRegistryStepProvider.INSTANCE;
	private MarkerFactory markerFactory = new MarkerFactory();

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		if (!StepPreferences.INSTANCE.isCheckStepDefinitionsEnabled()) {
			return null;
		}
		switch (kind) {
		case FULL_BUILD:
			System.out.println("step definitions full build");
			fullBuild(markerFactory, monitor);
			break;
		case AUTO_BUILD:
		case INCREMENTAL_BUILD:
			IResourceDelta delta = getDelta(getProject());
			System.out.println("step definitions incremental build on " + delta.getResource().getName());
			incrementalBuild(delta, markerFactory, monitor);
			break;
		case CLEAN_BUILD:
			System.out.println("clean build");
			break;
		default:
			break;
		}

		return null;
	}

	protected void fullBuild(MarkerFactory markerFactory, final IProgressMonitor monitor) throws CoreException {
		try {
			stepDefinitionsProvider.clean();
			getProject().accept(new CucumberStepDefinitionsFullBuildVisitor(markerFactory, monitor));
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	protected void incrementalBuild(IResourceDelta delta, MarkerFactory markerFactory, IProgressMonitor monitor)
			throws CoreException {
		try {
			// the visitor does the work.
			delta.accept(new CucumberStepDefinitionsIncrementalBuildVisitor(markerFactory, monitor));
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	class CucumberStepDefinitionsFullBuildVisitor implements IResourceVisitor {

		private IProgressMonitor monitor;
		private MarkerFactory markerFactory;

		public CucumberStepDefinitionsFullBuildVisitor(MarkerFactory markerFactory, IProgressMonitor monitor) {
			this.monitor = monitor;
			this.markerFactory = markerFactory;
		}

		@Override
		public boolean visit(IResource resource) throws CoreException {
			// System.out.println("CucumberFullBuildVisitor is building " +
			// resource.getName());
			// build the specified resource.
			// return true to continue visiting children.

			this.markerFactory.cleanMarkers(resource);
			stepDefinitionsProvider.findStepDefinitions(resource, markerFactory, monitor);

			return true;
		}

	}

	class CucumberStepDefinitionsIncrementalBuildVisitor implements IResourceDeltaVisitor {

		private IProgressMonitor monitor;
		private MarkerFactory markerFactory;

		public CucumberStepDefinitionsIncrementalBuildVisitor(MarkerFactory markerFactory, IProgressMonitor monitor) {
			this.monitor = monitor;
			this.markerFactory = markerFactory;
		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
//			System.out.println("CucumberIncrementalBuildVisitor is building " + delta.getResource().getName());
			IResource resource = delta.getResource();
			this.markerFactory.cleanMarkers(resource);
			stepDefinitionsProvider.findStepDefinitions(resource, markerFactory, monitor);

			return true;
		}

	}

	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		System.out.println("CucumberBuilder.clean");
		this.stepDefinitionsProvider.clean();
	}
}
