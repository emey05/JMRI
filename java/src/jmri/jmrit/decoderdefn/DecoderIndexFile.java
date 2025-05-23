package jmri.jmrit.decoderdefn;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JOptionPane;
import jmri.InstanceInitializer;
import jmri.InstanceManager;
import jmri.implementation.AbstractInstanceInitializer;
import jmri.jmrit.XmlFile;
import jmri.util.FileUtil;
import jmri.util.ThreadingUtil;
import org.jdom2.Attribute;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.ProcessingInstruction;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DecoderIndex represents the decoderIndex.xml (decoder types) and
 * nmra_mfg_list.xml (Manufacturer ID list) files in memory.
 * <p>
 * This allows a program to navigate to various decoder descriptions without
 * having to manipulate files.
 * <p>
 * This class doesn't provide tools for defining the index; that's done
 * by {@link jmri.jmrit.decoderdefn.DecoderIndexCreateAction}, which
 * rebuilds it from the decoder files.
 * <p>
 * Multiple DecoderIndexFile objects don't make sense, so we use an "instance"
 * member to navigate to a single one.
 * <p>
 * Previous to JMRI 4.19.1, the manufacturer information was kept in the
 * decoderIndex.xml file. Starting with that version it's in the separate
 * nmra_mfg_list.xml file, but still written to decoderIndex.xml when
 * one is created.
 *
 * @author Bob Jacobsen Copyright (C) 2001, 2019, 2025
 * @see jmri.jmrit.decoderdefn.DecoderIndexCreateAction
 */
public class DecoderIndexFile extends XmlFile {

    public static final String MANUFACTURER = "manufacturer";
    public static final String MFG_ID = "mfgID";
    public static final String DECODER_INDEX = "decoderIndex";
    public static final String VERSION = "version";
    public static final String LOW_VERSION_ID = "lowVersionID";
    public static final String HIGH_VERSION_ID = "highVersionID";
    // fill in abstract members
    protected List<DecoderFile> decoderList = new ArrayList<>();

    public int numDecoders() {
        return decoderList.size();
    }

    int fileVersion = -1;

    // map mfg ID numbers from & to mfg names
    protected HashMap<String, String> _mfgIdFromNameHash = new HashMap<>();
    protected HashMap<String, String> _mfgNameFromIdHash = new HashMap<>();

    protected ArrayList<String> mMfgNameList = new ArrayList<>();

    public List<String> getMfgNameList() {
        return mMfgNameList;
    }

    public String mfgIdFromName(String name) {
        return _mfgIdFromNameHash.get(name);
    }

    /**
     *
     * @param idNum String containing the manufacturer's NMRA
     *      manufacturer ID number
     * @return String containing the "friendly" name of the manufacturer
     */

    public String mfgNameFromID(String idNum) {
        return _mfgNameFromIdHash.get(idNum);
    }

    /**
     * Get a List of decoders matching (only) the programming mode.
     *
     * @param progMode  decoder programming mode as defined in a decoder's programming element
     * @return a list, possibly empty, of matching decoders
     */
    @Nonnull
    public List<DecoderFile> matchingDecoderList(String progMode) {
        return (matchingDecoderList(null, null, null, null, null,
                null, null, null, null, progMode));
    }

    /**
     * Get a List of decoders matching basic characteristics.
     *
     * @param mfg              decoder manufacturer
     * @param family           decoder family
     * @param decoderMfgID     NMRA decoder manufacturer ID
     * @param decoderVersionID decoder version ID
     * @param decoderProductID decoder product ID
     * @param model            decoder model
     * @return a list, possibly empty, of matching decoders
     */
    @Nonnull
    public List<DecoderFile> matchingDecoderList(String mfg, String family,
            String decoderMfgID, String decoderVersionID, String decoderProductID,
            String model) {
        return (matchingDecoderList(mfg, family, decoderMfgID, decoderVersionID, decoderProductID, model,
                null, null, null, null));
    }

    /**
     * Get a List of decoders matching basic characteristics + product ID etc.
     *
     * @param mfg              decoder manufacturer
     * @param family           decoder family
     * @param decoderMfgID     NMRA decoder manufacturer ID
     * @param decoderVersionID decoder version ID
     * @param decoderProductID decoder product ID
     * @param model            decoder model
     * @param developerID      developer ID number
     * @param manufacturerID   manufacturerID number
     * @param productID        productID number
     * @return a list, possibly empty, of matching decoders
     */
    @Nonnull
    public List<DecoderFile> matchingDecoderList(String mfg, String family,
            String decoderMfgID, String decoderVersionID,
            String decoderProductID, String model, String developerID, String manufacturerID, String productID) {
        return (matchingDecoderList(mfg, family, decoderMfgID, decoderVersionID, decoderProductID, model,
                null, null, null, null));
    }

    /**
     * Get a List of decoders matching basic characteristics + product ID etc. + programming mode.
     *
     * @param mfg              decoder manufacturer
     * @param family           decoder family
     * @param decoderMfgID     NMRA decoder manufacturer ID
     * @param decoderVersionID decoder version ID
     * @param decoderProductID decoder product ID
     * @param model            decoder model
     * @param developerID      developer ID number
     * @param manufacturerID   manufacturerID number
     * @param productID        productID number
     * @param progMode         programming mode as defined in a decoder's programming element
     * @return a list, possibly empty, of matching decoders
     */
    @Nonnull
    public List<DecoderFile> matchingDecoderList(String mfg, String family,
                                                 String decoderMfgID, String decoderVersionID,
                                                 String decoderProductID, String model, String developerID,
                                                 String manufacturerID, String productID, String progMode) {
        List<DecoderFile> l = new ArrayList<>();
        for (int i = 0; i < numDecoders(); i++) {
            if (checkEntry(i, mfg, family, decoderMfgID, decoderVersionID, decoderProductID, model, developerID,
                    manufacturerID, productID, progMode)) {
                l.add(decoderList.get(i));
            }
        }
        return l;
    }

    /**
     * Get a JComboBox representing the choices that match basic characteristics.
     *
     * @param mfg              decoder manufacturer
     * @param family           decoder family
     * @param decoderMfgID     NMRA decoder manufacturer ID
     * @param decoderVersionID decoder version ID
     * @param decoderProductID decoder product ID
     * @param model            decoder model
     * @return a combo box populated with matching decoders
     */
    public JComboBox<String> matchingComboBox(String mfg, String family, String decoderMfgID, String decoderVersionID,
                                              String decoderProductID, String model) {
        List<DecoderFile> l = matchingDecoderList(mfg, family, decoderMfgID, decoderVersionID, decoderProductID, model);
        return jComboBoxFromList(l);
    }

    /**
     * Get a new JComboBox made with the titles from a list of DecoderFile.
     *
     * @param l list of decoders
     * @return a combo box populated with the list
     */
    public static JComboBox<String> jComboBoxFromList(List<DecoderFile> l) {
        return new JComboBox<>(jComboBoxModelFromList(l));
    }

    /**
     * Get a new ComboBoxModel made with the titles from a list of DecoderFile.
     * entries.
     *
     * @param l list of decoders
     * @return a combo box model populated with the list
     */
    public static javax.swing.ComboBoxModel<String> jComboBoxModelFromList(List<DecoderFile> l) {
        javax.swing.DefaultComboBoxModel<String> b = new javax.swing.DefaultComboBoxModel<>();
        for (DecoderFile r : l) {
            b.addElement(r.titleString());
        }
        return b;
    }

    /**
     * Get a DecoderFile from a "title" string, typically a selection in a
     * matching ComboBox.
     *
     * @param title the decoder title
     * @return the decoder file
     */
    public DecoderFile fileFromTitle(String title) {
        for (int i = numDecoders() - 1; i >= 0; i--) {
            DecoderFile r = decoderList.get(i);
            if (r.titleString().equals(title)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Check if an entry consistent with specific properties. A null String
     * entry always matches. Strings are used for convenience in GUI building.
     * Don't bother asking about the model number...
     *
     * @param i                index of entry
     * @param mfgName          decoder manufacturer
     * @param family           decoder family
     * @param mfgID            NMRA decoder manufacturer ID
     * @param decoderVersionID decoder version ID
     * @param decoderProductID decoder product ID
     * @param model            decoder model
     * @param developerID      developer ID number
     * @param manufacturerID   manufacturer ID number
     * @param productID        product ID number
     * @param progMode         programming mode as defined in a decoder's programming element
     * @return true if entry at i matches the other parameters; false otherwise
     */
    public boolean checkEntry(int i, String mfgName, String family, String mfgID,
            String decoderVersionID, String decoderProductID, String model,
            String developerID, String manufacturerID, String productID, String progMode) {
        DecoderFile r = decoderList.get(i);
        if (mfgName != null && !mfgName.equals(r.getMfg())) {
            return false;
        }
        if (family != null && !family.equals(r.getFamily())) {
            return false;
        }
        if (mfgID != null && !mfgID.equals(r.getMfgID())) {
            return false;
        }
        if (model != null && !model.equals(r.getModel())) {
            return false;
        }
        // check version ID - no match if a range specified and out of range
        if (decoderVersionID != null) {
            int versionID = Integer.parseInt(decoderVersionID);
            if (!r.isVersion(versionID)) {
                return false;
            }
        }

        if (decoderProductID != null && !checkInCommaDelimString(decoderProductID, r.getProductID())) {
            return false;
        }

        if (developerID != null) {
            // must have a (LocoNet SV2) developerID value that matches to consider this entry a match
            if (!developerID.equals(r.getDeveloperID())) {
                // didn't match the getDeveloperID() value, so check the model developerID value
                if (r.getModelElement().getAttribute("developerID") == null) {
                    // no model developerID value, so not a match!
                    return false;
                }
                if (!("," + r.getModelElement().getAttribute("developerID").getValue() + ",").contains("," + developerID + ",")) {
                        return false;
                }
            }
            log.debug("developerID match");
        }

        if (manufacturerID != null) {
            log.debug("checking manufactureriD {}, mfgID {}, modelElement[manufacturerID] {}",
                    manufacturerID, r._mfgID, r.getModelElement().getAttribute("manufacturerID"));
            // must have a manufacturerID value that matches to consider this entry a match

            if ((r._mfgID == null) || (manufacturerID.compareTo(r._mfgID) != 0)) {
                // ID number from manufacturer name isn't identical; try another way
                if (!manufacturerID.equals(r.getManufacturerID())) {
                    // no match to the manufacturerID attribute at the (family?) level, so try model level
                    Attribute a = r.getModelElement().getAttribute("manufacturerID");
                    if ((a == null) || (a.getValue() == null) ||
                            (manufacturerID.compareTo(a.getValue())!=0)) {
                            // no model manufacturerID value, or model manufacturerID
                            // value does not match so this decoder is not a match!
                            return false;
                    }
                }
            }
            log.debug("manufacturerID match");
        }

        if (productID != null) {
            // must have a (LocoNet SV2 or the Uhlenbrock LNCV protocol) productID value that matches to consider this entry a match
            if (!productID.equals(r.getProductID())) {
                // didn't match the getProductID() value, so check the model productID value
                if (r.getModelElement().getAttribute("productID") == null) {
                    // no model productID value, so not a match!
                    return false;
                }
                if (!("," + r.getModelElement().getAttribute("productID").getValue() + ",").contains("," + productID + ",")) {
                        return false;
                }
            }
            log.debug("productID match");
        }

        if (progMode != null) {
            // must have a progMode value that matches to consider this entry a match
            return r.isProgrammingMode(progMode); // simplified logic while this is the last if in method
        }

        return true;
    }

    /**
     * Replace the managed instance with a new instance.
     */
    public static synchronized void resetInstance() {
        InstanceManager.getDefault().clear(DecoderIndexFile.class);
    }

    /**
     * Check whether the user's version of the decoder index file needs to be
     * updated; if it does, then forces the update.
     *
     * @return true is the index should be reloaded because it was updated
     * @throws JDOMException if unable to parse decoder index
     * @throws IOException     if unable to read decoder index
     */
    public static boolean updateIndexIfNeeded() throws JDOMException, IOException {
        switch (FileUtil.findFiles(defaultDecoderIndexFilename(), ".").size()) {
            case 0:
                log.debug("creating decoder index");
                forceCreationOfNewIndex();
                return true; // no index exists, so create one
            case 1:
                return false; // only one index, so nothing to compare
            default:
                // multiple indexes, so continue with more specific checks
                break;
        }

        // get version from master index; if not found, give up
        String masterVersion = null;
        DecoderIndexFile masterXmlFile = new DecoderIndexFile();
        URL masterFile = FileUtil.findURL("xml/" + defaultDecoderIndexFilename(), FileUtil.Location.INSTALLED);
        if (masterFile == null) {
            return false;
        }
        log.debug("checking for master file at {}", masterFile);
        Element masterRoot = masterXmlFile.rootFromURL(masterFile);
        if (masterRoot.getChild(DECODER_INDEX) != null) {
            if (masterRoot.getChild(DECODER_INDEX).getAttribute(VERSION) != null) {
                masterVersion = masterRoot.getChild(DECODER_INDEX).getAttribute(VERSION).getValue();
            }
            log.debug("master version found, is {}", masterVersion);
        } else {
            return false;
        }

        // get from user index.  Unless they are equal, force an update.
        // note we find this file via the search path; if not exists, so that
        // the master is found, we still do the right thing (nothing).
        String userVersion = null;
        DecoderIndexFile userXmlFile = new DecoderIndexFile();
        log.debug("checking for user file at {}", defaultDecoderIndexFilename());
        Element userRoot = userXmlFile.rootFromName(defaultDecoderIndexFilename());
        if (userRoot.getChild(DECODER_INDEX) != null) {
            if (userRoot.getChild(DECODER_INDEX).getAttribute(VERSION) != null) {
                userVersion = userRoot.getChild(DECODER_INDEX).getAttribute(VERSION).getValue();
            }
            log.debug("user version found, is {}", userVersion);
        }
        if (masterVersion != null && masterVersion.equals(userVersion)) {
            return false;
        }

        // force the update, with the version number located earlier is available
        log.debug("forcing update of decoder index due to {} and {}", masterVersion, userVersion);
        forceCreationOfNewIndex();
        // and force it to be used
        return true;
    }

    /**
     * Force creation of a new user index without incrementing version
     */
    public static void forceCreationOfNewIndex() {
        forceCreationOfNewIndex(false);
    }

    /**
     * Force creation of a new user index.
     *
     * @param increment true to increment the version of the decoder index
     */
    public static void forceCreationOfNewIndex(boolean increment) {
        log.info("update decoder index");
        // make sure we're using only the default manufacturer info
        // to keep from propagating wrong, old stuff
        File oldfile = new File(FileUtil.getUserFilesPath() + DECODER_INDEX_FILE_NAME);
        if (oldfile.exists()) {
            log.debug("remove existing user decoderIndex.xml file");
            if (!oldfile.delete()) // delete file, check for success
            {
                log.error("Failed to delete old index file");
            }
            // force read from distributed file on next access
            resetInstance();
        }

        // create an array of file names from decoders dir in preferences, count entries
        ArrayList<String> al = new ArrayList<>();
        FileUtil.createDirectory(FileUtil.getUserFilesPath() + DecoderFile.fileLocation);
        File fp = new File(FileUtil.getUserFilesPath() + DecoderFile.fileLocation);

        if (fp.exists()) {
            String[] list = fp.list();
            if (list !=null) {
                for (String sp : list) {
                    if (sp.endsWith(".xml") || sp.endsWith(".XML")) {
                        al.add(sp);
                    }
                }
            }
        } else {
            log.debug("{}decoders was missing, though tried to create it", FileUtil.getUserFilesPath());
        }
        // create an array of file names from xml/decoders, count entries
        String[] fileList = (new File(XmlFile.xmlDir() + DecoderFile.fileLocation)).list();
        if (fileList != null) {
            for (String sx : fileList ) {
                if (sx.endsWith(".xml") || sx.endsWith(".XML")) {
                    // Valid name.  Does it exist in preferences xml/decoders?
                    if (!al.contains(sx)) {
                        // no, include it!
                        al.add(sx);
                    }
                }
            }
        } else {
            log.error("Could not access decoder definition directory {}{}", XmlFile.xmlDir(), DecoderFile.fileLocation);
        }
        // copy the decoder entries to the final array
        String[] sbox = al.toArray(new String[0]);

        //the resulting array is now sorted on file-name to make it easier
        // for humans to read
        Arrays.sort(sbox);

        // create a new decoderIndex
        DecoderIndexFile index = new DecoderIndexFile();

        // For user operations the existing version is used, so that a new master file
        // with a larger one will force an update
        if (increment) {
            index.fileVersion = InstanceManager.getDefault(DecoderIndexFile.class).fileVersion + 2;
        } else {
            index.fileVersion = InstanceManager.getDefault(DecoderIndexFile.class).fileVersion;
        }

        // If not many entries, or headless, just recreate index without updating the UI
        // Also block if not on the GUI (event dispatch) thread
        if (sbox.length < 30 || GraphicsEnvironment.isHeadless() || !ThreadingUtil.isGUIThread()) {
            try {
                index.writeFile(DECODER_INDEX_FILE_NAME,
                            InstanceManager.getDefault(DecoderIndexFile.class), sbox, null, null);
            } catch (IOException ex) {
                log.error("Error writing new decoder index file: {}", ex.getMessage());
            }
            return;
        }

        // Create a dialog with a progress bar and a cancel button
        String message = Bundle.getMessage("DecoderProgressMessage", "..."); // NOI18N
        String title = Bundle.getMessage("DecoderProgressMessage", "");
        String cancel = Bundle.getMessage("ButtonCancel"); // NOI18N
        // HACK: add long blank space to message to make dialog wider.
        JOptionPane pane = new JOptionPane(message + "                            \t",
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                new String[]{cancel});
        JProgressBar pb = new JProgressBar(0, sbox.length);
        pb.setValue(0);
        pane.add(pb, 1);
        JDialog dialog = pane.createDialog(null, title);

        ThreadingUtil.newThread(() -> {
            try {
                index.writeFile(DECODER_INDEX_FILE_NAME,
                            InstanceManager.getDefault(DecoderIndexFile.class), sbox, pane, pb);
            // catch all exceptions, so progress dialog will close
            } catch (IOException e) {
                // TODO: show message in progress dialog?
                log.error("Error writing new decoder index file: {}", e.getMessage());
            }
            dialog.setVisible(false);
            dialog.dispose();
        }, "decoderIndexer").start();

        // improve visibility if any always on top frames present
        dialog.setAlwaysOnTop(true);
        dialog.toFront();
        // this will block until the thread completes, either by
        // finishing or by being cancelled
        dialog.setVisible(true);
    }

    /**
     * Read the contents of a decoderIndex XML file into this object. Note that
     * this does not clear any existing entries; reset the instance to do that.
     *
     * @param name the name of the decoder index file
     * @throws JDOMException if unable to parse to decoder index file
     * @throws IOException     if unable to read decoder index file
     */
    void readFile(String name) throws JDOMException, IOException {
        log.debug("readFile {}", name);

        // read file, find root
        Element root = rootFromName(name);

        // decode type, invoke proper processing routine if a decoder file
        if (root.getChild(DECODER_INDEX) != null) {
            if (root.getChild(DECODER_INDEX).getAttribute(VERSION) != null) {
                fileVersion = Integer.parseInt(root.getChild(DECODER_INDEX)
                        .getAttribute(VERSION)
                        .getValue()
                );
            }
            log.debug("found fileVersion of {}", fileVersion);
            readMfgSection();
            readFamilySection(root.getChild(DECODER_INDEX));
        } else {
            log.error("Unrecognized decoderIndex file contents in file: {}", name);
        }
    }

    void readMfgSection() throws JDOMException, IOException {
        // always reads the NMRA manufacturer file distributed with JMRI
        Element mfgList = rootFromName("nmra_mfg_list.xml");

        if (mfgList != null) {

            Attribute a;
            a = mfgList.getAttribute("nmraListDate");
            if (a != null) {
                nmraListDate = a.getValue();
            }
            a = mfgList.getAttribute("updated");
            if (a != null) {
                updated = a.getValue();
            }
            a = mfgList.getAttribute("lastadd");
            if (a != null) {
                lastAdd = a.getValue();
            }

            List<Element> l = mfgList.getChildren(MANUFACTURER);
            log.debug("readMfgSection sees {} children",l.size());
            for (Element el : l) {
                // handle each entry
                String mfg = el.getAttribute("mfg").getValue();
                mMfgNameList.add(mfg);
                Attribute attr = el.getAttribute(MFG_ID);
                if (attr != null) {
                    _mfgIdFromNameHash.put(mfg, attr.getValue());
                    _mfgNameFromIdHash.put(attr.getValue(), mfg);
                }
            }
        } else {
            log.debug("no mfgList found");
        }
    }

    void readFamilySection(Element decoderIndex) {
        Element familyList = decoderIndex.getChild("familyList");
        if (familyList != null) {

            List<Element> l = familyList.getChildren("family");
            log.trace("readFamilySection sees {} children", l.size());
            for (Element el : l) {
                // handle each entry
                readFamily(el);
            }
        } else {
            log.debug("no familyList found in decoderIndexFile");
        }
    }

    void readFamily(Element family) {
        Attribute attr;
        String filename = family.getAttribute("file").getValue();
        String parentLowVersID = ((attr = family.getAttribute(LOW_VERSION_ID)) != null ? attr.getValue() : null);
        String parentHighVersID = ((attr = family.getAttribute(HIGH_VERSION_ID)) != null ? attr.getValue() : null);
        String ParentReplacementFamilyName = ((attr = family.getAttribute("replacementFamily")) != null ? attr.getValue() : null);
        String familyName = ((attr = family.getAttribute("name")) != null ? attr.getValue() : null);
        String mfg = ((attr = family.getAttribute("mfg")) != null ? attr.getValue() : null);
        String developerID = ((attr = family.getAttribute("developerID")) != null ? attr.getValue() : null);
        String manufacturerID = ((attr = family.getAttribute("manufacturerID")) != null ? attr.getValue() : null);
        String productID = ((attr = family.getAttribute("productID")) != null ? attr.getValue() : null);
        String mfgID = null;
        if (mfg != null) {
            mfgID = mfgIdFromName(mfg);
        } else {
            log.error("Did not find required mfg attribute, may not find proper manufacturer");
        }

        // extract <programming> modes of a family's parent <decoder> element
        String modes = ((attr = family.getAttribute("modes")) != null ? attr.getValue() : null);

        List<Element> l = family.getChildren("model");
        log.trace("readFamily sees {} children", l.size());
        Element modelElement;
        if (l.isEmpty()) {
            log.error("Did not find at least one model in the {} family", familyName);
            modelElement = null;
        } else {
            modelElement = l.get(0);
        }

        // Record the family as a specific model, which allows you to select the
        // family as a possible thing to program
        DecoderFile vFamilyDecoderFile
                = new DecoderFile(mfg, mfgID, familyName,
                        parentLowVersID, parentHighVersID,
                        familyName,
                        filename,
                        (developerID != null) ? developerID : "-1",
                        (manufacturerID != null) ? manufacturerID : "-1",
                        (productID != null) ? productID : "-1",
                        -1, -1, modelElement,
                        ParentReplacementFamilyName, ParentReplacementFamilyName,
                        modes); // numFns, numOuts, XML element equal
        // add family model as the first decoder
        decoderList.add(vFamilyDecoderFile);

        // record each of the decoders
        for (Element decoder : l) {
            // handle each entry by creating a DecoderFile object containing all it knows
            String loVersID = ((attr = decoder.getAttribute(LOW_VERSION_ID)) != null ? attr.getValue() : parentLowVersID);
            String hiVersID = ((attr = decoder.getAttribute(HIGH_VERSION_ID)) != null ? attr.getValue() : parentHighVersID);
            String replacementModelName = ((attr = decoder.getAttribute("replacementModel")) != null ? attr.getValue() : null);
            String replacementFamilyName = ((attr = decoder.getAttribute("replacementFamily")) != null ? attr.getValue() : ParentReplacementFamilyName);
            int numFns = ((attr = decoder.getAttribute("numFns")) != null ? Integer.parseInt(attr.getValue()) : -1);
            int numOuts = ((attr = decoder.getAttribute("numOuts")) != null ? Integer.parseInt(attr.getValue()) : -1);
            String devId = ((attr = decoder.getAttribute("developerID")) != null ? attr.getValue() : "-1");
            String manufId = ((attr = decoder.getAttribute("manufacturerID")) != null ? attr.getValue() : "-1");
            String prodId = ((attr = decoder.getAttribute("productID")) != null ? attr.getValue() : "-1");

            DecoderFile df = new DecoderFile(mfg, mfgID,
                    ((attr = decoder.getAttribute("model")) != null ? attr.getValue() : null),
                    loVersID, hiVersID, familyName, filename, devId, manufId, prodId, numFns, numOuts, decoder,
                    replacementModelName, replacementFamilyName, modes);
            // and store it
            decoderList.add(df);
            // if there are additional version numbers defined, handle them too
            List<Element> vcodes = decoder.getChildren("versionCV");
            for (Element vcv : vcodes) {
                // for each versionCV element
                String vLoVersID = ((attr = vcv.getAttribute(LOW_VERSION_ID)) != null ? attr.getValue() : loVersID);
                String vHiVersID = ((attr = vcv.getAttribute(HIGH_VERSION_ID)) != null ? attr.getValue() : hiVersID);
                df.setVersionRange(vLoVersID, vHiVersID);
            }
        }
    }

    /**
     * Check if target string is in a comma-delimited string
     * <p>
     * Example:
     *      findString = "47"
     *      inString = "1,4,53,97"
     *      return value is 'false'
     * <p>
     * Example:
     *      findString = "47"
     *      inString = "1,31,47,51"
     *      return value is 'true'
     * <p>
     * Example:
     *      findString = "47"
     *      inString = "47"
     *      return value is true
     *
     * @param findString string to find
     * @param inString comma-delimited string of sub-strings
     * @return true if target string is found as sub-string within comma-
     *      delimited string
     */
    public boolean checkInCommaDelimString(String findString, String inString) {
        String bracketedFindString = ","+findString+",";
        String bracketedInString = ","+inString+",";
        return bracketedInString.contains(bracketedFindString);
    }

    /**
     * Build and write the decoder index file, based on a set of decoder files.
     * <p>
     * This creates the full DOM object for the decoder index based on reading the
     * supplied decoder xml files. It then saves the decoder index out to a new file.
     *
     * @param name name of the new index file
     * @param oldIndex old decoder index file
     * @param files array of files to read for new index
     * @param pane optional JOptionPane to check for cancellation
     * @param pb optional JProgressBar to update during operations
     * @throws IOException for errors writing the decoder index file
     */
    public void writeFile(String name, DecoderIndexFile oldIndex,
                          String[] files, JOptionPane pane, JProgressBar pb) throws IOException {
        log.debug("writeFile {}",name);

        // This is taken in large part from "Java and XML" page 368
        File file = new File(FileUtil.getUserFilesPath() + name);

        // create root element and document
        Element root = new Element("decoderIndex-config");
        root.setAttribute("noNamespaceSchemaLocation",
                "http://jmri.org/xml/schema/decoder-4-15-2.xsd",
                org.jdom2.Namespace.getNamespace("xsi",
                        "http://www.w3.org/2001/XMLSchema-instance"));

        Document doc = newDocument(root);

        // add XSLT processing instruction
        // <?xml-stylesheet type="text/xsl" href="XSLT/DecoderID.xsl"?>
        java.util.Map<String, String> m = new java.util.HashMap<>();
        m.put("type", "text/xsl");
        m.put("href", xsltLocation + "DecoderID.xsl");
        ProcessingInstruction p = new ProcessingInstruction("xml-stylesheet", m);
        doc.addContent(0, p);

        // add top-level elements
        Element index;
        root.addContent(index = new Element(DECODER_INDEX));
        index.setAttribute(VERSION, Integer.toString(fileVersion));
        log.debug("version written to file as {}", fileVersion);

        // add mfg list from existing DecoderIndexFile item
        Element mfgList = new Element("mfgList");
        // copy dates from original mfgList element
        if (oldIndex.nmraListDate != null) {
            mfgList.setAttribute("nmraListDate", oldIndex.nmraListDate);
        }
        if (oldIndex.updated != null) {
            mfgList.setAttribute("updated", oldIndex.updated);
        }
        if (oldIndex.lastAdd != null) {
            mfgList.setAttribute("lastadd", oldIndex.lastAdd);
        }

        // We treat "NMRA" special...
        Element mfg = new Element(MANUFACTURER);
        mfg.setAttribute("mfg", "NMRA");
        mfg.setAttribute(MFG_ID, "999");
        mfgList.addContent(mfg);
        // start working on the rest of the entries
        List<String> keys = new ArrayList<>(oldIndex._mfgIdFromNameHash.keySet());
        Collections.sort(keys);
        for (Object item : keys) {
            String mfgName = (String) item;
            if (!mfgName.equals("NMRA")) {
                mfg = new Element(MANUFACTURER);
                mfg.setAttribute("mfg", mfgName);
                mfg.setAttribute(MFG_ID, oldIndex._mfgIdFromNameHash.get(mfgName));
                mfgList.addContent(mfg);
            }
        }

        // add family list by scanning files
        Element familyList = new Element("familyList");
        int fileNum = 0;
        for (String fileName : files) {
            // update progress monitor, if passed in
            if (pb != null) {
                pb.setValue(fileNum++);
            }
            if (pane != null && pane.getValue() != JOptionPane.UNINITIALIZED_VALUE) {
                log.info("Decoder index recreation cancelled");
                return;
            }
            DecoderFile d = new DecoderFile();
            try {
                // get <family> element and add the file name
                Element droot = d.rootFromName(DecoderFile.fileLocation + fileName);
                Element family = droot.getChild("decoder").getChild("family").clone();
                // get decoder element's child programming and copy the mode children
                Element prog = droot.getChild("decoder").getChild("programming");
                if (prog != null) {
                    List<Element> modes = prog.getChildren("mode");
                    if (modes != null) {
                        StringBuilder supportedModes = new StringBuilder();
                        for (Element md : modes) { // typically only 1 mode element in a definition
                            String modeName = md.getText(); // example: LOCONETLNCVMODE
                            if (supportedModes.length() > 0) supportedModes.append(",");
                            supportedModes.append(modeName);
                        }
                        if (supportedModes.length() > 0) {
                            family.setAttribute("modes", supportedModes.toString());
                        }
                    }
                }
                family.setAttribute("file", fileName);

                // drop the decoder implementation content
                // comment is kept, so it displays
                // don't remove "outputs" due to use by ESU function map pane
                // family.removeChildren("output");
                // family.removeChildren("functionlabels");

                // and drop content of model elements
                for (Element element : family.getChildren()) { // model elements
                    element.removeAttribute("maxInputVolts");
                    element.removeAttribute("maxMotorCurrent");
                    element.removeAttribute("maxTotalCurrent");
                    element.removeAttribute("formFactor");
                    element.removeAttribute("connector");
                    // comment is kept so it displays
                    element.removeAttribute("nmraWarrant");
                    element.removeAttribute("nmraWarrantStart");

                    // element.removeContent();
                    element.removeChildren("size");

                    //element.removeChildren("functionlabels");

                    // don't remove "output" due to use by ESU function map pane
                    for (Element output : element.getChildren()) {
                        output.removeAttribute("connection");
                        output.removeAttribute("maxcurrent");
                        output.removeChildren("label");
                    }
                }

                // and store to output
                familyList.addContent(family);
            } catch (JDOMException exj) {
                log.error("could not parse {}: {}", fileName, exj.getMessage());
            } catch (FileNotFoundException exj) {
                log.error("could not read {}: {}", fileName, exj.getMessage());
            } catch (IOException exj) {
                log.error("other exception while dealing with {}: {}", fileName, exj.getMessage());
            }
        }

        index.addContent(new Comment("The manufacturer list is from the nmra_mfg_list.xml file"));
        index.addContent(mfgList);
        index.addContent(familyList);

        log.debug("Writing decoderIndex");
        try {
            writeXML(file, doc);
        } catch (Exception e) {
            log.error("Error writing file: {}", file, e);
        }

        // force a read of the new file next time
        resetInstance();
    }

    String nmraListDate = null;
    String updated = null;
    String lastAdd = null;

    /**
     * Get the filename for the default decoder index file, including location.
     * This is here to allow easy override in tests.
     *
     * @return the complete path to the decoder index
     */
    protected static String defaultDecoderIndexFilename() {
        return DECODER_INDEX_FILE_NAME;
    }

    protected static final String DECODER_INDEX_FILE_NAME = "decoderIndex.xml";
    private static final Logger log = LoggerFactory.getLogger(DecoderIndexFile.class);

    @ServiceProvider(service = InstanceInitializer.class)
    public static class Initializer extends AbstractInstanceInitializer {

        @Override
        @Nonnull
        public <T> Object getDefault(Class<T> type) {
            if (type.equals(DecoderIndexFile.class)) {
                // create and load
                DecoderIndexFile instance = new DecoderIndexFile();
                log.debug("DecoderIndexFile creating instance");
                try {
                    instance.readFile(defaultDecoderIndexFilename());
                } catch (IOException | JDOMException e) {
                    log.error("Exception during decoder index reading: ", e);
                }
                // see if needs to be updated
                try {
                    if (updateIndexIfNeeded()) {
                        try {
                            instance = new DecoderIndexFile();
                            instance.readFile(defaultDecoderIndexFilename());
                        } catch (IOException | JDOMException e) {
                            log.error("Exception during decoder index reload: ", e);
                        }
                    }
                } catch (IOException | JDOMException e) {
                    log.error("Exception during decoder index update: ", e);
                }
                log.debug("DecoderIndexFile returns instance {}", instance);
                return instance;
            }
            return super.getDefault(type);
        }

        @Override
        @Nonnull
        public Set<Class<?>> getInitalizes() {
            Set<Class<?>> set = super.getInitalizes();
            set.add(DecoderIndexFile.class);
            return set;
        }
    }

}
