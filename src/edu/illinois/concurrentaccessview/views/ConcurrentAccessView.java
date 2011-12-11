package edu.illinois.concurrentaccessview.views;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import sabazios.domains.ConcurrentAccesses;
import sabazios.domains.Loop;
import sabazios.domains.ObjectAccess;
import sabazios.util.CodeLocation;
import edu.illinois.concurrentaccessview.analysis.DataRaceAnalysis;

/**
 * Main class for the concurrent access view, which is used to display
 * concurrent accesses in code.
 */
public class ConcurrentAccessView extends ViewPart implements ISelectionListener {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "edu.illinois.concurrentaccessview.views.ConcurrentAccessView";

	/**
	 * A tree viewer used to display concurrent accesses.
	 */
	private TreeViewer viewer;
	
	/**
	 * A label used to display the trace of a concurrent access.
	 */
	private Label traceLabel;

	/**
	 * Run when the user double-clicks an item in the tree viewer.
	 */
	private Action doubleClickAction;

	/**
	 * The file in which concurrent accesses are being analyzed.
	 */
	private IFile file;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		// Create a layout with two equal-width columns.
		FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
		fillLayout.spacing = 4;
		parent.setLayout(fillLayout);
		
		// Create the tree viewer.
		viewer = new TreeViewer(parent);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());
		
		// Create and hook all actions.
		makeActions();
		hookDoubleClickAction();
		
		getSite().getPage().addSelectionListener((ISelectionListener) this);
		
		// Create the trace label.
		Label traceLabel = new Label(parent, SWT.LEFT);
	}

	/**
	 * Hooks a double-click listener to the tree viewer.
	 */
	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	/**
	 * Creates all actions needed by this view.
	 */
	private void makeActions() {
		
		// Create the double-click action.
		doubleClickAction = new Action() {
			public void run() {
				
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection) selection).getFirstElement();
				
				CodeLocation cl = null;
				
				if (obj instanceof Map.Entry) {
					Object obj2 = ((Map.Entry) obj).getKey();
					if (obj2 instanceof Loop) {
						Loop loop = (Loop) obj2;
						cl = loop.getCodeLocation();
					}
				} else if (obj instanceof ObjectAccess) {
					ObjectAccess oa = (ObjectAccess) obj;
					cl = oa.getCodeLocation();
				}
				
				try {
					if (cl != null) {
						openFileInEditor(cl.getLineNo());
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		};
	}
	
	/**
	 * Open the selected file in the Java editor and put a marker at the given
	 * line number.
	 * @param line A line number in the currently selected file.
	 * @throws CoreException
	 */
	private void openFileInEditor(int line) throws CoreException {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		HashMap<String, Object> markerAttributes = new HashMap<String, Object>();
		markerAttributes.put(IMarker.LINE_NUMBER, new Integer(line));
		markerAttributes.put(IDE.EDITOR_ID_ATTR, "org.eclipse.jdt.ui.CompilationUnitEditor");
		IMarker marker = this.file.createMarker(IMarker.TEXT);
		marker.setAttributes(markerAttributes);
		IDE.openEditor(activePage, marker);
		marker.delete();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof TreeSelection) {
			if (((TreeSelection) selection).getFirstElement() != null) {
				Object firstSelectedElement = ((TreeSelection) selection).getFirstElement();
				if (firstSelectedElement instanceof CompilationUnit) {
					try {
						
						CompilationUnit cu = (CompilationUnit) firstSelectedElement;
						
						DataRaceAnalysis analysis = new DataRaceAnalysis();
						
						String className = cu.getElementName();
						if (className.endsWith(".java")) {
							className = className.substring(0, className.length() - 5);
						}
						
						IJavaProject javaProject = cu.getJavaProject();
						IPath absoluteWorkspacePath = javaProject.getResource().getLocation(); // E:/eclipse/runtime-EclipseApplication/Test
						if (absoluteWorkspacePath.lastSegment().equals(javaProject.getElementName())) {
							absoluteWorkspacePath = absoluteWorkspacePath.removeLastSegments(1);
							absoluteWorkspacePath = absoluteWorkspacePath.removeTrailingSeparator();
						} // e:/eclipse/runtime-EclipseApplication
						
						IPath relativeBinPath = javaProject.getOutputLocation(); // /Test/bin
						IPath absoluteBinPath = absoluteWorkspacePath.append(relativeBinPath); // E:/eclipse/runtime-EclipseApplication/Test/bin
						analysis.addBinaryDependency(absoluteBinPath.toString());
						
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
						
						StringBuffer entryClass = new StringBuffer();
						entryClass.append('L');
						char[][] entryPackage = cu.getPackageName();
						for (int i = 0; i < entryPackage.length; i++) {
							entryClass.append(entryPackage[i]);
							entryClass.append('/');
						}
						entryClass.append(className);
						
						ConcurrentAccesses<?> ca = analysis.findConcurrentAccesses(entryClass.toString(), "verySimpleRace()V");
						this.viewer.setInput(ca);
						
						IPath p = cu.getPath();
						IProject project = javaProject.getProject();
						this.file = project.getFile(p.removeFirstSegments(1));
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}
