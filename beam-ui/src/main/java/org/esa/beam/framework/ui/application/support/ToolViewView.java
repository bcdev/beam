package org.esa.beam.framework.ui.application.support;

import org.flexdock.view.View;

/**
 * Created by tonio on 10.06.2014.
 */
public class ToolViewView extends View {

    private String flexdockSide;
    private int relativeIndex;

    public ToolViewView(String persistentId) {
        super(persistentId);
    }

    public String getFlexdockSide() {
        return flexdockSide;
    }

    public void setFlexdockSide(String flexdockSide) {
        this.flexdockSide = flexdockSide;
    }

    public int getRelativeIndex() {
        return relativeIndex;
    }

    public void setRelativeIndex(int relativeIndex) {
        this.relativeIndex = relativeIndex;
    }
}
