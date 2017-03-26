package de.tobifleig.lxc.net;

import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.log.LXCLogger;

import java.io.*;
import java.util.ArrayList;

/**
 * An ObjectInputStream used for untrusted input data validation.
 * Only a fixed set of classes is allowed to be loaded.
 */
public final class LookaheadObjectInputStream extends ObjectInputStream {

    private final LXCLogger logger;

    /**
     * List of white-listed classes.
     * This ObjectInputStream will never deserialize classes that are not on this list.
     * This includes all classes nested as members.
     */
    private final String[] permittedClassNames = new String[]{
            String.class.getName(),
            LXCFile.class.getName(),
            TransFileList.class.getName(),
            ArrayList.class.getName()
    };

    public LookaheadObjectInputStream(InputStream in) throws IOException {
        super(in);
        logger = LXCLogBackend.getLogger("network-security");
    }

    /**
     * Disallow deserialization of unknown classes.
     */
    @Override
    protected Class<?> resolveClass(ObjectStreamClass objectStreamClass) throws IOException, ClassNotFoundException {
        if (!isPermitted(objectStreamClass.getName())) {
            logger.warn("Blocked attempt to deserialize non-whitelisted class \"" + objectStreamClass.getName() + "\"");
            throw new InvalidClassException("Blocked attempt to deserialize non-whitelisted class \"" + objectStreamClass.getName() + "\"");
        }
        // permitted, go on
        return super.resolveClass(objectStreamClass);
    }


    /**
     * Checks if the given class name is allowed to be loaded.
     * @param className the class name to check
     * @return true, iff allowed
     */
    private boolean isPermitted(String className) {
        for (String s : permittedClassNames) {
            if (s.equals(className)) {
                return true;
            }
        }
        return false;
    }
}
