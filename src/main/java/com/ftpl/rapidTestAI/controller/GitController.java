package com.ftpl.rapidTestAI.controller;

import com.ftpl.rapidTestAI.service.GitClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/git")
public class GitController {

    @Autowired
    private GitClientService gitClientService;

    // API to clone the repository to the given local path
    @PostMapping("/clone-repository")
    public ResponseEntity<String> cloneRepository(@RequestParam final String localPath,
                                                  @RequestParam final String remoteRepoUrl) {
        try {
            gitClientService.cloneRepository(localPath, remoteRepoUrl);
            return ResponseEntity.ok("Repository cloned successfully");
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/compare-branches")
    public ResponseEntity<Map<String, Object>> compareBranches(
                @RequestParam final String localPath,
                @RequestParam final String remoteRepoUrl,
                @RequestParam final String sourceBranch,
                @RequestParam final String targetBranch) {
        Map<String, Object> response = new HashMap<>();
        try {
            gitClientService.cloneRepository(localPath, remoteRepoUrl);
            List<String> changedFiles = gitClientService.getChangedFilesInRemoteComparison(localPath, sourceBranch, targetBranch);

            Map<String, String> fileContents = new HashMap<>();
            for (String file : changedFiles) {
                if (!file.isEmpty()) {
                    String fileContent = gitClientService.getFileContentAtRevision(localPath, file, "origin/" + targetBranch);
                    fileContents.put(file, fileContent);
                }
            }

            response.put("changedFiles", changedFiles);
            response.put("fileContents", fileContents);

            return ResponseEntity.ok(response);

        } catch (IOException | InterruptedException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

