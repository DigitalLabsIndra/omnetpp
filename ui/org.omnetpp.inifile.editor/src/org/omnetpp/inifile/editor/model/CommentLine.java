/**
 * 
 */
package org.omnetpp.inifile.editor.model;


class CommentLine extends InifileLine {
	public CommentLine(int lineNumber, String rawText, String comment) {
		super(COMMENT, lineNumber, rawText, comment);
	}
}