package org.esa.beam.framework.ui.application.support;

//import com.jidesoft.docking.DockContext;
//import com.jidesoft.docking.DockableFrame;
//import com.jidesoft.docking.event.DockableFrameAdapter;
//import com.jidesoft.docking.event.DockableFrameEvent;

import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.util.Debug;
import org.flexdock.docking.DockingConstants;
import org.flexdock.view.View;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;

/**
 * Created by tonio on 04.06.2014.
 */
public class DefaultToolViewPane extends AbstractPageComponentPane {

    //    private DockableFrame dockableFrame;
    private boolean pageComponentControlCreated;
    private View view;

    public DefaultToolViewPane(PageComponent pageComponent) {
        super(pageComponent);
    }

    @Override
    protected JComponent createControl() {
        view = new View("Pane");
//        dockableFrame = new DockableFrame();
//        dockableFrame.setKey(getPageComponent().getId());
        configureControl(true);
//        view.addPropertyChangeListener();
//        dockableFrame.addDockableFrameListener(new DockableFrameHandler());
        nameComponent(view, "Pane");
        view.addAction(DockingConstants.CLOSE_ACTION);
        view.addAction(DockingConstants.PIN_ACTION);

//        nameComponent(dockableFrame, "Pane");
        return view;
    }

    @Override
    protected void pageComponentChanged(PropertyChangeEvent evt) {
        configureControl(false);
    }

    private void configureControl(boolean init) {
        ToolViewDescriptor toolViewDescriptor = (ToolViewDescriptor) getPageComponent().getDescriptor();

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
//            if (toolViewDescriptor.getInitIndex() >= 0) {
//                dockableFrame.getContext().setInitIndex(toolViewDescriptor.getInitIndex());
//            }
            if (toolViewDescriptor.getInitSide() != null) {
//                dockableFrame.getContext().setInitSide(toJideSide(toolViewDescriptor.getInitSide()));
            }
//            if (toolViewDescriptor.getInitState() != null) {
//                dockableFrame.getContext().setInitMode(toJideMode(toolViewDescriptor.getInitState()));
//            }
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

//    private int toJideMode(ToolViewDescriptor.State state) {
//        if (state == ToolViewDescriptor.State.DOCKED) {
//            return DockContext.STATE_FRAMEDOCKED;
//        } else if (state == ToolViewDescriptor.State.FLOATING) {
//            return DockContext.STATE_FLOATING;
//        } else if (state == ToolViewDescriptor.State.ICONIFIED) {
//            return DockContext.STATE_AUTOHIDE;
//        } else if (state == ToolViewDescriptor.State.ICONIFIED_SHOWING) {
//            return DockContext.STATE_AUTOHIDE_SHOWING;
//        } else if (state == ToolViewDescriptor.State.HIDDEN) {
//            return DockContext.STATE_HIDDEN;
//        }
//        throw new IllegalStateException("unhandled " + ToolViewDescriptor.State.class);
//    }

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
            view.getContentPane().add(pageComponentControl, BorderLayout.CENTER);
//            dockableFrame.getContentPane().add(pageComponentControl, BorderLayout.CENTER);
            pageComponentControlCreated = true;
            getPageComponent().componentOpened();
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

}
