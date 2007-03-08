package org.omnetpp.common.properties;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author rhornig
 * A boolean property editor using an SWT checkbox as the editor
 */
public class CheckboxControlCellEditor extends CellEditor {

    Button chkBtn;
    
    public CheckboxControlCellEditor(Composite parent) {
        super(parent);
    }

    @Override
    protected Control createControl(Composite parent) {
        chkBtn = new Button(parent, SWT.CHECK);
        
        // see also ComboBoxCellEditor, TextCellEditor etc for explanation
        chkBtn.addKeyListener(new KeyAdapter() {
            // hook key pressed - see PR 14201  
            public void keyPressed(KeyEvent e) {
                keyReleaseOccured(e);
                // note: as a result of processing the above call, clients may have
                // disposed this cell editor
            }
        });
        chkBtn.addTraverseListener(new TraverseListener() {
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_ESCAPE || e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                }
            }
        });
        return chkBtn;
    }

    @Override
    protected Object doGetValue() {
        return chkBtn.getSelection();
    }

    @Override
    protected void doSetFocus() {
        chkBtn.setFocus();
    }

    @Override
    protected void doSetValue(Object value) {
        Assert.isTrue(value != null && (value instanceof Boolean));
        chkBtn.setSelection(((Boolean)value));
    }

}
