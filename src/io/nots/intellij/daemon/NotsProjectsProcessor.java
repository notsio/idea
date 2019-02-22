package io.nots.intellij.daemon;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.MessageType;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.openapi.wm.WindowManager;
import io.nots.intellij.ProjectFilesUtils;
import io.nots.intellij.ui.ApiKeyManagement;


import java.util.ArrayList;
import java.util.List;

public class NotsProjectsProcessor implements Runnable {

    private static boolean userApiKeyWarningShown = false;
    private static boolean projectApiKeyWarningShown = false;

    @Override
    public void run() {

        LineMarkerManager lineMarkerManager = LineMarkerManager.getInstance();
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        List<Project> processedProjects = new ArrayList<>();
        for (Project project : projects) {
            String sha = ProjectFilesUtils.getProjectRootGitSHA(project);
            PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
            String userApiKey = propertiesComponent.getValue(ApiKeyManagement.IO_NOTS_USER_API_KEY);
            String projectKey = propertiesComponent.getValue(ApiKeyManagement.IO_NOTS_PROJECT_KEY);

            JBPopupFactory factory = JBPopupFactory.getInstance();

            if(StringUtil.isEmptyOrSpaces(userApiKey) && !userApiKeyWarningShown) {
                BalloonBuilder builder =
                        factory.createHtmlTextBalloonBuilder("Nots.io plugin is activated, but UserAPI key is not set. <br> Navigate to Tools -> Nots.io Project Config and set proper key.",
                                MessageType.ERROR, null);
                Balloon b = builder.createBalloon();
                b.show(
                        RelativePoint.getNorthEastOf(
                                WindowManager
                                        .getInstance()
                                        .getStatusBar(project)
                                        .getComponent()),
                        Balloon.Position.above);
                userApiKeyWarningShown = true;
            }
            if(StringUtil.isEmptyOrSpaces(projectKey) && !projectApiKeyWarningShown) {
                BalloonBuilder builder =
                        factory.createHtmlTextBalloonBuilder("Nots.io plugin is activated, but ProjectAPI key is not set. <br> Navigate to Tools -> Nots.io Project Config and set proper key.",
                                MessageType.ERROR, null);
                Balloon b = builder.createBalloon();
                b.show(
                        RelativePoint.getNorthEastOf(
                                WindowManager
                                        .getInstance()
                                        .getStatusBar(project)
                                        .getComponent()),
                        Balloon.Position.above);
                projectApiKeyWarningShown = true;
            }

            if (sha != null && !StringUtil.isEmptyOrSpaces(userApiKey) && !StringUtil.isEmptyOrSpaces(projectKey)) {
                List<PsiFile> files = ProjectFilesUtils.allOpenedFilesUnderProjectRoot(project);
                for (PsiFile psiFile: files) {
                    String fileName = ProjectFilesUtils.basePathForFile(psiFile);
                    ApplicationManager.getApplication().executeOnPooledThread(new NotsFileProcessor(lineMarkerManager, psiFile, sha, userApiKey, projectKey,fileName));
                }
                lineMarkerManager.cleanupFiles(files, project);
                processedProjects.add(project);
            }
        }
        lineMarkerManager.cleanupProject(processedProjects);

    }
}

