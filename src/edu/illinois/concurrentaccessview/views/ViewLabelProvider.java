package edu.illinois.concurrentaccessview.views;

import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import sabazios.domains.ConcurrentAccess;
import sabazios.domains.ConcurrentAccesses;
import sabazios.domains.ObjectAccess;

class ViewLabelProvider extends LabelProvider {

	@Override
	public Image getImage(Object obj) {
		String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
		if (obj instanceof ConcurrentAccesses ||
				obj instanceof Map.Entry ||
				obj instanceof ConcurrentAccess)
			imageKey = ISharedImages.IMG_OBJ_FOLDER;
		return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
	}

	@Override
	public String getText(Object obj) {
		if (obj instanceof Map.Entry) {
			Map.Entry entry = (Map.Entry) obj;
			return entry.getKey().toString();
		} else if (obj instanceof ConcurrentAccess) {
			return "Concurrent accesses";
		} else if (obj instanceof ObjectAccess) {
			ObjectAccess oa = (ObjectAccess) obj;
			return oa.toString();
		}
		return obj.toString();
	}
}
