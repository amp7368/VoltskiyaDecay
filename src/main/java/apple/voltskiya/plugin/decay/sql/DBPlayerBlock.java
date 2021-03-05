package apple.voltskiya.plugin.decay.sql;

import apple.voltskiya.plugin.decay.DataPlayerBlock;
import apple.voltskiya.plugin.decay.DecayModifiers;
import apple.voltskiya.plugin.decay.PluginDecay;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static apple.voltskiya.plugin.decay.PluginDecay.DECAY_PERCENTAGE;
import static apple.voltskiya.plugin.decay.sql.DBNames.PlayerBlock.*;

public class DBPlayerBlock {

    private static final String sqlDetermineStrength = String.format(
            "SELECT ifnull(sum(%s),0) \n" +
                    "FROM player_block\n" +
                    "WHERE %s BETWEEN %%d AND %%d\n" +
                    "  AND %s BETWEEN %%d AND %%d\n" +
                    "  AND %s BETWEEN %%d AND %%d",
            MY_STRENGTH,
            X,
            Y,
            Z
    );
    private static final String sqlPlaceBlock = String.format(
            "INSERT INTO %s ( %s,%s,%s,%s,%s,%s,%s,%s) VALUES ( %%d,%%d,%%d,%%s,%%d,( %%s ),%%s,%%s);",
            PLAYER_BLOCK,
            X,
            Y,
            Z,
            BLOCK,
            MY_STRENGTH,
            EFFECTIVE_STRENGTH,
            OWNER,
            WORLD_UUID
    );
    private static final String sqlUpdateStrength = String.format(
            "UPDATE %s\n" +
                    "SET %s = %s + %%s\n" +
                    "WHERE %s BETWEEN %%d AND %%d\n" +
                    "  AND %s BETWEEN %%d AND %%d\n" +
                    "  AND %s BETWEEN %%d AND %%d",
            PLAYER_BLOCK,
            EFFECTIVE_STRENGTH,
            EFFECTIVE_STRENGTH,
            X,
            Y,
            Z
    );
    private static final String SQL_GET_STRENGTH = String.format(
            " (SELECT %s FROM %s WHERE %s = %%d AND %s = %%d AND %s = %%d ) ",
            MY_STRENGTH,
            PLAYER_BLOCK,
            X,
            Y,
            Z
    );

    public static void playerPlace(BlockPlaceEvent event) throws SQLException {
        @NotNull Block blockPlaced = event.getBlockPlaced();
        synchronized (VerifyDecayDB.syncDB) {
            String sql = String.format(sqlPlaceBlock,
                    blockPlaced.getX(),
                    blockPlaced.getY(),
                    blockPlaced.getZ(),
                    "?",
                    DecayModifiers.getResistance(blockPlaced.getBlockData().getMaterial()), //TODO change the strength of blocks
                    String.format(
                            sqlDetermineStrength,
                            blockPlaced.getX() - PluginDecay.SUPPORT_RADIUS,
                            blockPlaced.getX() + PluginDecay.SUPPORT_RADIUS,
                            blockPlaced.getY() - PluginDecay.SUPPORT_RADIUS,
                            blockPlaced.getY() + PluginDecay.SUPPORT_RADIUS,
                            blockPlaced.getZ() - PluginDecay.SUPPORT_RADIUS,
                            blockPlaced.getZ() + PluginDecay.SUPPORT_RADIUS
                    ),
                    "?",
                    "?"
            );
            PreparedStatement statement = VerifyDecayDB.database.prepareStatement(sql);
            statement.setString(1, blockPlaced.getBlockData().getMaterial().toString());
            statement.setString(2, event.getPlayer().getUniqueId().toString());
            statement.setString(3, blockPlaced.getWorld().getUID().toString());
            statement.execute();
            statement.close();

            sql = String.format(
                    sqlUpdateStrength,
                    1, //TODO change the strength of blocks
                    blockPlaced.getX() - PluginDecay.SUPPORT_RADIUS,
                    blockPlaced.getX() + PluginDecay.SUPPORT_RADIUS,
                    blockPlaced.getY() - PluginDecay.SUPPORT_RADIUS,
                    blockPlaced.getY() + PluginDecay.SUPPORT_RADIUS,
                    blockPlaced.getZ() - PluginDecay.SUPPORT_RADIUS,
                    blockPlaced.getZ() + PluginDecay.SUPPORT_RADIUS
            );
            statement = VerifyDecayDB.database.prepareStatement(sql);
            statement.execute();
            statement.close();
        }
    }

    public static void remove(BlockBreakEvent event) throws SQLException {
        @NotNull Block blockPoofed = event.getBlock();
        int x = blockPoofed.getX();
        int y = blockPoofed.getY();
        int z = blockPoofed.getZ();
        remove(x, y, z);
    }

    public static void remove(int x, int y, int z) throws SQLException {
        synchronized (VerifyDecayDB.syncDB) {
            String sql = String.format(
                    sqlUpdateStrength,
                    String.format(
                            SQL_GET_STRENGTH,
                            x,
                            y,
                            z
                    ),
                    x - PluginDecay.SUPPORT_RADIUS,
                    x + PluginDecay.SUPPORT_RADIUS,
                    y - PluginDecay.SUPPORT_RADIUS,
                    y + PluginDecay.SUPPORT_RADIUS,
                    z - PluginDecay.SUPPORT_RADIUS,
                    z + PluginDecay.SUPPORT_RADIUS
            );
            PreparedStatement statement = VerifyDecayDB.database.prepareStatement(sql);
            statement.execute();
            statement.close();
            sql = String.format(
                    "DELETE\n" +
                            "FROM %s\n" +
                            "WHERE %s = %d\n" +
                            "  AND %s = %d\n" +
                            "  AND %s = %d",
                    PLAYER_BLOCK,
                    X,
                    x,
                    Y,
                    y,
                    Z,
                    z
            );
            statement = VerifyDecayDB.database.prepareStatement(sql);
            statement.execute();
            statement.close();

        }
    }

    public static List<DataPlayerBlock> getRandom() throws SQLException {
        synchronized (VerifyDecayDB.syncDB) {
            String sql = String.format("SELECT *\n" +
                    "FROM %s\n" +
                    "WHERE random()< %f;", PLAYER_BLOCK, DECAY_PERCENTAGE);
            PreparedStatement statement = VerifyDecayDB.database.prepareStatement(sql);
            List<DataPlayerBlock> blocks = new ArrayList<>();
            ResultSet response = statement.executeQuery();
            while (response.next()) {
                int x = response.getInt(X);
                int y = response.getInt(Y);
                int z = response.getInt(Z);
                String block = response.getString(BLOCK);
                int myStrength = response.getInt(MY_STRENGTH);
                int effectiveStrength = response.getInt(EFFECTIVE_STRENGTH);
                String owner = response.getString(OWNER);
                String worldUUIDString = response.getString(WORLD_UUID);
                blocks.add(
                        new DataPlayerBlock(
                                x,
                                y,
                                z,
                                Material.getMaterial(block),
                                myStrength,
                                effectiveStrength,
                                owner,
                                worldUUIDString
                        )
                );
            }
            response.close();
            statement.close();
            return blocks;
        }
    }

    public static void update(int x, int y, int z, int changeInDecay, Material nextMaterial) throws SQLException {

        synchronized (VerifyDecayDB.syncDB) {
            String sql = String.format("UPDATE %s\n" +
                            "SET %s = %s - %d,\n" +
                            "    %s = ?\n" +
                            "WHERE %s = %d\n" +
                            "AND %s = %d\n" +
                            "AND %s = %d",
                    PLAYER_BLOCK,
                    MY_STRENGTH,
                    MY_STRENGTH,
                    changeInDecay,
                    BLOCK,
                    X,
                    x,
                    Y,
                    y,
                    Z,
                    z
            );
            PreparedStatement statement = VerifyDecayDB.database.prepareStatement(sql);
            statement.setString(1, nextMaterial.name());
            statement.execute();
            statement.close();

            sql = String.format(
                    "UPDATE %s\n" +
                            "SET %s = %s + %d\n" +
                            "WHERE %s = %d\n" +
                            "  AND %s = %d\n" +
                            "  AND %s = %d",
                    PLAYER_BLOCK,
                    EFFECTIVE_STRENGTH,
                    EFFECTIVE_STRENGTH,
                    changeInDecay,
                    X,
                    x,
                    Y,
                    y,
                    Z,
                    z
            );
            statement = VerifyDecayDB.database.prepareStatement(sql);
            statement.execute();
            statement.close();


            sql = String.format(
                    sqlUpdateStrength,
                    -changeInDecay,
                    x - PluginDecay.SUPPORT_RADIUS,
                    x + PluginDecay.SUPPORT_RADIUS,
                    y - PluginDecay.SUPPORT_RADIUS,
                    y + PluginDecay.SUPPORT_RADIUS,
                    z - PluginDecay.SUPPORT_RADIUS,
                    z + PluginDecay.SUPPORT_RADIUS
            );
            statement = VerifyDecayDB.database.prepareStatement(sql);
            statement.execute();
            statement.close();

        }
    }
}
