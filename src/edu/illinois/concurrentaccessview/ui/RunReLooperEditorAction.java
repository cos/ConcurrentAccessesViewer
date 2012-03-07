package edu.illinois.concurrentaccessview.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

import edu.illinois.concurrentaccessview.analysis.DataRaceAnalysis;

/**
 * An editor action used to run data race analyses.
 */
public class RunReLooperEditorAction implements IEditorActionDelegate {

	/**
	 * The Java editor in which the file is opened.
	 */
	private JavaEditor editor;

	/**
	 * Add the necessary dependencies to the given analysis.
	 * @param analysis A data race analysis object
	 * @param cu The compilation unit containing the code to be analyzed
	 * @throws JavaModelException
	 */
	private void addAnalysisDependencies(
			DataRaceAnalysis analysis,
			ICompilationUnit cu) throws JavaModelException {
		
		// Get the Java project containing the code to be analyzed.
		IJavaProject javaProject = cu.getJavaProject();
		
		// Add the project's binary folder to the analysis.
		IPath absoluteWorkspacePath = javaProject.getResource().getLocation(); // E:/eclipse/runtime-EclipseApplication/Test
		if (absoluteWorkspacePath.lastSegment().equals(javaProject.getElementName())) {
			absoluteWorkspacePath = absoluteWorkspacePath.removeLastSegments(1);
			absoluteWorkspacePath = absoluteWorkspacePath.removeTrailingSeparator();
		} // e:/eclipse/runtime-EclipseApplication
		IPath relativeBinPath = javaProject.getOutputLocation(); // /Test/bin
		IPath absoluteBinPath = absoluteWorkspacePath.append(relativeBinPath); // E:/eclipse/runtime-EclipseApplication/Test/bin
		analysis.addBinaryDependency(absoluteBinPath.toString());
		
		// Add the project's included libraries to the analysis.
		IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
		for (int i = 0; i < rawClasspath.length; i++) {
			IClasspathEntry classpathEntry = rawClasspath[i];
			if (javaProject.getPath().isPrefixOf(classpathEntry.getPath()) &&
					classpathEntry.getContentKind() == IPackageFragmentRoot.K_BINARY &&
					classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				IPath absoluteLibraryPath = absoluteWorkspacePath.append(classpathEntry.getPath());
				analysis.addJarDependency(absoluteLibraryPath.toString());
			}
		}
	}

	/**
	 * @param analysis A data race analysis object
	 * @param cu The compilation unit containing the code to be analyzed
	 * @return The entry class for the given analysis and compilation unit
	 * @throws JavaModelException
	 */
	private String getEntryClass(DataRaceAnalysis analysis, ICompilationUnit cu)
			throws JavaModelException {
		String className = cu.getElementName();
		if (className.endsWith(".java")) {
			className = className.substring(0, className.length() - 5);
		}
		StringBuffer entryClass = new StringBuffer();
		entryClass.append('L');
		entryClass.append(cu.getParent().getElementName().replace('.', '/'));
		entryClass.append('/');
		entryClass.append(className);
		return entryClass.toString();
	}

	/**
	 * @param cu The compilation unit containing the code to be analyzed
	 * @return The file containing the code to be analyzed
	 */
	private IFile getFile(ICompilationUnit cu) {
		IPath p = cu.getPath();
		IProject project = cu.getJavaProject().getProject();
		IFile file = project.getFile(p.removeFirstSegments(1));
		return file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		try {
			IJavaElement[] elements = SelectionConverter.codeResolve(this.editor, true);
			if (elements.length == 1 && (elements[0] instanceof IMethod)) {
				
				IMethod method = (IMethod) elements[0];
				
				// Get the compilation unit containing the method.
				ICompilationUnit cu = method.getCompilationUnit();
				
				// Create the analysis object.
				DataRaceAnalysis analysis = new DataRaceAnalysis();
				
				// Add dependencies to the analysis.
				addAnalysisDependencies(analysis, cu);
				
				// Get the file containing the analyzed code.
				IFile file = getFile(cu);
				
				// Get the entry class for the analysis.
				String entryClass = getEntryClass(analysis, cu);
				
				// Get the entry method for the analysis.
				String entryMethod = method.getElementName() + method.getSignature();
				
				// Run the analysis.
				DataRaceAnalysis.runAnalysis(analysis, file, entryClass, entryMethod);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		if (targetEditor instanceof JavaEditor) {
			this.editor = (JavaEditor) targetEditor;
		}
	}
}
