package org.omnetpp.inifile.editor.model;

import static org.omnetpp.inifile.editor.model.ConfigRegistry.CFGID_DESCRIPTION;
import static org.omnetpp.inifile.editor.model.ConfigRegistry.CFGID_EXTENDS;
import static org.omnetpp.inifile.editor.model.ConfigRegistry.CFGID_NETWORK;
import static org.omnetpp.inifile.editor.model.ConfigRegistry.EXTENDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.omnetpp.common.ui.HoverSupport;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.inifile.editor.model.InifileAnalyzer.KeyType;
import org.omnetpp.ned.model.ex.ParamNodeEx;
import org.omnetpp.ned.model.pojo.ParamNode;
import org.omnetpp.ned.model.pojo.SubmoduleNode;

/**
 * Produces hover information for various Inifile parts.
 * 
 * @author Andras
 */
public class InifileHoverUtils {
	/**
	 * Generates tooltip for an inifile section.
	 * @param section  null is accepted
	 */
	public static String getSectionHoverText(String section, IInifileDocument doc, InifileAnalyzer analyzer, boolean allProblems) {
		if (section == null || !doc.containsSection(section))
			return null;

		// problem markers: display the section header's and extends's errors as text,
		// then display how many more there are within the section  
		IMarker[] markers1 = InifileUtils.getProblemMarkersFor(section, null, doc);
		IMarker[] markers2 = InifileUtils.getProblemMarkersFor(section, EXTENDS, doc);
		IMarker[] allMarkers = InifileUtils.getProblemMarkersForWholeSection(section, doc);
		List<IMarker> markersDisplayed = new ArrayList<IMarker>();
		markersDisplayed.addAll(Arrays.asList(markers1));
		markersDisplayed.addAll(Arrays.asList(markers2));

		int numErrors = 0, numWarnings = 0;
		for (IMarker m : allMarkers) {
			if (!markersDisplayed.contains(m)) {
				switch (m.getAttribute(IMarker.SEVERITY, -1)) {
				case IMarker.SEVERITY_ERROR: numErrors++; break;
				case IMarker.SEVERITY_WARNING: numWarnings++; break;
				}
			}
		}

		String numErrorsText = "";
		if (numErrors+numWarnings > 0) {
			if (numErrors>0 && numWarnings>0)
				numErrorsText += numErrors + " error(s) and " + numWarnings + " warning(s)"; 
			else if (numErrors>0)
				numErrorsText += numErrors + " error(s)"; 
			else if (numWarnings>0)
				numErrorsText += numWarnings + " warning(s)";
			if (markersDisplayed.size() > 0)
				numErrorsText += " more";
			numErrorsText += " in the section body";

			numErrorsText = "<i>" + numErrorsText + "</i><br><br>";
		}

		String text = getProblemsHoverText(markersDisplayed.toArray(new IMarker[]{}), false) + numErrorsText;
		
		// name and description
		text += "<b>"+section+"</b>";
		String description = doc.getValue(section, "description");
		if (description != null)
			text += " -- " + description;
		text += "<br>\n";

		// section chain
		String[] sectionChain = InifileUtils.resolveSectionChain(doc, section);
		if (sectionChain.length >= 2)
			text += "Fallback order: " + StringUtils.join(sectionChain, " &gt; ") + " <br>\n"; //XXX decide terminology: "Lookup order" or "Section fallback chain" ? also: "," or ">" ?

		// network
		String networkName = InifileUtils.lookupConfig(sectionChain, CFGID_NETWORK.getKey(), doc);
		text += "Network: " + (networkName==null ? "not set" : networkName) + " <br>\n";

		// unassigned parameters
		if (analyzer != null) {
			ParamResolution[] resList = analyzer.getUnassignedParams(section);
			if (resList.length==0) {
				if (networkName != null)
					text += "<br>\nNo unassigned NED parameters in this section.";
			}
			else {
				text += "<br>\nThis section does not seem to assign the following NED parameters:\n<ul>";
				for (ParamResolution res : resList)
					text += " <li>" + res.moduleFullPath + "." +res.paramDeclNode.getName() + "</li>\n";
				text += "</ul>";
			}
		}
		return HoverSupport.addHTMLStyleSheet(text);
	}

	/**
	 * Generate tooltip for an inifile entry.
	 * @param section  null is accepted
	 * @param key      null is accepted
	 */
	//XXX should tolerate analyzer==null
	public static String getEntryHoverText(String section, String key, IInifileDocument doc, InifileAnalyzer analyzer) {
		if (section == null || key == null || !doc.containsKey(section, key))
			return null;

		KeyType keyType = (key == null) ? KeyType.CONFIG : InifileAnalyzer.getKeyType(key);
		if (keyType==KeyType.CONFIG) {
			// config key: display description
			return getConfigHoverText(section, key, doc);
		}
		else if (keyType == KeyType.PARAM || key.endsWith(".apply-default")) { //XXX hardcoded key name
			// parameter assignment: display which parameters it matches
			return getParamKeyHoverText(section, key, analyzer);
		}
		else if (keyType == KeyType.PER_OBJECT_CONFIG) {
			return null; // TODO display which modules it applies to, plus comment maybe?
		}
		else {
			return null; // should not happen (invalid key type)
		}
	}

	/**
	 * Generates tooltip for a config entry.
	 */
	public static String getConfigHoverText(String section, String key, IInifileDocument doc) {
		IMarker[] markers = InifileUtils.getProblemMarkersFor(section, key, doc);
		String text = getProblemsHoverText(markers, false);
		ConfigKey entry = ConfigRegistry.getEntry(key);
		if (entry == null)
			return HoverSupport.addHTMLStyleSheet(text);

		text += "<b>[General]"+(entry.isGlobal() ? "" : " or [Config X]")+" / "+entry.getKey();
		text += " = &lt;" + entry.getDataType().name().replaceFirst("CFG_", "");
		if (entry.getDefaultValue()!=null && !entry.getDefaultValue().equals(""))
			text += ", default: " + entry.getDefaultValue();
		if (entry.getUnit()!=null)
			text += ", unit: "+entry.getUnit();
		text += "&gt;</b><br>\n<br>\n";
		text += entry.getDescription() + "<br>\n";

		if (doc != null && entry!=CFGID_DESCRIPTION && entry!=CFGID_EXTENDS) {
			List<String> sectionList = new ArrayList<String>();
			for (String sec : doc.getSectionNames())
				if (doc.containsKey(sec, entry.getKey()))
					sectionList.add(sec);
			if (sectionList.size()==0)
				text += "<br>\nCurrently not set in any sections.<br>\n";
			else
				text += "<br>\nSet in the following sections: <ul><li>"+StringUtils.join(sectionList.toArray(), "</li><li>")+"</li></ul><br>\n";
		}

		return HoverSupport.addHTMLStyleSheet(text);
	}

	/**
	 * Generate tooltip for a param key entry
	 */
	public static String getParamKeyHoverText(String section, String key, InifileAnalyzer analyzer) {
		//XXX somehow merge similar entries? (i.e. where pathModules[] and paramValueNode/paramDeclNode are the same)
		IMarker[] markers = InifileUtils.getProblemMarkersFor(section, key, analyzer.getDocument());
		String text = getProblemsHoverText(markers, false);
		text += "<b>[" + section + "] / " + key + "</b><br>\n";
		ParamResolution[] resList = analyzer.getParamResolutionsForKey(section, key);
		if (resList.length==0) {
			text += "Does not match any module parameters.";
			return HoverSupport.addHTMLStyleSheet(text);
		}

		// merge similar entries
		Set<ParamNode> paramDeclNodes = new LinkedHashSet<ParamNode>();
		for (ParamResolution res : resList)
			paramDeclNodes.add(res.paramDeclNode);

		text += "Applies to the following module parameters: <br>\n";
		for (ParamNode paramDeclNode : paramDeclNodes) {
			String paramName = paramDeclNode.getName();
			String paramType = paramDeclNode.getAttribute(ParamNode.ATT_TYPE);
			String paramDeclaredOn = paramDeclNode.getEnclosingTypeNode().getName();
			String comment = StringUtils.makeBriefDocu(paramDeclNode.getComment(), 60);
			String optComment = comment==null ? "" : (" -- \"" + comment + "\"");

			text += "<br>"+paramDeclaredOn + "." + paramName + " : "+ paramType + optComment + "\n<ul>\n";

			for (ParamResolution res : resList)
				if (res.paramDeclNode == paramDeclNode)
					text +=	" <li>" + res.moduleFullPath + (section.equals(res.activeSection) ? "" : ", for sub-config ["+res.activeSection+"]") + "</li>\n";
			text += "</ul>";
		}
		return HoverSupport.addHTMLStyleSheet(text);
	}

	public static String getProblemsHoverText(IMarker[] markers, boolean lineNumbers) {
		if (markers.length==0) 
			return "";
		
		String text = "";
		for (IMarker marker : markers) {
			String severity = "";
			switch (marker.getAttribute(IMarker.SEVERITY, -1)) {
				case IMarker.SEVERITY_ERROR: severity = "Error"; break;
				case IMarker.SEVERITY_WARNING: severity = "Warning"; break;
				case IMarker.SEVERITY_INFO: severity = "Info"; break;
			}
			String lineNumber = lineNumbers ? "Line "+marker.getAttribute(IMarker.LINE_NUMBER, -1)+": " : "";  
			text += "<i>"+lineNumber+severity+": " + marker.getAttribute(IMarker.MESSAGE, "") + "</i><br/>\n";
		}
		return text+"<br/>";
	}
	
	/**
	 * Generate tooltip for a NED parameter
	 */
	public static String getParamHoverText(SubmoduleNode[] pathModules, ParamNode paramDeclNode, ParamNode paramValueNode) {
		String paramName = paramDeclNode.getName();
		
		String paramType = paramDeclNode.getAttribute(ParamNode.ATT_TYPE);
		String paramDeclaredOn = paramDeclNode.getSelfOrEnclosingTypeNode().getName();
		String comment = StringUtils.makeBriefDocu(paramDeclNode.getComment(), 60);
		String optComment = comment==null ? "" : (" -- \"" + comment + "\"");

		String text = ""; //TODO
		text += paramDeclaredOn + "." + paramName + " : "+ paramType + optComment + "\n";
		//XXX more info.....
		return HoverSupport.addHTMLStyleSheet(text);
	}
	
}
