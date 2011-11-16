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
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import sabazios.domains.ConcurrentAccesses;
import sabazios.domains.Loop;
import sabazios.domains.ObjectAccess;
import sabazios.util.CodeLocation;
import edu.illinois.concurrentaccessview.analysis.DataRaceAnalysis;

/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class ConcurrentAccessView extends ViewPart implements ISelectionListener {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "edu.illinois.concurrentaccessview.views.ConcurrentAccessView";

	private TreeViewer viewer;
	
	private DrillDownAdapter drillDownAdapter;

	private Action action1;
	private Action action2;
	private Action doubleClickAction;

	private IFile file;

	/**
	 * The constructor.
	 */
	public ConcurrentAccessView() {
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(viewer);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		
		getSite().getPage().addSelectionListener((ISelectionListener) this);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(action1);
		manager.add(new Separator());
		manager.add(action2);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ConcurrentAccessView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private void makeActions() {
		action1 = new Action() {
			public void run() {
				showMessage("Action 1 executed");
			}
		};
		action1.setText("Action 1");
		action1.setToolTipText("Action 1 tooltip");
		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
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

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Concurrent Access View",
			message);
	}
	
	private void openFileInEditor(int line) throws CoreException {
		
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
		IWorkbenchPage page = win.getActivePage();
		
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(IMarker.LINE_NUMBER, new Integer(line));
		map.put(IDE.EDITOR_ID_ATTR, "org.eclipse.ui.DefaultTextEditor");
		IMarker marker = this.file.createMarker(IMarker.TEXT);
		marker.setAttributes(map);
		
		IDE.openEditor(page, marker);
		marker.delete();
	}
}
