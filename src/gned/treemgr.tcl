#==========================================================================
#  TREEMGR.TCL -
#            part of the GNED, the Tcl/Tk graphical topology editor of
#                            OMNeT++
#
#   By Andras Varga
#
#==========================================================================

#----------------------------------------------------------------#
#  Copyright (C) 1992-2003 Andras Varga
#
#  This file is distributed WITHOUT ANY WARRANTY. See the file
#  `license' for details on this and other legal matters.
#----------------------------------------------------------------#


# initTreeManager --
#
#
proc initTreeManager {} {
    global gned

    Tree:init $gned(manager).tree

    #
    # bindings for the tree
    #
    bind $gned(manager).tree <Button-1> {
        catch {destroy .popup}
        set key [Tree:nodeat %W %x %y]
        if {$key!=""} {
            Tree:setselection %W $key
        }
        dragAndDropStart $key %X %Y
    }

    bind $gned(manager).tree <B1-Motion> {
        dragAndDropMotion %X %Y
    }

    bind $gned(manager).tree <ButtonRelease-1> {
        dragAndDropFinish %X %Y
    }

    bind $gned(manager).tree <Double-1> {
        set key [Tree:nodeat %W %x %y]
        if {$key!=""} {
            # Tree:toggle %W $key
            treemanagerDoubleClick $key
        }
    }

    bind $gned(manager).tree <Button-3> {
        set key [Tree:nodeat %W %x %y]
        if {$key!=""} {
            Tree:setselection %W $key
            treemanagerPopup $key %X %Y
        }
    }
}


# updateTreeManager --
#
# Redraws the manager window (left side of main window).
#
proc updateTreeManager {} {
    global gned

    Tree:build $gned(manager).tree
}

# getNodeInfo --
#
# This user-supplied function gets called by the tree widget to get info about
# tree nodes. The widget itself only stores the state (open/closed) of the
# nodes, everything else comes from this function.
#
proc getNodeInfo {w op {key {}}} {
    global ned ned_desc icons

    switch $op {

      root {
        return 0
      }

      text {
        # FIXME: this should turn into a call to some itemDescription proc
        if {[info exist ned($key,name)] && [info exist ned($key,vectorsize)] && $ned($key,vectorsize)!=""} {
          # return "$key:$ned($key,type) $ned($key,name)\[$ned($key,vectorsize)\]"
          return "$ned($key,type) $ned($key,name)\[$ned($key,vectorsize)\]"
        } elseif {[info exist ned($key,name)] && [info exist ned($key,size)] && $ned($key,size)!=""} {
          # return "$key:$ned($key,type) $ned($key,name)\[$ned($key,size)\]"
          return "$ned($key,type) $ned($key,name)\[$ned($key,size)\]"
        } elseif {[info exist ned($key,name)] && [info exist ned($key,isvector)] && $ned($key,isvector)} {
          # return "$key:$ned($key,type) $ned($key,name)\[\]"
          return "$ned($key,type) $ned($key,name)\[\]"
        } elseif [info exist ned($key,name)] {
          # return "$key:$ned($key,type) $ned($key,name)"
          return "$ned($key,type) $ned($key,name)"
        } else {
          # return "$key:$ned($key,type)"
          return "$ned($key,type)"
        }
      }

      options {
        if [info exist ned($key,aux-isdirty)] {
          if {$ned($key,aux-isdirty)} {
             return "-fill #ff0000"
          } else {
             return ""
          }
        }
      }

      icon {
        set type $ned($key,type)
        if [info exist ned_desc($type,treeicon)] {
          return $icons($ned_desc($type,treeicon))
        } else {
          return $icons($ned_desc(root,treeicon))
        }
      }

      parent {
        return $ned($key,parentkey)
      }

      children {
        return $ned($key,childrenkeys)
      }

      haschildren {
        return [expr [llength $ned($key,childrenkeys)]!=0]

        ## OLD CODE: only allow top-level components (modules, channels etc.) to be displayed
        # set type $ned($key,type)
        # if {$type=="root" || $type=="nedfile"} {
        #   return [expr [llength $ned($key,childrenkeys)]!=0]
        # } else {
        #   return 0
        #}
      }
    }
}


#------------------------------
# Bindings for the tree manager
#------------------------------

proc treemanagerDoubleClick {key} {
    global ned gned

    if {$ned($key,type)=="nedfile"} {
        Tree:toggle $gned(manager).tree $key
    } elseif {$ned($ned($key,parentkey),type)=="nedfile"} {
        # top-level item
        if {$ned($key,type)=="module"} {
            openModuleOnCanvas $key
        } else {
            editProps $key
        }
    } else {
        Tree:toggle $gned(manager).tree $key
    }
}

proc treemanagerPopup {key x y} {
    global ned

    catch {destroy .popup}
    menu .popup -tearoff 0

    if {$ned($key,type)=="nedfile"} {
        nedfilePopup $key
    } elseif {$ned($ned($key,parentkey),type)=="nedfile"} {
        toplevelComponentPopup $key
    } else {
        defaultPopup $key
    }
    .popup post $x $y
}

proc nedfilePopup {key} {
    global ned

    foreach i {
      {cascade -menu .popup.newmenu -label {New} -underline 0}
      {separator}
      {command -command "fileSave $key" -label {Save} -underline 0}
      {command -command "fileSaveAs $key" -label {Save As...} -underline 1}
      {command -command "fileCloseNedfile $key" -label {Close} -underline 0}
      {separator}
      {command -command "displayCodeForItem $key" -label {Show NED code...} -underline 0}
      {separator}
      {command -command "moveUpItem $key; updateTreeManager" -label {Move up} -underline 5}
      {command -command "moveDownItem $key; updateTreeManager" -label {Move down} -underline 5}
    } {
       eval .popup add $i
    }

    menu .popup.newmenu -tearoff 0
    foreach i {
      {command -command "addItem imports $key; updateTreeManager" -label {imports} -underline 0}
      {command -command "addItemWithUniqueName channel $key; updateTreeManager" -label {channel} -underline 0}
      {command -command "addItemWithUniqueName simple $key;  updateTreeManager" -label {simple module}  -underline 0}
      {command -command "addItemWithUniqueName module $key;  updateTreeManager" -label {compound module}  -underline 0}
      {command -command "addItemWithUniqueName network $key; updateTreeManager" -label {network} -underline 0}
    } {
       eval .popup.newmenu add $i
    }
}

proc toplevelComponentPopup {key} {
    global ned

    .popup add command -command "editProps $key" -label {Properties...} -underline 0
    if {$ned($key,type)=="module"} {
      .popup add command -command "openModuleOnCanvas $key" -label {Open on canvas} -underline 0
    }
    .popup add command -command "displayCodeForItem $key" -label {Show NED code...} -underline 0
    if {$ned($key,type)=="module" || $ned($key,type)=="simple"} {
       .popup add separator
       .popup add command -command "createSubmoduleOnCanvas $key" -label {Drop an instance onto the canvas} -underline 1
    }
    foreach i {
      {separator}
      {command -command "moveUpItem $key; updateTreeManager" -label {Move up} -underline 5}
      {command -command "moveDownItem $key; updateTreeManager" -label {Move down} -underline 5}
      {separator}
      {command -command "deleteItem $key; updateTreeManager" -label {Delete}}
    } {
       eval .popup add $i
    }
}

proc defaultPopup {key} {
    global ned

    foreach i {
      {command -command "displayCodeForItem $key" -label {Show NED fragment...} -underline 0}
      {separator}
      {command -command "moveUpItem $key; updateTreeManager" -label {Move up} -underline 5}
      {command -command "moveDownItem $key; updateTreeManager" -label {Move down} -underline 5}
      {separator}
      {command -command "deleteItem $key; updateTreeManager" -label {Delete}}
    } {
       eval .popup add $i
    }
}

proc moveUpItem {key} {
    global ned

    set parentkey $ned($key,parentkey)
    set l $ned($parentkey,childrenkeys)

    set pos [lsearch -exact $l $key]
    if {$pos>0} {
        # swap with prev item
        set prevpos [expr $pos-1]
        set prevkey [lindex $l $prevpos]
        set ned($parentkey,childrenkeys) \
            [lreplace $l $prevpos $pos $key $prevkey]

        # nedfile changed
        if {$parentkey!=0} {
            markNedfileOfItemDirty $parentkey
        }
    }
}

proc moveDownItem {key} {
    global ned

    set parentkey $ned($key,parentkey)
    set l $ned($parentkey,childrenkeys)

    set pos [lsearch -exact $l $key]
    if {$pos<[expr [llength $l]-1]} {
        # swap with next item
        set nextpos [expr $pos+1]
        set nextkey [lindex $l $nextpos]
        set ned($parentkey,childrenkeys) \
            [lreplace $l $pos $nextpos $nextkey $key]

        # nedfile changed
        if {$parentkey!=0} {
            markNedfileOfItemDirty $parentkey
        }
    }
}

proc displayCodeForItem {key} {
    global ned fonts

    if [info exist ned($key,name)] {
        set txt "$ned($key,type) $ned($key,name)"
    } else {
        set txt "$ned($key,type)"
    }


    # open file viewer/editor window
    set w .nedcode
    catch {destroy $w}

    # create widgets
    toplevel $w -class Toplevel
    wm focusmodel $w passive
    #wm maxsize $w 1009 738
    wm minsize $w 1 1
    wm overrideredirect $w 0
    wm resizable $w 1 1
    wm title $w "NED code -- $txt"

    frame $w.main
    scrollbar $w.main.sb -borderwidth 1 -command "$w.main.text yview"
    pack $w.main.sb -anchor center -expand 0 -fill y -side right
    text $w.main.text -yscrollcommand "$w.main.sb set" \
         -wrap none -font $fonts(fixed) -relief sunken -bg #a0e0a0
    pack $w.main.text -anchor center -expand 1 -fill both -side left

    frame $w.butt
    button $w.butt.close -text Close -command "destroy $w"
    pack $w.butt.close -anchor n -side right -expand 0

    pack $w.butt -expand 0 -fill x -side bottom -padx 5 -pady 5
    pack $w.main -expand 1 -fill both -side top -padx 5 -pady 5
    focus $w.main.text

    # produce ned code and put it into text widget
    set nedcode [generateNed $key]
    $w.main.text insert end $nedcode
    $w.main.text mark set insert 1.0
    syntaxHighlight $w.main.text 1.0 end

    # set dimensions and disable widget (one can't insert text into a disabled
    # widget even from program)
    set numlines [lindex [split [$w.main.text index end] "."] 0]
    set height [expr $numlines>28 ? 30 : $numlines+2]
    $w.main.text config -state disabled -height $height -width 60

    # bindings
    bind $w <Key-Escape> "destroy $w"
    focus $w.butt.close
}


