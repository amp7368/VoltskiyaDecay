package apple.voltskiya.plugin.ore_regen.sql;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;
import apple.voltskiya.plugin.ore_regen.brush.ActiveBrush;
import apple.voltskiya.plugin.ore_regen.brush.Coords;
import apple.voltskiya.plugin.ore_regen.gui.RegenConfigInstance;
import apple.voltskiya.plugin.ore_regen.player_intervention.BrushAutoAssign;
import apple.voltskiya.plugin.ore_regen.player_intervention.BrushSelection;
import apple.voltskiya.plugin.ore_regen.player_intervention.BrushSelectionBuilder;
import apple.voltskiya.plugin.ore_regen.regen.RegenSectionInfo;
import apple.voltskiya.plugin.ore_regen.regen.RegenSectionInfoBuilder;
import apple.voltskiya.plugin.ore_regen.regen.RegenSectionManager;
import apple.voltskiya.plugin.utils.Pair;
import apple.voltskiya.plugin.utils.Triple;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static apple.voltskiya.plugin.DBNames.BLOCK_NAME;
import static apple.voltskiya.plugin.DBNames.PlayerBlock.WORLD_UUID;
import static apple.voltskiya.plugin.DBNames.Regen.*;

public class DBRegen {
    private static final String GET_NEXT_TOOL_UID = String.format("SELECT max( %s ) FROM %s", TOOL_UID, TOOL_UID_TABLE);
    private static final String INSERT_TOOL_UID_INFO = String.format(
            "INSERT INTO %s ( %s,%s,%s ) VALUES ( ?, ?, ? )",
            TOOL_UID_TABLE,
            TOOL_UID,
            BRUSH_TYPE,
            BRUSH_RADIUS
    );
    private static final String INSERT_TOOL_HOST_BLOCK = String.format(
            "INSERT INTO %s ( %s,%s,%s ) VALUES ( ?, ?, ? )",
            TOOL_TO_HOST_BLOCK_TABLE,
            TOOL_UID,
            BLOCK_NAME,
            BLOCK_COUNT
    );
    private static final String INSERT_TOOL_VEIN_BLOCK = String.format(
            "INSERT INTO %s ( %s,%s,%s,%s ) VALUES ( ?, ?, ?, ? )",
            TOOL_TO_VEIN_BLOCK_TABLE,
            TOOL_UID,
            BLOCK_NAME,
            VEIN_INDEX,
            BLOCK_COUNT
    );
    private static final String INSERT_TOOL_DENSITY_BLOCK = String.format(
            "INSERT INTO %s ( %s,%s,%s ) VALUES ( ?, ?, ? )",
            TOOL_TO_DENSITY_TABLE,
            TOOL_UID,
            BLOCK_NAME,
            BLOCK_COUNT
    );
    private static final String GET_SINGLE_TOOL_INFO = String.format("SELECT * FROM %s WHERE %s = %%d", TOOL_UID_TABLE, TOOL_UID);
    private static final String GET_SINGLE_HOST_BLOCKS = String.format("SELECT * FROM %s WHERE %s = %%d", TOOL_TO_HOST_BLOCK_TABLE, TOOL_UID);
    private static final String INSERT_SECTION_TO_BLOCK = String.format("INSERT INTO %s (%s,%s,%s,%s,%s,%s,%s,%s) VALUES " +
                    " ( %%d, %%d, %%d, %%d, %%d, %%b, %%b, %%d) ON CONFLICT(%s,%s,%s,%s) DO UPDATE SET %s = %%d, %s = %%b", SECTION_TO_BLOCK_TABLE,
            TOOL_UID, X, Y, Z, WORLD_UUID, IS_MARKED, IS_ORE, BLOCK_NAME, X, Y, Z, WORLD_UUID, TOOL_UID, IS_MARKED);
    private static final String GET_MARKED_BLOCKS_OF_TOOL = String.format("SELECT * FROM %s WHERE %s = %%d AND %s = %%b", SECTION_TO_BLOCK_TABLE, TOOL_UID, IS_MARKED);
    public static final String UPDATE_IS_MARKED = String.format("UPDATE %s SET %s = %%b where %s = %%d", SECTION_TO_BLOCK_TABLE, IS_MARKED, TOOL_UID);

    private static final String GET_HOST_BLOCKS = String.format("SELECT * FROM %s", TOOL_TO_HOST_BLOCK_TABLE);
    private static final String GET_VEIN_BLOCKS = String.format("SELECT * FROM %s", TOOL_TO_VEIN_BLOCK_TABLE);
    private static final String GET_DENSITY_BLOCKS = String.format("SELECT * FROM %s", TOOL_TO_DENSITY_TABLE);
    private static final String GET_SECTIONS = String.format("SELECT * FROM %s", SECTION_INFO_TABLE);
    private static final String GET_MAX_VEIN_INDEX = String.format("SELECT max(%s) FROM %s WHERE %s = %%d AND %s = %%d", VEIN_INDEX, TOOL_TO_VEIN_BLOCK_TABLE, TOOL_UID, BLOCK_NAME);
    private static final String ALL_ADJACENT;

    static {
        StringBuilder allAdjacent = new StringBuilder();
        boolean isFirst = true;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    int i = 0;
                    if (x != 0) i++;
                    if (y != 0) i++;
                    if (z != 0) i++;
                    if (i != 1) continue;
                    if (!isFirst) allAdjacent.append(" UNION ");
                    else isFirst = false;
                    allAdjacent.append(
                            String.format(
                                    " SELECT * FROM %s " +
                                            "WHERE %s = %s.%s + %d " +
                                            "AND %s = %s.%s + %d " +
                                            "AND %s = %s.%s + %d " +
                                            "AND %s = %s.%s ",
                                    SECTION_TO_BLOCK_TABLE,
                                    X, MINE, X, x,
                                    Y, MINE, Y, y,
                                    Z, MINE, Z, z,
                                    WORLD_UUID, MINE, WORLD_UUID
                            )
                    );
                }
            }
        }
        ALL_ADJACENT = String.format("( %s ) ", allAdjacent.toString());
    }

    public static long saveConfig(RegenConfigInstance config) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Save config");
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(GET_NEXT_TOOL_UID);
            long uid = response.next() ? response.getLong(1) + 1 : 1;
            statement.close();
            PreparedStatement preparedStatement = VerifyRegenDB.database.prepareStatement(INSERT_TOOL_UID_INFO);
            preparedStatement.setLong(1, uid);
            preparedStatement.setString(2, config.brushType.name());
            preparedStatement.setInt(3, config.brushRadius);
            preparedStatement.execute();

            preparedStatement.close();
            for (Map.Entry<Integer, Integer> block : config.getHostBlockToCount()) {
                preparedStatement = VerifyRegenDB.database.prepareStatement(INSERT_TOOL_HOST_BLOCK);
                preparedStatement.setLong(1, uid);
                preparedStatement.setInt(2, block.getKey());
                preparedStatement.setInt(3, block.getValue());
                preparedStatement.execute();
                preparedStatement.close();
            }
            preparedStatement.close();

            for (Map.Entry<Integer, List<Integer>> block : config.getVeinSizeBlockToCount()) {
                statement = VerifyRegenDB.database.createStatement();
                int index = statement.executeQuery(String.format(GET_MAX_VEIN_INDEX, uid, block.getKey())).getInt(1) + 1;
                statement.close();
                for (Integer count : block.getValue()) {
                    preparedStatement = VerifyRegenDB.database.prepareStatement(INSERT_TOOL_VEIN_BLOCK);
                    preparedStatement.setLong(1, uid);
                    preparedStatement.setInt(2, block.getKey());
                    preparedStatement.setInt(3, index++);
                    preparedStatement.setInt(4, count);
                    preparedStatement.execute();
                    preparedStatement.close();
                }
            }

            for (Map.Entry<Integer, Integer> block : config.getDensityDistributionBlockToCount()) {
                preparedStatement = VerifyRegenDB.database.prepareStatement(INSERT_TOOL_DENSITY_BLOCK);
                preparedStatement.setLong(1, uid);
                preparedStatement.setInt(2, block.getKey());
                preparedStatement.setInt(3, block.getValue());
                preparedStatement.execute();
                preparedStatement.close();
            }

            return uid;
        }
    }

    @Nullable
    public static ActiveBrush getBrush(long uid) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Get brush");
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(GET_SINGLE_TOOL_INFO, uid));
            if (response.isClosed()) return null;
            RegenConfigInstance.BrushType brushType = RegenConfigInstance.BrushType.valueOf(response.getString(BRUSH_TYPE));
            int radius = response.getInt(BRUSH_RADIUS);
            response.close();

            Map<Integer, Integer> hostBlocksUid = new HashMap<>();
            response = statement.executeQuery(String.format(GET_SINGLE_HOST_BLOCKS, uid));
            while (response.next()) {
                hostBlocksUid.put(
                        response.getInt(BLOCK_NAME),
                        response.getInt(BLOCK_COUNT)
                );
            }
            statement.close();
            Map<Material, Integer> hostBlocks = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : hostBlocksUid.entrySet())
                hostBlocks.put(DBUtils.getBlockName(entry.getKey()), entry.getValue());

            return new ActiveBrush(uid, brushType, radius, Material.EMERALD_BLOCK, hostBlocks);
        }
    }

    public static void findRegenOre(long uid, RegenSectionInfo.OreVein[] oreChoices) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "FindRegenOre");
        Runnable runAfter;
        synchronized (VerifyDecayDB.syncDB) {
            int air = DBUtils.getMyAir();
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(
                    String.format(
                            "SELECT *\n" +
                                    "FROM %s as %s\n" +
                                    "WHERE %s.%s = false " +
                                    "AND NOT %s = %%d\n" +
                                    "AND %s = %%d\n" +
                                    "ORDER BY random()\n" +
                                    "LIMIT %%d",
                            SECTION_TO_BLOCK_TABLE,
                            MINE,
                            MINE,
                            IS_ORE,
                            BLOCK_NAME,
                            TOOL_UID
                    ), air, uid, oreChoices.length
            ));

            int i = 0;
            while (response.next()) {
                if (oreChoices.length == i || oreChoices[i] == null) break;
                int x = response.getInt(X);
                int y = response.getInt(Y);
                int z = response.getInt(Z);
                int myWorldUid = response.getInt(WORLD_UUID);
                int blockType = response.getInt(BLOCK_NAME);
                oreChoices[i++].setCoords(x, y, z, myWorldUid, blockType);
            }
            statement.close();
            for (RegenSectionInfo.OreVein oreChoice : oreChoices) {
                if (oreChoice != null) oreChoice.updateWorldAndBlock();
            }
            runAfter = updateSectionInfoDB(uid);
        }
        runAfter.run();
    }

    public static void findRegenAir(long uid, RegenSectionInfo.OreVein[] oreChoices) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "FindRegenAir");
        synchronized (VerifyDecayDB.syncDB) {
            int air = DBUtils.getMyAir();
            Statement statement = VerifyRegenDB.database.createStatement();
            final String sql = String.format(
                    String.format(
                            "SELECT *\n" +
                                    "FROM %s as %s\n" +
                                    "WHERE %s.%s = %%d\n" +
                                    "  AND %s = %%d\n" +
                                    "  AND (SELECT count(*) != 0\n" +
                                    "       FROM %s as nearby\n" +
                                    "       WHERE  nearby.%s != %%d\n" +
                                    "       LIMIT 1\n" +
                                    ")\n" +
                                    "ORDER BY random()\n" +
                                    "LIMIT %%d",
                            SECTION_TO_BLOCK_TABLE,
                            MINE,
                            MINE,
                            BLOCK_NAME,
                            TOOL_UID,
                            ALL_ADJACENT,
                            BLOCK_NAME
                    ), air, uid, air, oreChoices.length
            );
            File f = new File(VoltskiyaPlugin.get().getDataFolder(), "hi.txt");
            try {
                Writer writer = new BufferedWriter(new FileWriter(f));
                writer.write(sql);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ResultSet response = statement.executeQuery(sql);
            int index = 0;
            while (response.next()) {
                System.out.println("response.next");
                if (index == oreChoices.length || oreChoices[index] == null) break;
                oreChoices[index++].setCoords(
                        response.getInt(X),
                        response.getInt(Y),
                        response.getInt(Z),
                        response.getInt(WORLD_UUID),
                        air
                );
                System.out.println(
                        response.getInt(X) + ", " +
                                response.getInt(Y) + ", " +
                                response.getInt(Z) + ", " +
                                response.getInt(WORLD_UUID));
            }
            statement.close();
            for (RegenSectionInfo.OreVein oreChoice : oreChoices) {
                if (oreChoice != null) oreChoice.updateWorldAndBlock();
            }
        }
    }

    public static void setMarked(Map<Long, List<Coords.CoordsWithUID>> allCoords, boolean marking) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "SetMarked");
        List<Runnable> runAfter = new ArrayList<>();
        synchronized (VerifyDecayDB.syncDB) {
            VerifyRegenDB.database.setAutoCommit(false);
            Statement statement = VerifyRegenDB.database.createStatement();

            Map<Pair<Long, Material>, Integer> changeInBlockCount = new HashMap<>();
            for (Map.Entry<Long, List<Coords.CoordsWithUID>> tool : allCoords.entrySet()) {
                long uid = tool.getKey();
                for (Coords.CoordsWithUID coords : tool.getValue()) {
                    statement.addBatch(String.format(INSERT_SECTION_TO_BLOCK,
                            uid,
                            coords.x,
                            coords.y,
                            coords.z,
                            coords.myWorldUID,
                            marking,
                            false, // not ore
                            coords.myLastBlockUid,
                            uid,
                            marking
                    ));
                    changeInBlockCount.compute(
                            new Pair<>(uid, coords.lastBlock),
                            (k, v) -> v == null ? 1 : v + 1
                    );
                }
            }
            statement.executeBatch();
            statement.close();
            VerifyRegenDB.database.commit();
            VerifyRegenDB.database.setAutoCommit(true);
            for (Pair<Long, Material> uid : changeInBlockCount.keySet()) {
                runAfter.add(updateSectionInfoDB(uid.getKey()));
            }
        }
        for (Runnable run : runAfter) {
            run.run();
        }
    }

    /**
     * @param toolUid the uid of the tool to update the section for
     * @return returns the updateSections method because it needs to be called out of sync
     * @throws SQLException just because databases
     */
    @CheckReturnValue
    private static Runnable updateSectionInfoDB(long toolUid) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Update Section Info");
        Map<Material, Integer> blocks = new HashMap<>();
        synchronized (VerifyDecayDB.syncDB) {
            Map<Integer, Integer> myBlockUids = new HashMap<>();
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(
                    String.format(
                            "SELECT %s, count(*) AS c " +
                                    "FROM %s " +
                                    "WHERE %s = %%d " +
                                    "AND %s = FALSE " +
                                    "GROUP BY %s",
                            BLOCK_NAME, SECTION_TO_BLOCK_TABLE, TOOL_UID, IS_MARKED, BLOCK_NAME
                    ),
                    toolUid
            ));
            while (response.next()) {
                myBlockUids.put(response.getInt(BLOCK_NAME),
                        response.getInt("c"));
            }
            for (Map.Entry<Integer, Integer> block : myBlockUids.entrySet()) {
                statement.execute(
                        String.format(
                                String.format(
                                        "INSERT INTO %s (%s, %s, %s)\n" +
                                                "VALUES (%%d, %%d, %%d)\n" +
                                                "ON CONFLICT (%s,%s)\n" +
                                                "    DO UPDATE\n" +
                                                "    SET %s = %%d",
                                        SECTION_INFO_TABLE,
                                        TOOL_UID,
                                        BLOCK_NAME,
                                        BLOCK_COUNT,
                                        TOOL_UID,
                                        BLOCK_NAME,
                                        BLOCK_COUNT
                                ), toolUid, block.getKey(), block.getValue(), block.getValue()
                        )
                );
            }
            statement.close();
            for (Map.Entry<Integer, Integer> block : myBlockUids.entrySet())
                blocks.put(DBUtils.getBlockName(block.getKey()), block.getValue());
        }
        return () -> RegenSectionManager.updateSectionInfo(toolUid, blocks);
    }

    public static void setBlockToRegen(long uid, int x, int y, int z, UUID worldUID, Material blockType, Material
            oldBlockType, boolean isOre) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Set single block to regen");
        synchronized (VerifyDecayDB.syncDB) {
            int myNewBlockUid = DBUtils.getMyBlockUid(blockType);
            int myOldBlockUid = DBUtils.getMyBlockUid(oldBlockType);
            int myWorldUid = DBUtils.getMyWorldUid(worldUID.toString());
            Statement statement = VerifyRegenDB.database.createStatement();
            statement.execute(String.format(
                    String.format(
                            "INSERT INTO %s (%s, %s, %s) " +
                                    "VALUES (%%d, %%d, 1) " +
                                    "ON CONFLICT (%s, %s) DO UPDATE SET %s = %s + 1",
                            SECTION_INFO_TABLE, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, BLOCK_COUNT
                    ), uid, myNewBlockUid
            ));
            statement.execute(String.format(
                    String.format(
                            "INSERT INTO %s (%s, %s, %s) " +
                                    "VALUES (%%d, %%d, -1) " +
                                    "ON CONFLICT (%s, %s) DO UPDATE SET %s = %s - 1",
                            SECTION_INFO_TABLE, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, BLOCK_COUNT
                    ), uid, myOldBlockUid
            ));
            statement.execute(String.format(
                    String.format("UPDATE %s\n" +
                                    "SET %s = false, %s = %%b, %s = %%d,  %s = %%d\n" +
                                    "WHERE %s = %%d AND %s = %%d AND %s = %%d AND %s = %%d",
                            SECTION_TO_BLOCK_TABLE,
                            IS_MARKED,
                            IS_ORE,
                            TOOL_UID,
                            BLOCK_NAME,
                            X,
                            Y,
                            Z,
                            WORLD_UUID
                    ),
                    isOre,
                    uid,
                    myNewBlockUid,
                    x,
                    y,
                    z,
                    myWorldUid
            ));
            statement.close();
        }
    }

    /**
     * set the designated coords from the oldBlock to the newBlock in the database and update SectionInfo
     *
     * @param blocksToUpdate the blocks to update in the DB
     * @return the blocks that don't belong to a section
     * @throws SQLException shouldn't be thrown
     */
    @NotNull
    public static List<Coords> setBlock(Collection<Coords> blocksToUpdate) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "set Block");
        List<Coords> blocksToAssign = new ArrayList<>();
        List<Runnable> updateSectionsRunnables = new ArrayList<>();
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            Set<Integer> toolIdsToRecount = new HashSet<>();
            for (Coords blockToUpdate : blocksToUpdate) {
                int myNewBlockUid = DBUtils.getMyBlockUid(blockToUpdate.newBlock);
                int myWorldUid = DBUtils.getMyWorldUid(blockToUpdate.worldUID.toString());
                ResultSet response = statement.executeQuery(
                        String.format(
                                String.format("SELECT %s FROM %s " +
                                                "WHERE %s = %%d AND %s = %%d AND %s = %%d AND %s = %%d",
                                        TOOL_UID, SECTION_TO_BLOCK_TABLE, X, Y, Z, WORLD_UUID
                                ), blockToUpdate.x, blockToUpdate.y, blockToUpdate.z, myWorldUid
                        )
                );
                if (response.isClosed()) {
                    blocksToAssign.add(blockToUpdate);
                    continue;
                }
                int toolUid = response.getInt(TOOL_UID);
                toolIdsToRecount.add(toolUid);
                statement.execute(String.format(
                        String.format("UPDATE %s\n" +
                                        "SET %s = false, %s = %%d\n" +
                                        "WHERE %s = %%d AND %s = %%d AND %s = %%d AND %s = %%d",
                                SECTION_TO_BLOCK_TABLE,
                                IS_ORE,
                                BLOCK_NAME,
                                X,
                                Y,
                                Z,
                                WORLD_UUID
                        ),
                        myNewBlockUid,
                        blockToUpdate.x,
                        blockToUpdate.y,
                        blockToUpdate.z,
                        myWorldUid
                ));
                statement.close();

            }// update the toolIds section info
            for (Integer toolId : toolIdsToRecount) {
                updateSectionsRunnables.add(updateSectionInfoDB(toolId));
            }
        }
        for (Runnable update : updateSectionsRunnables) {
            update.run();
        }
        return blocksToAssign;
    }

    public static List<Coords.CoordsWithUID> getAndUpdateMarking(long uid, boolean marking) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Get and Update Marking");
        Runnable runAfter;
        List<Coords.CoordsWithUID> coords = new ArrayList<>();
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(GET_MARKED_BLOCKS_OF_TOOL, uid, !marking));
            while (response.next()) {
                int x = response.getInt(X);
                int y = response.getInt(Y);
                int z = response.getInt(Z);
                int worldUid = response.getInt(WORLD_UUID);
                int block = response.getInt(BLOCK_NAME);
                coords.add(new Coords.CoordsWithUID(x, y, z, worldUid, Material.EMERALD_BLOCK, block));
            }
            response.close();
            statement.execute(String.format(UPDATE_IS_MARKED, marking, uid));
            statement.close();
            for (Coords.CoordsWithUID coord : coords)
                coord.updateBlockAndWorld();
            runAfter = updateSectionInfoDB(uid);
        }
        runAfter.run();
        return coords;
    }

    public static Set<RegenSectionInfo> getSections() throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Get Sections");
        Map<Long, RegenSectionInfoBuilder> regenSectionInfoBuilders = new HashMap<>();
        Set<RegenSectionInfo> regenSectionInfos = new HashSet<>();
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(GET_SECTIONS);
            while (response.next()) {
                long uid = response.getLong(TOOL_UID);
                if (!regenSectionInfoBuilders.containsKey(uid))
                    regenSectionInfoBuilders.put(uid, new RegenSectionInfoBuilder(uid));
                regenSectionInfoBuilders.get(uid).addActualBlockCount(response.getInt(BLOCK_NAME), response.getInt(BLOCK_COUNT));
            }
            response = statement.executeQuery(GET_HOST_BLOCKS);
            while (response.next()) {
                long uid = response.getLong(TOOL_UID);
                if (!regenSectionInfoBuilders.containsKey(uid))
                    regenSectionInfoBuilders.put(uid, new RegenSectionInfoBuilder(uid));
                regenSectionInfoBuilders.get(uid).addHostBlock(response.getInt(BLOCK_NAME), response.getInt(BLOCK_COUNT));
            }
            response = statement.executeQuery(GET_VEIN_BLOCKS);
            while (response.next()) {
                long uid = response.getLong(TOOL_UID);
                if (!regenSectionInfoBuilders.containsKey(uid))
                    regenSectionInfoBuilders.put(uid, new RegenSectionInfoBuilder(uid));
                regenSectionInfoBuilders.get(uid).addVeinSizes(response.getInt(BLOCK_NAME), response.getInt(BLOCK_COUNT));
            }
            response = statement.executeQuery(GET_DENSITY_BLOCKS);
            while (response.next()) {
                long uid = response.getLong(TOOL_UID);
                if (!regenSectionInfoBuilders.containsKey(uid))
                    regenSectionInfoBuilders.put(uid, new RegenSectionInfoBuilder(uid));
                regenSectionInfoBuilders.get(uid).addDesiredOreCount(response.getInt(BLOCK_NAME), response.getInt(BLOCK_COUNT));
            }
            statement.close();
            for (RegenSectionInfoBuilder builder : regenSectionInfoBuilders.values())
                regenSectionInfos.add(builder.build());
        }
        return regenSectionInfos;
    }

    public static List<Coords.CoordsWithUID> destroyBlocks(long toolUid) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Destroy blocks");
        List<Coords.CoordsWithUID> coords = new ArrayList<>();
        Runnable runAfter;
        synchronized (VerifyDecayDB.syncDB) {
            int air = DBUtils.getMyAir();
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(String.format(
                    "SELECT * FROM %s WHERE %s = %%d", SECTION_TO_BLOCK_TABLE, TOOL_UID), toolUid));
            while (response.next()) {
                coords.add(new Coords.CoordsWithUID(response.getInt(X), response.getInt(Y),
                        response.getInt(Z), response.getInt(WORLD_UUID), null, response.getInt(BLOCK_NAME)));
            }
            statement.execute(String.format(String.format("UPDATE %s\n" +
                    "SET %s = %%d\n" +
                    "WHERE %s = %%d;", SECTION_TO_BLOCK_TABLE, BLOCK_NAME, TOOL_UID), air, toolUid));
            statement.close();
            for (Coords.CoordsWithUID coord : coords) {
                coord.updateBlockAndWorld();
            }
            runAfter = updateSectionInfoDB(toolUid);
        }
        runAfter.run();
        return coords;
    }

    public static List<Coords> getNotAlreadyAssignedFromList(List<Coords> blocks) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Get not already assigned");
        Set<Coords> notAlreadyAssigned = new HashSet<>(blocks);
        synchronized (VerifyDecayDB.syncDB) {
            StringBuilder sql = new StringBuilder();
            boolean isFirst = true;
            for (Coords block : blocks) {
                if (!isFirst) sql.append(" OR ");
                else isFirst = false;
                sql.append(String.format(String.format(
                        "(%s = %%d AND %s = %%d AND %s = %%d AND %s = %%d)",
                        X, Y, Z, WORLD_UUID),
                        block.x, block.y, block.z, DBUtils.getMyWorldUid(block.worldUID.toString())
                ));
            }
            Statement statement = VerifyRegenDB.database.createStatement();
            final String s = String.format("SELECT * FROM %s WHERE %s", SECTION_TO_BLOCK_TABLE, sql.toString());
            ResultSet response = statement.executeQuery(s);
            Set<Pair<Integer, Triple<Integer, Integer, Integer>>> xyzs = new HashSet<>();
            while (response.next()) {
                final int world = response.getInt(WORLD_UUID);
                xyzs.add(new Pair<>(world, new Triple<>(
                        response.getInt(X),
                        response.getInt(Y),
                        response.getInt(Z)
                )));
            }
            statement.close();
            for (Pair<Integer, Triple<Integer, Integer, Integer>> xyz : xyzs) {
                notAlreadyAssigned.remove(new Coords(
                        xyz.getValue().getX(), xyz.getValue().getY(), xyz.getValue().getZ(),
                        DBUtils.getRealWorldUid(xyz.getKey()), null, null
                ));
            }
            return new ArrayList<>(notAlreadyAssigned);
        }
    }

    public static List<BrushSelection> getNearbyCenters(Coords coord, int radiusToLook) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Get nearby centers");
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            final String getMeanSql = String.format(String.format(
                    "SELECT ifnull(sum(%s) / count(%s), -1) AS %s,\n" +
                            "                ifnull(sum(%s) / count(%s), -1) AS %s,\n" +
                            "                ifnull(sum(%s) / count(%s), -1) AS %s,\n" +
                            "                ifnull(%s,-1) AS %s\n" +
                            "         FROM %s\n" +
                            "         WHERE %s in (\n" +
                            "             SELECT DISTINCT %s\n" +
                            "             FROM %s\n" +
                            "             WHERE %s = %%d\n" +
                            "               AND %s BETWEEN %%d AND %%d\n" +
                            "               AND %s BETWEEN %%d AND %%d\n" +
                            "               AND %s BETWEEN %%d AND %%d\n" +
                            "         )",
                    X, X, X, Y, Y, Y, Z, Z, Z, TOOL_UID, TOOL_UID,
                    SECTION_TO_BLOCK_TABLE, TOOL_UID,
                    TOOL_UID, SECTION_TO_BLOCK_TABLE,
                    WORLD_UUID, X, Y, Z
                    ), DBUtils.getMyWorldUid(coord.worldUID.toString()),
                    coord.x - radiusToLook, coord.x + radiusToLook,
                    coord.y - radiusToLook, coord.y + radiusToLook,
                    coord.z - radiusToLook, coord.z + radiusToLook
            );
            ResultSet response = statement.executeQuery(getMeanSql);
            Map<Integer, BrushSelection> selections = new HashMap<>();
            while (response.next()) {
                final int toolUid = response.getInt(TOOL_UID);
                if (toolUid != -1)
                    selections.put(toolUid, new BrushSelection(
                            response.getInt(X),
                            response.getInt(Y),
                            response.getInt(Z),
                            toolUid
                    ));
            }
            response.close();
            response = statement.executeQuery(String.format(
                    "SELECT me.%s,\n" +
                            "sqrt(sum(power(center.%s - me.%s, 2)) / count(me.%s)) AS sdx,\n" +
                            "       sqrt(sum(power(center.%s - me.%s, 2)) / count(me.%s)) AS sdy,\n" +
                            "       sqrt(sum(power(center.%s - me.%s, 2)) / count(me.%s)) AS sdz \n" +
                            "FROM (%s) as center\n" +
                            "         INNER JOIN %s AS me ON center.%s = me.%s\n" +
                            "GROUP BY me.%s;",
                    TOOL_UID,
                    X, X, X, Y, Y, Y, Z, Z, Z,
                    getMeanSql,
                    SECTION_TO_BLOCK_TABLE, TOOL_UID, TOOL_UID, TOOL_UID
            ));
            while (response.next()) {
                selections.get(response.getInt(TOOL_UID)).addStandardDeviation(
                        response.getDouble("sdx"),
                        response.getDouble("sdy"),
                        response.getDouble("sdz")
                );
            }
            response.close();
            statement.close();
            return new ArrayList<>(selections.values());
        }
    }

    public static void removeAlreadySelected(List<BlockState> blocks) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Blocks");
        synchronized (VerifyDecayDB.syncDB) {
            List<String> sql = new ArrayList<>();
            final int size = blocks.size();
            for (int i = 0; i < size; i += 50) {
                sql.add(
                        String.format("SELECT * FROM (%s)",
                                blocks.subList(i, Math.min(size, i + 50)).stream().map(block -> {
                                    int worldUid;
                                    try {
                                        worldUid = DBUtils.getMyWorldUid(block.getWorld().getUID().toString());
                                    } catch (SQLException throwables) {
                                        return " SELECT NULL ";
                                    }
                                    return String.format(String.format("SELECT *, count(*) != 0 as ct\n" +
                                                    "FROM %s\n" +
                                                    "WHERE %s = %%d\n" +
                                                    "  AND %s = %%d\n" +
                                                    "  AND %s = %%d\n" +
                                                    "  AND %s = %%d",
                                            SECTION_TO_BLOCK_TABLE,
                                            X,
                                            Y,
                                            Z,
                                            WORLD_UUID
                                    ), block.getX(), block.getY(), block.getZ(), worldUid);
                                }).collect(Collectors.joining(" UNION "))
                        )
                );
            }

            Statement statement = VerifyRegenDB.database.createStatement();
            List<Pair<Triple<Integer, Integer, Integer>, Integer>> coordsOfSelected = new ArrayList<>();
            for (String s : sql) {
                ResultSet response = statement.executeQuery(s);
                while (response.next()) {
                    if (response.getBoolean("ct"))
                        coordsOfSelected.add(
                                new Pair<>(
                                        new Triple<>(
                                                response.getInt(X),
                                                response.getInt(Y),
                                                response.getInt(Z)
                                        ), response.getInt(WORLD_UUID)
                                )
                        );
                }
            }
            statement.close();
            HashMap<Triple<Integer, Integer, Integer>, UUID> coordsOfSelectedWithWorld = new HashMap<>();
            for (Pair<Triple<Integer, Integer, Integer>, Integer> coords : coordsOfSelected) {
                coordsOfSelectedWithWorld.put(
                        coords.getKey(), DBUtils.getRealWorldUid(coords.getValue()));
            }
            blocks.removeIf(block -> {
                UUID coords = coordsOfSelectedWithWorld.get(new Triple<>(block.getX(), block.getY(), block.getZ()));
                return coords != null && block.getWorld().getUID().equals(coords);
            });
        }
    }

    public static void insertNewSelection(BrushSelectionBuilder selection) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Insert new selection");
        synchronized (VerifyDecayDB.syncDB) {
            List<String> insertSql = new ArrayList<>();
            insertSql.add(String.format(
                    "INSERT INTO %s (%s, %s, %s)\n" +
                            "VALUES (%%d, 'NO_BRUSH', -1)", TOOL_UID_TABLE, TOOL_UID, BRUSH_TYPE, BRUSH_RADIUS));
            insertSql.addAll(selection.getToolToDensity());
            insertSql.addAll(selection.getToolToVein());
            insertSql.addAll(selection.getToolToHost());
            insertSql.addAll(selection.getBlocks());
            Runnable updateSection;
            synchronized (VerifyDecayDB.syncDB) {
                VerifyRegenDB.database.setAutoCommit(false);
                Statement statement = VerifyRegenDB.database.createStatement();
                ResultSet response = statement.executeQuery(String.format(
                        "SELECT max(%s) + 1\n" +
                                "FROM %s;", TOOL_UID, TOOL_UID_TABLE));
                int toolId = response.getInt(1);
                for (String sql : insertSql) {
                    statement.addBatch(String.format(sql, toolId));
                }
                statement.executeBatch();
                statement.close();
                VerifyRegenDB.database.commit();
                VerifyRegenDB.database.setAutoCommit(true);
                updateSection = updateSectionInfoDB(toolId);
            }
            updateSection.run();
        }
    }

    public static void addToSelection(int toolUid, List<Coords.CoordsWithUID> blocks) throws SQLException {
        System.out.println("adding to selection");
        synchronized (VerifyDecayDB.syncDB) {
            VerifyRegenDB.database.setAutoCommit(false);
            Statement statement = VerifyRegenDB.database.createStatement();
            List<Pair<Coords.CoordsWithUID, Integer>> blockToNewBlockUid = new ArrayList<>(blocks.size());
            for (Coords.CoordsWithUID block : blocks) {
                blockToNewBlockUid.add(new Pair<>(block, DBUtils.getMyBlockUid(block.newBlock)));
            }
            for (Pair<Coords.CoordsWithUID, Integer> block : blockToNewBlockUid) {
                statement.addBatch(String.format(String.format(
                        "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s)\n" +
                                "VALUES (%%d,%%d,%%d,%%d,%%d,%%b,%%b,%%d) ON CONFLICT DO NOTHING",
                        SECTION_TO_BLOCK_TABLE, TOOL_UID, X, Y, Z, WORLD_UUID, IS_MARKED, IS_ORE, BLOCK_NAME
                        ),
                        toolUid,
                        block.getKey().x,
                        block.getKey().y,
                        block.getKey().z,
                        block.getKey().myWorldUID,
                        false,
                        BrushAutoAssign.ORE_LIKE_MATERIALS.contains(block.getKey().lastBlock),
                        block.getValue()));
            }
            statement.executeBatch();
            statement.close();
            VerifyRegenDB.database.commit();
            VerifyRegenDB.database.setAutoCommit(true);
        }
    }

    public static List<Coords> getAllBlocks(int toolUid) throws SQLException {
        DBLogging.log(DBLogging.DBRegen, "Get all blocks");
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery("SELECT * FROM main.section_to_block WHERE tool_uid = " + toolUid);
            List<Coords> coords = new ArrayList<>();
            while (response.next()) {
                coords.add(new Coords(response.getInt(X), response.getInt(Y), response.getInt(Z),
                        DBUtils.getRealWorldUid(response.getInt(WORLD_UUID)), null, DBUtils.getBlockName(response.getInt(BLOCK_NAME))));
            }
            statement.close();
            return coords;
        }
    }
}
