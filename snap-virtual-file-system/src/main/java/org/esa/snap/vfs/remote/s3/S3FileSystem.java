package org.esa.snap.vfs.remote.s3;

import org.esa.snap.vfs.remote.AbstractRemoteFileSystem;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

/**
 * File System for S3
 *
 * @author Jean Coravu
 */
public class S3FileSystem extends AbstractRemoteFileSystem {

    /**
     * Creates the new File System for VFS.
     *
     * @param provider The VFS provider
     */
    public S3FileSystem(AbstractRemoteFileSystemProvider provider, String fileSystemRoot) {
        super(provider, fileSystemRoot);
    }
}