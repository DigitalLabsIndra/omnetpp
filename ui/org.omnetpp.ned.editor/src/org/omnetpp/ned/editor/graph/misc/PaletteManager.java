/*--------------------------------------------------------------*
  Copyright (C) 2006-2008 OpenSim Ltd.
  
  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.ned.editor.graph.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.ConnectionCreationToolEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteSeparator;
import org.eclipse.gef.palette.PaletteStack;
import org.eclipse.gef.palette.PanningSelectionToolEntry;
import org.eclipse.gef.palette.ToolEntry;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.tools.MarqueeSelectionTool;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.omnetpp.common.displaymodel.IDisplayString;
import org.omnetpp.common.image.ImageFactory;
import org.omnetpp.common.util.StringUtils;
import org.omnetpp.ned.core.NEDResources;
import org.omnetpp.ned.core.NEDResourcesPlugin;
import org.omnetpp.ned.editor.graph.GraphicalNedEditor;
import org.omnetpp.ned.model.INEDElement;
import org.omnetpp.ned.model.ex.CompoundModuleElementEx;
import org.omnetpp.ned.model.ex.GateElementEx;
import org.omnetpp.ned.model.ex.NEDElementFactoryEx;
import org.omnetpp.ned.model.ex.NEDElementUtilEx;
import org.omnetpp.ned.model.ex.NedFileElementEx;
import org.omnetpp.ned.model.ex.SubmoduleElementEx;
import org.omnetpp.ned.model.interfaces.IChannelKindTypeElement;
import org.omnetpp.ned.model.interfaces.IHasDisplayString;
import org.omnetpp.ned.model.interfaces.IHasName;
import org.omnetpp.ned.model.interfaces.IModuleKindTypeElement;
import org.omnetpp.ned.model.interfaces.INEDTypeInfo;
import org.omnetpp.ned.model.interfaces.INedTypeElement;
import org.omnetpp.ned.model.pojo.ChannelInterfaceElement;
import org.omnetpp.ned.model.pojo.ModuleInterfaceElement;
import org.omnetpp.ned.model.pojo.NEDElementTags;
import org.omnetpp.ned.model.pojo.PropertyElement;

/**
 * Responsible for managing palette entries and keeping them in sync with
 * the components in NEDResources plugin
 *
 * @author rhornig, andras
 */
//TODO: palette entries: LRU list + all types 
// LRU list contains:
//   - all types from the local package (including inner types)
//   - all types imported into the current NED type
// --Andras
public class PaletteManager {
	private static final String NBSP = "\u00A0";
    private static final String GROUP_PROPERTY = "group";

    // encoding of internal IDs
    private static final String MRU_GROUP = "!mru";
    private static final String CONNECTIONS_GROUP = "!connections";
    private static final String TYPES_GROUP = "!types";
    private static final String GROUP_DELIMITER = "~";

    /**
     * A comparator that uses dictionary ordering and the short name part of a fully qualified name to order
     * (the part after the last . char)
     */
    private static final class ShortNameComparator implements Comparator<String> {
        public int compare(String first, String second) {
            String firstShortName = StringUtils.substringAfterLast(first, ".");
            if (StringUtils.isEmpty(firstShortName))
                firstShortName = first;
            String secondShortName = StringUtils.substringAfterLast(second, ".");
            if (StringUtils.isEmpty(secondShortName))
                secondShortName = second;
            return StringUtils.dictionaryCompare(firstShortName, secondShortName);
        }
    }
    private static ShortNameComparator shortNameComparator = new ShortNameComparator();

    protected class SubmoduleComparator implements Comparator<INEDTypeInfo> {
        private int calculatePoints(INEDTypeInfo typeInfo) {
            if (typeInfo == null)
                return 0; // for separators
            else {
                int point = 0;
                INedTypeElement element = typeInfo.getNEDElement();
                HashSet<String> gateLabels = new HashSet<String>();
                HashSet<String> containsLabels = new HashSet<String>();
                NedFileElementEx editedElement = hostingEditor.getModel();

                for (INedTypeElement nedTypeElement : editedElement.getTopLevelTypeNodes()) {
                    List<String> labels = NEDElementUtilEx.getProperties(nedTypeElement, "contains");
                    
                    if (nedTypeElement instanceof CompoundModuleElementEx) {
                        CompoundModuleElementEx compoundModule = (CompoundModuleElementEx)nedTypeElement;
    
                        if (labels.isEmpty() && compoundModule.isNetwork())
                            labels.add("node");
    
                        for (SubmoduleElementEx submodule : compoundModule.getSubmodules()) {
                            if (submodule.getNEDTypeInfo() == element.getNEDTypeInfo())
                                point += 5; // used as a submodule
                            
                            for (GateElementEx gate : submodule.getNEDTypeInfo().getGateDeclarations().values())
                                gateLabels.addAll(NEDElementUtilEx.getLabels(gate));
                        }
                    }
    
                    containsLabels.addAll(labels);
                }
    
                for (String label : NEDElementUtilEx.getLabels(element))
                    if (containsLabels.contains(label))
                        point += 10; // matching @contains and @labels
    
                for (GateElementEx gate : typeInfo.getGateDeclarations().values())
                    for (String label : NEDElementUtilEx.getLabels(gate))
                        if (gateLabels.contains(label))
                            point += 1; // matching gate @labels
    
                return point;
            }
        }
        
        private String getName(INEDTypeInfo typeInfo) {
            return typeInfo == null ? "" : typeInfo.getName();
        }

        public int compare(INEDTypeInfo first, INEDTypeInfo second) {
            int firstPoint = calculatePoints(first);
            int secondPoint = calculatePoints(second);
            String firstShortName = getName(first);
            String secondShortName = getName(second);
            return secondPoint - firstPoint + StringUtils.dictionaryCompare(firstShortName, secondShortName);
        }
    }
    
    // state
    protected String submodulesFilter;
    protected GraphicalNedEditor hostingEditor;
    protected PaletteRoot nedPalette;
    protected PaletteContainer toolsContainer;
    protected PaletteContainer channelsStack;
    protected List<PaletteEntry> tempChannelsStackContent;
    protected PaletteDrawer typesContainer;
    protected List<PaletteEntry> tempTypesContainerContent;
    protected PaletteDrawer defaultContainer;
    protected List<PaletteEntry> tempDefaultContainerContent;

    protected Map<String, PaletteEntry> currentEntries = new HashMap<String, PaletteEntry>();
    protected Map<String, PaletteDrawer> currentContainers = new HashMap<String, PaletteDrawer>();
    
    // NED packages whose contents should be excluded from the palette
    // (exclude list is preferred to include list, because then newly created packages
    // will be automatically included without explicit maintenance)
    protected Set<String> excludedPackages = new HashSet<String>();

    public PaletteManager(GraphicalNedEditor hostingEditor) {
        super();
        this.hostingEditor = hostingEditor;
        nedPalette = new PaletteRoot();
        // TODO: maybe a flag?
        // test specific code part (tests whether we are running under gui testing)
        // this is a hack because the testing framework cannot access the PalettStack correctly.
        if (System.getProperty("com.simulcraft.test.running") == null)
            channelsStack = new PaletteStack("Connections", "Connect modules using this tool",ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION));
        else
            channelsStack = new PaletteDrawer("Connections", ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION));
        toolsContainer = createTools();
        typesContainer = new PaletteDrawer("Types", ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_FOLDER));
        typesContainer.setInitialState(PaletteDrawer.INITIAL_STATE_PINNED_OPEN);
        defaultContainer = new PaletteDrawer(getSubmodulesDrawerLabel(), ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_FOLDER));

        refresh();
    }

    public String getSubmodulesFilter() {
        return submodulesFilter;
    }

    public void setSubmodulesFilter(String text) {
        if (StringUtils.isEmpty(text))
            submodulesFilter = null;
        else
            submodulesFilter = text;
        
        defaultContainer.setLabel(getSubmodulesDrawerLabel());
    }
    
    private boolean matchesSubmodulesFilter(INedTypeElement element) {
        if (submodulesFilter == null)
            return true;
        else {
            String regexp = ".*?" + submodulesFilter + ".*?";
            String fullyQualifiedName = element.getNEDTypeInfo().getFullyQualifiedName();
    
            for (String label : NEDElementUtilEx.getLabels(element))
                if (label.matches(regexp))
                    return true;
            
            return fullyQualifiedName.matches(regexp);
        }
    }

    private String getSubmodulesDrawerLabel() {
        return (StringUtils.isEmpty(submodulesFilter) ? "Submodules" : "Filter: " + submodulesFilter) + (excludedPackages.isEmpty() ? "" : "!");
    }

    public PaletteRoot getRootPalette() {
        return nedPalette;
    }

    public Set<String> getExcludedPackages() {
        return excludedPackages;
    }

    public void setExcludedPackages(Set<String> excludedPackages) {
        this.excludedPackages = excludedPackages;
        refresh();
    }

    protected Map<String, PaletteEntry> createPaletteModel() {
        Map<String, PaletteEntry> result = new LinkedHashMap<String, PaletteEntry>();

        IEditorInput input = hostingEditor.getEditorInput();
        if (!(input instanceof IFileEditorInput))
            return result; // sorry
        IFile file = ((IFileEditorInput)input).getFile();
        IProject contextProject = file.getProject();

        // connection and type creation tools
        result.putAll(createConnectionTools());
        Map<String, PaletteEntry> innerChannelTypes = createInnerTypes(file, false);
        if (innerChannelTypes.size() > 0) {
            result.putAll(innerChannelTypes);
        }
        Map<String, ToolEntry> channelsStackEntries = createChannelsStackEntries(contextProject);
        if (channelsStackEntries.size() > 0) {
            result.putAll(channelsStackEntries);
        }

        // type elements (simple/compound module/interfaces)
        result.putAll(createTypesEntries());

        // submodule creation tools
        Map<String, PaletteEntry> innerModuleTypes = createInnerTypes(file, true);
        if (innerModuleTypes.size() > 0) {
            result.putAll(innerModuleTypes);
            result.put("separator", new PaletteSeparator());
        }
        result.putAll(createSubmodules(contextProject));

        return result;
    }

    /**
     * Builds the palette (all drawers)
     */
    public void refresh() {
        // Debug.println("paletteManager refresh() start");
        // long startMillis = System.currentTimeMillis();

        tempChannelsStackContent = new ArrayList<PaletteEntry>();
        tempTypesContainerContent = new ArrayList<PaletteEntry>();
        tempDefaultContainerContent = new ArrayList<PaletteEntry>();

        Map<String, PaletteEntry> newEntries = createPaletteModel();
        for (String id : newEntries.keySet()) {
            // if the same tool already exist, use that object so the object identity
        	// will not change unnecessarily
            if (currentEntries.containsKey(id))
                newEntries.put(id, currentEntries.get(id));

            getContainerFor(id).add(newEntries.get(id));
        }
        currentEntries = newEntries;

        // TODO sort the containers by name
        ArrayList<PaletteContainer> drawers = new ArrayList<PaletteContainer>();
        drawers.add(toolsContainer);
        drawers.add(typesContainer);
        drawers.add(defaultContainer);
        // drawers.addAll(currentContainers.values());

        channelsStack.setChildren(tempChannelsStackContent);
        typesContainer.setChildren(tempTypesContainerContent);
        defaultContainer.setChildren(tempDefaultContainerContent);
//        for (PaletteContainer container : currentContainers.values())
//            container.setChildren(container.getChildren());
       
        nedPalette.setChildren(drawers);
        defaultContainer.setLabel(getSubmodulesDrawerLabel());
        
//        long dt = System.currentTimeMillis() - startMillis;
//        Debug.println("paletteManager refresh(): " + dt + "ms");
    }

    /**
     * The container belonging to this ID
     */
    public List<PaletteEntry> getContainerFor(String id) {
        if (!id.contains(GROUP_DELIMITER))
            return tempDefaultContainerContent;
        String group = StringUtils.substringBefore(id, GROUP_DELIMITER);
        if (MRU_GROUP.equals(group))
            return tempDefaultContainerContent;
        if (CONNECTIONS_GROUP.equals(group))
            return tempChannelsStackContent;
        if (TYPES_GROUP.equals(group))
            return tempTypesContainerContent;

        // TODO add grouping support
//        PaletteDrawer drawer = currentContainers.get(group);
//        if (drawer == null) {
//            drawer = new PaletteDrawer(group, ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_FOLDER));
//            drawer.setInitialState(PaletteDrawer.INITIAL_STATE_CLOSED);
//            currentContainers.put(group, drawer);
//        }
//
//        return drawer;
        return tempDefaultContainerContent;
    }

    /**
     * Builds a drawer containing basic tools like selection connection etc.
     */
    private PaletteContainer createTools() {
        PaletteGroup controlGroup = new PaletteGroup("Tools");

        ToolEntry tool = new PanningSelectionToolEntry("Selector","Select module(s)");
        tool.setToolClass(NedSelectionTool.class);
        controlGroup.add(tool);
        getRootPalette().setDefaultEntry(tool);

        controlGroup.add(channelsStack);
        return controlGroup;
    }

    private static Map<String, ToolEntry> createConnectionTools() {
        Map<String, ToolEntry> entries = new LinkedHashMap<String, ToolEntry>();

        ConnectionCreationToolEntry defaultConnectionTool = new ConnectionCreationToolEntry(
                "Connection",
                "Create connections between submodules, or submodule and parent module",
                new ModelFactory(NEDElementTags.NED_CONNECTION),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION)
        );

        // sets the required connection tool
        defaultConnectionTool.setToolClass(NedConnectionCreationTool.class);
        entries.put(CONNECTIONS_GROUP+GROUP_DELIMITER+"connection", defaultConnectionTool);

        // connection selection
        MarqueeToolEntry marquee = new MarqueeToolEntry("Connection"+NBSP+"selector", "Drag out an area to select connections in it");
        marquee.setToolProperty(MarqueeSelectionTool.PROPERTY_MARQUEE_BEHAVIOR,
                MarqueeSelectionTool.BEHAVIOR_CONNECTIONS_TOUCHED);
        entries.put(CONNECTIONS_GROUP+GROUP_DELIMITER+"marquee", marquee);
        return entries;
    }
    
    /**
     * Iterates over all top level (module types) types in a NED file and gathers all NEDTypes from all components.
     * Returns a Container containing all types in this file.
     */
    private static Map<String, PaletteEntry> createInnerTypes(IFile file, boolean moduleTypes) {
        List<String> innerTypeNames = new ArrayList<String>();
        
        // add module and module interface *inner* types of NED types in this file
        for (INEDElement element : NEDResourcesPlugin.getNEDResources().getNedFileElement(file))
            if (element instanceof INedTypeElement)
            	for (INedTypeElement typeElement : ((INedTypeElement)element).getNEDTypeInfo().getInnerTypes().values())
            		if (typeElement instanceof IModuleKindTypeElement && moduleTypes || typeElement instanceof IChannelKindTypeElement && !moduleTypes)
            		    innerTypeNames.add(typeElement.getNEDTypeInfo().getFullyQualifiedName());
        // TODO: use SubmoduleComparator to sort the inner types
        Collections.sort(innerTypeNames, shortNameComparator);
            		    
        Map<String, PaletteEntry> entries = new LinkedHashMap<String, PaletteEntry>();
        for(String fqName : innerTypeNames) {
            INEDTypeInfo toplevelOrInnerNedTypeInfo = NEDResourcesPlugin.getNEDResources().getToplevelOrInnerNedType(fqName, file.getProject());
            if (toplevelOrInnerNedTypeInfo != null)
                addToolEntry(toplevelOrInnerNedTypeInfo.getNEDElement(), moduleTypes ? MRU_GROUP : CONNECTIONS_GROUP, entries);
        }
        return entries;
    }

    /**
     * Creates several submodule drawers using currently parsed types,
     * and using the GROUP property as the drawer name.
     */
    protected Map<String, PaletteEntry> createSubmodules(IProject contextProject) {
        Map<String, PaletteEntry> entries = new LinkedHashMap<String, PaletteEntry>();

        // get all the possible type names in alphabetical order
        List<INEDTypeInfo> typeInfos = new ArrayList<INEDTypeInfo>();
        for (INEDTypeInfo typeInfo : NEDResourcesPlugin.getNEDResources().getNedTypes(contextProject))
            if (NEDResources.MODULE_FILTER.matches(typeInfo) || NEDResources.MODULEINTERFACE_FILTER.matches(typeInfo))
                typeInfos.add(typeInfo);
        typeInfos.add(null); // representing the separator
        Collections.sort(typeInfos, new SubmoduleComparator());

        for (INEDTypeInfo typeInfo : typeInfos) {
            if (typeInfo == null)
                entries.put("separator", new PaletteSeparator());
            else {
                INedTypeElement typeElement = typeInfo.getNEDElement();
    
                // skip this type if it is a network
                if (typeElement instanceof CompoundModuleElementEx && ((CompoundModuleElementEx)typeElement).isNetwork())
                    continue;
    
                // add it if package filter matches
                String packageName = typeElement.getContainingNedFileElement().getPackage();
                if (!excludedPackages.contains(packageName) && matchesSubmodulesFilter(typeElement)) {
    
                    // determine which palette group it belongs to or put it into the default
                    PropertyElement property = typeElement.getNEDTypeInfo().getProperties().get(GROUP_PROPERTY);
                    String group = (property == null) ? "" : NEDElementUtilEx.getPropertyValue(property);
    
                    addToolEntry(typeElement, group, entries);
                }
            }
        }

        return entries;
    }

    private static void addToolEntry(INedTypeElement typeElement, String group, Map<String, PaletteEntry> entries) {
        String fullyQualifiedTypeName = typeElement.getNEDTypeInfo().getFullyQualifiedName();

        String key = fullyQualifiedTypeName;
        if (StringUtils.isNotEmpty(group))
            key = group+GROUP_DELIMITER+key;

        // set the default images for the palette entry
        ImageDescriptor imageDescNorm = ImageFactory.getDescriptor(ImageFactory.DEFAULT,"vs",null,0);
        ImageDescriptor imageDescLarge = ImageFactory.getDescriptor(ImageFactory.DEFAULT,"s",null,0);
        if (typeElement instanceof IHasDisplayString) {
            IDisplayString dps = ((IHasDisplayString)typeElement).getDisplayString();
            String imageId = dps.getAsString(IDisplayString.Prop.IMAGE);
            if (StringUtils.isNotEmpty(imageId)) {
                imageDescNorm = ImageFactory.getDescriptor(imageId,"vs",null,0);
                imageDescLarge = ImageFactory.getDescriptor(imageId,"s",null,0);
                key += ":"+imageId;
            }
        }

        // create the tool entry (if we are currently dropping an interface, we should use the IF type for the like parameter
        String instanceName = StringUtils.toInstanceName(typeElement.getName());
		CombinedTemplateCreationEntry toolEntry = new CombinedTemplateCreationEntry(
                getLabelFor(typeElement.getNEDTypeInfo()), 
                StringUtils.makeBriefDocu(typeElement.getComment(), 300),
                new ModelFactory(NEDElementTags.NED_SUBMODULE, instanceName, fullyQualifiedTypeName, typeElement instanceof ModuleInterfaceElement),
                imageDescNorm, imageDescLarge );

        entries.put(key, toolEntry);
    }
    
    private Map<String, ToolEntry> createChannelsStackEntries(IProject contextProject) {
        Map<String, ToolEntry> entries = new LinkedHashMap<String, ToolEntry>();

        ConnectionCreationToolEntry defaultConnectionTool = new ConnectionCreationToolEntry(
                "Connection",
                "Create connections between submodules, or submodule and parent module",
                new ModelFactory(NEDElementTags.NED_CONNECTION),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION)
        );

        // sets the required connection tool
        defaultConnectionTool.setToolClass(NedConnectionCreationTool.class);
        entries.put(CONNECTIONS_GROUP+GROUP_DELIMITER+"connection", defaultConnectionTool);

        // connection selection
        MarqueeToolEntry marquee = new MarqueeToolEntry("Connection"+NBSP+"selector", "Drag out an area to select connections in it");
        marquee.setToolProperty(MarqueeSelectionTool.PROPERTY_MARQUEE_BEHAVIOR,
                MarqueeSelectionTool.BEHAVIOR_CONNECTIONS_TOUCHED);
        entries.put(CONNECTIONS_GROUP+GROUP_DELIMITER+"marquee", marquee);

        // get all the possible type names in alphabetical order
        List<String> channelNames = new ArrayList<String>();
        channelNames.addAll(NEDResourcesPlugin.getNEDResources().getChannelQNames(contextProject));
        channelNames.addAll(NEDResourcesPlugin.getNEDResources().getChannelInterfaceQNames(contextProject));
        Collections.sort(channelNames, shortNameComparator);

        for (String fullyQualifiedName : channelNames) {
            INEDTypeInfo typeInfo = NEDResourcesPlugin.getNEDResources().getToplevelNedType(fullyQualifiedName, contextProject);
            INedTypeElement modelElement = typeInfo.getNEDElement();

            // add it if package filter matches
            String packageName = modelElement.getContainingNedFileElement().getPackage();
            if (!excludedPackages.contains(packageName)) {
                ConnectionCreationToolEntry tool = new ConnectionCreationToolEntry(
                        getLabelFor(typeInfo),
                        StringUtils.makeBriefDocu(modelElement.getComment(), 300),
                        new ModelFactory(NEDElementTags.NED_CONNECTION, "n/a", fullyQualifiedName, modelElement instanceof ChannelInterfaceElement),
                        ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION),
                        ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CONNECTION)
                );
                // sets the required connection tool
                tool.setToolClass(NedConnectionCreationTool.class);
                entries.put(CONNECTIONS_GROUP+GROUP_DELIMITER+fullyQualifiedName, tool);
            }
        }
        return entries;
    }

    /**
     * A label used for the palette entry containing fully qualified name if needed, the containing compound module
     * and the interface keyword.
     */
    private static String getLabelFor(INEDTypeInfo typeInfo) {
        INedTypeElement modelElement = typeInfo.getNEDElement();
        String packageName = typeInfo.getNEDElement().getContainingNedFileElement().getPackage();
        boolean isInterface = modelElement instanceof ChannelInterfaceElement || modelElement instanceof ModuleInterfaceElement;
        
        String label = modelElement.getName();
        
        if (modelElement.getEnclosingTypeElement() != null)
            label += NBSP+"in"+NBSP+modelElement.getEnclosingTypeElement().getName();
        if (isInterface)
            label += NBSP+"(I)";
        if (packageName != null)
            label += NBSP + "(" + packageName + ")";
        return label;
    }
    
    /**
     * Builds a tool entry list containing base top level NED components like simple, module, channel etc.
     */
    private static Map<String, ToolEntry> createTypesEntries() {
        Map<String, ToolEntry> entries = new LinkedHashMap<String, ToolEntry>();

        CombinedTemplateCreationEntry entry = new CombinedTemplateCreationEntry(
                "Simple"+NBSP+"Module",
                "Create a simple module type",
                new ModelFactory(NEDElementTags.NED_SIMPLE_MODULE, IHasName.DEFAULT_TYPE_NAME),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_SIMPLEMODULE),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_SIMPLEMODULE)
        );
        entries.put(TYPES_GROUP+GROUP_DELIMITER+"simple", entry);

        entry = new CombinedTemplateCreationEntry(
                "Compound"+NBSP+"Module",
                "Create a compound module type that may contain submodules",
                new ModelFactory(NEDElementTags.NED_COMPOUND_MODULE, IHasName.DEFAULT_TYPE_NAME),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_COMPOUNDMODULE),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_COMPOUNDMODULE)
        );
        entries.put(TYPES_GROUP+GROUP_DELIMITER+"compound", entry);

        // network tool
        CreationFactory networkFactory = new CreationFactory() {
			public Object getNewObject() {
				CompoundModuleElementEx network = (CompoundModuleElementEx)NEDElementFactoryEx.getInstance().createElement(NEDElementTags.NED_COMPOUND_MODULE);
				network.setName("Network");
				network.setIsNetwork(true);
				return network;
			}
			public Object getObjectType() {
				return "Network";
			}
        };
        
        entry = new CombinedTemplateCreationEntry(
                "Network",
                "Create a network type",
                networkFactory,
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_NETWORK),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_NETWORK)
        );
        entries.put(TYPES_GROUP+GROUP_DELIMITER+"network", entry);

        entry = new CombinedTemplateCreationEntry(
                "Channel",
                "Create a channel type",
                new ModelFactory(NEDElementTags.NED_CHANNEL, IHasName.DEFAULT_TYPE_NAME),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CHANNEL),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CHANNEL)
        );
        entries.put(TYPES_GROUP+GROUP_DELIMITER+"channel", entry);

        entry = new CombinedTemplateCreationEntry(
        		"Module"+NBSP+"Interface",
        		"Create a module interface type",
        		new ModelFactory(NEDElementTags.NED_MODULE_INTERFACE, IHasName.DEFAULT_TYPE_NAME),
        		ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_INTERFACE),
        		ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_INTERFACE)
        );
        entries.put(TYPES_GROUP+GROUP_DELIMITER+"moduleinterface", entry);

        entry = new CombinedTemplateCreationEntry(
                "Channel"+NBSP+"Interface",
                "Create a channel interface type",
                new ModelFactory(NEDElementTags.NED_CHANNEL_INTERFACE, IHasName.DEFAULT_TYPE_NAME),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CHANNELINTERFACE),
                ImageFactory.getDescriptor(ImageFactory.MODEL_IMAGE_CHANNELINTERFACE)
        );
        entries.put(TYPES_GROUP+GROUP_DELIMITER+"channelinterface", entry);

        return entries;
    }
}
