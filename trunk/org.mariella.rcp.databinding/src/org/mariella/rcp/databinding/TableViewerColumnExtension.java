package org.mariella.rcp.databinding;

import org.eclipse.jface.viewers.TableViewer;
import org.mariella.rcp.databinding.internal.InternalBindingContext;
import org.mariella.rcp.databinding.internal.TableController;
import org.mariella.rcp.databinding.internal.VTableViewerObservableList;

public class TableViewerColumnExtension implements VBindingDomainExtension {

String propertyPath;
Object domainSymbol;
VBindingDomain domain;
String headerText;
int weight;
boolean resizable=false;

public TableViewerColumnExtension(String propertyPath, VBindingDomain domain, String headerText, int weight) {
	this.propertyPath = propertyPath;
	this.domain = domain;
	this.headerText = headerText;
	this.weight = weight;
}

public TableViewerColumnExtension(String propertyPath, Object domainSymbol, String headerText, int weight) {
	this.propertyPath = propertyPath;
	this.domainSymbol = domainSymbol;
	this.headerText = headerText;
	this.weight = weight;
}

public TableViewerColumnExtension(String propertyPath, VBindingDomain domain, String headerText, int weight, boolean resizable) {
	this.propertyPath = propertyPath;
	this.domain = domain;
	this.headerText = headerText;
	this.weight = weight;
	this.resizable = resizable;
}

public TableViewerColumnExtension(String propertyPath, Object domainSymbol, String headerText, int weight, boolean resizable) {
	this.propertyPath = propertyPath;
	this.domainSymbol = domainSymbol;
	this.headerText = headerText;
	this.weight = weight;
	this.resizable = resizable;
}

public void install(VBinding binding) {
	TableController controller = ((InternalBindingContext)binding.getBindingContext()).getMainContext().tableControllerMap.get(getTableViewer(binding));
	controller.install(this, binding);
}

private TableViewer getTableViewer(VBinding binding) {
	return ((VTableViewerObservableList)binding.getBinding().getTarget()).getTableViewer();
}

public String getHeaderText() {
	return headerText;
}

public int getWeight() {
	return weight;
}

public String getPropertyPath() {
	return propertyPath;
}

public VBindingDomain getDomain() {
	return domain;
}

public Object getDomainSymbol() {
	return domainSymbol;
}

public void setDomain(VBindingDomain domain) {
	this.domain = domain;
}

public boolean isResizable() {
	return resizable;
}

}
