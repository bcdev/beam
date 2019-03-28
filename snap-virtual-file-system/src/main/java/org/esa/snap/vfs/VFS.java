package org.esa.snap.vfs;

import org.esa.snap.vfs.preferences.model.Property;
import org.esa.snap.vfs.preferences.model.VFSRemoteFileRepository;
import org.esa.snap.vfs.remote.AbstractRemoteFileSystemProvider;

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Created by jcoravu on 21/3/2019.
 */
public class VFS {

    private List<FileSystemProvider> installedProviders;

    private static final VFS instance;

    static {
        instance = new VFS();
        instance.loadInstalledProviders();
    }

    private VFS() {
    }

    public static VFS getInstance() {
        return instance;
    }

    public List<FileSystemProvider> getInstalledProviders() {
        return installedProviders;
    }

    public FileSystemProvider getFileSystemProviderByScheme(String scheme) {
        for (FileSystemProvider fileSystemProvider : this.installedProviders) {
            if (scheme.equalsIgnoreCase(fileSystemProvider.getScheme())) {
                return fileSystemProvider;
            }
        }
        return null;
    }

    public void initRemoteInstalledProviders(List<VFSRemoteFileRepository> vfsRepositories) {
        for (FileSystemProvider fileSystemProvider : this.installedProviders) {
            if (fileSystemProvider instanceof AbstractRemoteFileSystemProvider) {
                AbstractRemoteFileSystemProvider remoteFileSystemProvider = (AbstractRemoteFileSystemProvider) fileSystemProvider;

                VFSRemoteFileRepository foundRepository = null;
                for (int k = 0; k < vfsRepositories.size() && foundRepository == null; k++) {
                    VFSRemoteFileRepository repository = vfsRepositories.get(k);
                    if (repository.getScheme().equalsIgnoreCase(remoteFileSystemProvider.getScheme())) {
                        foundRepository = repository;
                    }
                }

                if (foundRepository != null) {
                    Map<String, String> connectionData = new HashMap<>();
                    for (Property vfsRemoteFileRepositoryProperty : foundRepository.getProperties()) {
                        connectionData.put(vfsRemoteFileRepositoryProperty.getName(), vfsRemoteFileRepositoryProperty.getValue());
                    }
                    remoteFileSystemProvider.setConnectionData(foundRepository.getAddress(), connectionData);
                }
            }
        }
    }

    public Path getPath(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("Missing scheme.");
        }
        // check for default provider to avoid loading of installed providers
        if (scheme.equalsIgnoreCase("file")) {
            return FileSystems.getDefault().provider().getPath(uri);
        }
        for (FileSystemProvider provider : this.installedProviders) {
            if (provider.getScheme().equalsIgnoreCase(scheme)) {
                return provider.getPath(uri);
            }
        }
        throw new FileSystemNotFoundException("The file system provider with the scheme '" + scheme + "' is not installed.");
    }

    public Path get(String first, String... more) {
        Path path = getVirtualPath(first, more);
        if (path != null) {
            return path;
        }
        return FileSystems.getDefault().getPath(first, more);
    }

    public boolean isVirtualFileSystemPath(String path) {
        return (getVirtualPath(path) != null);
    }

    public Path getVirtualPath(String first, String... more) {
        for (FileSystemProvider provider : VFS.getInstance().getInstalledProviders()) {
            if (provider instanceof AbstractRemoteFileSystemProvider) {
                AbstractRemoteFileSystemProvider remoteFileSystemProvider = (AbstractRemoteFileSystemProvider)provider;
                Path path = remoteFileSystemProvider.getPathIfFileSystemRootMatches(first, more);
                if (path != null) {
                    return path;
                }
            }
        }
        return null;
    }

    private void loadInstalledProviders() {
        this.installedProviders = new ArrayList<>();

        Set<String> uniqueSchemes = new HashSet<>();

        // load the remote file system providers
        ServiceLoader<FileSystemProvider> serviceLoader = ServiceLoader.load(FileSystemProvider.class);
        for (FileSystemProvider provider : serviceLoader) {
            if (provider instanceof AbstractRemoteFileSystemProvider) {
                if (uniqueSchemes.add(provider.getScheme())) {
                    this.installedProviders.add(provider);
                } else {
                    throw new IllegalStateException("The remote file system provider type '" + provider.getClass() + "' with the scheme '" + provider.getScheme() + "' is not unique.");
                }
            }
        }

        // load the default file system providers
        for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if (!(provider instanceof AbstractRemoteFileSystemProvider)) {
                if (uniqueSchemes.add(provider.getScheme())) {
                    this.installedProviders.add(provider);
                } else {
                    throw new IllegalStateException("The default file system provider type '" + provider.getClass() + "' with the scheme '" + provider.getScheme() + "' is not unique.");
                }
            }
        }
    }
}
