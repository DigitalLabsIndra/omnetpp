//==========================================================================
//   CPOLYMORPHIC.H - header for
//                             OMNeT++
//            Discrete System Simulation in C++
//
//  Declaration of the following classes:
//    cPolymorphic : general base class
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2004 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CPOLYMORPHIC_H
#define __CPOLYMORPHIC_H

#include "defs.h"
#include "opp_string.h"

/**
 * This is the ultimate base class for cObject, and thus for nearly all
 * OMNeT++ classes. cPolymorphic is a <b>lightweight common base class</b>:
 * it only contains virtual member functions but NO DATA MEMBERS at all.
 *
 * It is recommended to use cPolymorphic as a base class for any class
 * that has at least one virtual member function. This makes the class more
 * interoperable with OMNeT++, and causes no extra overhead at all.
 * sizeof(cPolymorphic) should yield 4 on a 32-bit architecture (4-byte
 * <i>vptr</i>, and using cPolymorphic as a base class doesn't add anything
 * to the size because a class with a virtual function already has a vptr.
 *
 * cPolymorphic allows the object to be displayed in graphical user
 * interface (Tkenv) via the className(), info() and detailedInfo() methods.
 *
 * Using cPolymorphic also strengthens type safety. <tt>cPolymorphic *</tt>
 * pointers should replace <tt>void *</tt> in most places where you need
 * pointers to "any data structure". Using cPolymorphic will allow safe
 * downcasts using <tt>dynamic_cast</tt>.
 *
 * @ingroup SimCore
 */
class SIM_API cPolymorphic
{
  public:
    /**
     * Constructor. It has an empty body. (The class doesn't have data members
     * and there's nothing special to do at construction time.)
     */
    cPolymorphic() {}

    /**
     * Destructor. It has an empty body (the class doesn't have data members.)
     * It is declared here only to make the class polymorphic and make its
     * destructor virtual.
     */
    virtual ~cPolymorphic() {}

    /**
     * Returns a pointer to the class name string. This method is implemented
     * using typeid (C++ RTTI), and it does not need to be overridden
     * in subclasses.
     */
    virtual const char *className() const;

    /**
     * Can be redefined to produce a one-line description of object into `buf'.
     * The string appears in the graphical user interface (Tkenv) e.g. when
     * the object is displayed in a listbox.
     *
     * The provided buffer is of finite size (MAX_OBJECTINFO, currently 500),
     * and the function should take care not to overrun it. Creating a very
     * long description is useless anyway, because it will be displayed all
     * on one line.
     *
     * @see detailedInfo()
     */
    virtual void info(char *buf)  {*buf='\0';}

    /**
     * Can be redefined to produce a detailed, multi-line, arbitrarily long
     * description of the object. The buffer can be expanded as needed using
     * member functions of opp_string. The string appears in the graphical
     * user interface (Tkenv) together with other object data (e.g. class name)
     * wherever it is feasible to display a multi-line string.
     *
     * The function should return the same buf object it received.
     */
    virtual opp_string& detailedInfo(opp_string& buf)  {return buf;}
};

#endif

