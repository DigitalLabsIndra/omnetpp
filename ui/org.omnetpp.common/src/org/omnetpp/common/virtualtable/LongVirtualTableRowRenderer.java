/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.common.virtualtable;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.common.color.ColorFactory;

/**
 * This class serves debugging purposes.
 */
public class LongVirtualTableRowRenderer extends LabelProvider implements IVirtualTableRowRenderer<Long> {
    protected Font font = JFaceResources.getDefaultFont();

    protected int fontHeight;

    public void setInput(Object input) {
        // void
    }

    public void drawCell(GC gc, Long element, int index, boolean isSelected) {
        if (isSelected) {
            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
            gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_SELECTION));
        }
        else
            gc.setForeground(ColorFactory.BLACK);
        gc.drawText("the number " + element.toString(), 5, 0); //XXX if we start at x=0, 1-2 pixel columns are off-screen
    }

    public int getRowHeight(GC gc) {
        if (fontHeight == 0) {
            Font oldFont = gc.getFont();
            gc.setFont(font);
            fontHeight = gc.getFontMetrics().getHeight();
            gc.setFont(oldFont);
        }

        return fontHeight + 2;
    }

    public String getTooltipText(Long element) {
        return null;
    }
}