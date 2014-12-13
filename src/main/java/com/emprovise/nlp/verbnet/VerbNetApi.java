package com.emprovise.nlp.verbnet;

import com.emprovise.nlp.util.FileUtil;
import edu.mit.jverbnet.data.*;
import edu.mit.jverbnet.index.IVerbIndex;
import edu.mit.jverbnet.index.VerbIndex;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.util.*;

public class VerbNetApi {

    private IVerbIndex index;
    private Map<String, String> verbsIndexedByClassID = new LinkedHashMap();
    private Map<String, String> verbsIndexedByClassName = new LinkedHashMap();

    private static Logger log = Logger.getLogger(VerbNetApi.class);

    public VerbNetApi() {
        try {
            File inputFile = new File(getClass().getClassLoader().getResource("verbnet/verbnet-3.2.tar.gz").toURI());
            File outputFile = new File("library/verbnet");

            if (!outputFile.exists() || outputFile.list().length == 0){
                FileUtil.extractFile(inputFile, outputFile);
            }

            // make a url pointing to the Verbnet data
            URL url = new URL("file", null, outputFile.getAbsolutePath() + "/new_vn");
            index = new VerbIndex(url);
            index.open();
            makeVerbNetMappings();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * Requres a verb formatted like "hit-18.1"
     * @param verbString the formatted string
     * @return
     */
    public IVerbClass lookupVerb(String verbString) {
        IVerbClass verb = index.getVerb(verbString);
        return verb;
    }

    /**
     * Loads verb class for the specified verb string.
     * Does not require any formatting for the verb string.
     * @param verbString
     *          verb string to load verb class
     * @return
     */
    public IVerbClass lookupVerbByName(String verbString) {
        return lookupVerb(verbsIndexedByClassName.get(verbString));
    }

    public IVerbClass lookupVerbByID(String verbString) {
        return lookupVerb(verbsIndexedByClassID.get(verbString));
    }

    /**
     * Turns a string that represents the member of a verbclass into a reference
     * to the member of that class
     *
     * @param verb
     *            A verb class
     * @param member
     *            A string representing the member of that verb class
     * @return
     */
    public IMember lookupMemberByName(IVerbClass verb, String member) {
        for (IMember m : verb.getMembers()) {
            if (m.getName().equals(member))
                return m;
        }
        return null;
    }

    public IVerbIndex getIndex() {
        return index;
    }

    public void setIndex(IVerbIndex index) {
        this.index = index;
    }

    /**
     * SemLink's class field refers to verb classes by their id's (e.g., 18.1),
     * but JVerbNet refers to classes by their id's concatenated with the class
     * name (e.g., hit-18.1). This makes a map of id's to class names (18.1,
     * hit-18.1)
     *
     * @return map of ids to class names
     */
    public void makeVerbNetMappings() {
        Iterator<IVerbClass> itr = index.iterator();
        while (itr.hasNext()) {
            IVerbClass verb = itr.next();
            String id = verb.getID();
            String[] parts = id.split("-", 2);
            if (parts.length == 2) {
                verbsIndexedByClassName.put(parts[0], id);
                verbsIndexedByClassID.put(parts[1], id);
            } else {
                log.error("Couldn't make a VerbNet mapping for" + verb.getID());
            }
        }
    }

    public Set<IMember> getMembers(String wordnetKey) {
        return index.getMembers(WordnetKey.parseKey(wordnetKey));
    }

    public Set<String> getMembers(IVerbClass verb) {
        Set<String> members = new LinkedHashSet<String>();
        for (IMember m : verb.getMembers()) {
            members.add(m.getName());
        }
        return members;
    }

    public IFrame lookupFrameByID(IVerbClass verb, String frameID) {

        for(IFrame frame : verb.getFrames()) {
            if(frame.getPrimaryType().getID().equals(frameID)) {
                return frame;
            }
        }
        return null;
    }

    public Set<IWordnetKey> getKeys(IMember member) throws Exception {
        return member.getWordnetTypes().keySet();
    }

    public static void main(String[] args) {
        VerbNetApi api = new VerbNetApi();
    }
}
