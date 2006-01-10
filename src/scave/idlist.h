//=========================================================================
//  IDLIST.H - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef _IDLIST_H_
#define _IDLIST_H_

#include <vector>
#include <exception>
#include <stdexcept>

class ResultFileManager;

// define int64, our equivalent of Java's "long" type
#ifdef _MSC_VER
typedef __int64 int64;
#else
typedef long long int64;
#endif


/**
 * Result ID -- identifies a scalar or a vector in a ResultFileManager
 */
typedef int64 ID;


/**
 * Stores a set of unique IDs. Order is not important, and may occasionally
 * change (after merge(), substract() or intersect()).
 *
 * Beware: Copy ctor implements transfer-of-ownership semantics!
 */
class IDList
{
    private:
        friend class ResultFileManager;
        typedef std::vector<ID> V;
        V *v;

        void operator=(const IDList&); // undefined, to prevent calling it
        void checkV() const {if (!v) throw new std::runtime_error("this is a zombie IDList");}
        void uncheckedAdd(ID id) {v->push_back(id);} // doesn't check if already in there
        void discardDuplicates();
        void checkIntegrity(ResultFileManager *mgr) const;
        void checkIntegrityAllScalars(ResultFileManager *mgr) const;
        void checkIntegrityAllVectors(ResultFileManager *mgr) const;

    public:
        IDList()  {v = new V;}
        IDList(unsigned int sz)  {v = new V(sz);}
        IDList(const IDList& ids); // transfer of ownership semantics!
        ~IDList()  {delete v;}
        unsigned int size() const  {checkV(); return v->size();}
        bool isEmpty() const  {checkV(); return v->empty();}
        void clear()  {checkV(); v->clear();}
        void set(const IDList& ids);
        void add(ID x);
        ID get(int i) const {checkV(); return v->at(i);} // at() includes bounds check
        void set(int i, ID x);
        void erase(int i);
        void substract(ID x); // this -= {x}
        void merge(IDList& ids);  // this += ids
        void substract(IDList& ids);  // this -= ids
        void intersect(IDList& ids);  // this = intersection(this,ids)
        IDList getSubsetByIndices(int *array, int n) const;
        IDList dup() const;
        void sortByFileAndRunRef(ResultFileManager *mgr);
        int itemTypes() const;  // SCALAR, VECTOR or their binary OR
        bool areAllScalars() const;
        bool areAllVectors() const;
        // sorting
        void sortByDirectory(ResultFileManager *mgr, bool ascending);
        void sortByFileName(ResultFileManager *mgr, bool ascending);
        void sortByRun(ResultFileManager *mgr, bool ascending);
        void sortByModule(ResultFileManager *mgr, bool ascending);
        void sortByName(ResultFileManager *mgr, bool ascending);
        void sortScalarsByValue(ResultFileManager *mgr, bool ascending);
        void sortVectorsByLength(ResultFileManager *mgr, bool ascending);
        void sortVectorsByMean(ResultFileManager *mgr, bool ascending);
        void sortVectorsByStdDev(ResultFileManager *mgr, bool ascending);
        void reverse();
        void toByteArray(char *array, int n) const;
        void fromByteArray(char *array, int n);
};

#endif


