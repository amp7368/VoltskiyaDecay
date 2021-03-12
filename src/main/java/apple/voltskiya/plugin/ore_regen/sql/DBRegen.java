package apple.voltskiya.plugin.ore_regen.sql;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;
import apple.voltskiya.plugin.ore_regen.brush.ActiveBrush;
import apple.voltskiya.plugin.ore_regen.brush.Coords;
import apple.voltskiya.plugin.ore_regen.gui.RegenConfigInstance;
import apple.voltskiya.plugin.ore_regen.regen.RegenSectionInfo;
import apple.voltskiya.plugin.ore_regen.regen.RegenSectionInfoBuilder;
import apple.voltskiya.plugin.ore_regen.regen.RegenSectionManager;
import apple.voltskiya.plugin.utils.Pair;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

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
                    " ( %%d, %%d, %%d, %%d, '%%s', %%b, %%b, '%%s') ON CONFLICT(%s,%s,%s,%s) DO UPDATE SET %s = %%d, %s = %%b", SECTION_TO_BLOCK_TABLE,
            TOOL_UID, X, Y, Z, WORLD_UUID, IS_MARKED, IS_ORE, BLOCK_NAME, X, Y, Z, WORLD_UUID, TOOL_UID, IS_MARKED);
    private static final String GET_MARKED_BLOCKS_OF_TOOL = String.format("SELECT * FROM %s WHERE %s = %%d AND %s = %%b", SECTION_TO_BLOCK_TABLE, TOOL_UID, IS_MARKED);
    public static final String UPDATE_IS_MARKED = String.format("UPDATE %s SET %s = %%b where %s = %%d", SECTION_TO_BLOCK_TABLE, IS_MARKED, TOOL_UID);

    private static final String GET_HOST_BLOCKS = String.format("SELECT * FROM %s", TOOL_TO_HOST_BLOCK_TABLE);
    private static final String GET_VEIN_BLOCKS = String.format("SELECT * FROM %s", TOOL_TO_VEIN_BLOCK_TABLE);
    private static final String GET_DENSITY_BLOCKS = String.format("SELECT * FROM %s", TOOL_TO_DENSITY_TABLE);
    private static final String GET_SECTIONS = String.format("SELECT * FROM %s", SECTION_INFO_TABLE);
    private static final String GET_MAX_VEIN_INDEX = String.format("SELECT max(%s) FROM %s WHERE %s = %%d AND %s = '%%s'", VEIN_INDEX, TOOL_TO_VEIN_BLOCK_TABLE, TOOL_UID, BLOCK_NAME);
    private static final String ALL_ADJACENT;

    static {
        StringBuilder allAdjacent = new StringBuilder();
        boolean isFirst = true;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (!isFirst) allAdjacent.append(" INNER JOIN ");
                    else isFirst = false;
                    allAdjacent.append(
                            String.format(
                                    " ( SELECT * FROM %s " +
                                            "WHERE %s = %s.%s + %d " +
                                            "AND %s = %s.%s + %d " +
                                            "AND %s = %s.%s + %d " +
                                            "AND %s = %s.%s ) ",
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
        ALL_ADJACENT = allAdjacent.toString();
    }

    public static long saveConfig(RegenConfigInstance config) throws SQLException {
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

            for (Map.Entry<String, Integer> block : config.getHostBlockToCount()) {
                preparedStatement = VerifyRegenDB.database.prepareStatement(INSERT_TOOL_HOST_BLOCK);
                preparedStatement.setLong(1, uid);
                preparedStatement.setString(2, block.getKey());
                preparedStatement.setInt(3, block.getValue());
                preparedStatement.execute();
            }
            preparedStatement.close();

            for (Map.Entry<String, List<Integer>> block : config.getVeinSizeBlockToCount()) {
                statement = VerifyRegenDB.database.createStatement();
                int index = statement.executeQuery(String.format(GET_MAX_VEIN_INDEX, uid, block.getKey())).getInt(1) + 1;
                statement.close();
                for (Integer count : block.getValue()) {
                    preparedStatement = VerifyRegenDB.database.prepareStatement(INSERT_TOOL_VEIN_BLOCK);
                    preparedStatement.setLong(1, uid);
                    preparedStatement.setString(2, block.getKey());
                    preparedStatement.setInt(3, index++);
                    preparedStatement.setInt(4, count);
                    preparedStatement.execute();
                }
            }

            for (Map.Entry<String, Integer> block : config.getDensityDistributionBlockToCount()) {
                preparedStatement = VerifyRegenDB.database.prepareStatement(INSERT_TOOL_DENSITY_BLOCK);
                preparedStatement.setLong(1, uid);
                preparedStatement.setString(2, block.getKey());
                preparedStatement.setInt(3, block.getValue());
                preparedStatement.execute();
            }

            return uid;
        }
    }

    @Nullable
    public static ActiveBrush getBrush(long uid) throws SQLException {
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(GET_SINGLE_TOOL_INFO, uid));
            if (response.isClosed()) return null;
            RegenConfigInstance.BrushType brushType = RegenConfigInstance.BrushType.valueOf(response.getString(BRUSH_TYPE));
            int radius = response.getInt(BRUSH_RADIUS);
            response.close();

            Map<Material, Integer> hostBlocks = new HashMap<>();
            response = statement.executeQuery(String.format(GET_SINGLE_HOST_BLOCKS, uid));
            while (response.next()) {
                hostBlocks.put(
                        Material.getMaterial(response.getString(BLOCK_NAME)),
                        response.getInt(BLOCK_COUNT)
                );
            }
            statement.close();

            return new ActiveBrush(uid, brushType, radius, Material.EMERALD_BLOCK, hostBlocks);
        }
    }

    public static void regen(long uid, RegenSectionInfo.OreVein[] oreChoices) throws SQLException {
        Runnable runAfter;
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(
                    String.format(
                            "SELECT *\n" +
                                    "FROM %s as %s\n" +
                                    "WHERE %s.%s != 'AIR'\n" +
                                    "ORDER BY random()\n" +
                                    "LIMIT %%d",
                            SECTION_TO_BLOCK_TABLE,
                            MINE,
                            MINE,
                            BLOCK_NAME
                    ), uid, oreChoices.length
            ));

            int i = 0;
            while (response.next()) {
                if (oreChoices.length == i || oreChoices[i] == null) break;
                int x = response.getInt(X);
                int y = response.getInt(Y);
                int z = response.getInt(Z);
                UUID worldUid = UUID.fromString(response.getString(WORLD_UUID));
                Material blockType = Material.valueOf(response.getString(BLOCK_NAME));
                oreChoices[i++].setCoords(x, y, z, worldUid, blockType);
            }
            runAfter = updateSectionInfoDB(uid);
        }
        runAfter.run();
    }

    public static void regenAir(long uid, RegenSectionInfo.OreVein[] oreChoices) throws SQLException {
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            final String sql = String.format(
                    String.format(
                            "SELECT *\n" +
                                    "FROM %s as %s\n" +
                                    "WHERE %s.%s = 'AIR'\n" +
                                    "  AND %s = %%d\n" +
                                    "  AND (SELECT count(*) != 0\n" +
                                    "       FROM %s as nearby\n" +
                                    "       WHERE  nearby.%s != 'AIR'\n" +
                                    "       ORDER BY random()\n" +
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
                    ), uid, oreChoices.length
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
                oreChoices[index++].setCoords(
                        response.getInt(X),
                        response.getInt(Y),
                        response.getInt(Z),
                        UUID.fromString(response.getString(WORLD_UUID)),
                        Material.AIR
                );
            }
        }
    }

    public static void setMarked(Map<Long, List<Coords>> allCoords, boolean marking) throws SQLException {
        List<Runnable> runAfter = new ArrayList<>();
        synchronized (VerifyDecayDB.syncDB) {
            VerifyRegenDB.database.setAutoCommit(false);
            Statement statement = VerifyRegenDB.database.createStatement();

            Map<Pair<Long, Material>, Integer> changeInBlockCount = new HashMap<>();
            for (Map.Entry<Long, List<Coords>> tool : allCoords.entrySet()) {
                long uid = tool.getKey();
                for (Coords coords : tool.getValue()) {
                    statement.addBatch(String.format(INSERT_SECTION_TO_BLOCK,
                            uid,
                            coords.x,
                            coords.y,
                            coords.z,
                            coords.worldUID.toString(),
                            marking,
                            false, // not ore
                            coords.lastBlock.name(),
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
        Map<Material, Integer> blocks = new HashMap<>();
        synchronized (VerifyDecayDB.syncDB) {
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
                blocks.put(Material.valueOf(response.getString(BLOCK_NAME)),
                        response.getInt("c"));
            }
            for (Map.Entry<Material, Integer> block : blocks.entrySet()) {
                statement.execute(
                        String.format(
                                String.format(
                                        "INSERT INTO %s (%s, %s, %s)\n" +
                                                "VALUES (%%d, '%%s', %%d)\n" +
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
                                ), toolUid, block.getKey().name(), block.getValue(), block.getValue()
                        )
                );
            }
        }
        return () -> RegenSectionManager.updateSectionInfo(toolUid, blocks);
    }

    public static void setOre(long uid, int x, int y, int z, UUID worldUID, Material blockType, Material oldBlockType) throws SQLException {
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            statement.execute(String.format(
                    String.format(
                            "INSERT INTO %s (%s, %s, %s) " +
                                    "VALUES (%%d, '%%s', 1) " +
                                    "ON CONFLICT (%s, %s) DO UPDATE SET %s = %s + 1",
                            SECTION_INFO_TABLE, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, BLOCK_COUNT
                    ), uid, blockType.name()
            ));
            statement.execute(String.format(
                    String.format(
                            "INSERT INTO %s (%s, %s, %s) " +
                                    "VALUES (%%d, '%%s', -1) " +
                                    "ON CONFLICT (%s, %s) DO UPDATE SET %s = %s - 1",
                            SECTION_INFO_TABLE, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, BLOCK_COUNT
                    ), uid, oldBlockType.name()
            ));
            statement.execute(String.format(
                    String.format("UPDATE %s\n" +
                                    "SET %s = false, %s = '%%s', %s = true\n" +
                                    "WHERE %s = %%d AND %s = %%d AND %s = %%d AND %s = '%%s' AND %s = '%%s'",
                            SECTION_TO_BLOCK_TABLE,
                            IS_MARKED,
                            BLOCK_NAME,
                            IS_ORE,
                            TOOL_UID,
                            X,
                            Y,
                            Z,
                            WORLD_UUID
                    ),
                    blockType.name(),
                    uid,
                    x,
                    y,
                    z,
                    worldUID
            ));
        }
    }

    public static void setBlock(UUID world, int x, int y, int z, Material oldBlock, Material newBlock) throws SQLException {
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(
                    String.format(
                            String.format("SELECT %s FROM %s " +
                                            "WHERE %s = %%d AND %s = %%d AND %s = %%d AND %s = '%%s'",
                                    TOOL_UID, SECTION_TO_BLOCK_TABLE, X, Y, Z, WORLD_UUID
                            ), x, y, z, world.toString()
                    )
            );
            if (!response.isClosed()) {
                long toolUid = response.getLong(TOOL_UID);
                statement.execute(String.format(
                        String.format("UPDATE %s\n" +
                                        "SET %s = false, %s = '%%s'\n" +
                                        "WHERE %s = %%d AND %s = %%d AND %s = %%d AND %s = '%%s'",
                                SECTION_TO_BLOCK_TABLE,
                                IS_ORE,
                                BLOCK_NAME,
                                X,
                                Y,
                                Z,
                                WORLD_UUID
                        ),
                        newBlock,
                        x,
                        y,
                        z,
                        world.toString()
                ));
                statement.execute(String.format(
                        String.format("INSERT INTO %s (%s, %s, %s)\n" +
                                        "VALUES (%%d, '%%s', -1)\n" +
                                        "ON CONFLICT (%s,%s) DO UPDATE SET %s = %s - 1;\n",
                                SECTION_INFO_TABLE, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, BLOCK_COUNT
                        ), toolUid, oldBlock.name()
                ));
                statement.execute(String.format(
                        String.format("INSERT INTO %s (%s, %s, %s)\n" +
                                        "VALUES (%%d, '%%s', 1)\n" +
                                        "ON CONFLICT (%s,%s) DO UPDATE SET %s = %s + 1;\n",
                                SECTION_INFO_TABLE, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, TOOL_UID, BLOCK_NAME, BLOCK_COUNT, BLOCK_COUNT
                        ), toolUid, newBlock.name()
                ));
            }
        }
    }

    public static List<Coords> getAndUpdateMarking(long uid, boolean marking) throws SQLException {
        Runnable runAfter;
        List<Coords> coords = new ArrayList<>();
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(GET_MARKED_BLOCKS_OF_TOOL, uid, !marking));
            while (response.next()) {
                int x = response.getInt(X);
                int y = response.getInt(Y);
                int z = response.getInt(Z);
                UUID worldUid = UUID.fromString(response.getString(WORLD_UUID));
                Material block = Material.getMaterial(response.getString(BLOCK_NAME));
                coords.add(new Coords(x, y, z, worldUid, Material.EMERALD_BLOCK, block));
            }
            response.close();
            statement.execute(String.format(UPDATE_IS_MARKED, marking, uid));
            statement.close();
            runAfter = updateSectionInfoDB(uid);
        }
        runAfter.run();
        return coords;
    }

    public static Set<RegenSectionInfo> getSections() throws SQLException {
        Map<Long, RegenSectionInfoBuilder> regenSectionInfoBuilders = new HashMap<>();
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(GET_SECTIONS);
            while (response.next()) {
                long uid = response.getLong(TOOL_UID);
                if (!regenSectionInfoBuilders.containsKey(uid))
                    regenSectionInfoBuilders.put(uid, new RegenSectionInfoBuilder(uid));
                regenSectionInfoBuilders.get(uid).addActualBlockCount(response.getString(BLOCK_NAME), response.getInt(BLOCK_COUNT));
            }
            response = statement.executeQuery(GET_HOST_BLOCKS);
            while (response.next()) {
                long uid = response.getLong(TOOL_UID);
                if (!regenSectionInfoBuilders.containsKey(uid))
                    regenSectionInfoBuilders.put(uid, new RegenSectionInfoBuilder(uid));
                regenSectionInfoBuilders.get(uid).addHostBlock(response.getString(BLOCK_NAME), response.getInt(BLOCK_COUNT));
            }
            response = statement.executeQuery(GET_VEIN_BLOCKS);
            while (response.next()) {
                long uid = response.getLong(TOOL_UID);
                if (!regenSectionInfoBuilders.containsKey(uid))
                    regenSectionInfoBuilders.put(uid, new RegenSectionInfoBuilder(uid));
                regenSectionInfoBuilders.get(uid).addVeinSizes(response.getString(BLOCK_NAME), response.getInt(BLOCK_COUNT));
            }
            response = statement.executeQuery(GET_DENSITY_BLOCKS);
            while (response.next()) {
                long uid = response.getLong(TOOL_UID);
                if (!regenSectionInfoBuilders.containsKey(uid))
                    regenSectionInfoBuilders.put(uid, new RegenSectionInfoBuilder(uid));
                regenSectionInfoBuilders.get(uid).addDesiredOreCount(response.getString(BLOCK_NAME), response.getInt(BLOCK_COUNT));
            }
            statement.close();
        }
        Set<RegenSectionInfo> regenSectionInfos = new HashSet<>();
        for (RegenSectionInfoBuilder builder : regenSectionInfoBuilders.values())
            regenSectionInfos.add(builder.build());
        return regenSectionInfos;
    }

    public static List<Coords> destroyBlocks(long toolUid) throws SQLException {
        List<Coords> coords = new ArrayList<>();
        Runnable runAfter;
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(String.format(
                    "SELECT * FROM %s WHERE %s = %%d", SECTION_TO_BLOCK_TABLE, TOOL_UID), toolUid));
            while (response.next()) {
                coords.add(new Coords(response.getInt(X), response.getInt(Y),
                        response.getInt(Z), UUID.fromString(response.getString(WORLD_UUID)), null, null));
            }
            statement.execute(String.format(String.format("UPDATE %s\n" +
                    "SET %s = 'AIR'\n" +
                    "WHERE %s = %%d;", SECTION_TO_BLOCK_TABLE, BLOCK_NAME, TOOL_UID), toolUid));
            runAfter = updateSectionInfoDB(toolUid);
        }
        runAfter.run();
        return coords;
    }
}
