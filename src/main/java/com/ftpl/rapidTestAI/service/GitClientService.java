package com.ftpl.rapidTestAI.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class GitClientService {

    public void cloneRepository(final String localRepositoryPath,
                                final String remoteUrl)
            throws IOException, InterruptedException {
        final File localDir = new File(localRepositoryPath);
        if (localDir.exists() && localDir.isDirectory()) {
            log.info("Local repository already exists at: {}", localRepositoryPath);
            return;
        }
        // Create parent directories if they don't exist
        if (!localDir.getParentFile().exists()) {
            localDir.getParentFile().mkdirs();
        }
        final ProcessBuilder processBuilder = new ProcessBuilder("git", "clone", remoteUrl, localRepositoryPath);
        final Process process = processBuilder.start();
        logProcessOutput(process);
        // Also log any error output to help diagnose issues
        logProcessError(process);
        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Git clone failed with exit code: " + exitCode + ". Check if the remote URL is correct and you have proper permissions.");
        }
        log.info("Repository cloned successfully to: {}", localRepositoryPath);
    }


    public List<String> getChangedFilesLocal(final String localRepositoryPath,
                                             final String baseRef,
                                             final String headRef)
            throws IOException, InterruptedException {
        final List<String> changedFiles = new ArrayList<>();
        final ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "--name-only", baseRef, headRef);
        processBuilder.directory(new File(localRepositoryPath));
        final Process process = processBuilder.start();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                changedFiles.add(line.trim());
            }
        }

        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            logProcessError(process);
            throw new IOException("Git diff failed with exit code: " + exitCode);
        }

        return changedFiles;
    }

    private void logProcessOutput(final Process process) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }
        }
    }

    private void logProcessError(final Process process) throws IOException {
        try (final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                log.error("Git Error: {}", errorLine);
            }
        }
    }

    public List<String> getChangedFilesLocal(final String localRepositoryPath) throws IOException, InterruptedException {
        final List<String> changedFiles = new ArrayList<>();
        final ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "--name-only");
        processBuilder.directory(new File(localRepositoryPath));
        final Process process = processBuilder.start();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                changedFiles.add(line.trim());
            }
        }

        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            try (final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    log.error("Error while executing getChangedFilesLocal function, Git Error: {}", errorLine);
                }
            }
            throw new IOException("Git command failed with exit code: " + exitCode);
        }

        return changedFiles;
    }

    public List<String> getChangedFilesInRemoteComparison(final String localRepositoryPath,
                                                          final String baseBranch,
                                                          final String headBranch)
            throws IOException, InterruptedException {
        // Ensure we have the latest info from the remote
        final ProcessBuilder fetchProcessBuilder = new ProcessBuilder("git", "fetch", "origin");
        fetchProcessBuilder.directory(new File(localRepositoryPath));
        final Process fetchProcess = fetchProcessBuilder.start();
        fetchProcess.waitFor();

        return getChangesFromOrigin(localRepositoryPath, baseBranch, headBranch);
    }

    public List<String> getLocalChangesComparedToRemote(final String localRepositoryPath,
                                                        final String remoteBranch)
            throws IOException, InterruptedException {
        return getChangedFilesLocal(localRepositoryPath, remoteBranch, "HEAD"); // 'HEAD' refers to your current local state
    }

    public List<String> getChangesFromOrigin(final String localRepositoryPath,
                                             final String baseBranch,
                                             final String headBranch)
            throws IOException, InterruptedException {
        final List<String> changedFiles = new ArrayList<>();
        final ProcessBuilder processBuilder = new ProcessBuilder("git", "diff", "--name-only",
                "origin/" + baseBranch, "origin/" + headBranch);
        processBuilder.directory(new File(localRepositoryPath));
        final Process process = processBuilder.start();

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                changedFiles.add(line.trim());
            }
        }

        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            logProcessError(process);
            throw new IOException("Git diff failed with exit code: " + exitCode);
        }

        return changedFiles;
    }

    public String getFileContentAtRevision(final String localRepositoryPath,
                                           final String filePath,
                                           final String revision)
            throws IOException, InterruptedException {
        final ProcessBuilder processBuilder = new ProcessBuilder("git", "show", revision + ":" + filePath);
        processBuilder.directory(new File(localRepositoryPath));
        final Process process = processBuilder.start();

        final StringBuilder content = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            logProcessError(process);
            throw new IOException("Error getting file content for " + filePath + " at revision " + revision + " (exit code: " + exitCode + ")");
        }

        return content.toString();
    }


}
