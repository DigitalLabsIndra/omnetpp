/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
 *--------------------------------------------------------------*/

package org.omnetpp.figures.misc;

import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.omnetpp.common.Debug;
import org.omnetpp.figures.IProblemDecorationSupport;
import org.omnetpp.figures.ITooltipTextProvider;

/**
 * A collection of figure-related utility functions
 *
 * @author Andras
 */
public class FigureUtils {

    public static final Image ICON_ERROR = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK); //ImageFactory.getImage(ImageFactory.DECORATOR_IMAGE_ERROR);
    public static final Image ICON_WARNING = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK); //ImageFactory.getImage(ImageFactory.DECORATOR_IMAGE_WARNING);
    public static final Image ICON_INFO = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK); //ImageFactory.getImage(ImageFactory.DECORATOR_IMAGE_INFO);

    public static Image getProblemImageFor(int severity) {
        Image image;
        switch (severity) {
            case -1: image = null; break;
            case IProblemDecorationSupport.SEVERITY_ERROR: image = ICON_ERROR; break;
            case IProblemDecorationSupport.SEVERITY_WARNING: image = ICON_WARNING; break;
            case IProblemDecorationSupport.SEVERITY_INFO: image = ICON_INFO; break;
            default: throw new RuntimeException("invalid severity value");
        }
        return image;
    }

    /**
     * Finds the root figure for any figure
     */
    static public IFigure getRootFigure(IFigure anyFigure) {
        IFigure f = anyFigure;
        while (f.getParent() != null)
            f = f.getParent();
        return f;
    }

    static public void debugPrintRootFigureHierarchy(IFigure anyFigure) {
        debugPrintFigureHierarchy(getRootFigure(anyFigure), "");
    }

    /**
     * Debug function to display the figure hierarchy
     */
    static public void debugPrintFigureHierarchy(IFigure f, String indent) {
        Debug.println(indent + f.getClass().getSimpleName() + " @"
                + f.hashCode() + "  " + f.getBounds() + "  useLocalCoords=" + f.isCoordinateSystem());
        for (Object child : f.getChildren()) {
            debugPrintFigureHierarchy((IFigure) child, indent + ".   ");
        }
    }

    /**
     * Debug function to display the ancestor chain of a figure
     */
    static public void debugPrintFigureAncestors(IFigure f, String indent) {
        Debug.println(indent + f.getClass().getSimpleName() + " @"
                + f.hashCode() + "  " + f.getBounds() + "  useLocalCoords=" + f.isCoordinateSystem());
        if (f.getParent() != null) {
            debugPrintFigureAncestors(f.getParent(), indent + ".   ");
        }
    }

    /**
     * Adds a tooltip listener to the provided canvas. Figures that want to display a tooltip
     * have to implement ITooltipTextProvider.
     */
    static public void addTooltipSupport(final Canvas canvas, final IFigure rootFigure) {
        // SWT-based tooltip for figures
        Listener listener = new Listener() {
            public void handleEvent(Event e) {
                switch (e.type) {
                case SWT.MouseMove:
                    if (canvas.getToolTipText() != null)
                        canvas.setToolTipText(null);
                    break;
                case SWT.MouseHover:
                    String figureTooltip = getFigureTooltip(rootFigure, e.x, e.y);
                    // NOTE: should set the tooltip ONLY if it has changed otherwise
                    // it interferes with the HoverSupport
                    if (!ObjectUtils.equals(figureTooltip, canvas.getToolTipText()))
                        canvas.setToolTipText(figureTooltip);
                    break;
                }
            }
        };
        canvas.addListener(SWT.MouseMove, listener);
        canvas.addListener(SWT.MouseHover, listener);

    }

    static private String getFigureTooltip(IFigure rootFigure, int x, int y) {
        for (IFigure f = rootFigure.findFigureAt(x, y); f != null; f = f.getParent()) {
            if (f instanceof ITooltipTextProvider)
                return ((ITooltipTextProvider) f).getTooltipText(x, y);
        }
        return null;
    }
}
