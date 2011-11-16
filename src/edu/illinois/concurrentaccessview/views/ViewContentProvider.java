package edu.illinois.concurrentaccessview.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import sabazios.domains.ConcurrentAccess;
import sabazios.domains.ConcurrentAccesses;

class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {
	
	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ConcurrentAccesses) {
			ConcurrentAccesses cas = (ConcurrentAccesses) parentElement;
			return cas.entrySet().toArray();
		} else if (parentElement instanceof Map.Entry) {
			Map.Entry entry = (Map.Entry) parentElement;
			if (entry.getValue() instanceof Set) {
				Set set = (Set) entry.getValue();
				return set.toArray();
			}
		} else if (parentElement instanceof ConcurrentAccess) {
			ConcurrentAccess ca = (ConcurrentAccess) parentElement;
			HashMap<String, Set> retVal = new HashMap<String, Set>();
			retVal.put("Alpha accesses", ca.alphaAccesses);
			retVal.put("Beta accesses", ca.betaAccesses);
			return retVal.entrySet().toArray();
		}
		return new Object[0];
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ConcurrentAccesses) {
			ConcurrentAccesses ca = (ConcurrentAccesses) inputElement;
			return ca.entrySet().toArray();
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof Map.Entry || element instanceof ConcurrentAccess);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// do nothing
	}
}
