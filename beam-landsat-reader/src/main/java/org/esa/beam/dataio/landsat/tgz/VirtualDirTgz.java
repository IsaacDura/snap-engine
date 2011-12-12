package org.esa.beam.dataio.landsat.tgz;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.util.io.FileUtils;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

import java.io.*;

public class VirtualDirTgz extends VirtualDir {

    private final File archiveFile;
    private File extractDir;

    public VirtualDirTgz(File tgz) throws IOException {
        if (tgz == null) {
            throw new IllegalArgumentException("Input file shall not be null");
        }
        archiveFile = tgz;
        extractDir = null;
    }

    @Override
    public String getBasePath() {
        return archiveFile.getPath();
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        ensureUnpacked();
        final File file = new File(extractDir, path);
        if (!file.isFile()) {
            throw new IOException();
        }
        return new FileInputStream(file);
    }

    @Override
    public File getFile(String path) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String[] list(String path) throws IOException {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() {
        if (extractDir != null) {
            FileUtils.deleteTree(extractDir);
            extractDir = null;
        }
    }

    // @todo 3 tb/** this code is almost completey dublicated from com.bc.ceres.core.VirtualDir
    // it maybe makes sense to move it to a file utility class
    static File createTargetDirInTemp(String name) throws IOException {
        File tempDir = null;
        String tempDirName = System.getProperty("java.io.tmpdir");
        if (tempDirName != null) {
            tempDir = new File(tempDirName);
        }
        if (tempDir == null) {
            tempDir = new File(new File(System.getProperty("user.home", ".")), ".beam/temp");
            if (!tempDir.exists()) {
                if (!tempDir.mkdirs()) {
                    throw new IOException("unable to create directory: " + tempDir.getAbsolutePath());
                }
            }
        }

        File targetDir = new File(tempDir, name);
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("unable to create directory: " + targetDir.getAbsolutePath());
            }
        }

        return targetDir;
    }

    static String getFilenameFromPath(String path) {
        int lastSepIndex = path.lastIndexOf("/");
        if (lastSepIndex == -1) {
            lastSepIndex = path.lastIndexOf("\\");
            if (lastSepIndex == -1) {
                return path;
            }
        }

        return path.substring(lastSepIndex + 1, path.length());
    }

    private void ensureUnpacked() throws IOException {
        if (extractDir == null) {
            extractDir = createTargetDirInTemp(archiveFile.getName());

            final TarInputStream tis = new TarInputStream(new BufferedInputStream(new FileInputStream(archiveFile)));
            TarEntry entry;

            while ((entry = tis.getNextEntry()) != null) {
                final String entryName = entry.getName();
                if (entry.isDirectory()) {
                    final File directory = new File(extractDir, entryName);
                    if (!directory.mkdirs()) {
                        throw new IOException("unable to create directory: " + directory.getAbsolutePath());
                    }
                    continue;
                }

                final String fileNameFromPath = getFilenameFromPath(entryName);
                final int pathIndex = entryName.indexOf(fileNameFromPath);
                final String tarPath = entryName.substring(0, pathIndex - 1);
                final File targetDir = new File(extractDir, tarPath);
                final File targetFile = new File(targetDir, fileNameFromPath);
                if (!targetFile.createNewFile()) {
                    throw new IOException("Unable to create file: " + targetFile.getAbsolutePath());
                }

                final OutputStream outStream = new BufferedOutputStream(new FileOutputStream(targetFile));
                final byte data[] = new byte[1024 * 1024];
                int count;
                while ((count = tis.read(data)) != -1) {
                    outStream.write(data, 0, count);
                }

                // @todo 3 tb/tb try finally - make sure everything is closed
                outStream.flush();
                outStream.close();
            }
        }
    }
}