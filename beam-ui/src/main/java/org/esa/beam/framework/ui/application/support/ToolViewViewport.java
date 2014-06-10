package org.esa.beam.framework.ui.application.support;

import org.flexdock.docking.DockingManager;
import org.flexdock.view.Viewport;

/**
 * Created by tonio on 10.06.2014.
 */
public class ToolViewViewport extends Viewport {

    static {
        DockingManager.setDockingStrategy(Viewport.class, new UsefulDockingStrategy());
    }

}
