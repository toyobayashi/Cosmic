package net.packet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketFactoryCharsetAuditTest {
    private static final List<Path> SOURCE_FILES = List.of(
            Path.of("src/main/java/tools/PacketCreator.java"),
            Path.of("src/main/java/net/server/guild/GuildPackets.java"),
            Path.of("src/main/java/tools/packets/WeddingPackets.java")
    );

    private static final Map<String, Classification> CLASSIFICATIONS = new LinkedHashMap<>();

    static {
        classify(Classification.FIXED,
                "charInfo(Character chr)",
                "charNameResponse(String charname, boolean nameUsed)",
                "showAllCharacterInfo(int worldid, List<Character> chars, boolean usePic)",
                "sendNameTransferCheck(String availableName, boolean canUseName)",
                "BBSThreadList(ResultSet rs, int start)",
                "showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS)"
        );

        classify(Classification.ASCII_PROTOCOL,
                "sendGuestTOS()",
                "clientRemoteAssetSession(RemoteAssetSession session)",
                "environmentChange(String env, int mode)",
                "environmentMove(String env, int mode)",
                "environmentMoveList(Set<Entry<String, Integer>> envList)",
                "mapEffect(String path)",
                "mapSound(String path)",
                "showIntro(String path)",
                "showInfo(String path)",
                "showForeignInfo(int cid, String path)",
                "sendDojoAnimation(byte firstByte, String animation)",
                "getEnergy(String info, int amount)",
                "bunnyPacket()",
                "OnMarriageResult(final byte msg)"
        );

        classify(Classification.DEFERRED_RISK,
                "updateHiredMerchantBox(HiredMerchant hm)",
                "updatePlayerShopBox(PlayerShop shop)",
                "modifyInventory(boolean updateTick, final List<ModifyInventory> mods)",
                "addNewCharEntry(Character chr)",
                "sendPolice(String text)",
                "getServerList(int serverId, String serverName, int flag, String eventmsg, List<Channel> channelLoad)",
                "sendTV(Character chr, List<String> messages, int type, Character partner)",
                "spawnKite(int objId, int itemId, String name, String msg, Point pos, int ft)",
                "onNewYearCardRes(Character user, NewYearCardRecord newyear, int mode, int msg)",
                "updateAriantPQRanking(Map<Character, Integer> playerScore)",
                "sendRecommended(List<Pair<Integer, String>> worlds)",
                "updateQuest(Character chr, QuestStatus qs, boolean infoUpdate)",
                "getPlayerShopChat(Character chr, String chat, boolean owner)",
                "getPlayerShopNewVisitor(Character chr, int slot)",
                "getTradePartnerAdd(Character chr)",
                "tradeInvite(Character chr)",
                "getPlayerShopOwnerUpdate(PlayerShop.SoldItem item, int position)",
                "getPlayerShop(PlayerShop shop, boolean owner)",
                "giveFameResponse(int mode, String charname, int newfame)",
                "receiveFame(int mode, String charnameFrom)",
                "startMapEffect(String msg, int itemId, boolean active)",
                "sendHint(String hint, int width, int height)",
                "petChat(int cid, byte index, int act, String text)",
                "changePetName(Character chr, String newname, int slot)",
                "getMacros(SkillMacro[] macros)",
                "getMiniGameNewVisitor(MiniGame minigame, Character chr, int slot)",
                "getMatchCardNewVisitor(MiniGame minigame, Character chr, int slot)",
                "getPlayerShopChat(Character chr, String chat, byte slot)",
                "getTradeChat(Character chr, String chat, boolean owner)",
                "getTradeItemAdd(byte number, Item item)",
                "getPlayerShopItemUpdate(PlayerShop shop)",
                "getStorage(int npcId, byte slots, Collection<Item> items, int meso)",
                "storeStorage(byte slots, InventoryType type, Collection<Item> items)",
                "takeOutStorage(byte slots, InventoryType type, Collection<Item> items)",
                "arrangeStorage(byte slots, Collection<Item> items)",
                "showPet(Character chr, Pet pet, boolean remove, boolean hunger)",
                "getHiredMerchant(Character chr, HiredMerchant hm, boolean firstTime)",
                "hiredMerchantChat(String message, byte slot)",
                "viewMerchantVisitorHistory(List<HiredMerchant.PastVisitor> pastVisitors)",
                "viewMerchantBlacklist(Set<String> chrNames)",
                "hiredMerchantVisitorAdd(Character chr, int slot)",
                "spawnHiredMerchantBox(HiredMerchant hm)",
                "getFredrick(Character chr)",
                "addOmokBox(Character chr, int amount, int type)",
                "addMatchCardBox(Character chr, int amount, int type)",
                "updateHiredMerchant(HiredMerchant hm, Character chr)",
                "getPlayerNPC(PlayerNPC npc)",
                "sendMTS(List<MTSItemInfo> items, int tab, int type, int page, int pages)",
                "useChalkboard(Character chr, boolean close)",
                "notYetSoldInv(List<MTSItemInfo> items)",
                "transferInventory(List<MTSItemInfo> items)",
                "loadFamily(Character player)",
                "getFamilyInfo(FamilyEntry f)",
                "getEmptyFamilyInfo()",
                "updateAreaInfo(int area, String info)",
                "itemMegaphone(String msg, boolean whisper, int channel, Item item)",
                "showInfoText(String text)",
                "getMultiMegaphone(String[] messages, int channel, boolean showEar)",
                "sendFamilyInvite(int playerId, String inviter)",
                "sendFamilySummonRequest(String familyName, String from)",
                "sendFamilyLoginNotice(String name, boolean loggedIn)",
                "sendFamilyJoinResponse(boolean accepted, String added)",
                "getSeniorMessage(String name)",
                "sendGainRep(int gain, String from)",
                "showWorldTransferSuccess(Item item, int accountId)",
                "showNameChangeSuccess(Item item, int accountId)",
                "showCouponRedeemedItems(int accountId, int maplePoints, int mesos, List<Item> cashItems, List<Pair<Integer, Integer>> items)",
                "sendDueyParcelReceived(String from, boolean quick)",
                "sendDuey(int operation, List<DueyPackage> packages)",
                "getDojoInfo(String info)",
                "getDojoInfoMessage(String message)",
                "showPedigree(FamilyEntry entry)",
                "updateDojoStats(Character chr, int belt)",
                "levelUpMessage(int type, int level, String charname)",
                "marriageMessage(int type, String charname)",
                "jobMessage(int type, int job, String charname)",
                "hpqMessage(String text)",
                "talkGuide(String talk)",
                "showBoughtCashPackage(List<Item> cashPackage, int accountId)",
                "showBoughtCashItem(Item item, int accountId)",
                "showGifts(List<Pair<Item, String>> gifts)",
                "takeFromCashInventory(Item item)",
                "putIntoCashInventory(Item item, int accountId)",
                "showBoughtCashRing(Item ring, String recipient, int accountId)",
                "showGiftSucceed(String to, CashItem item)",
                "earnTitleMessage(String msg)",
                "playerSummoned(String name, int tab, int number)",
                "playerDiedMessage(String name, int lostCP, int team)",
                "setNPCScriptable(Map<Integer, String> scriptableNpcIds)",
                "onTakePhoto(String ReservedGroomName, String ReservedBrideName, int m_dwField, List<Character> m_dwUsers)"
        );
    }

    @Test
    void clientAwareFactoriesDoNotWriteTextWithDefaultCharset() throws IOException {
        List<AuditFinding> riskyClientAwareFindings = collectDirectFindings().stream()
                .filter(finding -> finding.signature().parameterList().contains("Client "))
                .toList();

        assertTrue(riskyClientAwareFindings.isEmpty(), formatFindings("""
                Client-aware packet factories must not keep default-charset text writes.
                Add a charset-aware OutPacket.create(..., c.getPacketCharset()) or explicitly split the helper.
                """, riskyClientAwareFindings));
    }

    @Test
    void defaultCharsetTextFactoriesStayExplicitlyClassified() throws IOException {
        List<AuditFinding> findings = collectFindings();
        Set<String> actualIds = findings.stream()
                .map(AuditFinding::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> configuredIds = new LinkedHashSet<>(CLASSIFICATIONS.keySet());

        List<String> missing = actualIds.stream()
                .filter(id -> !configuredIds.contains(id))
                .toList();
        List<String> stale = configuredIds.stream()
                .filter(id -> !actualIds.contains(id))
                .toList();

        StringBuilder message = new StringBuilder();
        if (!missing.isEmpty()) {
            message.append("Unclassified default-charset text factories:\n");
            findings.stream()
                    .filter(finding -> missing.contains(finding.id()))
                    .forEach(finding -> message.append(" - ")
                            .append(finding.id())
                            .append(" @ ")
                            .append(finding.relativePath())
                            .append(':')
                            .append(finding.lineNumber())
                            .append('\n'));
        }
        if (!stale.isEmpty()) {
            if (message.length() > 0) {
                message.append('\n');
            }
            message.append("Stale classifications (method no longer matched by audit):\n");
            stale.forEach(id -> message.append(" - ").append(id).append('\n'));
        }

        assertTrue(missing.isEmpty() && stale.isEmpty(), message.toString());
    }

    @Test
    void defaultCharsetWrapperFactoriesStayVisibleToAudit() throws IOException {
        Set<String> findingIds = collectFindings().stream()
                .map(AuditFinding::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        assertTrue(findingIds.contains("charInfo(Character chr)"),
                "Expected audit to see charInfo(Character chr) via writeCharInfo helper");
        assertTrue(findingIds.contains("showAllCharacterInfo(int worldid, List<Character> chars, boolean usePic)"),
                "Expected audit to see showAllCharacterInfo(int, ...) via shared character entry writer");
        assertTrue(findingIds.contains("BBSThreadList(ResultSet rs, int start)"),
                "Expected audit to see BBSThreadList(ResultSet, int) via shared BBS writer");
        assertTrue(findingIds.contains("showThread(int localthreadid, ResultSet threadRS, ResultSet repliesRS)"),
                "Expected audit to see showThread(int, ResultSet, ResultSet) via shared BBS writer");
    }

    @Test
    void fixedEntriesStillHaveClientAwareSibling() throws IOException {
        List<AuditFinding> findings = collectFindings();
        Map<String, List<MethodSignature>> signaturesByFile = collectMethodSignatures();

        List<String> invalidFixedEntries = new ArrayList<>();
        for (AuditFinding finding : findings) {
            if (CLASSIFICATIONS.get(finding.id()) != Classification.FIXED) {
                continue;
            }

            boolean hasClientAwareSibling = signaturesByFile.getOrDefault(finding.relativePath(), List.of()).stream()
                    .anyMatch(candidate -> candidate.methodName().equals(finding.signature().methodName())
                            && !candidate.parameterList().equals(finding.signature().parameterList())
                            && candidate.parameterList().contains("Client "));
            if (!hasClientAwareSibling) {
                invalidFixedEntries.add(finding.id());
            }
        }

        assertTrue(invalidFixedEntries.isEmpty(), "Fixed entries without client-aware sibling:\n - "
                + String.join("\n - ", invalidFixedEntries));
    }

    private static List<AuditFinding> collectFindings() throws IOException {
        List<AuditFinding> findings = new ArrayList<>();
        for (Path sourceFile : SOURCE_FILES) {
            findings.addAll(scanSource(sourceFile));
        }
        findings.sort(Comparator.comparing(AuditFinding::relativePath).thenComparingInt(AuditFinding::lineNumber));
        return findings;
    }

    private static List<AuditFinding> collectDirectFindings() throws IOException {
        List<AuditFinding> findings = new ArrayList<>();
        for (Path sourceFile : SOURCE_FILES) {
            findings.addAll(scanSourceDirect(sourceFile));
        }
        findings.sort(Comparator.comparing(AuditFinding::relativePath).thenComparingInt(AuditFinding::lineNumber));
        return findings;
    }

    private static Map<String, List<MethodSignature>> collectMethodSignatures() throws IOException {
        Map<String, List<MethodSignature>> signaturesByFile = new LinkedHashMap<>();
        for (Path sourceFile : SOURCE_FILES) {
            String source = Files.readString(sourceFile);
            signaturesByFile.put(normalizePath(sourceFile), parseMethods(source).stream()
                    .map(MethodBlock::signature)
                    .toList());
        }
        return signaturesByFile;
    }

    private static List<AuditFinding> scanSource(Path sourceFile) throws IOException {
        String source = Files.readString(sourceFile);
        String relativePath = normalizePath(sourceFile);
        List<MethodBlock> methods = parseMethods(source);
        Map<String, List<MethodBlock>> methodsByName = indexMethodsByName(methods);

        List<AuditFinding> findings = new ArrayList<>();
        for (MethodBlock method : methods) {
            AuditFinding finding = evaluateMethod(relativePath, method, methodsByName);
            if (finding != null) {
                findings.add(finding);
            }
        }
        return findings;
    }

    private static List<AuditFinding> scanSourceDirect(Path sourceFile) throws IOException {
        String source = Files.readString(sourceFile);
        String relativePath = normalizePath(sourceFile);

        List<AuditFinding> findings = new ArrayList<>();
        for (MethodBlock method : parseMethods(source)) {
            AuditFinding finding = evaluateDirectMethod(relativePath, method);
            if (finding != null) {
                findings.add(finding);
            }
        }
        return findings;
    }

    private static AuditFinding evaluateMethod(String relativePath,
                                               MethodBlock method,
                                               Map<String, List<MethodBlock>> methodsByName) {
        Integer lineNumber = findDefaultTextWriteLine(method, Map.of(), methodsByName, new LinkedHashSet<>());
        if (lineNumber != null) {
            if (method.signature().parameterList().contains("Client ")) {
                return null;
            }
            return new AuditFinding(relativePath, method.signature(), lineNumber, method.signature().id());
        }
        return null;
    }

    private static AuditFinding evaluateDirectMethod(String relativePath, MethodBlock method) {
        Map<String, PacketCharsetState> packetStates = new LinkedHashMap<>();
        for (MethodLine line : method.lines()) {
            CreateMatch createMatch = matchCreate(line.scanText());
            if (createMatch != null) {
                packetStates.put(createMatch.varName(), classifyCreate(createMatch.arguments()));
                continue;
            }

            Matcher writeMatcher = TEXT_WRITE.matcher(line.scanText());
            if (writeMatcher.find()) {
                String packetVar = writeMatcher.group("var");
                if (packetStates.get(packetVar) == PacketCharsetState.DEFAULT) {
                    return new AuditFinding(relativePath, method.signature(), line.lineNumber(), method.signature().id());
                }
            }
        }
        return null;
    }

    private static Integer findDefaultTextWriteLine(MethodBlock method,
                                                    Map<String, PacketCharsetState> incomingPacketStates,
                                                    Map<String, List<MethodBlock>> methodsByName,
                                                    Set<String> callStack) {
        if (!callStack.add(method.signature().id())) {
            return null;
        }

        try {
            Map<String, PacketCharsetState> packetStates = new LinkedHashMap<>(incomingPacketStates);
            for (MethodLine line : method.lines()) {
                CreateMatch createMatch = matchCreate(line.scanText());
                if (createMatch != null) {
                    packetStates.put(createMatch.varName(), classifyCreate(createMatch.arguments()));
                    continue;
                }

                Matcher writeMatcher = TEXT_WRITE.matcher(line.scanText());
                if (writeMatcher.find()) {
                    String packetVar = writeMatcher.group("var");
                    if (packetStates.get(packetVar) == PacketCharsetState.DEFAULT) {
                        return line.lineNumber();
                    }
                }

                if (passesDefaultPacketIntoTextWriter(line.scanText(), packetStates, methodsByName, callStack)) {
                    return line.lineNumber();
                }
            }
            return null;
        } finally {
            callStack.remove(method.signature().id());
        }
    }

    private static boolean passesDefaultPacketIntoTextWriter(String line,
                                                             Map<String, PacketCharsetState> packetStates,
                                                             Map<String, List<MethodBlock>> methodsByName,
                                                             Set<String> callStack) {
        for (MethodCall methodCall : findMethodCalls(line)) {
            List<MethodBlock> candidates = methodsByName.getOrDefault(methodCall.name(), List.of());
            if (candidates.isEmpty()) {
                continue;
            }

            List<String> arguments = splitTopLevel(methodCall.arguments());
            for (MethodBlock candidate : candidates) {
                Map<String, PacketCharsetState> calleePacketStates = mapPacketArguments(candidate, arguments, packetStates);
                if (calleePacketStates.isEmpty()) {
                    continue;
                }

                if (findDefaultTextWriteLine(candidate, calleePacketStates, methodsByName, callStack) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static PacketCharsetState classifyCreate(String arguments) {
        return arguments.contains(",") ? PacketCharsetState.CHARSET_AWARE : PacketCharsetState.DEFAULT;
    }

    private static void classify(Classification classification, String... methodIds) {
        for (String methodId : methodIds) {
            CLASSIFICATIONS.put(methodId, classification);
        }
    }

    private static CreateMatch matchCreate(String line) {
        Matcher declarationMatcher = OUT_PACKET_CREATE_DECLARATION.matcher(line);
        if (declarationMatcher.find()) {
            return new CreateMatch(declarationMatcher.group("var"), declarationMatcher.group("args"));
        }

        Matcher assignmentMatcher = OUT_PACKET_CREATE_ASSIGNMENT.matcher(line);
        if (assignmentMatcher.find()) {
            return new CreateMatch(assignmentMatcher.group("var"), assignmentMatcher.group("args"));
        }

        return null;
    }

    private static Map<String, List<MethodBlock>> indexMethodsByName(List<MethodBlock> methods) {
        Map<String, List<MethodBlock>> methodsByName = new LinkedHashMap<>();
        for (MethodBlock method : methods) {
            methodsByName.computeIfAbsent(method.signature().methodName(), ignored -> new ArrayList<>()).add(method);
        }
        return methodsByName;
    }

    private static List<MethodBlock> parseMethods(String source) {
        String sanitizedSource = stripComments(source);
        List<MethodBlock> methods = new ArrayList<>();
        String[] originalLines = source.split("\\R", -1);
        String[] sanitizedLines = sanitizedSource.split("\\R", -1);
        int braceDepth = 0;
        int methodBraceDepth = -1;
        MethodSignature currentSignature = null;
        List<MethodParameter> currentParameters = null;
        List<MethodLine> currentLines = null;

        for (int i = 0; i < originalLines.length; i++) {
            String line = originalLines[i];
            String sanitizedLine = sanitizedLines[i];
            Matcher matcher = METHOD_HEADER.matcher(sanitizedLine);
            if (currentSignature == null && matcher.matches()) {
                currentSignature = new MethodSignature(
                        matcher.group("name"),
                        normalizeWhitespace(matcher.group("params"))
                );
                currentLines = new ArrayList<>();
                braceDepth += count(sanitizedLine, '{') - count(sanitizedLine, '}');
                methodBraceDepth = braceDepth;
                currentParameters = parseParameters(matcher.group("params"));
                continue;
            }

            if (currentSignature != null) {
                currentLines.add(new MethodLine(i + 1, line, sanitizedLine));
            }

            braceDepth += count(sanitizedLine, '{') - count(sanitizedLine, '}');

            if (currentSignature != null && braceDepth < methodBraceDepth) {
                methods.add(new MethodBlock(currentSignature, currentParameters, currentLines));
                currentSignature = null;
                currentParameters = null;
                currentLines = null;
                methodBraceDepth = -1;
            }
        }

        return methods;
    }

    private static List<MethodParameter> parseParameters(String parameterList) {
        if (parameterList.isBlank()) {
            return List.of();
        }

        List<MethodParameter> parameters = new ArrayList<>();
        for (String rawParameter : splitTopLevel(parameterList)) {
            String normalized = normalizeWhitespace(rawParameter);
            if (normalized.isEmpty()) {
                continue;
            }

            int lastSpace = normalized.lastIndexOf(' ');
            if (lastSpace < 0) {
                continue;
            }

            String name = normalized.substring(lastSpace + 1).trim();
            String type = normalized.substring(0, lastSpace).trim();
            parameters.add(new MethodParameter(type, name));
        }
        return parameters;
    }

    private static Map<String, PacketCharsetState> mapPacketArguments(MethodBlock method,
                                                                      List<String> arguments,
                                                                      Map<String, PacketCharsetState> callerPacketStates) {
        if (method.parameters().size() != arguments.size()) {
            return Map.of();
        }

        Map<String, PacketCharsetState> packetStates = new LinkedHashMap<>();
        for (int i = 0; i < method.parameters().size(); i++) {
            MethodParameter parameter = method.parameters().get(i);
            if (!parameter.isOutPacket()) {
                continue;
            }

            PacketCharsetState state = callerPacketStates.get(arguments.get(i).trim());
            if (state != null) {
                packetStates.put(parameter.name(), state);
            }
        }
        return packetStates;
    }

    private static List<MethodCall> findMethodCalls(String line) {
        List<MethodCall> calls = new ArrayList<>();
        Matcher matcher = METHOD_CALL_START.matcher(line);
        int searchIndex = 0;
        while (matcher.find(searchIndex)) {
            int openParenIndex = matcher.end() - 1;
            int closeParenIndex = findMatchingParen(line, openParenIndex);
            if (closeParenIndex < 0) {
                break;
            }

            calls.add(new MethodCall(
                    matcher.group("name"),
                    line.substring(openParenIndex + 1, closeParenIndex)
            ));
            searchIndex = closeParenIndex + 1;
        }
        return calls;
    }

    private static int findMatchingParen(String line, int openParenIndex) {
        int depth = 0;
        for (int i = openParenIndex; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<String> splitTopLevel(String value) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int angleDepth = 0;
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaping) {
                current.append(ch);
                escaping = false;
                continue;
            }

            if ((inSingleQuote || inDoubleQuote) && ch == '\\') {
                current.append(ch);
                escaping = true;
                continue;
            }

            if (!inSingleQuote && ch == '"') {
                inDoubleQuote = !inDoubleQuote;
                current.append(ch);
                continue;
            }

            if (!inDoubleQuote && ch == '\'') {
                inSingleQuote = !inSingleQuote;
                current.append(ch);
                continue;
            }

            if (inSingleQuote || inDoubleQuote) {
                current.append(ch);
                continue;
            }

            switch (ch) {
                case '<' -> angleDepth++;
                case '>' -> angleDepth = Math.max(0, angleDepth - 1);
                case '(' -> parenDepth++;
                case ')' -> parenDepth = Math.max(0, parenDepth - 1);
                case '[' -> bracketDepth++;
                case ']' -> bracketDepth = Math.max(0, bracketDepth - 1);
                case '{' -> braceDepth++;
                case '}' -> braceDepth = Math.max(0, braceDepth - 1);
                case ',' -> {
                    if (angleDepth == 0 && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                        parts.add(current.toString().trim());
                        current.setLength(0);
                        continue;
                    }
                }
                default -> {
                }
            }

            current.append(ch);
        }

        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    private static String stripComments(String source) {
        StringBuilder sanitized = new StringBuilder(source.length());
        boolean inLineComment = false;
        boolean inBlockComment = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaping = false;

        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (ch == '\n') {
                    inLineComment = false;
                    sanitized.append(ch);
                } else if (ch == '\r') {
                    sanitized.append(ch);
                } else {
                    sanitized.append(' ');
                }
                continue;
            }

            if (inBlockComment) {
                if (ch == '*' && next == '/') {
                    sanitized.append("  ");
                    i++;
                    inBlockComment = false;
                } else if (ch == '\n' || ch == '\r') {
                    sanitized.append(ch);
                } else {
                    sanitized.append(' ');
                }
                continue;
            }

            if (escaping) {
                sanitized.append(ch);
                escaping = false;
                continue;
            }

            if (ch == '\\' && (inSingleQuote || inDoubleQuote)) {
                sanitized.append(ch);
                escaping = true;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && ch == '/' && next == '/') {
                sanitized.append("  ");
                i++;
                inLineComment = true;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && ch == '/' && next == '*') {
                sanitized.append("  ");
                i++;
                inBlockComment = true;
                continue;
            }

            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }

            sanitized.append(ch);
        }

        return sanitized.toString();
    }

    private static String normalizeWhitespace(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static int count(String line, char target) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    private static String formatFindings(String header, List<AuditFinding> findings) {
        if (findings.isEmpty()) {
            return header.strip();
        }

        StringBuilder message = new StringBuilder(header.strip());
        message.append('\n');
        findings.forEach(finding -> message.append(" - ")
                .append(finding.id())
                .append(" @ ")
                .append(finding.relativePath())
                .append(':')
                .append(finding.lineNumber())
                .append('\n'));
        return message.toString();
    }

    private enum Classification {
        FIXED,
        ASCII_PROTOCOL,
        DEFERRED_RISK
    }

    private enum PacketCharsetState {
        DEFAULT,
        CHARSET_AWARE
    }

    private record MethodSignature(String methodName, String parameterList) {
        String id() {
            return methodName + "(" + parameterList + ")";
        }
    }

    private record MethodLine(int lineNumber, String text, String scanText) {
    }

    private record MethodParameter(String type, String name) {
        boolean isOutPacket() {
            return type.contains("OutPacket");
        }
    }

    private record MethodBlock(MethodSignature signature, List<MethodParameter> parameters, List<MethodLine> lines) {
    }

    private record AuditFinding(String relativePath, MethodSignature signature, int lineNumber, String id) {
        AuditFinding {
            Objects.requireNonNull(relativePath);
            Objects.requireNonNull(signature);
            Objects.requireNonNull(id);
        }
    }

    private record CreateMatch(String varName, String arguments) {
    }

    private record MethodCall(String name, String arguments) {
    }

    private static final Pattern METHOD_HEADER = Pattern.compile(
            "^\\s*(?:public|private|protected)\\s+(?:static\\s+)?[^\\n;=]+?\\s+(?<name>[A-Za-z0-9_]+)\\s*\\((?<params>[^)]*)\\)\\s*(?:throws\\s+[^\\{]+)?\\{\\s*$"
    );
    private static final Pattern OUT_PACKET_CREATE_DECLARATION = Pattern.compile(
            "\\b(?:final\\s+)?OutPacket\\s+(?<var>[A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*OutPacket\\.create\\((?<args>[^;]*)\\);"
    );
    private static final Pattern OUT_PACKET_CREATE_ASSIGNMENT = Pattern.compile(
            "\\b(?<var>[A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*OutPacket\\.create\\((?<args>[^;]*)\\);"
    );
    private static final Pattern METHOD_CALL_START = Pattern.compile(
            "\\b(?<name>[A-Za-z_][A-Za-z0-9_]*)\\s*\\("
    );
    private static final Pattern TEXT_WRITE = Pattern.compile(
            "\\b(?<var>[A-Za-z_][A-Za-z0-9_]*)\\.write(?:String|FixedString)\\s*\\("
    );
}
