//==========================================================================
//  CMERSENNETWISTER.CC - part of
//                 OMNeT++/OMNEST
//              Discrete System Simulation in C++
//
// Contents:
//   class cMersenneTwister
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2002-2015 Andras Varga
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_CMERSENNETWISTER_H
#define __OMNETPP_CMERSENNETWISTER_H

#include "simkerneldefs.h"
#include "globals.h"
#include "crng.h"
#include "cconfiguration.h"
#include "mersennetwister.h"

NAMESPACE_BEGIN


/**
 * Wraps the Mersenne Twister RNG by Makoto Matsumoto and Takuji Nishimura.
 * Cycle length is 2^19937-1, and 623-dimensional equidistribution property
 * is assured.
 *
 * http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/ewhat-is-mt.html
 *
 * Actual code used is MersenneTwister.h from Richard J. Wagner,
 * v1.0, 15 May 2003, rjwagner@writeme.com.
 *
 * http://www-personal.engin.umich.edu/~wagnerr/MersenneTwister.html
 */
class SIM_API cMersenneTwister : public cRNG
{
  protected:
    MTRand rng;

  public:
    cMersenneTwister() {}
    virtual ~cMersenneTwister() {}

    /** Sets up the RNG. */
    virtual void initialize(int seedSet, int rngId, int numRngs,
                            int parsimProcId, int parsimNumPartitions,
                            cConfiguration *cfg) override;

    /** Tests correctness of the RNG */
    virtual void selfTest() override;

    /** Random integer in the range [0,intRandMax()] */
    virtual unsigned long intRand() override;

    /** Maximum value that can be returned by intRand() */
    virtual unsigned long intRandMax() override;

    /** Random integer in [0,n), n < intRandMax() */
    virtual unsigned long intRand(unsigned long n) override;

    /** Random double on the [0,1) interval */
    virtual double doubleRand() override;

    /** Random double on the (0,1) interval */
    virtual double doubleRandNonz() override;

    /** Random double on the [0,1] interval */
    virtual double doubleRandIncl1() override;
};

NAMESPACE_END


#endif

