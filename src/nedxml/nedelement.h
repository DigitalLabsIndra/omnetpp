//==========================================================================
// nedelement.h  - part of the OMNeT++ Discrete System Simulation System
//
// Contents:
//   class NEDElement
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2003 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __NEDELEMENT_H
#define __NEDELEMENT_H

#ifdef _MSC_VER
// disable tons of "identifier was truncated to '255' characters in the debug
// information" warnings that comes as bonus with using STL
#pragma warning(disable:4786)
#endif

#include <string>

/**
 * Base class for objects in a NED object tree, the XML-based
 * in-memory representation for NED files. An instance of a NEDElement
 * subclass represent an XML element.
 * NEDElement provides a DOM-like, generic access to the tree;
 * subclasses additionally provide a typed interface.
 *
 * @ingroup Data
 */
class NEDElement
{
  private:
    int id;
    std::string srcloc;
    NEDElement *parent;
    NEDElement *firstchild;
    NEDElement *lastchild;
    NEDElement *prevsibling;
    NEDElement *nextsibling;
    static long lastid;

  protected:
    static bool stringToBool(const char *s);
    static const char *boolToString(bool b);
    static int stringToEnum(const char *s, const char *vals[], int nums[], int n);
    static const char *enumToString(int b, const char *vals[], int nums[], int n);

  public:
    /** @name Constructor, destructor */
    //@{

    /**
     * Constructor
     */
    NEDElement();

    /**
     * Constructor. Takes parent element.
     */
    NEDElement(NEDElement *parent);

    /**
     * Destructor. Destroys children too.
     */
    virtual ~NEDElement();
    //@}

    /** @name Common properties */
    //@{

    /**
     * Overridden in subclasses to return the name of the XML element the class
     * represents.
     */
    virtual const char *getTagName() const = 0;

    /**
     * Overridden in subclasses to return the numeric code (NED_xxx) of the
     * XML element the class represents.
     */
    virtual int getTagCode() const = 0;

    /**
     * Returns a unique id, originally set by the contructor.
     */
    virtual long getId() const;

    /**
     * Unique id assigned by the constructor can be overwritten here.
     */
    virtual void setId(long id);

    /**
     * Returns a string containing a file/line position showing where this
     * element originally came from.
     */
    virtual const char *getSourceLocation() const;

    /**
     * Sets location string (a string containing a file/line position showing
     * where this element originally came from). Called by the (NED/XML) parser.
     */
    virtual void setSourceLocation(const char *loc);
    //@}

    /** @name Generic access to attributes (Methods have to be redefined in subclasses!) */
    //@{

    /**
     * Sets every attribute to its default value (as returned by getAttributeDefault()).
     * Attributes without a default value are not affected.
     *
     * This method is called from the constructors of derived classes.
     */
    virtual void applyDefaults();

    /**
     * Pure virtual method, it should be redefined in subclasses to return
     * the number of attributes defined in the DTD.
     */
    virtual int getNumAttributes() const = 0;

    /**
     * Pure virtual method, it should be redefined in subclasses to return
     * the name of the kth attribute as defined in the DTD.
     *
     * It should return NULL if k is out of range (i.e. negative or greater than
     * getNumAttributes()).
     */
    virtual const char *getAttributeName(int k) const = 0;

    /**
     * Returns the index of the given attribute. It returns -1 if the attribute
     * is not found. Relies on getNumAttributes() and getAttributeName().
     */
    virtual int lookupAttribute(const char *attr) const;

    /**
     * Pure virtual method, it should be redefined in subclasses to return
     * the value of the kth attribute (i.e. the attribute with the name
     * getAttributeName(k)).
     *
     * It should return NULL if k is out of range (i.e. negative or greater than
     * getNumAttributes()).
     */
    virtual const char *getAttribute(int k) const = 0;

    /**
     * Returns the value of the attribute with the given name.
     * Relies on lookupAttribute() and getAttribute().
     *
     * It returns NULL if the given attribute is not found.
     */
    virtual const char *getAttribute(const char *attr) const;

    /**
     * Pure virtual method, it should be redefined in subclasses to set
     * the value of the kth attribute (i.e. the attribute with the name
     * getAttributeName(k)).
     *
     * If k is out of range (i.e. negative or greater than getNumAttributes()),
     * the call should be ignored.
     */
    virtual void setAttribute(int k, const char *value) = 0;

    /**
     * Sets the value of the attribute with the given name.
     * Relies on lookupAttribute() and setAttribute().
     *
     * If the given attribute is not found, the call has no effect.
     */
    virtual void setAttribute(const char *attr, const char *value);

    /**
     * Pure virtual method, it should be redefined in subclasses to return
     * the default value of the kth attribute, as defined in the DTD.
     *
     * It should return NULL if k is out of range (i.e. negative or greater than
     * getNumAttributes()).
     */
    virtual const char *getAttributeDefault(int k) const = 0;

    /**
     * Returns the default value of the given attribute, as defined in the DTD.
     * Relies on lookupAttribute() and getAttributeDefault().
     *
     * It returns NULL if the given attribute is not found.
     */
    virtual const char *getAttributeDefault(const char *attr) const;
    //@}

    /** @name Generic access to children and siblings */
    //@{

    /**
     * Returns the parent element, or NULL if this element has no parent.
     */
    virtual NEDElement *getParent() const;

    /**
     * Returns pointer to the first child element, or NULL if this element
     * has no children.
     */
    virtual NEDElement *getFirstChild() const;

    /**
     * Returns pointer to the last child element, or NULL if this element
     * has no children.
     */
    virtual NEDElement *getLastChild() const;

    /**
     * Returns pointer to the next sibling of this element (i.e. the next child
     * in the parent element). Return NULL if there're no subsequent elements.
     *
     * getFirstChild() and getNextSibling() can be used to loop through
     * the child list:
     *
     * <pre>
     * for (NEDElement *child=node->getFirstChild(); child; child = child->getNextSibling())
     * {
     *    ...
     * }
     * </pre>
     *
     */
    virtual NEDElement *getNextSibling() const;

    /**
     * Appends the given element at the end of the child element list.
     *
     * The node pointer passed should not be NULL.
     */
    virtual void appendChild(NEDElement *node);

    /**
     * Inserts the given element just before the specified child element
     * in the child element list.
     *
     * The where element must be a child of this element.
     * The node pointer passed should not be NULL.
     */
    virtual void insertChildBefore(NEDElement *where, NEDElement *newnode);

    /**
     * Removes the given element from the child element list.
     *
     * The pointer passed should be a child of this element.
     */
    virtual NEDElement *removeChild(NEDElement *node);

    /**
     * Returns pointer to the first child element with the given tag code,
     * or NULL if this element has no such children.
     */
    virtual NEDElement *getFirstChildWithTag(int tagcode) const;

    /**
     * Returns pointer to the next sibling of this element with the given
     * tag code. Return NULL if there're no such subsequent elements.
     *
     * getFirstChildWithTag() and getNextSiblingWithTag() are a convient way
     * to loop through elements with a certain tag code in the child list:
     *
     * <pre>
     * for (NEDElement *child=node->getFirstChildWithTag(tagcode); child; child = child->getNextSiblingWithTag(tagcode))
     * {
     *     ...
     * }
     * </pre>
     */
    virtual NEDElement *getNextSiblingWithTag(int tagcode) const;
    //@}
};

#endif

