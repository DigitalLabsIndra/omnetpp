//==========================================================================
//   CXMLPARIMPL.H  - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//  Author: Andras Varga
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2015 Andras Varga
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_CXMLPARIMPL_H
#define __OMNETPP_CXMLPARIMPL_H

#include "cparimpl.h"

NAMESPACE_BEGIN

/**
 * A cParImpl subclass that stores a module/channel parameter of type XML.
 *
 * @ingroup Internals
 */
class SIM_API cXMLParImpl : public cParImpl
{
  protected:
    // selector: flags & FL_ISEXPR
    cExpression *expr;
    cXMLElement *val;

  private:
    void copy(const cXMLParImpl& other);

  protected:
    void deleteOld();

  public:
    /** @name Constructors, destructor, assignment. */
    //@{

    /**
     * Constructor.
     */
    explicit cXMLParImpl();

    /**
     * Copy constructor.
     */
    cXMLParImpl(const cXMLParImpl& other) : cParImpl(other) {copy(other);}

    /**
     * Destructor.
     */
    virtual ~cXMLParImpl();

    /**
     * Assignment operator.
     */
    void operator=(const cXMLParImpl& otherpar);
    //@}

    /** @name Redefined cObject member functions */
    //@{

    /**
     * Creates and returns an exact copy of this object.
     */
    virtual cXMLParImpl *dup() const override  {return new cXMLParImpl(*this);}

    /**
     * Returns a multi-line description of the contained XML element.
     */
    virtual std::string detailedInfo() const override;

    /**
     * Serializes the object into a buffer.
     */
    virtual void parsimPack(cCommBuffer *buffer) const override;

    /**
     * Deserializes the object from a buffer.
     */
    virtual void parsimUnpack(cCommBuffer *buffer) override;
    //@}

    /** @name Redefined cParImpl setter functions. */
    //@{

    /**
     * Raises an error: cannot convert bool to XML.
     */
    virtual void setBoolValue(bool b) override;

    /**
     * Raises an error: cannot convert long to XML.
     */
    virtual void setLongValue(long l) override;

    /**
     * Raises an error: cannot convert double to XML.
     */
    virtual void setDoubleValue(double d) override;

    /**
     * Raises an error: cannot convert string to XML.
     */
    virtual void setStringValue(const char *s) override;

    /**
     * Sets the value to the given cXMLElement tree.
     */
    virtual void setXMLValue(cXMLElement *node) override;

    /**
     * Sets the value to the given expression. This object will
     * assume the responsibility to delete the expression object.
     */
    virtual void setExpression(cExpression *e) override;
    //@}

    /** @name Redefined cParImpl getter functions. */
    //@{

    /**
     * Raises an error: cannot convert XML to bool.
     */
    virtual bool boolValue(cComponent *context) const override;

    /**
     * Raises an error: cannot convert XML to long.
     */
    virtual long longValue(cComponent *context) const override;

    /**
     * Raises an error: cannot convert XML to double.
     */
    virtual double doubleValue(cComponent *context) const override;

    /**
     * Raises an error: cannot convert XML to string.
     */
    virtual const char *stringValue(cComponent *context) const override;

    /**
     * Raises an error: cannot convert XML to string.
     */
    virtual std::string stdstringValue(cComponent *context) const override;

    /**
     * Returns the value of the parameter.
     */
    virtual cXMLElement *xmlValue(cComponent *context) const override;

    /**
     * Returns pointer to the expression stored by the object, or NULL.
     */
    virtual cExpression *getExpression() const override;
    //@}

    /** @name Type, prompt text, input flag, change flag. */
    //@{

    /**
     * Returns XML.
     */
    virtual Type getType() const override;

    /**
     * Returns false.
     */
    virtual bool isNumeric() const override;
    //@}

    /** @name Redefined cParImpl misc functions. */
    //@{

    /**
     * Replaces for non-const values, replaces the stored expression with its
     * evaluation.
     */
    virtual void convertToConst(cComponent *context) override;

    /**
     * Returns the value in text form.
     */
    virtual std::string str() const override;

    /**
     * Converts from text.
     */
    virtual void parse(const char *text) override;

    /**
     * Object comparison.
     */
    virtual int compare(const cParImpl *other) const override;
    //@}
};

NAMESPACE_END


#endif


