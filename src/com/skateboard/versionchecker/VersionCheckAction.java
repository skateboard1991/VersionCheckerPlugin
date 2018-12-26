package com.skateboard.versionchecker;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import java.io.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionCheckAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        System.out.println("start versionchecker");
        System.out.println("project path is " + e.getProject().getBasePath());
        versionCheck(e.getProject().getBasePath());
    }

    private HashMap<String, Boolean> groupNameMap = new HashMap<>();
    private StringBuilder versionsContentBuilder = createVersionsContentBuilder();
    private StringBuilder depsContentBuilder = new StringBuilder();

    private static final String VERSIONS = "versions.gradle";


    private void versionCheck(String projectPath) {
        handleBuildFiles(projectPath);
        String versionsFilePath = createVersionsFile(projectPath);
        if (!versionsFilePath.isEmpty()) {
            writeContent(versionsFilePath, versionsContentBuilder.append(depsContentBuilder).toString());
        }
    }

    private String createVersionsFile(String projectPath) {

        String path = "";
        File file = new File(projectPath, VERSIONS);
        if (!file.exists()) {
            try {
                file.createNewFile();
                path = file.getPath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path;
    }


    private void handleBuildFiles(String filePath) {

        File file = new File(filePath);
        if (file.getPath().contains("build.gradle")) {
            handleBuildFile(file.getPath());
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {

                handleBuildFiles(files[i].getPath());
            }
        }
    }


    private void handleBuildFile(String filePath) {

        try {
            StringBuilder buildFileContentBuilder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
            String line = reader.readLine();
            while (line != null) {

                LibItem libItem = extractLibItem(line);
                if (libItem != null) {
                    versionsContentBuilder.append("versions.")
                            .append(libItem.getArticleId().replace("-", "_")).append("=")
                            .append('\'')
                            .append(libItem.getVersion())
                            .append('\'')
                            .append("\n");

                    Boolean hasGroup = groupNameMap.get(libItem.getGroupName());
                    if (hasGroup != null && hasGroup) {

                        depsContentBuilder.append(libItem.getGroupName())
                                .append(".")
                                .append(libItem.getArticleId().replace("-", "_"))
                                .append("=")
                                .append("\"")
                                .append(libItem.getGroup() + ":" + libItem.getArticleId() + ":" + "${versions." + libItem.getArticleId().replace("-", "_") + "}")
                                .append("\"")
                                .append("\n");
                    } else {

                        depsContentBuilder.append("def ")
                                .append(libItem.getGroupName())
                                .append("=")
                                .append("[:]")
                                .append("\n")
                                .append("ext.deps." + libItem.getGroupName())
                                .append("=")
                                .append(libItem.getGroupName())
                                .append("\n")
                                .append("ext.deps." + libItem.getGroupName() + "." + libItem.getArticleId().replace("-", "_") + "=")
                                .append("\"")
                                .append(libItem.getGroup() + ":" + libItem.getArticleId() + ":" + "${versions." + libItem.getArticleId().replace("-", "_") + "}")
                                .append("\"")
                                .append("\n")
                                .append("\n");
                        groupNameMap.put(libItem.getGroupName(), true);
                    }


                    buildFileContentBuilder.append(libItem.getMethod() + " " + "deps." + libItem.getGroupName() + "." + libItem.getArticleId().replace("-", "_") + "\n");

                } else {
                    buildFileContentBuilder.append(line).append("\n");
                }

                line = reader.readLine();
            }
            writeContent(filePath, buildFileContentBuilder.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeContent(String filePath, String content) {

        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath)));
            writer.write(content, 0, content.length());
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private StringBuilder createVersionsContentBuilder() {

        StringBuilder versionsContentBuilder = new StringBuilder();
        versionsContentBuilder.append("ext.deps=[:]\n")
                .append("def versions=[:]\n")
                .append("ext.deps.versions=versions\n");
        return versionsContentBuilder;
    }

    private LibItem extractLibItem(String line) {
        Pattern pattern = Pattern.compile("(\\w+)\\s*[\'\"](\\S+):(\\S+):(\\S+)[\'\"]");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            LibItem libItem = new LibItem();
            libItem.setMethod(matcher.group(1));
            String group = matcher.group(2);
            libItem.setGroup(group);
            String[] groupList = group.split("\\.");
            if (groupList.length > 0) {
                libItem.setGroupName(groupList[groupList.length - 1]);
            }
            libItem.setArticleId(matcher.group(3));
            libItem.setVersion(matcher.group(4));
            libItem.setLib(line);
            return libItem;
        }
        return null;
    }


}
