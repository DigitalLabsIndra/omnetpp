#=================================================================
#  GNED.TCL - part of
#
#                     OMNeT++/OMNEST
#            Discrete System Simulation in C++
#
#=================================================================

#----------------------------------------------------------------#
#  Copyright (C) 1992-2004 Andras Varga
#
#  This file is distributed WITHOUT ANY WARRANTY. See the file
#  `license' for details on this and other legal matters.
#----------------------------------------------------------------#

#
# intro text
#
puts {GNED 3.0a3 - Graphical Network Editor, part of OMNeT++
(c) 1992-2004 Andras Varga
See the license for distribution terms and warranty disclaimer.

GNED uses human-readable NED as the ONLY file format. It is a fully
two-way tool: you can edit the modules in graphics or in NED source form,
and switch to the other view any time.
}

#
# Load library files
#

# OMNETPP_GNED_DIR is set from gned.cc
if [info exist OMNETPP_GNED_DIR] {

   set dir $OMNETPP_GNED_DIR
   source [file join $dir combobox.tcl]
   source [file join $dir datadict.tcl]
   source [file join $dir widgets.tcl]
   source [file join $dir data.tcl]
   source [file join $dir canvas.tcl]
   source [file join $dir drawitem.tcl]
   source [file join $dir plotedit.tcl]
   source [file join $dir canvlbl.tcl]
   source [file join $dir textedit.tcl]
   source [file join $dir findrepl.tcl]
   source [file join $dir drawopts.tcl]
   source [file join $dir fileview.tcl]
   source [file join $dir loadsave.tcl]
   source [file join $dir makened.tcl]
   source [file join $dir genxml.tcl]
   source [file join $dir parsexml.tcl]
   source [file join $dir parsened.tcl]
   source [file join $dir dispstr.tcl]
   source [file join $dir menuproc.tcl]
   source [file join $dir switchvi.tcl]
   source [file join $dir icons.tcl]
   source [file join $dir tree.tcl]
   source [file join $dir treemgr.tcl]
   source [file join $dir dragdrop.tcl]
   source [file join $dir main.tcl]
   source [file join $dir gnedrc.tcl]
   source [file join $dir balloon.tcl]
   source [file join $dir props.tcl]
   source [file join $dir chanprops.tcl]
   source [file join $dir connprops.tcl]
   source [file join $dir imptprops.tcl]
   source [file join $dir modprops.tcl]
   source [file join $dir netwprops.tcl]
   source [file join $dir props.tcl]
   source [file join $dir submprops.tcl]
}

#
# Exec startup code
#
proc startGNED {argv} {
   global config OMNETPP_BITMAP_PATH HAVE_BLT

   wm withdraw .
   checkTclTkVersion
   setupTkOptions
   init_balloons
   createMainWindow
   loadBitmaps $OMNETPP_BITMAP_PATH
   fileNewNedfile

   if {!$HAVE_BLT} {
      puts "\n*** BLT NOT FOUND. Please install this Tcl/Tk extension to get an improved GUI!"
   }

   if [file readable $config(configfile)] {
       loadConfig $config(configfile)
       set config(connmodeauto) 1  ;# FIXME deliberately change this setting; this line may be removed in the future
   }
   reflectConfigInGUI

   set convertandexit 0
   set imgsuffix "gif"
   set outdir "html"
   set files {}
   processCommandline $argv convertandexit imgsuffix outdir files

   if {!$convertandexit} {
       wm deiconify .
   }

   foreach fname $files {
       if {!$convertandexit || [string match "*.ned" $fname]} {
           if [file exist $fname] {
               loadNED $fname
           } else {
               fileNewNedfile $fname
           }
       }
   }

   # implement the -c option
   if {$convertandexit} {
       # just save the canvases to file then exit
       exportCanvasesToPostscript $outdir [file join $outdir "images.xml"] $imgsuffix
       fileExit
   }
}

proc processCommandline {argv _convertandexit _imgsuffix _outdir _files} {
   upvar $_convertandexit convertandexit
   upvar $_imgsuffix imgsuffix
   upvar $_outdir outdir
   upvar $_files files

   global tcl_platform

   #foreach arg $argv ..
   for {set i 0} {$i<[llength $argv]} {incr i} {
       set arg [lindex $argv $i]
       if {$tcl_platform(platform) == "windows"} {
          # Tcl on Windows doesn't like backslashes in file names
          regsub -all -- {\\} $arg "/" arg
       }
       if {$arg == "--"} {
           # ignore
       } elseif {$arg == "-c"} {
           incr i
           set convertandexit 1
           set arg [lindex $argv $i]
           if {$arg!=""} {set outdir $arg}
       } elseif {$arg == "-e"} {
           incr i
           set arg [lindex $argv $i]
           if {$arg!=""} {set imgsuffix $arg}
       } elseif {[string match "-*" $arg]} {
           puts stderr "<!> warning: invalid argument $arg, ignoring"
       } elseif {[string match "@*" $arg]} {
           # list file
           if [catch {
               set fname [string range $arg 1 end]
               set f [open $fname "r"]
               set listfile [read $f]
               if {$tcl_platform(platform) == "windows"} {
                  # Tcl on Windows doesn't like backslashes in file names
                  regsub -all -- {\\} $listfile "/" listfile
               }
               close $f
           } err] {
               puts stderr "<!> warning: could not read list file '$fname' ($err), ignoring"
           } else {
               foreach line $listfile {
                   foreach fname [glob -nocomplain $line] {
                       lappend files $fname
                   }
               }
           }
       } else {
           # expand wildcards (on Windows, the shell doesn't do it for us)
           if [catch {
               glob $arg
           }] {
               # probably a new file that should be created
               lappend files $arg
           } else {
               foreach fname [glob -nocomplain $arg] {
                   lappend files $fname
               }
           }
       }
   }
}

