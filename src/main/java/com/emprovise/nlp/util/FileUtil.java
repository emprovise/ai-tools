package com.emprovise.nlp.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;

public class FileUtil {

    final static int BUFFER = 2048;

    private FileUtil() {
        throw new UnsupportedOperationException();
    }

    public static void extractFile(File sourceFile, File destDir) throws IOException {

        if(!destDir.exists()) {
            destDir.mkdirs();
        }

        FileInputStream fin = new FileInputStream(sourceFile);
        BufferedInputStream in = new BufferedInputStream(fin);
        GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
        TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);
        TarArchiveEntry entry = null;

        while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {

            System.out.println("Extracting: " + entry.getName());

            /** If the entry is a directory, create the directory. **/
            if (entry.isDirectory()) {
                File f = new File(destDir, entry.getName());
                f.mkdirs();
            }

            /**
             * If the entry is a file,write the decompressed file to the disk
             * and close destination stream.
             **/
            else {
                int count;
                byte data[] = new byte[BUFFER];
                FileOutputStream fos = new FileOutputStream(new File(destDir, entry.getName()));
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = tarIn.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }

                dest.close();
            }
        }

        /** Close the input stream **/
        tarIn.close();
        gzIn.close();
    }
}
