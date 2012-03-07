package edu.illinois.concurrentaccessview.analysis;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import sabazios.A;
import sabazios.domains.ConcurrentAccesses;
import sabazios.wala.WalaAnalysis;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

import edu.illinois.concurrentaccessview.views.ConcurrentAccessView;

/**
 * A helper class used to run the data race analyses.
 */
public class DataRaceAnalysis extends WalaAnalysis {

	/**
	 * The entry class that was used in the last analysis.
	 */
	public static String lastEntryClass = null;
	
	/**
	 * The entry method that was used in the last analysis.
	 */
	public static String lastEntryMethod = null;

	/**
	 * Run the given analysis and display the results.
	 * @param analysis The data race analysis object
	 * @param file The file containing the analyzed code
	 * @param entryClass The entry class for the analysis
	 * @param entryMethod The entry method for the analysis
	 */
	public static void runAnalysis(
			final DataRaceAnalysis analysis,
			final IFile file,
			final String entryClass,
			final String entryMethod) {
		
		// Create the job.
		Job job = new Job("Data Race Analysis Job") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				// Run the analysis.
				final ConcurrentAccesses<?> ca = analysis.findConcurrentAccesses(entryClass, entryMethod);
				
				// Open the concurrent access view once the analysis is done
				// and results are returned.
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						try {
							IViewPart view = activePage.showView("edu.illinois.concurrentaccessview.views.ConcurrentAccessView");
							if (view != null && view instanceof ConcurrentAccessView) {
								ConcurrentAccessView caView = (ConcurrentAccessView) view;
								caView.setFile(file);
								caView.setInput(ca);
							}
						} catch (PartInitException e) {
							e.printStackTrace();
						}
					}
				});
				
				return Status.OK_STATUS;
			}
		};

		// Start the job.
		job.schedule();
	}
	
	/**
	 * Analyze the code inside the given entry class and method.
	 * @param entryClass
	 * @param entryMethod
	 * @return The concurrent accesses found in the given entry class and method
	 */
	public ConcurrentAccesses<?> findConcurrentAccesses(String entryClass, String entryMethod) {

		try {
			lastEntryClass = entryClass;
			lastEntryMethod = entryMethod;
			if (pointerAnalysis == null)
				setup(entryClass, entryMethod);
			A a = new A(callGraph, pointerAnalysis);
			return a.compute();
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		lastEntryClass = null;
		lastEntryMethod = null;
		
		return null;
	}
}
