package io.nots.intellij.daemon;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LineMarkerManager {

    private static final LineMarkerManager INSTANCE = new LineMarkerManager();

    public static LineMarkerManager getInstance() {
        return INSTANCE;
    }

    protected LineMarkerManager() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    boolean stop = false;
                    while (!stop) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                ApplicationManager.getApplication().runReadAction(new NotsProjectsProcessor()));
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            stop = true;
                        }

                    }
                }
        );
    }

    protected Map<Project, Map<PsiFile, Map<PsiElement, LineMarkerInfo>>> projectContainer = new ConcurrentHashMap();
    protected Map<Project, Map<PsiFile, Integer>> projectNotsHash = new ConcurrentHashMap<>();

    public Map<PsiElement, LineMarkerInfo> getAllFileMarkers(PsiFile psiFile) {
        return projectContainer.getOrDefault(psiFile.getProject(), Collections.emptyMap()).getOrDefault(psiFile, Collections.emptyMap());
    }

    public LineMarkerInfo getInfoForElement(@NotNull PsiElement psiElement) {
        return projectContainer.getOrDefault(psiElement.getProject(), Collections.emptyMap()).getOrDefault(psiElement.getContainingFile(), Collections.emptyMap()).get(psiElement);
    }

    public Integer getNotsHashForFile(@NotNull PsiFile psiFile) {
        return projectNotsHash.getOrDefault(psiFile.getProject(), Collections.emptyMap()).get(psiFile);
    }

    public void cleanupProject(List<Project> activeProjectsList) {
        removeAllAbsent(activeProjectsList, projectContainer.entrySet().iterator());
        removeAllAbsent(activeProjectsList, projectNotsHash.entrySet().iterator());
    }

    protected <T, O> void removeAllAbsent(List<T> active, Iterator<Map.Entry<T, O>> iterator) {
        while (iterator.hasNext()) {
            Map.Entry<T, O> element = iterator.next();
            if (!active.contains(element.getKey())) {
                iterator.remove();
            }
        }
    }

    public void cleanupFiles(List<PsiFile> activeFiles, Project project) {
        removeAllAbsent(activeFiles, projectContainer.getOrDefault(project, new ConcurrentHashMap<>()).entrySet().iterator());
        removeAllAbsent(activeFiles, projectNotsHash.getOrDefault(project, new ConcurrentHashMap<>()).entrySet().iterator());
    }

    public void setLineMarkerInfo(@NotNull Map<PsiElement, LineMarkerInfo> psiElementLineMarkerInfoMap, Project project, PsiFile psiFile) {
        projectContainer.computeIfAbsent(project, (key) -> new ConcurrentHashMap<>())
                .computeIfAbsent(psiFile, (key) -> new ConcurrentHashMap<>())
                .clear();
        projectContainer.computeIfAbsent(project, (key) -> new ConcurrentHashMap<>())
                .computeIfAbsent(psiFile, (key) -> new ConcurrentHashMap<>())
                .putAll(psiElementLineMarkerInfoMap);

    }

    public void setHashForFile(Integer hash, PsiFile file) {
        projectNotsHash.computeIfAbsent(file.getProject(), (key) -> new ConcurrentHashMap<>()).put(file, hash);
    }
}
