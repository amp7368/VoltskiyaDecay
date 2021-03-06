package apple.voltskiya.plugin.ore_regen.sql;

import apple.voltskiya.plugin.ore_regen.brush.ActiveBrush;
import apple.voltskiya.plugin.ore_regen.brush.Coords;
import apple.voltskiya.plugin.ore_regen.gui.RegenConfigInstance;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

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
            "INSERT INTO %s ( %s,%s,%s ) VALUES ( ?, ?, ? )",
            TOOL_TO_VEIN_BLOCK_TABLE,
            TOOL_UID,
            BLOCK_NAME,
            BLOCK_COUNT
    );
    private static final String INSERT_TOOL_DENSITY_BLOCK = String.format(
            "INSERT INTO %s ( %s,%s,%s ) VALUES ( ?, ?, ? )",
            TOOL_TO_DENSITY_TABLE,
            TOOL_UID,
            BLOCK_NAME,
            BLOCK_COUNT
    );
    private static final String INSERT_TOOL_SECTION_INFO = String.format(
            "INSERT INTO %s ( %s,%s,%s ) VALUES ( ?, ?, ? )",
            SECTION_INFO_TABLE,
            TOOL_UID,
            BLOCK_NAME,
            BLOCK_COUNT
    );
    private static final String GET_TOOL_INFO = String.format("SELECT * FROM %s WHERE %s = %%d", TOOL_UID_TABLE, TOOL_UID);
    private static final String GET_HOST_BLOCKS = String.format("SELECT * FROM %s WHERE %s = %%d", TOOL_TO_HOST_BLOCK_TABLE, TOOL_UID);
    private static final String INSERT_SECTION_TO_BLOCK = String.format("INSERT INTO %s (%s,%s,%s,%s,%s,%s,%s) VALUES" +
            " ( %%d, %%d, %%d, %%d, '%%s', %%b, '%%s' )", SECTION_TO_BLOCK_TABLE, TOOL_UID, X, Y, Z, WORLD_UUID, IS_MARKED, BLOCK_NAME);
    private static final String GET_MARKED_BLOCKS_OF_TOOL = String.format("SELECT * FROM %s WHERE %s = %%d AND %s = %%b", SECTION_TO_BLOCK_TABLE, TOOL_UID, IS_MARKED);
    public static final String UPDATE_IS_MARKED = String.format("UPDATE %s SET %s = %%b where %s = %%d", SECTION_TO_BLOCK_TABLE, IS_MARKED, TOOL_UID);

    public static long saveConfig(RegenConfigInstance config) throws SQLException {
        synchronized (VerifyRegenDB.syncDB) {
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

            for (Map.Entry<String, Integer> block : config.getVeinSizeBlockToCount()) {
                preparedStatement = VerifyRegenDB.database.prepareStatement(INSERT_TOOL_VEIN_BLOCK);
                preparedStatement.setLong(1, uid);
                preparedStatement.setString(2, block.getKey());
                preparedStatement.setInt(3, block.getValue());
                preparedStatement.execute();
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
        synchronized (VerifyRegenDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(GET_TOOL_INFO, uid));
            if (response.isClosed()) return null;
            RegenConfigInstance.BrushType brushType = RegenConfigInstance.BrushType.valueOf(response.getString(BRUSH_TYPE));
            int radius = response.getInt(BRUSH_RADIUS);
            response.close();

            Map<Material, Integer> hostBlocks = new HashMap<>();
            response = statement.executeQuery(String.format(GET_HOST_BLOCKS, uid));
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

    public static void setMarked(Map<Long, List<Coords>> allCoords) throws SQLException {
        synchronized (VerifyRegenDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            for (Map.Entry<Long, List<Coords>> tool : allCoords.entrySet()) {
                long uid = tool.getKey();
                for (Coords coords : tool.getValue()) {
                    statement.addBatch(String.format(INSERT_SECTION_TO_BLOCK,
                            uid,
                            coords.x,
                            coords.y,
                            coords.z,
                            coords.worldUID.toString(),
                            true,
                            coords.lastBlock));
                }
            }
            statement.executeBatch();
            statement.close();
        }
    }

    public static List<Coords> setUnmarked(long uid) throws SQLException {
        synchronized (VerifyRegenDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(GET_MARKED_BLOCKS_OF_TOOL, uid, true));
            List<Coords> coords = new ArrayList<>();
            while (response.next()) {
                int x = response.getInt(X);
                int y = response.getInt(Y);
                int z = response.getInt(Z);
                UUID worldUid = UUID.fromString(response.getString(WORLD_UUID));
                Material block = Material.getMaterial(response.getString(BLOCK_NAME));
                coords.add(new Coords(x, y, z, worldUid, Material.EMERALD_BLOCK, block));
            }
            response.close();
            statement.execute(String.format(UPDATE_IS_MARKED, false, uid));
            statement.close();
            return coords;
        }
    }
}
