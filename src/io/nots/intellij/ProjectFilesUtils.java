package io.nots.intellij;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import git4idea.branch.GitBranchUtil;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProjectFilesUtils {

    public static List<PsiFile> allOpenedFilesUnderProjectRoot(Project project) {

        List<PsiFile> files = new ArrayList<>();
        String projectBasePath = project.getBasePath();
        if (projectBasePath == null) {
            return Collections.emptyList();
        }
        FileEditorManager manager = FileEditorManager.getInstance(project);
        PsiManager psiManager = PsiManager.getInstance(project);
        VirtualFile[] openedFiles = manager.getOpenFiles();
        for (VirtualFile virtualFile : openedFiles) {
            PsiFile psiFile = psiManager.findFile(virtualFile);
            if (psiFile != null && virtualFile.getCanonicalPath().startsWith(projectBasePath)) {
                files.add(psiFile);
            }
        }
        return files;
    }

    public static String basePathForFile(PsiFile file) {
        String projectBasePath = file.getProject().getBasePath();
        VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile.getCanonicalPath().substring(projectBasePath.length() + 1);
    }

    public static String getProjectRootGitSHA(Project project) {
        try {
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(project.getBasePath());
            GitRepository repo = GitBranchUtil.getCurrentRepository(project);
            if (repo instanceof GitRepository) {
                //GitLocalBranch lb = repo.getCurrentBranch();

                //VcsRevisionNumber revisionNumber = GitHistoryUtils.getCurrentRevision(project, VcsUtil.getFilePath(virtualFile), lb.getName());
                return repo.getCurrentRevision();
            }
        } catch (Exception ignore) {

        }
        return null;
    }


    public static int getStartOffset(PsiFile file, int line) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        return document.getLineStartOffset(line);
    }

    @Nullable
    public static PsiElement getElementAtLine(PsiFile file, int offset) {
        return file.getViewProvider().findElementAt(offset);
    }

}
