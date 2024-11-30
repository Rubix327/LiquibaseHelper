package me.rubix327.liquibasehelper.form;

import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ChangeEvent;

public class LJBCheckBox extends JBCheckBox {

    public LJBCheckBox(@Nullable String text) {
        super(text);
    }

    private boolean lastValue = isSelected();

    public final void addLChangeListener(LChangeListener l) {
        super.addChangeListener(e -> {
            if (isSelected() != lastValue){
                l.stateChanged(new LChangeEvent(e.getSource(), lastValue, isSelected()));
                lastValue = isSelected();
            }
        });
    }

    public interface LChangeListener{
        void stateChanged(LChangeEvent e);
    }

    public static class LChangeEvent extends ChangeEvent {
        public LChangeEvent(Object source, boolean oldValue, boolean newValue) {
            super(source);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        private final boolean oldValue;
        private final boolean newValue;

        public boolean wasSelected() {
            return oldValue;
        }

        public boolean isNowSelected() {
            return newValue;
        }
    }

}
