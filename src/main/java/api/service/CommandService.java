package api.service;

import client.command.CommandsExecutor;
import tools.Pair;

import java.util.*;

public class CommandService {

    public Map<String, Object> getCommandList(Integer level, String syntax, Integer page, Integer size) {
        List<Map<String, Object>> allCommands = new ArrayList<>();
        CommandsExecutor executor = CommandsExecutor.getInstance();
        List<Pair<List<String>, List<String>>> commandsNameDesc = executor.getGmCommands();

        for (int levelIdx = 0; levelIdx < commandsNameDesc.size(); levelIdx++) {
            int cmdLevel = levelIdx;
            Pair<List<String>, List<String>> levelPair = commandsNameDesc.get(levelIdx);
            List<String> names = levelPair.getLeft();
            List<String> descs = levelPair.getRight();

            if (names == null || names.isEmpty()) continue;

            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                String desc = (descs != null && i < descs.size()) ? descs.get(i) : "";

                if (level != null && level != -1 && cmdLevel != level) continue;
                if (syntax != null && !syntax.trim().isEmpty()
                        && !name.toLowerCase().contains(syntax.toLowerCase())) continue;

                Map<String, Object> cmd = new LinkedHashMap<>();
                cmd.put("id", allCommands.size() + 1);
                cmd.put("syntax", name);
                cmd.put("defaultLevel", cmdLevel);
                cmd.put("level", cmdLevel);
                cmd.put("description", desc);
                cmd.put("clazz", "");
                cmd.put("enabled", true);
                allCommands.add(cmd);
            }
        }

        long total = allCommands.size();
        int pageNum = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 20;
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, allCommands.size());

        List<Map<String, Object>> records;
        if (start >= allCommands.size()) {
            records = Collections.emptyList();
        } else {
            records = allCommands.subList(start, end);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("totalRow", total);
        return result;
    }
}
