//==========================================================================
//  LAYOUTERENV.H - part of
//
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2015 Andras Varga
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_LAYOUTERENV_H
#define __OMNETPP_LAYOUTERENV_H

#include "layout/graphlayouter.h"
#include "qtenvdefs.h"

NAMESPACE_BEGIN

class cModule;
class cDisplayString;

namespace qtenv {


class TkenvGraphLayouterEnvironment : public GraphLayouterEnvironment
{
   protected:
      // configuration
      const char *widgetToGrab;
      const char *canvas;
      Tcl_Interp *interp;
      cModule *parentModule;
      const cDisplayString& displayString;

      // state
      struct timeval beginTime;
      struct timeval lastCheck;
      bool grabActive;

   public:
      TkenvGraphLayouterEnvironment(Tcl_Interp *interp, cModule *parentModule, const cDisplayString& displayString);

      void setWidgetToGrab(const char *w) { this->widgetToGrab = w; }
      void setCanvas(const char *canvas) { this->canvas = canvas; }

      void cleanup();

      virtual bool inspected() { return canvas && interp; }
      virtual bool okToProceed();

      virtual bool getBoolParameter(const char *tagName, int index, bool defaultValue);
      virtual long getLongParameter(const char *tagName, int index, long defaultValue);
      virtual double getDoubleParameter(const char *tagName, int index, double defaultValue);

      virtual void clearGraphics();
      virtual void showGraphics(const char *text);
      virtual void drawText(double x, double y, const char *text, const char *tags, const char *color);
      virtual void drawLine(double x1, double y1, double x2, double y2, const char *tags, const char *color);
      virtual void drawRectangle(double x1, double y1, double x2, double y2, const char *tags, const char *color);
};

} //namespace
NAMESPACE_END


#endif