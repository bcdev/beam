package org.esa.beam.framework.ui.application.support;

import org.flexdock.docking.Dockable;
import org.flexdock.docking.DockingManager;
import org.flexdock.docking.DockingPort;
import org.flexdock.docking.defaults.DefaultDockingStrategy;
import org.flexdock.docking.drag.DragManager;
import org.flexdock.docking.drag.DragOperation;
import org.flexdock.docking.event.DockingEvent;
import org.flexdock.docking.floating.frames.DockingFrame;
import org.flexdock.docking.state.FloatManager;
import org.flexdock.event.EventManager;
import org.flexdock.util.DockingUtility;
import org.flexdock.util.SwingUtility;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;

/**
 * Created by tonio on 10.06.2014.
 */
public class UsefulDockingStrategy extends DefaultDockingStrategy {

    public boolean dock(Dockable dockable, DockingPort port, String region,
                        DragOperation operation) {
//        if (!isDockingPossible(dockable, port, region, operation))
//            return false;
        if (dockable instanceof ToolViewView) {
            final int relativeIndex = ((ToolViewView) dockable).getRelativeIndex();
            if (relativeIndex >= 0) {
                port.getDockable(region);
            }
        }

        if (!dragThresholdElapsed(operation))
            return false;

        // cache the old parent
        DockingPort oldPort = dockable.getDockingPort();

        // perform the drop operation.
//        if(DockingUtility.isFloating(dockable)) {
        DockingResults results = dropComponent(dockable, port, region,
                                               operation);
//        }

        // perform post-drag operations
        DockingPort newPort = results.dropTarget;
        int evtType = results.success ? DockingEvent.DOCKING_COMPLETE
                : DockingEvent.DOCKING_CANCELED;
        Map dragContext = DragManager.getDragContext(dockable);
        DockingEvent evt = new DockingEvent(dockable, oldPort, newPort,
                                            evtType, dragContext);
        // populate DockingEvent status info
        evt.setRegion(region);
        evt.setOverWindow(operation == null ? true : operation.isOverWindow());

        // notify the old docking port, new dockingport,and dockable
        Object[] evtTargets = {oldPort, newPort, dockable};
        EventManager.dispatch(evt, evtTargets);

        return results.success;
    }

    @Override
    protected DockingResults floatComponent(Dockable dockable, DockingPort target, DragOperation token) {
        // otherwise, setup a new DockingFrame and retarget to the CENTER region
        DockingResults results = new DockingResults(target, false);

        // determine the bounds of the new frame
//        Point screenLoc = token.getCurrentMouse(true);
//        SwingUtility.add(screenLoc, token.getMouseOffset());
        Rectangle screenBounds = dockable.getComponent().getBounds();
        final Point dockedLocation = dockable.getComponent().getLocation();
        dockedLocation.setLocation(dockedLocation.getX() + 10, dockedLocation.getY() + 10);
        screenBounds.setLocation(dockedLocation);

        // create the frame
        FloatManager mgr = DockingManager.getFloatManager();
        DockingFrame frame = mgr.floatDockable(dockable, dockable
                .getComponent(), screenBounds);

        // grab a reference to the frame's dockingPort for posterity
        results.dropTarget = frame.getDockingPort();

        results.success = true;
        return results;
    }

    @Override
    protected boolean isFloatable(Dockable dockable, DragOperation token) {
        return true;
    }

    @Override
    protected DockingResults dropComponent(Dockable dockable,
                                           DockingPort target, String region, DragOperation token) {
        if (target == null && region == null && !DockingUtility.isFloating(dockable))
            return floatComponent(dockable, target, token);

        DockingResults results = new DockingResults(target, false);

        if (UNKNOWN_REGION.equals(region) || target == null) {
            return results;
        }

        Component docked = target.getDockedComponent();
        Component dockableCmp = dockable.getComponent();
        if (dockableCmp != null && dockableCmp == docked) {
            // don't allow docking the same component back into the same port
            return results;
        }

        // obtain a reference to the content pane that holds the target
        // DockingPort.
        // MUST happen before undock(), in case the undock() operation removes
        // the
        // target DockingPort from the container tree.

//        Container contentPane = SwingUtility.getContentPane((Component) target);
//        Point contentPaneLocation = token == null ? null : token
//                .getCurrentMouse(contentPane);
//        if(contentPaneLocation == null) {
//            contentPaneLocation = contentPane.getLocation();
//        }

        // undock the current Dockable instance from it's current parent
        // container
        undock(dockable);

        // when the original parent reevaluates its container tree after
        // undocking, it checks to see how
        // many immediate child components it has. split layouts and tabbed
        // interfaces may be managed by
        // intermediate wrapper components. When undock() is called, the docking
        // port
        // may decide that some of its intermedite wrapper components are no
        // longer needed, and it may get
        // rid of them. this isn't a hard rule, but it's possible for any given
        // DockingPort implementation.
        // In this case, the target we had resolved earlier may have been
        // removed from the component tree
        // and may no longer be valid. to be safe, we'll resolve the target
        // docking port again and see if
        // it has changed. if so, we'll adopt the resolved port as our new
        // target.

//        if (contentPaneLocation != null && contentPane != null) {
//            results.dropTarget = DockingUtility.findDockingPort(contentPane,
//                                                                contentPaneLocation);
//            target = results.dropTarget;
//        }

        results.success = target.dock(dockableCmp, region);
        SwingUtility.revalidate((Component) target);
        return results;
    }

    @Override
    protected DockingPort createDockingPortImpl(DockingPort base) {
        return new ToolViewViewport();
    }

}
