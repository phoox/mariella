package org.mariella.rcp.databinding.internal;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.widgets.Widget;
import org.mariella.rcp.databinding.VDataBindingContext;

/**
 * Copied from original TableCursor and modified in a way that 
 * - selection events are
 * also sent when the use clicks on the current cell. (this is needed when the
 * user selects a control outside the table and clicks on the cell he had left). 
 * - tab next / tab previous events are handled 
 * - when the border
 * of the table is reached (for example on the right side) and the user presses
 * continues traversing in that direction (presses the right key), the cursors
 * appears on the next row (row below, cell 0).
 * 
 * 
 * @author maschmid
 * 
 */
public class TableControllerCursor extends Canvas {
Table table;
TableController tableController;
TableViewer tableViewer;
TableItem row = null;
TableColumn column = null;
Listener tableListener, resizeListener, disposeItemListener, disposeColumnListener;
VDataBindingContext dataBindingContext;

Listener listener = new Listener() {
	public void handleEvent(Event event) {
		switch (event.type) {
		// maschmid: we also need the mouse down event
		case SWT.MouseDown:
			mouseDown(event);
			break;
		case SWT.Dispose:
			dispose(event);
			break;
		case SWT.FocusIn:
		case SWT.FocusOut:
			redraw();
			break;
		case SWT.KeyDown:
			keyDown(event);
			break;
		case SWT.Paint:
			paint(event);
			break;
		case SWT.Traverse: {
			event.doit = true;
			handleTraverse(event);
			break;
		}
		}
	}
};

// By default, invert the list selection colors
static final int BACKGROUND = SWT.COLOR_LIST_SELECTION_TEXT;

static final int FOREGROUND = SWT.COLOR_LIST_SELECTION;

/**
 * Constructs a new instance of this class given its parent table and a style
 * value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in class
 * <code>SWT</code> which is applicable to instances of this class, or must be
 * built by <em>bitwise OR</em>'ing together (that is, using the
 * <code>int</code> "|" operator) two or more of those <code>SWT</code>
 * style constants. The class description lists the style constants that are
 * applicable to the class. Style bits are also inherited from superclasses.
 * </p>
 * 
 * @param parent
 *            a Table control which will be the parent of the new instance
 *            (cannot be null)
 * @param style
 *            the style of control to construct
 * 
 * @exception IllegalArgumentException
 *                <ul>
 *                <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 *                </ul>
 * @exception SWTException
 *                <ul>
 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
 *                thread that created the parent</li>
 *                <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed
 *                subclass</li>
 *                </ul>
 * 
 * @see SWT#BORDER
 * @see Widget#checkSubclass()
 * @see Widget#getStyle()
 */
public TableControllerCursor(VDataBindingContext dbc, TableController tableController, TableViewer tableViewer, int style) {
	super(tableViewer.getTable(), style);
	this.dataBindingContext = dbc;
	this.tableViewer = tableViewer;
	this.tableController = tableController;
	table = tableViewer.getTable();
	setBackground(null);
	setForeground(null);

	int[] events = new int[] { SWT.MouseDown, SWT.Dispose, SWT.FocusIn, SWT.FocusOut, SWT.KeyDown, SWT.Paint, SWT.Traverse };
	for (int i = 0; i < events.length; i++) {
		addListener(events[i], listener);
	}

	tableListener = new Listener() {
		public void handleEvent(Event event) {
			switch (event.type) {
			case SWT.MouseDown:
				tableMouseDown(event);
				break;
			case SWT.FocusIn:
				tableFocusIn(event);
				break;
			}
		}
	};
	table.addListener(SWT.FocusIn, tableListener);
	table.addListener(SWT.MouseDown, tableListener);

	disposeItemListener = new Listener() {
		public void handleEvent(Event event) {
			row = null;
			column = null;
			_resize();
		}
	};
	disposeColumnListener = new Listener() {
		public void handleEvent(Event event) {
			row = null;
			column = null;
			_resize();
		}
	};
	resizeListener = new Listener() {
		public void handleEvent(Event event) {
			_resize();
		}
	};
	ScrollBar hBar = table.getHorizontalBar();
	if (hBar != null) {
		hBar.addListener(SWT.Selection, resizeListener);
	}
	ScrollBar vBar = table.getVerticalBar();
	if (vBar != null) {
		vBar.addListener(SWT.Selection, resizeListener);
	}
}

/**
 * Adds the listener to the collection of listeners who will be notified when
 * the user changes the receiver's selection, by sending it one of the messages
 * defined in the <code>SelectionListener</code> interface.
 * <p>
 * When <code>widgetSelected</code> is called, the item field of the event
 * object is valid. If the receiver has <code>SWT.CHECK</code> style set and
 * the check selection changes, the event object detail field contains the value
 * <code>SWT.CHECK</code>. <code>widgetDefaultSelected</code> is typically
 * called when an item is double-clicked.
 * </p>
 * 
 * @param listener
 *            the listener which should be notified when the user changes the
 *            receiver's selection
 * 
 * @exception IllegalArgumentException
 *                <ul>
 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 *                </ul>
 * @exception SWTException
 *                <ul>
 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
 *                thread that created the receiver</li>
 *                </ul>
 * 
 * @see SelectionListener
 * @see SelectionEvent
 * @see #removeSelectionListener(SelectionListener)
 * 
 */
public void addSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null)
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener(listener);
	addListener(SWT.Selection, typedListener);
	addListener(SWT.DefaultSelection, typedListener);
}

void dispose(Event event) {
	table.removeListener(SWT.FocusIn, tableListener);
	table.removeListener(SWT.MouseDown, tableListener);
	if (column != null) {
		column.removeListener(SWT.Dispose, disposeColumnListener);
		column.removeListener(SWT.Move, resizeListener);
		column.removeListener(SWT.Resize, resizeListener);
		column = null;
	}
	if (row != null) {
		row.removeListener(SWT.Dispose, disposeItemListener);
		row = null;
	}
	ScrollBar hBar = table.getHorizontalBar();
	if (hBar != null) {
		hBar.removeListener(SWT.Selection, resizeListener);
	}
	ScrollBar vBar = table.getVerticalBar();
	if (vBar != null) {
		vBar.removeListener(SWT.Selection, resizeListener);
	}
}

void keyDown(Event event) {
	if (row == null)
		return;
	switch (event.character) {
	case SWT.CR:
		notifyListeners(SWT.DefaultSelection, new Event());
		return;
	}
	final int rowIndex = table.indexOf(row);
	final int columnIndex = column == null ? 0 : table.indexOf(column);
	switch (event.keyCode) {
	case SWT.ARROW_UP:
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				setRowColumn(Math.max(0, rowIndex - 1), columnIndex, true);
			}
		});
		break;
	case SWT.ARROW_DOWN:
		if (event.widget instanceof Control && ((Control)event.widget).isFocusControl())
			Display.getCurrent().asyncExec(new Runnable() {
				public void run() {
					setRowColumn(Math.min(rowIndex + 1, table.getItemCount() - 1), columnIndex, true);
				}
			});
		break;
	}
}

public void attachTraverseListener(Control editControl) {
	editControl.addListener(SWT.Traverse, listener);
}

void handleTraverse(final Event event) {
	switch (event.detail) {
	case SWT.TRAVERSE_TAB_NEXT:
		if (moveCursorLeftRight(+1))
			// forward move works automatically
			event.doit = false;
		break;
	case SWT.TRAVERSE_TAB_PREVIOUS:
		if (!moveCursorLeftRight(-1))
			// back move has to be started by table, otherwise we run into our tableFocusIn(...) method
			dataBindingContext.getTraverseHandler().incrementFocusControl(table, -1);
		event.doit = false;
		break;
	}
}

/**
 * Returns true if the cursor could be moved within the table bounds.
 * 
 * @param direction
 * @return
 */
private boolean moveCursorLeftRight(int direction) {
	if (row == null)
		return false;
	int rowIndex = table.indexOf(row);
	int columnIndex = column == null ? 0 : table.indexOf(column);

	int columnCount = table.getColumnCount();
	if (columnCount == 0)
		return false;

	if (direction == -1) {
		while (columnIndex > 0) {
			columnIndex--;
			if (tableController.isEditable(columnIndex)) {
				setRowColumn(rowIndex, columnIndex, true);
				return true;
			}
		}
		if (rowIndex > 0) {
			tableViewer.setSelection(new StructuredSelection(tableViewer.getElementAt(rowIndex-1)));
			setRowColumn(rowIndex-1, columnCount-1, true);
			return true;
		} else {
			return false;
		}
	} else {
		while (columnIndex < columnCount-1) {
			columnIndex++;
			if (tableController.isEditable(columnIndex)) {
				setRowColumn(rowIndex, columnIndex, true);
				return true;
			}
		}
		if (rowIndex < table.getItemCount()-1) {
			tableViewer.setSelection(new StructuredSelection(tableViewer.getElementAt(rowIndex+1)));
			setRowColumn(rowIndex+1, 0, true);
			return true;
		} else {
			return false;
		}
	}
}

void paint(Event event) {
	//System.out.println("Row: " + row);
	if (row == null)
		return;
	
	int columnIndex = column == null ? 0 : table.indexOf(column);
	GC gc = event.gc;
	Display display = getDisplay();
	gc.setBackground(getBackground());
	gc.setForeground(getForeground());
	gc.fillRectangle(event.x, event.y, event.width, event.height);
	int x = 0;
	Point size = getSize();
	Image image = row.getImage(columnIndex);
	if (image != null) {
		Rectangle imageSize = image.getBounds();
		int imageY = (size.y - imageSize.height) / 2;
		gc.drawImage(image, x, imageY);
		x += imageSize.width;
	}
	String text = row.getText(columnIndex);
	if (text.length() > 0) {
		Rectangle bounds = row.getBounds(columnIndex);
		Point extent = gc.stringExtent(text);
		// Temporary code - need a better way to determine table trim
		String platform = SWT.getPlatform();
		if ("win32".equals(platform)) { //$NON-NLS-1$
			if (table.getColumnCount() == 0 || columnIndex == 0) {
				x += 2;
			} else {
				int alignmnent = column.getAlignment();
				switch (alignmnent) {
				case SWT.LEFT:
					x += 6;
					break;
				case SWT.RIGHT:
					x = bounds.width - extent.x - 6;
					break;
				case SWT.CENTER:
					x += (bounds.width - x - extent.x) / 2;
					break;
				}
			}
		} else {
			if (table.getColumnCount() == 0) {
				x += 5;
			} else {
				int alignmnent = column.getAlignment();
				switch (alignmnent) {
				case SWT.LEFT:
					x += 5;
					break;
				case SWT.RIGHT:
					x = bounds.width - extent.x - 2;
					break;
				case SWT.CENTER:
					x += (bounds.width - x - extent.x) / 2 + 2;
					break;
				}
			}
		}
		int textY = (size.y - extent.y) / 2;
		gc.drawString(text, x, textY);
	}
	if (isFocusControl()) {
		gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		gc.setForeground(display.getSystemColor(SWT.COLOR_WHITE));
		gc.drawFocus(0, 0, size.x, size.y);
	}
}

void tableFocusIn(Event event) {
	if (isDisposed())
		return;
	if (isVisible()) {
		setFocus();
		if (row == null) {
			if (table.getItemCount() > 0) {
				tableViewer.setSelection(new StructuredSelection(tableViewer.getElementAt(0)));
				setRowColumn(0, 0, true);
			}
		}
		if (row != null) {
			int rowIndex = table.indexOf(row);
			int columnIndex = column == null ? 0 : table.indexOf(column);
			setRowColumn(rowIndex, columnIndex, true);
		}
	}
}

void setColumn(int colIndex) {
	int rowIndex = table.indexOf(row);
	setRowColumn(rowIndex, colIndex, true);
}

void mouseDown(Event event) {
	setRowColumn(row, column, true);
}

void tableMouseDown(Event event) {
	if (isDisposed() || !isVisible())
		return;
	Point pt = new Point(event.x, event.y);
	int lineWidth = table.getLinesVisible() ? table.getGridLineWidth() : 0;
	TableItem item = table.getItem(pt);
	if ((table.getStyle() & SWT.FULL_SELECTION) != 0) {
		if (item == null)
			return;
	} else {
		int start = item != null ? table.indexOf(item) : table.getTopIndex();
		int end = table.getItemCount();
		Rectangle clientRect = table.getClientArea();
		for (int i = start; i < end; i++) {
			TableItem nextItem = table.getItem(i);
			Rectangle rect = nextItem.getBounds(0);
			if (pt.y >= rect.y && pt.y < rect.y + rect.height + lineWidth) {
				item = nextItem;
				break;
			}
			if (rect.y > clientRect.y + clientRect.height)
				return;
		}
		if (item == null)
			return;
	}
	TableColumn newColumn = null;
	int columnCount = table.getColumnCount();
	if (columnCount > 0) {
		for (int i = 0; i < columnCount; i++) {
			Rectangle rect = item.getBounds(i);
			rect.width += lineWidth;
			rect.height += lineWidth;
			if (rect.contains(pt)) {
				newColumn = table.getColumn(i);
				break;
			}
		}
		if (newColumn == null) {
			newColumn = table.getColumn(0);
		}
	}
	setRowColumn(item, newColumn, true);
	setFocus();
	return;
}

void setRowColumn(int row, int column, boolean notify) {
	TableItem item = row == -1 ? null : table.getItem(row);
	TableColumn col = column == -1 || table.getColumnCount() == 0 ? null : table.getColumn(column);
	setRowColumn(item, col, notify);
}

void resetRowColumn() {
	row = null;
	column = null;
}

void setRowColumn(TableItem row, TableColumn column, boolean notify) {
	if (this.row != null && this.row != row) {
		this.row.removeListener(SWT.Dispose, disposeItemListener);
		this.row = null;
	}
	if (this.column != null && this.column != column) {
		this.column.removeListener(SWT.Dispose, disposeColumnListener);
		this.column.removeListener(SWT.Move, resizeListener);
		this.column.removeListener(SWT.Resize, resizeListener);
		this.column = null;
	}
	if (row != null) {
		if (this.row != row) {
			this.row = row;
			row.addListener(SWT.Dispose, disposeItemListener);
			table.showItem(row);
		}
		if (this.column != column && column != null) {
			this.column = column;
			column.addListener(SWT.Dispose, disposeColumnListener);
			column.addListener(SWT.Move, resizeListener);
			column.addListener(SWT.Resize, resizeListener);
			table.showColumn(column);
		}
		int columnIndex = column == null ? 0 : table.indexOf(column);
		setBounds(row.getBounds(columnIndex));
		redraw();
		if (notify) {
			notifyListeners(SWT.Selection, new Event());
		}
	}
}

public void setVisible(boolean visible) {
	checkWidget();
	if (visible)
		_resize();
	super.setVisible(visible);
}

/**
 * Removes the listener from the collection of listeners who will be notified
 * when the user changes the receiver's selection.
 * 
 * @param listener
 *            the listener which should no longer be notified
 * 
 * @exception IllegalArgumentException
 *                <ul>
 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 *                </ul>
 * @exception SWTException
 *                <ul>
 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
 *                thread that created the receiver</li>
 *                </ul>
 * 
 * @see SelectionListener
 * @see #addSelectionListener(SelectionListener)
 * 
 * @since 3.0
 */
public void removeSelectionListener(SelectionListener listener) {
	checkWidget();
	if (listener == null) {
		SWT.error(SWT.ERROR_NULL_ARGUMENT);
	}
	removeListener(SWT.Selection, listener);
	removeListener(SWT.DefaultSelection, listener);
}

void _resize() {
	if (row == null || !isFocusControl()) {
		setBounds(-200, -200, 0, 0);
	} else {
		int columnIndex = column == null ? 0 : table.indexOf(column);
		setBounds(row.getBounds(columnIndex));
	}
}

/**
 * Returns the column over which the TableCursor is positioned.
 * 
 * @return the column for the current position
 * 
 * @exception SWTException
 *                <ul>
 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
 *                thread that created the receiver</li>
 *                </ul>
 */
public int getColumn() {
	checkWidget();
	return column == null ? 0 : table.indexOf(column);
}

/**
 * Returns the row over which the TableCursor is positioned.
 * 
 * @return the item for the current position
 * 
 * @exception SWTException
 *                <ul>
 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
 *                thread that created the receiver</li>
 *                </ul>
 */
public TableItem getRow() {
	checkWidget();
	return row;
}

public void setBackground(Color color) {
	if (color == null)
		color = getDisplay().getSystemColor(BACKGROUND);
	super.setBackground(color);
	redraw();
}

public void setForeground(Color color) {
	if (color == null)
		color = getDisplay().getSystemColor(FOREGROUND);
	super.setForeground(color);
	redraw();
}

/**
 * Positions the TableCursor over the cell at the given row and column in the
 * parent table.
 * 
 * @param row
 *            the index of the row for the cell to select
 * @param column
 *            the index of column for the cell to select
 * 
 * @exception SWTException
 *                <ul>
 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
 *                thread that created the receiver</li>
 *                </ul>
 * 
 */
public void setSelection(int row, int column) {
	checkWidget();
	int columnCount = table.getColumnCount();
	int maxColumnIndex = columnCount == 0 ? 0 : columnCount - 1;
	if (row < 0 || row >= table.getItemCount() || column < 0 || column > maxColumnIndex)
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	setRowColumn(row, column, false);
}

/**
 * Positions the TableCursor over the cell at the given row and column in the
 * parent table.
 * 
 * @param row
 *            the TableItem of the row for the cell to select
 * @param column
 *            the index of column for the cell to select
 * 
 * @exception SWTException
 *                <ul>
 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
 *                thread that created the receiver</li>
 *                </ul>
 * 
 */
public void setSelection(TableItem row, int column) {
	checkWidget();
	int columnCount = table.getColumnCount();
	int maxColumnIndex = columnCount == 0 ? 0 : columnCount - 1;
	if (row == null || row.isDisposed() || column < 0 || column > maxColumnIndex)
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	setRowColumn(table.indexOf(row), column, false);
}

public boolean forceFocus() {
	return false;
}
}
