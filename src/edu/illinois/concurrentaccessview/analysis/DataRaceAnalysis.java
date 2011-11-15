package edu.illinois.concurrentaccessview.analysis;

import java.io.IOException;

import sabazios.A;
import sabazios.domains.ConcurrentAccesses;
import sabazios.wala.WalaAnalysis;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class DataRaceAnalysis extends WalaAnalysis {

	public ConcurrentAccesses<?> findConcurrentAccesses(String entryClass, String entryMethod) {

		try {
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

		return null;
	}
}
