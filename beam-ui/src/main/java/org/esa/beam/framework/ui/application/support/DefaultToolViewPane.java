package org.esa.beam.framework.ui.application.support;

//import com.jidesoft.docking.DockContext;
//import com.jidesoft.docking.DockableFrame;
//import com.jidesoft.docking.event.DockableFrameAdapter;
//import com.jidesoft.docking.event.DockableFrameEvent;

import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.util.Debug;
import org.flexdock.docking.Dockable;
import org.flexdock.docking.DockingConstants;
import org.flexdock.docking.DockingManager;
import org.flexdock.docking.DockingPort;
import org.flexdock.docking.DockingStrategy;
import org.flexdock.docking.event.DockingEvent;
import org.flexdock.docking.event.DockingListener;
import org.flexdock.docking.state.FloatManager;
import org.flexdock.event.EventManager;
import org.flexdock.util.DockingUtility;
import org.flexdock.view.View;
import org.flexdock.view.actions.ViewAction;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;

/**
 * Created by tonio on 04.06.2014.
 */
public class DefaultToolViewPane extends AbstractPageComponentPane {

    private final View mainView;
    //    private DockableFrame dockableFrame;
    private boolean pageComponentControlCreated;
    private final ToolViewView view;
    private final DockingPort port;
    //    private String flexdockSide;

//    public DefaultToolViewPane(PageComponent pageComponent) {
//        super(pageComponent);
//    }

    public DefaultToolViewPane(PageComponent pageComponent, DockingPort viewport, View mainView) {
        super(pageComponent);
        this.mainView = mainView;
        port = viewport;
        view = new ToolViewView(pageComponent.getId());

    }

    @Override
    protected JComponent createControl() {
//        dockableFrame = new DockableFrame();
//        dockableFrame.setKey(getPageComponent().getId());
        configureControl(true);
//        view.addPropertyChangeListener();
//        dockableFrame.addDockableFrameListener(new DockableFrameHandler());
        nameComponent(view, "Pane");
        DockingManager.setFloatingEnabled(true);
        view.addAction(DockingConstants.CLOSE_ACTION);
        view.addAction(DockingConstants.PIN_ACTION);
        view.addAction(new DockAction(view.getPersistentId()));
        view.addDockingListener(new ToolViewDockableListener());
        return view;
    }

    @Override
    protected void pageComponentChanged(PropertyChangeEvent evt) {
        configureControl(false);
    }

    private void configureControl(boolean init) {
        ToolViewDescriptor toolViewDescriptor = (ToolViewDescriptor) getPageComponent().getDescriptor();
//        view.setContentPane(getPageComponent().getControl());
        view.setTitle(toolViewDescriptor.getTitle());
        view.setTabText(toolViewDescriptor.getTabTitle());
        view.setIcon(toolViewDescriptor.getSmallIcon());
        view.setToolTipText(toolViewDescriptor.getDescription());

//        dockableFrame.setTitle(toolViewDescriptor.getTitle());
//        dockableFrame.setTabTitle(toolViewDescriptor.getTabTitle());
//        dockableFrame.setFrameIcon(toolViewDescriptor.getSmallIcon());
//        dockableFrame.setToolTipText(toolViewDescriptor.getDescription());

        if (init) {
            if (toolViewDescriptor.getFloatingBounds() != null) {
                view.setBounds(toolViewDescriptor.getFloatingBounds());
//                dockableFrame.setUndockedBounds(toolViewDescriptor.getFloatingBounds());
            }

            if (toolViewDescriptor.getDockedWidth() > 0) {
                view.setSize(toolViewDescriptor.getDockedWidth(), view.getHeight());
//                dockableFrame.getContext().setDockedWidth(toolViewDescriptor.getDockedWidth());
            }
            if (toolViewDescriptor.getDockedHeight() > 0) {
                view.setSize(view.getWidth(), toolViewDescriptor.getDockedHeight());
//                dockableFrame.getContext().setDockedHeight(toolViewDescriptor.getDockedHeight());
            }
            if (toolViewDescriptor.getInitIndex() >= 0) {
//                dockableFrame.getContext().setInitIndex(toolViewDescriptor.getInitIndex());
                view.setRelativeIndex(toolViewDescriptor.getInitIndex());
            }
            if (toolViewDescriptor.getInitSide() != null) {
                view.setFlexdockSide(toFlexdockSide(toolViewDescriptor.getInitSide()));
//                flexdockSide = toFlexdockSide(toolViewDescriptor.getInitSide());
//                mainView.dock(view, toFlexdockSide(toolViewDescriptor.getInitSide()));
//                dockableFrame.getContext().setInitSide(toJideSide(toolViewDescriptor.getInitSide()));
            }
            if (toolViewDescriptor.getInitState() != null) {
                initState(toolViewDescriptor.getInitState());
//                dockableFrame.getContext().setInitMode(toJideMode(toolViewDescriptor.getInitState()));
            }
        }
    }

//    private ToolViewDescriptor.State toState(int jideState) {
//        if (jideState == DockContext.STATE_FRAMEDOCKED) {
//            return ToolViewDescriptor.State.DOCKED;
//        } else if (jideState == DockContext.STATE_FLOATING) {
//            return ToolViewDescriptor.State.FLOATING;
//        } else if (jideState == DockContext.STATE_AUTOHIDE) {
//            return ToolViewDescriptor.State.ICONIFIED;
//        } else if (jideState == DockContext.STATE_AUTOHIDE_SHOWING) {
//            return ToolViewDescriptor.State.ICONIFIED_SHOWING;
//        } else if (jideState == DockContext.STATE_HIDDEN) {
//            return ToolViewDescriptor.State.HIDDEN;
//        }
//        return ToolViewDescriptor.State.UNKNOWN;
//    }

    private void initState(ToolViewDescriptor.State state) {
        if (state == ToolViewDescriptor.State.DOCKED) {
//            mainView.dock(view, toFlexdockSide(toolViewDescriptor.getInitSide()));
//            DockingManager.dock((Dockable) view, (Dockable) mainView, view.getFlexdockSide());
            dock();
            return;
//            return DockContext.STATE_FRAMEDOCKED;
        } else if (state == ToolViewDescriptor.State.FLOATING) {
            DockingManager.getFloatManager().floatDockable(view, mainView);
            return;
//            return DockContext.STATE_FLOATING;
        } else if (state == ToolViewDescriptor.State.ICONIFIED) {
            DockingManager.setMinimized(view, true);
            return;
//            return DockContext.STATE_AUTOHIDE;
        } else if (state == ToolViewDescriptor.State.ICONIFIED_SHOWING) {
            DockingManager.setMinimized(view, true);
            return;
//            return DockContext.STATE_AUTOHIDE_SHOWING;
        } else if (state == ToolViewDescriptor.State.HIDDEN) {
//            DockingManager.setMinimized(view, true);
            return;
//            return DockContext.STATE_HIDDEN;
        }
        throw new IllegalStateException("unhandled " + ToolViewDescriptor.State.class);
    }

    private void dock() {
//        mainView.getDockingPort().getDockingStrategy()
        DockingManager.dock((Dockable) view, port, view.getFlexdockSide());
    }

//    private ToolViewDescriptor.DockSide toSide(int jideSide) {
//        if (jideSide == DockContext.DOCK_SIDE_ALL) {
//            return ToolViewDescriptor.DockSide.ALL;
//        } else if (jideSide == DockContext.DOCK_SIDE_CENTER) {
//            return ToolViewDescriptor.DockSide.CENTER;
//        } else if (jideSide == DockContext.DOCK_SIDE_WEST) {
//            return ToolViewDescriptor.DockSide.WEST;
//        } else if (jideSide == DockContext.DOCK_SIDE_EAST) {
//            return ToolViewDescriptor.DockSide.EAST;
//        } else if (jideSide == DockContext.DOCK_SIDE_NORTH) {
//            return ToolViewDescriptor.DockSide.NORTH;
//        } else if (jideSide == DockContext.DOCK_SIDE_SOUTH) {
//            return ToolViewDescriptor.DockSide.SOUTH;
//        }
//        return ToolViewDescriptor.DockSide.UNKNOWN;
//    }

//    private int toJideSide(ToolViewDescriptor.DockSide dockSide) {
//        if (dockSide == ToolViewDescriptor.DockSide.ALL) {
//            return DockContext.DOCK_SIDE_ALL;
//        } else if (dockSide == ToolViewDescriptor.DockSide.CENTER) {
//            return DockContext.DOCK_SIDE_CENTER;
//        } else if (dockSide == ToolViewDescriptor.DockSide.WEST) {
//            return DockContext.DOCK_SIDE_WEST;
//        } else if (dockSide == ToolViewDescriptor.DockSide.EAST) {
//            return DockContext.DOCK_SIDE_EAST;
//        } else if (dockSide == ToolViewDescriptor.DockSide.NORTH) {
//            return DockContext.DOCK_SIDE_NORTH;
//        } else if (dockSide == ToolViewDescriptor.DockSide.SOUTH) {
//            return DockContext.DOCK_SIDE_SOUTH;
//        }
//        throw new IllegalStateException("unhandled " + ToolViewDescriptor.DockSide.class);
//    }

    private String toFlexdockSide(ToolViewDescriptor.DockSide dockSide) {
        if (dockSide == ToolViewDescriptor.DockSide.ALL) {
            return DockingConstants.UNKNOWN_REGION;
        } else if (dockSide == ToolViewDescriptor.DockSide.CENTER) {
            return DockingConstants.CENTER_REGION;
        } else if (dockSide == ToolViewDescriptor.DockSide.WEST) {
            return DockingConstants.WEST_REGION;
        } else if (dockSide == ToolViewDescriptor.DockSide.EAST) {
            return DockingConstants.EAST_REGION;
        } else if (dockSide == ToolViewDescriptor.DockSide.NORTH) {
            return DockingConstants.NORTH_REGION;
        } else if (dockSide == ToolViewDescriptor.DockSide.SOUTH) {
            return DockingConstants.SOUTH_REGION;
        }
        throw new IllegalStateException("unhandled " + ToolViewDescriptor.DockSide.class);
    }

    private void ensurePageComponentControlCreated() {
        if (!pageComponentControlCreated) {
            Debug.trace("Creating control for page component " + getPageComponent().getId());
            JComponent pageComponentControl;
            try {
                pageComponentControl = getPageComponent().getControl();
            } catch (Throwable e) {
                e.printStackTrace();
                // todo - delegate to application exception handler service
                String message = "An internal error occurred.\n " +
                        "Not able to create user interface control for \n" +
                        "page component '" + getPageComponent().getDescriptor().getTitle() + "'.";
                JOptionPane.showMessageDialog(getPageComponent().getContext().getPage().getWindow(),
                                              message, "Internal Error",
                                              JOptionPane.ERROR_MESSAGE);
                pageComponentControl = new JLabel(message);
            }
            if (pageComponentControl.getName() == null) {
                nameComponent(pageComponentControl, "Control");
            }
            view.setContentPane(pageComponentControl);
            view.setSize(pageComponentControl.getPreferredSize());
            view.setPreferredSize(pageComponentControl.getPreferredSize());
            pageComponentControl.validate();
//            view.getContentPane().add(pageComponentControl);
//            view.getContentPane().add(pageComponentControl, BorderLayout.CENTER);
//            dockableFrame.getContentPane().add(pageComponentControl, BorderLayout.CENTER);
            pageComponentControlCreated = true;
            getPageComponent().componentOpened();
        }
    }

    private class ToolViewDockableListener implements DockingListener {

        @Override
        public void dockingComplete(DockingEvent evt) {
            ensurePageComponentControlCreated();
        }

        @Override
        public void dockingCanceled(DockingEvent evt) {
            ensurePageComponentControlCreated();
        }

        @Override
        public void dragStarted(DockingEvent evt) {
            ensurePageComponentControlCreated();
        }

        @Override
        public void dropStarted(DockingEvent evt) {
            ensurePageComponentControlCreated();
        }

        @Override
        public void undockingComplete(DockingEvent evt) {
            ensurePageComponentControlCreated();
        }

        @Override
        public void undockingStarted(DockingEvent evt) {
            ensurePageComponentControlCreated();
        }
    }

//    private class DockableFrameHandler extends DockableFrameAdapter {
//
//        public DockableFrameHandler() {
//        }
//
//        @Override
//        public void dockableFrameAdded(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//        }
//
//        @Override
//        public void dockableFrameRemoved(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//        }
//
//        @Override
//        public void dockableFrameShown(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//            ensurePageComponentControlCreated();
//            getPageComponent().componentShown();
//        }
//
//        @Override
//        public void dockableFrameHidden(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//            if (pageComponentControlCreated) {
//                getPageComponent().componentHidden();
//            }
//        }
//
//        @Override
//        public void dockableFrameActivated(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//            ensurePageComponentControlCreated();
//        }
//
//        @Override
//        public void dockableFrameDeactivated(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//        }
//
//        @Override
//        public void dockableFrameDocked(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//        }
//
//        @Override
//        public void dockableFrameFloating(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//            ensurePageComponentControlCreated();
//        }
//
//        @Override
//        public void dockableFrameAutohidden(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//        }
//
//        @Override
//        public void dockableFrameAutohideShowing(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//            ensurePageComponentControlCreated();
//        }
//
//        @Override
//        public void dockableFrameTabShown(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//        }
//
//        @Override
//        public void dockableFrameTabHidden(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//        }
//
//        @Override
//        public void dockableFrameMaximized(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//            ensurePageComponentControlCreated();
//        }
//
//        @Override
//        public void dockableFrameRestored(DockableFrameEvent dockableFrameEvent) {
//            Debug.trace("dockableFrameEvent = " + dockableFrameEvent);
//            ensurePageComponentControlCreated();
//        }
//    }

    class DockAction extends ViewAction {

        DockAction(String viewId) {
            setViewId(viewId);
            View view = View.getInstance(viewId);
            if (view != null) {
                putValue(Action.NAME, view.getTitle());
                view.getTitlebar().createActionButton(this);
            }
        }

        @Override
        public void actionPerformed(View view, ActionEvent actionEvent) {
            final FloatManager floatManager = DockingManager.getFloatManager();
            if (!view.isFloating()) {
//                DockingManager.setFloatingEnabled(true);
//                view.getDockingPort().undock(view);
//                DockingManager.undock((Dockable) view);
//                floatManager.floatDockable(view, getPageComponent().getContext().getPage().getWindow());
//                floatManager.addToGroup(view, view.getPersistentId());
//                DockingManager.dock((Dockable) view, )

                DockingStrategy docker = DockingManager.getDockingStrategy(view);
                DockingPort currentPort = DockingUtility.getParentDockingPort((Dockable) view);
                DockingPort targetPort = null;
                String region = null;

                // issue a DockingEvent to allow any listeners the chance to cancel the operation.
                DockingEvent evt = new DockingEvent(view, currentPort, targetPort, DockingEvent.DROP_STARTED, actionEvent, null);
//                DockingEvent evt = new DockingEvent(view, currentPort, targetPort, DockingEvent.DROP_STARTED, actionEvent, getDragContext());
                evt.setRegion(region);
                evt.setOverWindow(false);
//		EventManager.notifyDockingMonitor(dockable, evt);
                EventManager.dispatch(evt, view);


                // attempt to complete the docking operation
                if (!evt.isConsumed())
                    docker.dock(view, targetPort, region);

            } else {
//                floatManager.removeFromGroup(view);
//                DockingManager.dock((Dockable) view, (Dockable) mainView, DockingConstants.SOUTH_REGION);
                DockingStrategy docker = DockingManager.getDockingStrategy(view);
                DockingPort currentPort = DockingUtility.getParentDockingPort((Dockable) view);
                DockingPort targetPort = mainView.getDockingPort();
                String region = DockingConstants.SOUTH_REGION;

                // issue a DockingEvent to allow any listeners the chance to cancel the operation.
                DockingEvent evt = new DockingEvent(view, currentPort, targetPort, DockingEvent.DROP_STARTED, actionEvent, null);
//                DockingEvent evt = new DockingEvent(view, currentPort, targetPort, DockingEvent.DROP_STARTED, actionEvent, getDragContext());
                evt.setRegion(region);
                evt.setOverWindow(true);
//		EventManager.notifyDockingMonitor(dockable, evt);
                EventManager.dispatch(evt, view);

                // attempt to complete the docking operation
                if (!evt.isConsumed())
                    docker.dock(view, targetPort, region);
            }
//            System.out.println(DockingManager.isDocked((Dockable) view));
//            DockingManager.getFloatManager().removeFromGroup(view);
//            view.getDockingPort().undock(view);
//            mainView.dock(view, DockingConstants.NORTH_REGION);
//            view.updateUI();
//            DockingManager.dock((Dockable)view, (Dockable)mainView);
//            System.out.println(DockingManager.isDocked((Dockable) view));
        }
    }

}
