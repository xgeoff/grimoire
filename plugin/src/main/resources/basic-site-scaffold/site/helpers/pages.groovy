import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

return {String pagesPath, Options options ->

    def toSidebarItems
    toSidebarItems = { File baseDir, File rootDir ->
        List<Map<String, Object>> items = new ArrayList<>();

        File[] files = baseDir.listFiles();
        if (files == null) return items;

        for (File file : files) {
            if (file.isDirectory()) {
                List<Map<String, Object>> children = toSidebarItems(file, rootDir);
                if (!children.isEmpty()) {
                    Map<String, Object> dir = new HashMap<>();
                    dir.put("type", "directory");
                    dir.put("name", file.getName());
                    dir.put("children", children);
                    items.add(dir);
                }
            } else if (file.name ==~ /(?i).*\.md|\.html?$/) {
                def relativePath = rootDir.toPath().relativize(file.toPath()).toString().replace(File.separator, "/")
                def name = file.name.replaceFirst(/(?i)\.(md|html?)$/, "")

                def fileMap = [
                        type: "file",
                        name: name,
                        path: relativePath
                ]
                items << fileMap
            }
        }

        // Sort directories first, then files, both alphabetically
        return items.stream()
                .sorted(Comparator.comparing((Map<String, Object> i) -> i.get("type").toString())
                        .thenComparing(i -> i.get("name").toString().toLowerCase()))
                .collect(Collectors.toList());
    };

    File rootDir = new File(pagesPath);
    if (!rootDir.exists() || !rootDir.isDirectory()) {
        return Collections.emptyList();
    }

    return toSidebarItems(rootDir, rootDir);

} as Helper<String>