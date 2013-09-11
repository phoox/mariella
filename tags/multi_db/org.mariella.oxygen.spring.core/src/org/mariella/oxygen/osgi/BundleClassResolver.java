package org.mariella.oxygen.osgi;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.persistence.PersistenceException;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.mariella.oxygen.basic_core.ClassResolver;
import org.osgi.framework.Bundle;

public class BundleClassResolver implements ClassResolver {

	private Collection<Bundle> bundles;
	
public static BundleClassResolver create() {
	Collection<String> bundleIds = new HashSet<String>();
	// resolve remotable bundles
	IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.mariella.persistence.persistenceBundles");
	for (IConfigurationElement element : elements) {
		if (element.getName().equals("bundle")) {
			String bundleId = element.getAttribute("bundleId");
			Bundle bundle = Platform.getBundle(bundleId);
			if (bundle == null) {
				throw new PersistenceException("Persistence bundle '" + bundleId + "' not found.");
			}
			bundleIds.add(bundleId);
		}
	}
	// resolve remotable bundles
	elements = Platform.getExtensionRegistry().getConfigurationElementsFor("at.rufus.base.remoting.remotableBundles");
	for (IConfigurationElement element : elements) {
		if (element.getName().equals("bundle")) {
			String bundleId = element.getAttribute("bundleId");
			Bundle bundle = Platform.getBundle(bundleId);
			if (bundle == null) {
				throw new PersistenceException("Remotable bundle '" + bundleId + "' not found.");
			}
			bundleIds.add(bundleId);
		}
	}
	
	return new BundleClassResolver(bundleIds);
}


protected BundleClassResolver(Collection<String> bundleIds) {
	Bundle[] bundles = new Bundle[bundleIds.size()];
	int i = 0;
	for(String bundleId : bundleIds) {
		Bundle bundle = Platform.getBundle(bundleId);
		if(bundle == null) {
			throw new RuntimeException("Bundle '" + bundleId +"' cannot be resolved.");
		}
		bundles[i++] = bundle;
	}
	this.bundles = getTopLevelBundles(bundles);
}

protected BundleClassResolver(Bundle... bundles) {
	this.bundles = getTopLevelBundles(bundles);
}

private Collection<Bundle> getTopLevelBundles(Bundle... bundles) {
	return Arrays.asList(bundles);
//		Collection<Bundle> topLevelBundles = new LinkedList<Bundle>();
//		BundleDescription[] bundleDescriptions = new BundleDescription[bundles.length];
//		Map<String, BundleDescription>[] dependencies = new Map[bundles.length];
//		int i = 0;
//		for(; i < bundles.length; i++) {
//			BundleDescription bundleDescription = (BundleDescription) bundles[i].adapt(BundleRevision.class);
//			if(bundleDescription == null) {
//				throw new RuntimeException("Unable to resolve bundle description for bundle '" + bundles[i].getSymbolicName() + "'.");
//			}
//			bundleDescriptions[i] = bundleDescription;
//			dependencies[i] = BundleDependencies.resolve(true, bundleDescription);
//		}
//		int j;
//		for(i = 0; i < bundles.length; i++) {
//			for(j = 0; j < bundles.length; j++) {
//				if (i != j) {
//					if(dependencies[j].containsKey(bundleDescriptions[i].getSymbolicName())) {
//						break;
//					}
//				}
//			}
//			if(j == bundles.length) {
//				topLevelBundles.add(bundles[i]);
//			}
//		}
//		return topLevelBundles;
}

public Class<?> resolveClass(String className) throws ClassNotFoundException {
	for (Bundle bundle : bundles) {
		try {
			return bundle.loadClass(className);
		} catch (ClassNotFoundException ex) {
			// continue
		}
	}
	throw new ClassNotFoundException("Class '" + className + "' not found from bundles.");
}

}
