package io.nots.intellij.daemon;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.impl.LineMarkersPass;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.hash.HashMap;
import io.nots.intellij.ProjectFilesUtils;
import io.nots.intellij.editor.NotsIcon;
import io.nots.intellij.editor.NotsIconLoader;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class NotsFileProcessor implements Runnable {

    private PsiFile psiFile;

    private String projectSHA;

    private String userApiKey;

    private String projectKey;

    private String fileName;

    private LineMarkerManager lineMarkerManager;

    public NotsFileProcessor(LineMarkerManager lineMarkerManager, PsiFile psiFile, String projectSHA, String userApiKey, String projectKey, String fileName) {
        this.lineMarkerManager = lineMarkerManager;
        this.psiFile = psiFile;
        this.projectSHA = projectSHA;
        this.userApiKey = userApiKey;
        this.projectKey = projectKey;
        this.fileName = fileName;
    }

    @Override
    public void run() {

        String url = String.format("https://nots.io/api/editor?api_key=%s&project_key=%s&file_name=%s&commit_sha=%s", enc(userApiKey), enc(projectKey), enc(fileName), enc(projectSHA));
        List<NotsIcon> notsIcons = NotsIconLoader.getInstance().load(url);
        int newHash = notsIcons.hashCode();
        boolean hasUpdates = !Integer.valueOf(newHash).equals(lineMarkerManager.getNotsHashForFile(psiFile));

        if (hasUpdates) {
            lineMarkerManager.setHashForFile(newHash, psiFile);
            WriteCommandAction.runWriteCommandAction(psiFile.getProject(), () -> removeAllNotsLineMarkers(psiFile));
            ApplicationManager.getApplication().runReadAction(() ->  {
                lineMarkerManager.setLineMarkerInfo(prepareLineMarkers(notsIcons, psiFile), psiFile.getProject(), psiFile);
                LineMarkersPass.queryLineMarkers(psiFile, PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile));
            });

        }

    }

    public void removeAllNotsLineMarkers(PsiFile psiFile) {

        Project project = psiFile.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        MarkupModelEx markupModel = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, true);

        Map<PsiElement, LineMarkerInfo> marker = lineMarkerManager.getAllFileMarkers(psiFile);
        for (LineMarkerInfo lineMarkerInfo: marker.values()) {
            try {
                markupModel.removeHighlighter(lineMarkerInfo.highlighter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected static String enc(String line) {
        try {
            return URLEncoder.encode(line, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    protected static Map<PsiElement, LineMarkerInfo> prepareLineMarkers(@NotNull List<NotsIcon> icons, PsiFile psiFile) {
        Map<PsiElement, LineMarkerInfo> markerMap = new HashMap<>();

        for (NotsIcon notsIcon : icons) {

            int startOffset = ProjectFilesUtils.getStartOffset(psiFile, notsIcon.lineNumber);
            PsiElement psiElement = ProjectFilesUtils.getElementAtLine(psiFile, startOffset);

            try {
                IconLoader.CachedImageIcon cachedImageIcon = IconLoader.CachedImageIcon.class.cast(IconLoader.findIcon(new URL(notsIcon.iconURL), true));
                float scale = 12f / Math.max(cachedImageIcon.getIconHeight(), cachedImageIcon.getIconWidth()); //scale to Editor gutter	size 12x12
                markerMap.put(psiElement,
                        new LineMarkerInfo(
                                psiElement,
                                new TextRange(startOffset, startOffset),
                                cachedImageIcon.scale(scale),
                                Pass.LINE_MARKERS,
                                (o) -> notsIcon.title,
                                (ev, element) -> BrowserUtil.browse(notsIcon.url),
                                GutterIconRenderer.Alignment.LEFT
                        ));

            } catch (Exception e) {
                Notifications.Bus.notify(new Notification("Nots.io", "Failed to create like marker for nots icon " + notsIcon.Id, e.getMessage(), NotificationType.ERROR));
            }
        }
        return markerMap;
    }
}
