package org.mariella.persistence.runtime;

import java.io.Serializable;

import org.mariella.persistence.schema.ClassDescription;
import org.mariella.persistence.schema.CollectionPropertyDescription;
import org.mariella.persistence.schema.PropertyDescription;
import org.mariella.persistence.schema.ReferencePropertyDescription;
import org.mariella.persistence.schema.RelationshipPropertyDescription;

public class RIListener implements ModificationTrackerListener, Serializable {
	private static final long serialVersionUID = 1L;

	private final ModificationTracker modificationTracker;

	private boolean updating = false;

public RIListener(ModificationTracker modificationTracker) {
	super();
	this.modificationTracker = modificationTracker;
}

public void indexedPropertyChanged(Object modifiable, String propertyName, int index, Object oldValue, Object newValue) {
	if(!updating) {
		updating = true;
		try {
			ClassDescription cd = modificationTracker.getSchemaDescription().getClassDescription(modifiable.getClass().getName());
			CollectionPropertyDescription mine = (CollectionPropertyDescription)cd.getPropertyDescription(propertyName);
			RelationshipPropertyDescription reverse = mine.getReversePropertyDescription();
			if(reverse != null) {
				if(oldValue == null) {
					// add
					if(reverse instanceof ReferencePropertyDescription) {
						Object old = getReferenceProperty(newValue, (ReferencePropertyDescription)reverse);
						if(old != null) {
							setReferenceProperty(newValue, (ReferencePropertyDescription)reverse, null);
						}
						setReferenceProperty(newValue, (ReferencePropertyDescription)reverse, modifiable);
					} else if(reverse instanceof CollectionPropertyDescription) {
						// [teofanos, 8.9.2011] -> @aist: warum der tote code hier? ich lass den zombie mal so stehn ;-)
						if(oldValue != null) {
							getTrackedList(oldValue, (CollectionPropertyDescription)reverse).add(modifiable);
						}
					} else {
						throw new IllegalStateException();
					}
				} else {
					// remove
					if(reverse instanceof ReferencePropertyDescription) {
						setReferenceProperty(oldValue, (ReferencePropertyDescription)reverse, null);
					} else if(reverse instanceof CollectionPropertyDescription) {
						getTrackedList(oldValue, (CollectionPropertyDescription)reverse).remove(modifiable);
					} else {
						throw new IllegalStateException();
					}
				}
			}
		} finally {
			updating = false;
		}
	}
}

public void propertyChanged(Object modifiable, String propertyName, Object oldValue, Object newValue) {
	if(!updating) {
		updating = true;
		try {
			ClassDescription cd = modificationTracker.getSchemaDescription().getClassDescription(modifiable.getClass().getName());
			PropertyDescription pd = cd.getPropertyDescription(propertyName);
			if(pd instanceof ReferencePropertyDescription) {
				ReferencePropertyDescription mine = (ReferencePropertyDescription)pd;
				RelationshipPropertyDescription reverse = mine.getReversePropertyDescription();
				if(reverse != null && oldValue != null) {
					if(reverse instanceof ReferencePropertyDescription) {
						setReferenceProperty(oldValue, (ReferencePropertyDescription)reverse, null);
					} else {
						getTrackedList(oldValue, (CollectionPropertyDescription)reverse).remove(modifiable);
					}
				}
				if(newValue != null && reverse != null) {
					if(reverse instanceof ReferencePropertyDescription) {
						setReferenceProperty(newValue, (ReferencePropertyDescription)reverse, modifiable);
					} else {
						getTrackedList(newValue, (CollectionPropertyDescription)reverse).add(modifiable);
					}
				}
			}
		} finally {
			updating = false;
		}
	}
}

private void setReferenceProperty(Object receiver, ReferencePropertyDescription pd, Object value) {
	ModifiableAccessor.Singleton.setValue(receiver, pd, value);
}

private Object getReferenceProperty(Object receiver, ReferencePropertyDescription pd) {
	return (Object) ModifiableAccessor.Singleton.getValue(receiver, pd);
}

@SuppressWarnings("unchecked")
private <T> TrackedList<T> getTrackedList(Object receiver, CollectionPropertyDescription pd) {
	return (TrackedList<T>)ModifiableAccessor.Singleton.getValue(receiver, pd);
}

}
