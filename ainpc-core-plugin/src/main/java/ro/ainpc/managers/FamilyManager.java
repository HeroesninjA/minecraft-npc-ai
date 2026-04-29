package ro.ainpc.managers;

import ro.ainpc.AINPCPlugin;
import ro.ainpc.npc.AINPC;
import ro.ainpc.spawn.FamilyBindingPlan;
import ro.ainpc.spawn.FamilyBindingResult;
import ro.ainpc.utils.NPCNameGenerator;

import java.sql.*;
import java.util.*;

/**
 * Manager pentru sistemul de familie al NPC-urilor
 */
public class FamilyManager {

    private final AINPCPlugin plugin;
    private final Random random;

    public FamilyManager(AINPCPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    /**
     * Genereaza familia pentru un NPC nou
     */
    public void generateFamily(AINPC npc) {
        if (!plugin.getConfig().getBoolean("family.auto_generate", true)) {
            return;
        }

        // Sansa de a avea sot/sotie
        int spouseChance = plugin.getConfig().getInt("family.spouse_chance", 60);
        if (random.nextInt(100) < spouseChance && npc.getAge() >= 20) {
            generateSpouse(npc);
        }

        // Sansa de a avea parinti in viata
        int livingParentsChance = plugin.getConfig().getInt("family.living_parents_chance", 40);
        generateParents(npc, random.nextInt(100) < livingParentsChance);

        // Genereaza copii daca are sot/sotie si varsta potrivita
        if (hasSpouse(npc) && npc.getAge() >= 25) {
            int maxChildren = plugin.getConfig().getInt("family.max_children", 4);
            int numChildren = random.nextInt(maxChildren + 1);
            for (int i = 0; i < numChildren; i++) {
                generateChild(npc);
            }
        }

        // Genereaza frati/surori
        int numSiblings = random.nextInt(4); // 0-3 frati
        for (int i = 0; i < numSiblings; i++) {
            generateSibling(npc);
        }

        plugin.debug("Familie generata pentru " + npc.getName());
    }

    /**
     * Genereaza sot/sotie pentru NPC
     */
    private void generateSpouse(AINPC npc) {
        String spouseGender = npc.getGender().equals("male") ? "female" : "male";
        String spouseName = generateName(spouseGender);
        
        // Varsta apropiata de NPC
        int spouseAge = npc.getAge() + random.nextInt(11) - 5; // +/- 5 ani
        spouseAge = Math.max(18, spouseAge);

        String backstory = "S-au casatorit acum " + (random.nextInt(10) + 1) + " ani.";
        
        addFamilyMember(npc, spouseName, "spouse", true, null, backstory);
    }

    /**
     * Genereaza parintii NPC-ului
     */
    private void generateParents(AINPC npc, boolean alive) {
        // Tatal
        String fatherName = generateName("male");
        boolean fatherAlive = alive && random.nextBoolean();
        String fatherBackstory = fatherAlive ? 
            "Lucreaza ca " + getRandomOccupation() + "." : 
            "A murit acum " + (random.nextInt(20) + 1) + " ani.";
        addFamilyMember(npc, fatherName, "father", fatherAlive, null, fatherBackstory);

        // Mama
        String motherName = generateName("female");
        boolean motherAlive = alive && random.nextBoolean();
        String motherBackstory = motherAlive ? 
            "Se ocupa de " + getRandomActivity() + "." : 
            "A murit acum " + (random.nextInt(20) + 1) + " ani.";
        addFamilyMember(npc, motherName, "mother", motherAlive, null, motherBackstory);
    }

    /**
     * Genereaza un copil pentru NPC
     */
    private void generateChild(AINPC npc) {
        String childGender = random.nextBoolean() ? "male" : "female";
        String childName = generateName(childGender);
        String relationType = childGender.equals("male") ? "son" : "daughter";
        
        // Varsta copilului
        int maxChildAge = npc.getAge() - 18;
        if (maxChildAge < 1) return;
        int childAge = random.nextInt(Math.min(maxChildAge, 25)) + 1;
        
        String backstory = "Are " + childAge + " ani. " + 
            (childAge >= 18 ? "Lucreaza ca " + getRandomOccupation() + "." : "Este inca la scoala.");
        
        addFamilyMember(npc, childName, relationType, true, null, backstory);
    }

    /**
     * Genereaza un frate/sora pentru NPC
     */
    private void generateSibling(AINPC npc) {
        String siblingGender = random.nextBoolean() ? "male" : "female";
        String siblingName = generateName(siblingGender);
        String relationType = siblingGender.equals("male") ? "brother" : "sister";
        
        // Varsta fratelui/surorii
        int siblingAge = npc.getAge() + random.nextInt(21) - 10; // +/- 10 ani
        siblingAge = Math.max(5, siblingAge);
        
        boolean alive = random.nextInt(100) < 90; // 90% sansa sa fie in viata
        
        String backstory = alive ? 
            "Are " + siblingAge + " ani. Locuieste in " + getRandomLocation() + "." :
            "A murit acum " + (random.nextInt(10) + 1) + " ani.";
        
        addFamilyMember(npc, siblingName, relationType, alive, null, backstory);
    }

    /**
     * Adauga un membru al familiei in baza de date
     */
    public void addFamilyMember(AINPC npc, String name, String relationType, 
                                 boolean isAlive, Integer relatedNpcId, String backstory) {
        String sql = """
            INSERT INTO npc_family (npc_id, related_npc_id, related_name, relation_type, is_alive, backstory)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, npc.getDatabaseId());
            if (relatedNpcId != null) {
                stmt.setInt(2, relatedNpcId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setString(3, name);
            stmt.setString(4, relationType);
            stmt.setInt(5, isAlive ? 1 : 0);
            stmt.setString(6, backstory);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Eroare la adaugarea membrului familiei: " + e.getMessage());
        }
    }

    /**
     * Obtine familia unui NPC
     */
    public List<FamilyMemberRecord> getFamily(AINPC npc) {
        List<FamilyMemberRecord> family = new ArrayList<>();

        String sql = """
            SELECT related_name, relation_type, is_alive, related_npc_id, backstory
            FROM npc_family
            WHERE npc_id = ?
            ORDER BY 
                CASE relation_type
                    WHEN 'spouse' THEN 1
                    WHEN 'father' THEN 2
                    WHEN 'mother' THEN 3
                    WHEN 'son' THEN 4
                    WHEN 'daughter' THEN 5
                    WHEN 'brother' THEN 6
                    WHEN 'sister' THEN 7
                    ELSE 8
                END
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, npc.getDatabaseId());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer relatedNpcId = rs.getInt("related_npc_id");
                    if (rs.wasNull()) relatedNpcId = null;
                    
                    family.add(new FamilyMemberRecord(
                        rs.getString("related_name"),
                        rs.getString("relation_type"),
                        rs.getInt("is_alive") == 1,
                        relatedNpcId
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Eroare la obtinerea familiei: " + e.getMessage());
        }

        return family;
    }

    /**
     * Verifica daca NPC-ul are sot/sotie
     */
    public boolean hasSpouse(AINPC npc) {
        String sql = "SELECT COUNT(*) FROM npc_family WHERE npc_id = ? AND relation_type = 'spouse' AND is_alive = 1";

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, npc.getDatabaseId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Eroare la verificarea sotului/sotiei: " + e.getMessage());
        }

        return false;
    }

    /**
     * Obtine un raport formatat al familiei
     */
    public String getFamilyReport(AINPC npc) {
        StringBuilder sb = new StringBuilder();
        List<FamilyMemberRecord> family = getFamily(npc);

        sb.append("&6=== Familia lui ").append(npc.getName()).append(" ===\n\n");

        if (family.isEmpty()) {
            sb.append("&7Nu are familie cunoscuta.\n");
            return sb.toString();
        }

        // Grupeaza dupa tip
        Map<String, List<FamilyMemberRecord>> grouped = new LinkedHashMap<>();
        for (FamilyMemberRecord member : family) {
            grouped.computeIfAbsent(member.relationType(), k -> new ArrayList<>()).add(member);
        }

        for (Map.Entry<String, List<FamilyMemberRecord>> entry : grouped.entrySet()) {
            String relationName = getRelationNameRomanian(entry.getKey());
            sb.append("&e").append(relationName).append(":\n");
            
            for (FamilyMemberRecord member : entry.getValue()) {
                sb.append("  &f- ").append(member.name());
                if (!member.alive()) {
                    sb.append(" &8(decedat)");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Leaga doi NPC-uri ca familie
     */
    public void linkNPCsAsFamily(AINPC npc1, AINPC npc2, String relation1to2, String relation2to1) {
        // NPC1 -> NPC2
        addFamilyMember(npc1, npc2.getName(), relation1to2, true, npc2.getDatabaseId(), null);
        // NPC2 -> NPC1
        addFamilyMember(npc2, npc1.getName(), relation2to1, true, npc1.getDatabaseId(), null);
    }

    /**
     * Leaga o familie fizica dupa ce toti membrii au fost spawnati si salvati.
     */
    public FamilyBindingResult bindSpawnedFamily(FamilyBindingPlan plan) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (plan == null) {
            errors.add("FamilyBindingPlan este null.");
            return new FamilyBindingResult(false, "", 0, errors, warnings);
        }

        if (plan.familyId().isBlank()) {
            errors.add("FamilyBindingPlan nu are familyId.");
        }

        List<FamilyBindingPlan.Member> members = validateFamilyMembers(plan.members(), errors, warnings);
        if (members.size() < 2) {
            errors.add("Familia " + plan.familyId() + " are nevoie de cel putin 2 membri fizici salvati.");
        }

        if (!errors.isEmpty()) {
            return new FamilyBindingResult(false, plan.familyId(), 0, errors, warnings);
        }

        int relationsCreated = 0;
        for (int i = 0; i < members.size(); i++) {
            FamilyBindingPlan.Member left = members.get(i);
            for (int j = i + 1; j < members.size(); j++) {
                FamilyBindingPlan.Member right = members.get(j);
                RelationPair relationPair = resolveRelationPair(left.familyRole(), right.familyRole());
                if ("relative".equals(relationPair.leftToRight()) && "relative".equals(relationPair.rightToLeft())) {
                    warnings.add("Relatie generica folosita intre " + left.npcName() + " si " + right.npcName()
                        + " pentru roluri: " + left.familyRole() + " / " + right.familyRole() + ".");
                }

                if (addFamilyMemberIfMissing(left.npcId(), right.npcId(), right.npcName(), relationPair.leftToRight())) {
                    relationsCreated++;
                }
                if (addFamilyMemberIfMissing(right.npcId(), left.npcId(), left.npcName(), relationPair.rightToLeft())) {
                    relationsCreated++;
                }
            }
        }

        if (relationsCreated == 0) {
            warnings.add("Nu au fost create relatii noi pentru familia " + plan.familyId() + ".");
        } else {
            plugin.debug("Familie fizica legata: " + plan.familyId() + " cu " + relationsCreated + " relatii.");
        }

        return new FamilyBindingResult(true, plan.familyId(), relationsCreated, errors, warnings);
    }

    private List<FamilyBindingPlan.Member> validateFamilyMembers(List<FamilyBindingPlan.Member> rawMembers,
                                                                 List<String> errors,
                                                                 List<String> warnings) {
        List<FamilyBindingPlan.Member> validMembers = new ArrayList<>();
        Set<Integer> seenNpcIds = new HashSet<>();

        for (FamilyBindingPlan.Member member : rawMembers) {
            if (member.npcId() <= 0) {
                errors.add("Membrul familiei '" + member.npcKey() + "' nu are databaseId valid.");
                continue;
            }
            if (member.npcName().isBlank()) {
                errors.add("Membrul familiei cu id " + member.npcId() + " nu are nume.");
                continue;
            }
            if (member.familyRole().isBlank()) {
                warnings.add("Membrul " + member.npcName() + " nu are familyRole; folosesc 'relative'.");
            }
            if (!seenNpcIds.add(member.npcId())) {
                errors.add("NPC duplicat in FamilyBindingPlan: id=" + member.npcId() + ".");
                continue;
            }

            validMembers.add(member);
        }

        return validMembers;
    }

    private boolean addFamilyMemberIfMissing(int npcId, int relatedNpcId, String relatedName, String relationType) {
        if (familyRelationExists(npcId, relatedNpcId, relationType)) {
            return false;
        }

        String sql = """
            INSERT INTO npc_family (npc_id, related_npc_id, related_name, relation_type, is_alive, backstory)
            VALUES (?, ?, ?, ?, 1, ?)
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, npcId);
            stmt.setInt(2, relatedNpcId);
            stmt.setString(3, relatedName);
            stmt.setString(4, relationType);
            stmt.setString(5, "Relatie fizica legata dupa spawn.");
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Eroare la legarea familiei dupa spawn: " + e.getMessage());
            return false;
        }
    }

    private boolean familyRelationExists(int npcId, int relatedNpcId, String relationType) {
        String sql = """
            SELECT COUNT(*)
            FROM npc_family
            WHERE npc_id = ? AND related_npc_id = ? AND relation_type = ?
        """;

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, npcId);
            stmt.setInt(2, relatedNpcId);
            stmt.setString(3, relationType);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Eroare la verificarea relatiei familiale existente: " + e.getMessage());
            return false;
        }
    }

    private RelationPair resolveRelationPair(String leftRole, String rightRole) {
        String left = normalizeRole(leftRole);
        String right = normalizeRole(rightRole);

        if (isParentRole(left) && isParentRole(right)) {
            return new RelationPair("spouse", "spouse");
        }
        if (isSpouseRole(left) && isSpouseRole(right)) {
            return new RelationPair("spouse", "spouse");
        }
        if (isParentRole(left) && isChildRole(right)) {
            return new RelationPair(childRelation(right), parentRelation(left));
        }
        if (isChildRole(left) && isParentRole(right)) {
            return new RelationPair(parentRelation(right), childRelation(left));
        }
        if (isChildRole(left) && isChildRole(right)) {
            return new RelationPair(siblingRelation(right), siblingRelation(left));
        }
        if (isSiblingRole(left) && isSiblingRole(right)) {
            return new RelationPair(siblingRelation(right), siblingRelation(left));
        }

        return new RelationPair("relative", "relative");
    }

    private boolean isParentRole(String role) {
        return role.equals("father") || role.equals("mother") || role.equals("parent");
    }

    private boolean isChildRole(String role) {
        return role.equals("son") || role.equals("daughter") || role.equals("child");
    }

    private boolean isSpouseRole(String role) {
        return role.equals("spouse") || role.equals("husband") || role.equals("wife");
    }

    private boolean isSiblingRole(String role) {
        return role.equals("brother") || role.equals("sister") || role.equals("sibling");
    }

    private String parentRelation(String role) {
        return switch (role) {
            case "father" -> "father";
            case "mother" -> "mother";
            default -> "parent";
        };
    }

    private String childRelation(String role) {
        return switch (role) {
            case "son" -> "son";
            case "daughter" -> "daughter";
            default -> "child";
        };
    }

    private String siblingRelation(String role) {
        return switch (role) {
            case "brother", "son" -> "brother";
            case "sister", "daughter" -> "sister";
            default -> "sibling";
        };
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "relative";
        }

        return role.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private record RelationPair(String leftToRight, String rightToLeft) {
    }

    /**
     * Sterge familia unui NPC
     */
    public void clearFamily(AINPC npc) {
        String sql = "DELETE FROM npc_family WHERE npc_id = ?";

        try (PreparedStatement stmt = plugin.getDatabaseManager().prepareStatement(sql)) {
            stmt.setInt(1, npc.getDatabaseId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Eroare la stergerea familiei: " + e.getMessage());
        }
    }

    // Helper methods pentru generare

    private String generateName(String gender) {
        return NPCNameGenerator.randomName(gender, random);
    }

    private String getRandomOccupation() {
        String[] occupations = {
            "fermier", "fierar", "pescar", "negustor", "miner", "tamplar",
            "soldat", "paznic", "brutar", "croitor", "alchimist", "medic"
        };
        return occupations[random.nextInt(occupations.length)];
    }

    private String getRandomActivity() {
        String[] activities = {
            "casa si copii", "gradinarit", "gatit", "tesut",
            "ingrijirea familiei", "comertul local"
        };
        return activities[random.nextInt(activities.length)];
    }

    private String getRandomLocation() {
        String[] locations = {
            "satul vecin", "orasul mare", "peste munti", "langa rau",
            "in padure", "la marginea regatului", "peste mare"
        };
        return locations[random.nextInt(locations.length)];
    }

    private String getRelationNameRomanian(String relation) {
        return switch (relation.toLowerCase()) {
            case "spouse" -> "Sot/Sotie";
            case "father" -> "Tata";
            case "mother" -> "Mama";
            case "son" -> "Fiu";
            case "daughter" -> "Fiica";
            case "brother" -> "Frate";
            case "sister" -> "Sora";
            case "grandfather" -> "Bunic";
            case "grandmother" -> "Bunica";
            default -> relation;
        };
    }
}
