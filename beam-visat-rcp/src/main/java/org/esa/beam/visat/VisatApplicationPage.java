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

package org.esa.beam.visat;

import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.SelectionManager;
import com.bc.swing.desktop.TabbedDesktopPane;
import org.esa.beam.framework.ui.BasicView;
import org.esa.beam.framework.ui.application.DocView;
import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.PageComponentPane;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.framework.ui.application.support.AbstractApplicationPage;
import org.esa.beam.framework.ui.application.support.DefaultToolViewPane;
import org.esa.beam.framework.ui.application.support.ToolViewView;
import org.esa.beam.framework.ui.application.support.ToolViewViewport;
import org.esa.beam.framework.ui.command.CommandManager;
import org.flexdock.docking.DockingConstants;
import org.flexdock.docking.event.DockingEvent;
import org.flexdock.docking.event.DockingListener;
import org.flexdock.view.View;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;
import java.beans.PropertyVetoException;

//import com.jidesoft.docking.DockableFrame;
//import com.jidesoft.docking.DockingManager;

public class VisatApplicationPage extends AbstractApplicationPage {

    private final Window window;
    private final CommandManager commandManager;
    private final SelectionManager selectionManager;
    private final TabbedDesktopPane documentPane;
    private ToolViewViewport viewport;
    private final View mainView;

    public VisatApplicationPage(Window window,
                                CommandManager commandManager,
                                SelectionManager selectionManager,
                                TabbedDesktopPane documentPane) {
        this.window = window;
        this.commandManager = commandManager;
        this.selectionManager = selectionManager;
        viewport = new ToolViewViewport();
        window.add(viewport, BorderLayout.CENTER);
        mainView = createMainView(documentPane);
//        mainView = new View("main", null, null);
//        mainView.setTerritoryBlocked(DockingConstants.CENTER_REGION, true);
//        mainView.setTitlebar(null);
//        mainView1.setContentPane(documentPane);
        viewport.dock(mainView);
        this.documentPane = documentPane;
    }

    private View createMainView(TabbedDesktopPane documentPane) {
        String id = "startPage";
        View view = new ToolViewView(id);
        view.setTerritoryBlocked(DockingConstants.CENTER_REGION, true);
        view.setTitlebar(null);
        view.setContentPane(documentPane);
        return view;
    }

    @Override
    public Window getWindow() {
        return window;
    }

    @Override
    public CommandManager getCommandManager() {
        return commandManager;
    }

    @Override
    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    @Override
    public ToolViewDescriptor getToolViewDescriptor(String id) {
        return VisatActivator.getInstance().getToolViewDescriptor(id);
    }

    @Override
    protected void doAddToolView(final ToolView toolView) {
//        DockableFrame dockableFrame = (DockableFrame) toolView.getContext().getPane().getControl();
//        dockableFrame.addDockableFrameListener(new DockableFrameAdapter() {
//            @Override
//            public void dockableFrameActivated(DockableFrameEvent dockableFrameEvent) {
//                setActiveComponent();
//            }
//
//            @Override
//            public void dockableFrameDeactivated(DockableFrameEvent dockableFrameEvent) {
//                setActiveComponent();
//            }
//        });
//        dockingManager.addFrame(dockableFrame);
        View view = (View) toolView.getContext().getPane().getControl();
        view.addDockingListener(new DockingListener() {
//            @Override
//            public void dockableFrameActivated(DockableFrameEvent dockableFrameEvent) {
//                setActiveComponent();
//            }
//
//            @Override
//            public void dockableFrameDeactivated(DockableFrameEvent dockableFrameEvent) {
//                setActiveComponent();
//            }

            @Override
            public void dockingComplete(DockingEvent dockingEvent) {
                setActiveComponent();
            }

            @Override
            public void dockingCanceled(DockingEvent dockingEvent) {
                setActiveComponent();
            }

            @Override
            public void dragStarted(DockingEvent dockingEvent) {
                setActiveComponent();
            }

            @Override
            public void dropStarted(DockingEvent dockingEvent) {
                setActiveComponent();
            }

            @Override
            public void undockingComplete(DockingEvent dockingEvent) {
                setActiveComponent();
            }

            @Override
            public void undockingStarted(DockingEvent dockingEvent) {
                setActiveComponent();
            }
        });
//        viewport.dock(view);
//        dockingManager.addFrame(view);

    }

    @Override
    protected void doRemoveToolView(ToolView toolView) {
        viewport.remove(toolView.getControl());
//        dockingManager.removeFrame(toolView.getId());
    }

    @Override
    protected void doShowToolView(ToolView toolView) {
//        dockingManager.showFrame(toolView.getId());
//        if (shouldFloat(toolView)) {
//            dockingManager.floatFrame(toolView.getId(), null, false);
//        }
    }

    @Override
    protected void doHideToolView(ToolView toolView) {
//        dockingManager.hideFrame(toolView.getId());
    }

    @Override
    protected boolean giveFocusTo(PageComponent pageComponent) {
        if (pageComponent instanceof ToolView) {
//            dockingManager.activateFrame(pageComponent.getId());
        } else if (pageComponent instanceof DocView) {
            JInternalFrame frame = (JInternalFrame) pageComponent.getContext().getPane().getControl();
            try {
                frame.setSelected(true);
            } catch (PropertyVetoException e) {
                // ignore
            }
        } else {
            throw new IllegalArgumentException(pageComponent.getClass() + " not handled");
        }
        return getActiveComponent() == pageComponent;
    }

    @Override
    protected PageComponentPane createToolViewPane(ToolView toolView) {
//        return new DefaultToolViewPane(toolView);
        return new DefaultToolViewPane(toolView, viewport, mainView);
    }

    @Override
    protected JComponent createControl() {
        return viewport.getRootPane();
//        return dockingManager.getDockedFrameContainer();
    }

    @Override
    protected void setActiveComponent() {
//        String activeFrameKey = dockingManager.getActiveFrameKey();
//        Debug.trace("setActiveComponent: " + activeFrameKey);

        ToolView toolView = null;
//        if (activeFrameKey != null) {
//            DockableFrame activeFrame = dockingManager.getFrame(activeFrameKey);
//            if (activeFrame != null) {
//                toolView = getToolView(activeFrame);
//            }
//        }
        if (toolView != null) {
            setActiveComponent(toolView);
        } else {
            SelectionContext context = null;
            // No tool view currently selected, must look for active "DocView".
            JInternalFrame selectedFrame = documentPane.getSelectedFrame();
            if (selectedFrame != null) {
                Container pageComponent = selectedFrame.getContentPane();
                if (pageComponent instanceof BasicView) {
                    BasicView view = (BasicView) pageComponent;
                    context = view.getSelectionContext();
                }
            }
            getSelectionManager().setSelectionContext(context);
        }
    }

    private ToolView getToolView(View view) {
        ToolView[] toolViews = getToolViews();
        for (ToolView toolView : toolViews) {
            if (view == toolView.getContext().getPane().getControl()) {
                return toolView;
            }
        }
        return null;
    }

    private boolean shouldFloat(ToolView toolView) {
//        ToolViewDescriptor toolViewDescriptor = getToolViewDescriptor(toolView.getId());
//        State initState = toolViewDescriptor.getInitState();
//        DockableFrame frame = dockingManager.getFrame(toolView.getId());
//        return frame != null
//                && frame.getContext().getDockPreviousState() == null
//                && frame.getContext().getFloatPreviousState() == null
//                && initState == State.HIDDEN;
        return false;
    }


}
