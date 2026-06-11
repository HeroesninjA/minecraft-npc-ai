# Raport Cod Duplicat - Proiect AINPC

**Data analizei:** 25 Mai 2026  
**Metodă:** Analiză statică folosind MCP context server și grep search

---

## 1. Pattern-uri de Validare Duplicate

### 1.1 Validare String Null/Blank (Java)

**Pattern identificat:** `if (value == null || value.isBlank()) { ... }`

**Locații găsite (50+ instanțe):**

#### NPCManager.java
- Linia 349: `if (profileData == null || profileData.isBlank())`
- Linia 725: `if (traitId == null || traitId.isBlank())`
- Linia 2638: `if (name == null || name.isBlank())`
- Linia 2687: `if (candidateName == null || candidateName.isBlank())`
- Linia 2761: `if (inferredOccupation == null || inferredOccupation.isBlank())`
- Linia 3428: `if (occupation == null || occupation.isBlank())`
- Linia 3541: `if (occupation == null || occupation.isBlank())`

#### AINPCCommand.java
- Linia 529: `if (uuid == null || uuid.isBlank())`
- Linia 1584: `if (selector == null || selector.isBlank())`
- Linia 1706: `if (npcSelector == null || npcSelector.isBlank())`
- Linia 2202: `if (playerName == null || playerName.isBlank())`
- Linia 2214: `if (npcSelector == null || npcSelector.isBlank())`
- Linia 2974: `if (value == null || value.isBlank())`
- Linia 3954: `if (selector == null || selector.isBlank())`
- Linia 3979: `if (placeId == null || placeId.isBlank())`
- Linia 5134: `if (option == null || option.isBlank())`
- Linia 6426: `if (value == null || value.isBlank())`
- Linia 6522: `if (entryId == null || entryId.isBlank())`
- Linia 6602: `if (materialId == null || materialId.isBlank())`
- Linia 6612: `if (entityId == null || entityId.isBlank())`
- Linia 6713: `if (rawProgress == null || rawProgress.isBlank())`
- Linia 6754: `if (key == null || key.isBlank())`
- Linia 7215: `if (source == null || source.isBlank())`
- Linia 7271: `if (profileData == null || profileData.isBlank())`
- Linia 7472: `if (placeId == null || placeId.isBlank())`
- Linia 7488: `if (nodeId == null || nodeId.isBlank())`
- Linia 7510: `if (placeId == null || placeId.isBlank())`
- Linia 7535: `if (nodeId == null || nodeId.isBlank())`
- Linia 7588: `if (profileData == null || profileData.isBlank())`
- Linia 7648: `if (ownerNpcId == null || ownerNpcId.isBlank())`
- Linia 8443: `if (placeId == null || placeId.isBlank())`
- Linia 8848: `if (rawValue == null || rawValue.isBlank())`
- Linia 8854: `if (token == null || token.isBlank())`
- Linia 9370: `if (message == null || message.isBlank())`
- Linia 9555: `if (selector == null || selector.isBlank())`

**Recomandare:** Creați o metodă utilitar `StringUtils.isNullOrBlank(String value)` pentru a reduce duplicarea.

---

### 1.2 Validare String Null/Blank (Kotlin)

**Pattern identificat:** `if (value.isNullOrBlank()) { ... }`

**Locații găsite (30+ instanțe):**

#### StoryStateService.kt
- Linia 36: `if (regionId.isNullOrBlank())`
- Linia 117: `if (placeId.isNullOrBlank())`
- Linia 373: `if (value.isNullOrBlank())`
- Linia 380: `if (json.isNullOrBlank())`
- Linia 393: `if (json.isNullOrBlank())`

#### WorldAdminService.kt
- Linia 274: `if (requestedRegionId.isNullOrBlank())`
- Linia 625: `if (!placeId.isNullOrBlank())`
- Linia 763: `if (!node.placeId.isNullOrBlank())`
- Linia 847: `if (tag.isNullOrBlank())`
- Linia 957: `if (localId.isNullOrBlank())`
- Linia 1100: `if (node.placeId.isNullOrBlank())`
- Linia 1146: `if (worldName.isNullOrBlank())`
- Linia 1184: `if (!explicitName.isNullOrBlank())`

#### WorldContextSnapshotBuilder.kt
- Linia 108: `if (anchor == null || anchor.worldName().isNullOrBlank())`

#### HouseholdPersistenceServiceState.kt
- Linia 72: `if (householdId.isNullOrBlank())`
- Linia 121: `if (householdId.isNullOrBlank())`
- Linia 141: `if (homePlaceId.isNullOrBlank())`
- Linia 826: `if (homePlaceId.isNullOrBlank())`
- Linia 1004: `if (sourceKey.isNullOrBlank())`

#### Alte fișiere Kotlin
- WorldNodeType.kt, linia 23
- RegionType.kt, linia 16
- VillageGapAnalyzer.kt, liniile 232, 341
- StoryContextService.kt, linia 372
- MappingDraftKind.kt, linia 18
- MappingWandMode.kt, linia 29
- MappingDraftFactory.kt, liniile 83, 555
- MessageUtils.kt, linia 38
- TopologyCategory.kt, liniile 110, 130
- ProgressionService.kt, linia 387
- SpawnBatchTracker.kt, liniile 478, 500
- NpcSpawnOrchestrator.kt, linia 377
- HouseAllocationValidator.kt, liniile 270, 296, 418, 427
- ProgressionFilter.kt, linia 167

**Recomandare:** Pattern-ul este deja idiomatică în Kotlin, dar verificați dacă există logică duplicată în blocurile if.

---

## 2. Metode Getter/Setter Duplicate

### 2.1 Getteri Simpli în ScenarioEngine.java

**Pattern identificat:** Getteri care returnează direct un câmp privat

**Locații găsite (25+ instanțe în clasa ScenarioTemplate):**

```java
public String getTemplateId() { return templateId; }
public String getDisplayName() { return displayName; }
public String getDescription() { return description; }
public String getSourcePackId() { return sourcePackId; }
public String getHint() { return hint; }
public String getProgressionMechanicId() { return progressionMechanicId; }
public String getProgressionKind() { return progressionKind; }
public String getProgressionLabel() { return progressionLabel; }
public String getProgressionSingularLabel() { return progressionSingularLabel; }
public String getProgressionPluralLabel() { return progressionPluralLabel; }
public int getProgressionMaxActive() { return progressionMaxActive; }
public String getQuestCode() { return questCode; }
public String getQuestGiverProfession() { return questGiverProfession; }
public long getQuestCooldownSeconds() { return questCooldownSeconds; }
public QuestScenarioContract getQuestContract() { return questContract; }
```

**Recomandare:** Considerați folosirea Java Records (Java 14+) sau Lombok @Getter pentru a reduce boilerplate-ul.

---

## 3. Logică de Reparare Duplicate

### 3.1 Metode de Reparare Duplicate în NPCManager.java

**Metode identificate:**
- `repairDuplicateNPCs(boolean apply)` - linia 1457
- `repairDuplicateLiveNPCEntities(boolean apply)` - linia 1484
- `reconcileDuplicateLiveNPCEntities(String reason)` - linia 1507

**Pattern comun:**
```java
List<String> actions = new ArrayList<>();
List<String> warnings = new ArrayList<>();
List<String> errors = new ArrayList<>();
// ... logică de procesare ...
return new DuplicateRepairResult(..., actions, warnings, errors);
```

**Recomandare:** Extrageți logica comună într-o clasă de bază sau metodă helper pentru gestionarea rapoartelor de reparare.

---

## 4. Metode de Salvare Duplicate

### 4.1 Overload-uri saveNPC în NPCManager.java

```java
public boolean saveNPC(AINPC npc) {
    return saveNPC(npc, true);
}

public boolean saveNPC(AINPC npc, boolean syncFromEntity) {
    if (syncFromEntity) {
        npc.syncLocationFromEntity();
    }
    // ... logică de salvare ...
}
```

**Locații similare:**
- `saveAllNPCs()` / `saveAllNPCs(boolean syncFromEntity)` - liniile 1102-1109

**Recomandare:** Pattern-ul este acceptabil pentru overload-uri, dar verificați dacă există logică duplicată în corpul metodelor.

---

## 5. Metode de Căutare Duplicate

### 5.1 Metode getNPC* în NPCManager.java

```java
public AINPC getNPCByUuid(UUID uuid) { return npcsByUuid.get(uuid); }
public AINPC getNPCByUUID(UUID uuid) { return getNPCByUuid(uuid); }
public AINPC getNPCById(int id) { return npcsById.get(id); }
public AINPC getNPCByName(String name) { /* iterare prin npcsByUuid */ }
```

**Recomandare:** Eliminați duplicatul `getNPCByUUID` - păstrați doar `getNPCByUuid` pentru consistență.

---

## 6. Validare Collection Null/Empty

### 6.1 Pattern în AINPCCommand.java

**Pattern identificat:** `if (collection == null || collection.isEmpty()) { ... }`

**Locații găsite:**
- Linia 2360: `if (metadata == null || metadata.isEmpty())`
- Linia 3294: `if (values == null || values.isEmpty())`
- Linia 5950: `if (scenariosBySelector == null || scenariosBySelector.isEmpty())`
- Linia 6103: `if (mechanics == null || mechanics.isEmpty())`
- Linia 6444: `if (lines == null || lines.isEmpty())`
- Linia 6947: `if (scenariosBySelector == null || scenariosBySelector.isEmpty())`
- Linia 6964: `if (candidates == null || candidates.isEmpty())`

**Recomandare:** Creați o metodă utilitar `CollectionUtils.isNullOrEmpty(Collection<?> collection)`.

---

## 7. Metode Factory Duplicate

### 7.1 Metode createNPC în NPCManager.java

```java
public AINPC createNPC(String name, Location location) {
    return createNPC(name, location, null, null, 30, "male", null);
}

public AINPC createNPCFromPlan(ResolvedNpcSpawnPlan resolvedPlan) {
    // logică similară de creare NPC
}
```

**Recomandare:** Verificați dacă logica de creare poate fi consolidată într-o singură metodă cu parametri opționali.

---

## 8. Metode de Quest Duplicate în ScenarioEngine.java

### 8.1 Overload-uri pentru Quest Actions

**Pattern identificat:** Metode cu și fără parametrul `progressionKind`

```java
public QuestInteractionResult acceptQuest(Player player, AINPC npc) {
    return acceptQuest(player, npc, "");
}

public QuestInteractionResult acceptQuest(Player player, AINPC npc, String progressionKind) {
    // logică principală
}
```

**Metode similare:**
- `declineQuest` - liniile 555-559
- `abandonQuest` - liniile 607-609
- `startQuestManually` - liniile 715-719

**Recomandare:** Pattern-ul este acceptabil pentru overload-uri, dar verificați consistența validărilor.

---

## 9. Metode de Validare în AINPCCommand.java

### 9.1 Metode validate* pentru Quest Audit

**Pattern identificat:** Metode care validează referințe și adaugă erori la raport

```java
private void validateQuestEntryId(AuditReport report, String label, String entryKind, String entryId) {
    if (entryId == null || entryId.isBlank()) {
        report.error(label + " are " + entryKind + " fara ID stabil.");
        return;
    }
    // ... validare suplimentară ...
}

private void validateMaterialReference(AuditReport report, String label, String materialId) {
    if (materialId == null || materialId.isBlank()) {
        report.error(label + " nu are item/material.");
        return;
    }
    // ... validare suplimentară ...
}

private void validateEntityReference(AuditReport report, String label, String entityId) {
    if (entityId == null || entityId.isBlank()) {
        report.error(label + " nu are mob/entity.");
        return;
    }
    // ... validare suplimentară ...
}
```

**Recomandare:** Creați o metodă generică de validare cu parametri pentru tipul de validare.

---

## 10. Metode de Normalizare Duplicate

### 10.1 Metode normalize* în AINPCCommand.java

```java
private String normalizeQuestObjectiveLookupKey(String value) {
    if (value == null || value.isBlank()) {
        return "";
    }
    return value.trim().toLowerCase();
}

private String normalizeQuestStageReference(String value) {
    if (value == null || value.isBlank()) {
        return "";
    }
    return value.trim().toLowerCase();
}
```

**Recomandare:** Consolidați într-o singură metodă `normalizeIdentifier(String value)`.

---

## Rezumat și Recomandări Prioritare

### Prioritate Înaltă
1. **Creați clase utilitar pentru validări comune:**
   - `StringUtils.isNullOrBlank(String)` pentru Java
   - `CollectionUtils.isNullOrEmpty(Collection<?>)`
   - `StringUtils.normalizeIdentifier(String)`

2. **Eliminați metode duplicate:**
   - `getNPCByUUID` → folosiți doar `getNPCByUuid`

3. **Consolidați logica de validare:**
   - Creați o metodă generică pentru validările din audit

### Prioritate Medie
4. **Considerați folosirea Java Records sau Lombok:**
   - Pentru clasele cu mulți getteri/setteri simpli
   - Reduce boilerplate-ul semnificativ

5. **Extrageți pattern-uri comune de raportare:**
   - Clasă de bază pentru rapoarte de reparare
   - Builder pattern pentru construirea rapoartelor

### Prioritate Scăzută
6. **Revizuiți overload-urile de metode:**
   - Verificați dacă toate sunt necesare
   - Considerați parametri opționali sau builder pattern

---

## Statistici

- **Total pattern-uri de validare null/blank găsite:** 80+
- **Total metode getter/setter simple:** 25+ (doar în ScenarioTemplate)
- **Total metode de reparare duplicate:** 3 principale
- **Total metode de normalizare duplicate:** 2+
- **Total metode de validare duplicate:** 3+

**Estimare reducere cod:** 15-20% prin aplicarea recomandărilor de prioritate înaltă.
