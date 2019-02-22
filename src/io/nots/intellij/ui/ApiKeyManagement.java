package io.nots.intellij.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

public class ApiKeyManagement extends DialogWrapper {
    public static final String IO_NOTS_USER_API_KEY = "io.nots.user.api.key";
    public static final String IO_NOTS_PROJECT_KEY = "io.nots.project.key";
    private JTextField userApiKey;
    private JTextField projectKey;
    private JPanel panel;
    private PropertiesComponent propertiesComponent;

    public ApiKeyManagement(@Nullable Project project) {
        super(project);
        propertiesComponent = PropertiesComponent.getInstance(project);
        userApiKey.setText(Optional.ofNullable(propertiesComponent.getValue(IO_NOTS_USER_API_KEY)).orElse(""));
        projectKey.setText(Optional.ofNullable(propertiesComponent.getValue(IO_NOTS_PROJECT_KEY)).orElse(""));
        init();
    }

    @Override
    protected void doOKAction() {
        propertiesComponent.setValue(IO_NOTS_USER_API_KEY, userApiKey.getText());
        propertiesComponent.setValue(IO_NOTS_PROJECT_KEY, projectKey.getText());
        super.doOKAction();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[]{new DeleteAction()};
    }

    protected class DeleteAction extends DialogWrapper.DialogWrapperAction {
        private DeleteAction() {
            super("Delete");
        }

        protected void doAction(ActionEvent e) {
            propertiesComponent.unsetValue(IO_NOTS_USER_API_KEY);
            propertiesComponent.unsetValue(IO_NOTS_PROJECT_KEY);
            close(OK_EXIT_CODE);
        }
    }
}
