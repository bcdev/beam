/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.actions.pgrab.ui;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.visat.actions.pgrab.model.*;
import org.esa.beam.visat.actions.pgrab.model.dataprovider.ProductPropertiesProvider;
import org.esa.beam.visat.actions.pgrab.model.dataprovider.QuicklookProvider;
import org.esa.beam.visat.actions.pgrab.model.dataprovider.WorldMapProvider;
import org.esa.beam.visat.actions.pgrab.util.Callback;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;

public class ProductGrabber {

    private static final int quickLookWidth = 150;
    private static final String stopCommand = "stop";
    private static final String updateCommand = "update";
    private static final ImageIcon updateIcon = UIUtils.loadImageIcon("icons/Update24.gif");
    private static final ImageIcon updateRolloverIcon = ToolButtonFactory.createRolloverIcon(updateIcon);
    private static final ImageIcon stopIcon = UIUtils.loadImageIcon("icons/Stop24.gif");
    private static final ImageIcon stopRolloverIcon = ToolButtonFactory.createRolloverIcon(stopIcon);
    private JPanel mainPanel;
    private JComboBox repositoryList;
    private JTable repositoryTable;
    private SortingDecorator sortedModel;
    private JLabel statusLabel;
    private JPanel progressPanel;
    private JButton openButton;
    private JButton removeButton;
    private JButton updateButton;
    private JProgressBar progressBar;
    private JPanel headerPanel;
    private UiCallBack uiCallBack;
    private final RepositoryManager repositoryManager;
    private File currentDirectory;
    private ProductOpenHandler openHandler;
    private final ProductGrabberConfig pgConfig;
    private String helpId;
    private JFrame mainFrame;

    public ProductGrabber(final BasicApp basicApp, final RepositoryManager repositoryManager, final String helpId) {
        pgConfig = new ProductGrabberConfig(basicApp.getPreferences());
        this.repositoryManager = repositoryManager;
        this.repositoryManager.addListener(new MyRepositoryManagerListener());
        addDefaultDataProvider(this.repositoryManager);
        uiCallBack = new UiCallBack();
        this.helpId = helpId;
    }

    /**
     * Sets the ProductOpenHandler which handles the action when products should
     * be opened.
     *
     * @param handler The <code>ProductOpenHandler</code>, can be
     *                <code>null</code> to unset the handler.
     */
    public void setProductOpenHandler(final ProductOpenHandler handler) {
        openHandler = handler;
    }

    public JFrame getFrame() {
        if (mainFrame == null) {
            mainFrame = new JFrame("Product Grabber");
            mainFrame.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            initUI();
            mainFrame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentHidden(final ComponentEvent e) {
                    repositoryManager.stopUpdateRepository();
                }
            });
            mainFrame.add(mainPanel);
            mainFrame.setBounds(pgConfig.getWindowBounds());
            applyConfig(pgConfig);
            mainFrame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentMoved(final ComponentEvent e) {
                    pgConfig.setWindowBounds(e.getComponent().getBounds());
                }

                @Override
                public void componentResized(final ComponentEvent e) {
                    pgConfig.setWindowBounds(e.getComponent().getBounds());
                }
            });
            setUIComponentsEnabled(repositoryList.getItemCount() > 0);
        }
        return mainFrame;
    }

    private void applyConfig(final ProductGrabberConfig config) {
        final Repository[] repositories = config.getRepositories();
        for (Repository repository : repositories) {
            repositoryManager.addRepository(repository);
        }

        final String lastSelectedRepositoryDir = config.getLastSelectedRepositoryDir();
        final Repository selectedRepository = repositoryManager.getRepository(lastSelectedRepositoryDir);
        if (selectedRepository != null) {
            repositoryList.setSelectedItem(selectedRepository);
        } else if (repositoryManager.getNumRepositories() > 0) {
            repositoryList.setSelectedItem(repositoryManager.getRepository(0));
        }
    }

    private void performOpenAction() {
        if (openHandler != null) {
            final Repository repository = (Repository) repositoryList.getSelectedItem();
            if (repository == null) {
                return;
            }
            final int[] selectedRows = getSelectedRows();
            final File[] productFilesToOpen = new File[selectedRows.length];
            for (int i = 0; i < selectedRows.length; i++) {
                final RepositoryEntry entry = repository.getEntry(selectedRows[i]);
                productFilesToOpen[i] = entry.getProductFile();
            }
            openHandler.openProducts(productFilesToOpen);
        }
    }

    private int[] getSelectedRows() {
        final int[] selectedRows = repositoryTable.getSelectedRows();
        final int[] sortedRows = new int[selectedRows.length];
        if (sortedModel != null) {
            for (int i = 0; i < selectedRows.length; i++) {
                sortedRows[i] = sortedModel.getSortedIndex(selectedRows[i]);
            }
            return sortedRows;
        } else {
            return selectedRows;
        }

    }

    private void addRepository() {
        final File baseDir = promptForRepositoryBaseDir();
        if (baseDir == null) {
            return;
        }

        if (repositoryManager.getRepository(baseDir.getPath()) != null) {
            JOptionPane.showMessageDialog(mainPanel,
                                          "The selected directory is already in the repository list.",
                                          "Add Repository",
                                          JOptionPane.WARNING_MESSAGE); /* I18N */
            repositoryList.setSelectedItem(repositoryManager.getRepository(baseDir.getPath()));
            return;
        }

        if (!baseDir.exists()) {
            JOptionPane.showMessageDialog(mainPanel,
                                          "Directory does not exist.", "Add Repository",
                                          JOptionPane.ERROR_MESSAGE); /* I18N */
            return;
        }

        final int answer = JOptionPane.showOptionDialog(mainPanel,
                                                        "Search directory recursively?", "Add Repository",
                                                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                                        null, null); /* I18N */

        boolean doRecursive = false;
        if (answer == JOptionPane.YES_OPTION) {
            doRecursive = true;
        }

        final SwingWorker repositoryCollector = new RepositoryCollector(baseDir, doRecursive,
                                                                        new ProgressBarProgressMonitor(progressBar,
                                                                                                       statusLabel));
        repositoryCollector.execute();
    }

    private void removeRepository(final Repository repository) {
        if (repository != null) {
            repositoryManager.removeRepository(repository);
            final boolean repositoryListIsNotEmpty = repositoryManager.getNumRepositories() > 0;
            if (repositoryListIsNotEmpty) {
                final Repository firstElement = repositoryManager.getRepository(0);
                repositoryList.setSelectedItem(firstElement);
            }
            setUIComponentsEnabled(repositoryListIsNotEmpty);
        }
    }

    private void setUIComponentsEnabled(final boolean enable) {
        openButton.setEnabled(enable);
        removeButton.setEnabled(enable);
        updateButton.setEnabled(enable);
        repositoryList.setEnabled(enable);
    }

    private void toggleUpdateButton(final String command) {
        if (command.equals(stopCommand)) {
            updateButton.setIcon(stopIcon);
            updateButton.setRolloverIcon(stopRolloverIcon);
            updateButton.setActionCommand(stopCommand);
        } else {
            updateButton.setIcon(updateIcon);
            updateButton.setRolloverIcon(updateRolloverIcon);
            updateButton.setActionCommand(updateCommand);
        }
    }

    private File promptForRepositoryBaseDir() {
        final JFileChooser fileChooser = createDirectoryChooser();
        fileChooser.setCurrentDirectory(currentDirectory);
        final int response = fileChooser.showOpenDialog(mainPanel);
        currentDirectory = fileChooser.getCurrentDirectory();
        final File selectedDir = fileChooser.getSelectedFile();
        if (response == JFileChooser.APPROVE_OPTION) {
            return selectedDir;
        }
        return null;
    }

    private static JFileChooser createDirectoryChooser() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(final File f) {
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Directories"; /* I18N */
            }
        });
        fileChooser.setDialogTitle("Select Directory"); /* I18N */
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setApproveButtonText("Select"); /* I18N */
        fileChooser.setApproveButtonMnemonic('S');
        return fileChooser;
    }

    private void initUI() {
        mainPanel = new JPanel(new BorderLayout(4, 4));
        final JPanel southPanel = new JPanel(new BorderLayout(4, 4));
        final JPanel northPanel = new JPanel(new BorderLayout(4, 4));
        repositoryList = new JComboBox();
        setComponentName(repositoryList, "repositoryList");
        repositoryTable = new JTable();
        statusLabel = new JLabel("");
        progressPanel = new JPanel();
        openButton = new JButton();
        setComponentName(openButton, "openButton");
        removeButton = new JButton();
        setComponentName(removeButton, "removeButton");
        updateButton = new JButton();
        setComponentName(updateButton, "updateButton");
        progressBar = new JProgressBar();
        setComponentName(progressBar, "progressBar");
        headerPanel = new JPanel();

        northPanel.add(headerPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.CENTER);
        southPanel.add(progressPanel, BorderLayout.EAST);

        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(repositoryTable), BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressPanel.setLayout(new BorderLayout());
        progressPanel.add(progressBar);
        progressPanel.setVisible(false);

        repositoryList.addItemListener(new RepositoryChangeHandler());
        repositoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        repositoryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        repositoryTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                final int clickCount = e.getClickCount();
                if (clickCount == 2) {
                    performOpenAction();
                }
            }
        });
        initHeaderPanel(headerPanel);
    }

    private void setComponentName(JComponent openButton, String name) {
        openButton.setName(getClass().getName() + name);
    }

    private void initHeaderPanel(final JPanel headerBar) {
        headerBar.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        openButton = createToolButton(UIUtils.loadImageIcon("icons/Open24.gif"));
        openButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                performOpenAction();
            }
        });
        headerBar.add(openButton, gbc);

        updateButton = createToolButton(updateIcon);
        updateButton.setActionCommand(updateCommand);
        updateButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals("stop")) {
                    updateButton.setEnabled(false);
                    mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    repositoryManager.stopUpdateRepository();
                } else {
                    repositoryManager.startUpdateRepository((Repository) repositoryList.getSelectedItem(),
                                                            new ProgressBarProgressMonitor(progressBar, statusLabel),
                                                            uiCallBack);
                }
            }
        });
        headerBar.add(updateButton, gbc);

        headerBar.add(new JLabel("Repository:")); /* I18N */
        gbc.weightx = 99;
        headerBar.add(repositoryList, gbc);
        gbc.weightx = 0;

        JButton _addButton = createToolButton(UIUtils.loadImageIcon("icons/Plus24.gif"));
        _addButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                addRepository();
            }
        });
        headerBar.add(_addButton, gbc);

        removeButton = createToolButton(UIUtils.loadImageIcon("icons/Minus24.gif"));
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                removeRepository((Repository) repositoryList.getSelectedItem());
            }
        });
        headerBar.add(removeButton, gbc);

        JButton helpButton = createToolButton(UIUtils.loadImageIcon("icons/Help22.png"));
        setComponentName(helpButton, "helpButton");
        HelpSys.enableHelpOnButton(helpButton, helpId);
        headerBar.add(helpButton, gbc);
    }

    private JButton createToolButton(final ImageIcon icon) {
        final JButton button = (JButton) ToolButtonFactory.createButton(icon,
                                                                        false);
        button.setBackground(headerPanel.getBackground());
        return button;
    }

    private static void addDefaultDataProvider(final RepositoryManager repositoryManager) {
        repositoryManager.addDataProvider(new ProductPropertiesProvider());
        repositoryManager.addDataProvider(new QuicklookProvider(quickLookWidth));
        repositoryManager.addDataProvider(new WorldMapProvider(false));
    }

    private class RepositoryChangeHandler implements ItemListener {

        public void itemStateChanged(final ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                final Repository repository = (Repository) e.getItem();
                pgConfig.setLastSelectedRepository(repository);
                final RepositoryTableModel tableModel = new RepositoryTableModel(repository);
                sortedModel = createSortedModel(tableModel);
                repositoryTable.setModel(sortedModel);
                repositoryTable.setColumnModel(tableModel.getColumnModel());
                repositoryManager.startUpdateRepository(repository,
                                                        new ProgressBarProgressMonitor(progressBar, statusLabel),
                                                        uiCallBack);
            }
        }

        private SortingDecorator createSortedModel(final RepositoryTableModel tableModel) {
            return new SortingDecorator(tableModel, repositoryTable.getTableHeader());
        }
    }

    /**
     * A {@link com.bc.ceres.core.ProgressMonitor} which uses a
     * Swing's {@link javax.swing.ProgressMonitor} to display progress.
     */
    private class ProgressBarProgressMonitor implements ProgressMonitor {

        private JProgressBar progressBar;
        private JLabel messageLabel;

        private double currentWork;
        private double totalWork;

        private int totalWorkUI;
        private int currentWorkUI;
        private int lastWorkUI;
        private boolean cancelRequested;

        public ProgressBarProgressMonitor(JProgressBar progressBar, JLabel messageLabel) {
            this.progressBar = progressBar;
            this.messageLabel = messageLabel;
        }

        /**
         * Notifies that the main task is beginning.  This must only be called once
         * on a given progress monitor instance.
         *
         * @param name      the name (or description) of the main task
         * @param totalWork the total number of work units into which
         *                  the main task is been subdivided. If the value is <code>UNKNOWN</code>
         *                  the implementation is free to indicate progress in a way which
         *                  doesn't require the total number of work units in advance.
         */
        public void beginTask(String name, int totalWork) {
            Assert.notNull(name, "name");
            currentWork = 0.0;
            this.totalWork = totalWork;
            currentWorkUI = 0;
            lastWorkUI = 0;
            totalWorkUI = totalWork;
            if (messageLabel != null) {
                messageLabel.setText(name);
            }
            cancelRequested = false;
            setDescription(name);
            setVisibility(true);
            progressBar.setMaximum(totalWork);
            toggleUpdateButton(stopCommand);

        }

        /**
         * Notifies that the work is done; that is, either the main task is completed
         * or the user canceled it. This method may be called more than once
         * (implementations should be prepared to handle this case).
         */
        public void done() {
            runInUI(new Runnable() {
                public void run() {
                    if (progressBar != null) {
                        progressBar.setValue(progressBar.getMaximum());
                        setVisibility(false);
                        toggleUpdateButton(updateCommand);
                        updateButton.setEnabled(true);
                        mainPanel.setCursor(Cursor.getDefaultCursor());
                    }
                }
            });
        }


        /**
         * Internal method to handle scaling correctly. This method
         * must not be called by a client. Clients should
         * always use the method </code>worked(int)</code>.
         *
         * @param work the amount of work done
         */
        public void internalWorked(double work) {
            currentWork += work;
            currentWorkUI = (int) (totalWorkUI * currentWork / totalWork);
            if (currentWorkUI > lastWorkUI) {
                runInUI(new Runnable() {
                    public void run() {
                        if (progressBar != null) {
                            int progress = progressBar.getMinimum() + currentWorkUI;
                            progressBar.setValue(progress);
                            setVisibility(true);
                            toggleUpdateButton(stopCommand);
                        }
                        lastWorkUI = currentWorkUI;
                    }
                });
            }
        }

        /**
         * Returns whether cancelation of current operation has been requested.
         * Long-running operations should poll to see if cancelation
         * has been requested.
         *
         * @return <code>true</code> if cancellation has been requested,
         *         and <code>false</code> otherwise
         *
         * @see #setCanceled(boolean)
         */
        public boolean isCanceled() {
            return cancelRequested;
        }

        /**
         * Sets the cancel state to the given value.
         *
         * @param canceled <code>true</code> indicates that cancelation has
         *                 been requested (but not necessarily acknowledged);
         *                 <code>false</code> clears this flag
         *
         * @see #isCanceled()
         */
        public void setCanceled(boolean canceled) {
            cancelRequested = canceled;
            if (canceled) {
                done();
            }
        }

        /**
         * Sets the task name to the given value. This method is used to
         * restore the task label after a nested operation was executed.
         * Normally there is no need for clients to call this method.
         *
         * @param name the name (or description) of the main task
         *
         * @see #beginTask(String, int)
         */
        public void setTaskName(final String name) {
            runInUI(new Runnable() {
                public void run() {
                    if (messageLabel != null) {
                        messageLabel.setText(name);
                    }
                }
            });
        }

        /**
         * Notifies that a subtask of the main task is beginning.
         * Subtasks are optional; the main task might not have subtasks.
         *
         * @param name the name (or description) of the subtask
         */
        public void setSubTaskName(final String name) {
            setVisibility(true);
            messageLabel.setText(name);
            toggleUpdateButton(stopCommand);
        }

        /**
         * Notifies that a given number of work unit of the main task
         * has been completed. Note that this amount represents an
         * installment, as opposed to a cumulative amount of work done
         * to date.
         *
         * @param work the number of work units just completed
         */
        public void worked(int work) {
            internalWorked(work);
        }

        ////////////////////////////////////////////////////////////////////////
        // Stuff to be performed in Swing's event-dispatching thread

        private void runInUI(Runnable task) {
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeLater(task);
            }
        }

        private void setDescription(final String description) {
            statusLabel.setText(description);
        }

        private void setVisibility(final boolean visible) {
            progressPanel.setVisible(visible);
            statusLabel.setVisible(visible);
        }
    }

    private class MyRepositoryManagerListener implements RepositoryManagerListener {

        /**
         * Implementation should handle that a new
         * <code>Repository<code> was added.
         *
         * @param repository the <code>Repository<code> that was added.
         */
        public void repositoryAdded(final Repository repository) {
            repositoryList.insertItemAt(repository, repositoryList.getItemCount());
            pgConfig.setRepositories(repositoryManager.getRepositories());
        }

        /**
         * Implementation should handle that a new
         * <code>Repository<code> was removed.
         *
         * @param repository the <code>Repository<code> that was removed.
         */
        public void repositoryRemoved(final Repository repository) {
            repositoryList.removeItem(repository);
            if (repositoryList.getItemCount() == 0) {
                repositoryTable.setModel(new DefaultTableModel());
                repositoryTable.setColumnModel(new DefaultTableColumnModel());
            }
            pgConfig.setRepositories(repositoryManager.getRepositories());
        }
    }


    /**
     * Should be implemented for handling opening {@link org.esa.beam.framework.datamodel.Product} files.
     */
    public static interface ProductOpenHandler {

        /**
         * Implemetation should open the given files as {@link org.esa.beam.framework.datamodel.Product}s.
         *
         * @param productFiles the files to open.
         */
        public void openProducts(File[] productFiles);
    }

    private class UiCallBack implements Callback {

        public void callback() {
            repositoryTable.updateUI();
        }
    }

    private class RepositoryCollector extends SwingWorker<Repository[], Object> {

        private File baseDir;
        private boolean doRecursive;
        private ProgressMonitor pm;


        public RepositoryCollector(final File baseDir, final boolean doRecursive, final ProgressMonitor pm) {
            this.pm = pm;
            this.baseDir = baseDir;
            this.doRecursive = doRecursive;
        }

        @Override
        protected Repository[] doInBackground() throws Exception {
            final ArrayList<File> dirList = new ArrayList<File>();
            dirList.add(baseDir);
            if (doRecursive) {
                final File[] subDirs = collectAllSubDirs(baseDir);
                for (File subDir : subDirs) {
                    dirList.add(subDir);
                }
            }

            pm.beginTask("Collecting repositories...", dirList.size());
            final ArrayList<Repository> repositoryList = new ArrayList<Repository>();
            try {
                RepositoryScanner.ProductFileFilter filter = new RepositoryScanner.ProductFileFilter();
                for (File subDir : dirList) {
                    final File[] subDirFiles = subDir.listFiles(filter);
                    if (subDirFiles.length > 0) {
                        final Repository repository = new Repository(subDir);
                        repositoryList.add(repository);
                    }

                    if (pm.isCanceled()) {
                        return repositoryList.toArray(new Repository[repositoryList.size()]);
                    }
                    pm.worked(1);
                }
                return repositoryList.toArray(new Repository[repositoryList.size()]);
            } finally {
                pm.done();
            }
        }

        @Override
        public void done() {
            Repository[] repositories;
            try {
                repositories = get();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(mainPanel, "An internal Error occured:\n" + e.getMessage());
                return;
            }
            if (repositories.length == 0) {
                JOptionPane.showMessageDialog(mainPanel,
                                              "No readable products found in the specified directory: \n"
                                              + "'" + baseDir.getPath() + "'.\n"
                                              + "It is not added to the repository list."); /* I18N */
                return;
            }

            for (Repository repository : repositories) {
                repositoryManager.addRepository(repository);
            }
            if (repositories[0] != null) {
                // triggers also an update of the repository
                repositoryList.setSelectedItem(repositories[0]);
            }
            setUIComponentsEnabled(repositoryManager.getNumRepositories() > 0);
        }

        private File[] collectAllSubDirs(final File dir) {
            final ArrayList<File> dirList = new ArrayList<File>();
            final RepositoryScanner.DirectoryFileFilter dirFilter = new RepositoryScanner.DirectoryFileFilter();

            final File[] subDirs = dir.listFiles(dirFilter);
            for (final File subDir : subDirs) {
                dirList.add(subDir);
                final File[] dirs = collectAllSubDirs(subDir);
                for (final File file : dirs) {
                    dirList.add(file);
                }
            }
            return dirList.toArray(new File[dirList.size()]);
        }

    }
}
