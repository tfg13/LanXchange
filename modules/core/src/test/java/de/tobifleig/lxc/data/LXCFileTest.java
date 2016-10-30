package de.tobifleig.lxc.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class LXCFileTest {

    @org.junit.Test
    public void testConvertToVirtual_simple() throws Exception {
        ArrayList<File> input = new ArrayList<File>();
        File file = new File("/tmp/foobar");
        input.add(file);

        List<VirtualFile> result = LXCFile.convertToVirtual(input);

        assertEquals(1, result.size());

        VirtualFile resultFile = result.get(0);

        assertEquals("foobar", resultFile.getName());
        assertEquals(0, resultFile.size());
    }

    @org.junit.Test
    public void testConvertToVirtual_commonPrefix() throws Exception {
        ArrayList<File> input = new ArrayList<File>();
        File file = new File("/tmp/foobar.tar");
        File file2 = new File("/tmp/foobar.tar.gz");
        input.add(file);
        input.add(file2);

        List<VirtualFile> result = LXCFile.convertToVirtual(input);

        // old code assumed common prefixes are directories and created only one file
        assertEquals(2, result.size());

        // verify names
        // maybe improve this, this also requires the element to be in the right order
        assertEquals("foobar.tar", result.get(0).getName());
        assertEquals("foobar.tar.gz", result.get(1).getName());
    }

}