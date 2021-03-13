package apple.voltskiya.plugin.decay.sql;

import apple.voltskiya.plugin.decay.DataPlayerBlock;
import apple.voltskiya.plugin.decay.DecayModifiers;
import apple.voltskiya.plugin.ore_regen.PluginOreRegen;
import apple.voltskiya.plugin.ore_regen.sql.VerifyRegenDB;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static apple.voltskiya.plugin.ore_regen.PluginOreRegen.DECAY_PERCENTAGE;
import static apple.voltskiya.plugin.DBNames.PlayerBlock.*;

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

    public static void playerPlace(Set<PlayerBlockMonitor.BlockForDB> blocks) throws SQLException {
        synchronized (VerifyDecayDB.syncDB) {
            VerifyDecayDB.database.setAutoCommit(false);
            final Statement statement = VerifyDecayDB.database.createStatement();
            for (PlayerBlockMonitor.BlockForDB blockPlaced : blocks) {
                String sql = String.format(sqlPlaceBlock,
                        blockPlaced.getX(),
                        blockPlaced.getY(),
                        blockPlaced.getZ(),
                        blockPlaced.getMaterial(),
                        DecayModifiers.getResistance(blockPlaced.getMaterial()), //TODO change the strength of blocks
                        String.format(
                                sqlDetermineStrength,
                                blockPlaced.getX() - PluginOreRegen.SUPPORT_RADIUS,
                                blockPlaced.getX() + PluginOreRegen.SUPPORT_RADIUS,
                                blockPlaced.getY() - PluginOreRegen.SUPPORT_RADIUS,
                                blockPlaced.getY() + PluginOreRegen.SUPPORT_RADIUS,
                                blockPlaced.getZ() - PluginOreRegen.SUPPORT_RADIUS,
                                blockPlaced.getZ() + PluginOreRegen.SUPPORT_RADIUS
                        ),
                        blockPlaced.getOwner(),
                        blockPlaced.getWorldUUID().toString()
                );
                statement.addBatch(sql);
                sql = String.format(
                        sqlUpdateStrength,
                        1, //TODO change the strength of blocks
                        blockPlaced.getX() - PluginOreRegen.SUPPORT_RADIUS,
                        blockPlaced.getX() + PluginOreRegen.SUPPORT_RADIUS,
                        blockPlaced.getY() - PluginOreRegen.SUPPORT_RADIUS,
                        blockPlaced.getY() + PluginOreRegen.SUPPORT_RADIUS,
                        blockPlaced.getZ() - PluginOreRegen.SUPPORT_RADIUS,
                        blockPlaced.getZ() + PluginOreRegen.SUPPORT_RADIUS
                );
                statement.addBatch(sql);
            }
            statement.executeBatch();
            VerifyDecayDB.database.commit();
            VerifyDecayDB.database.setAutoCommit(true);
        }
    }


    public static void remove(Set<PlayerBlockMonitor.BlockForDB> blocks) throws SQLException {
        synchronized (VerifyDecayDB.syncDB) {
            VerifyDecayDB.database.setAutoCommit(false);
            Statement statement = VerifyDecayDB.database.createStatement();
            for (PlayerBlockMonitor.BlockForDB block : blocks) {
                int x = block.getX();
                int y = block.getY();
                int z = block.getZ();
                String sql = String.format(
                        sqlUpdateStrength,
                        String.format(
                                SQL_GET_STRENGTH,
                                x,
                                y,
                                z
                        ),
                        x - PluginOreRegen.SUPPORT_RADIUS,
                        x + PluginOreRegen.SUPPORT_RADIUS,
                        y - PluginOreRegen.SUPPORT_RADIUS,
                        y + PluginOreRegen.SUPPORT_RADIUS,
                        z - PluginOreRegen.SUPPORT_RADIUS,
                        z + PluginOreRegen.SUPPORT_RADIUS
                );
                System.out.println(sql);
                statement.addBatch(sql);
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
                System.out.println(sql);
                statement.addBatch(sql);
            }
            statement.executeBatch();
            VerifyDecayDB.database.commit();
            VerifyDecayDB.database.setAutoCommit(true);
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
                    x - PluginOreRegen.SUPPORT_RADIUS,
                    x + PluginOreRegen.SUPPORT_RADIUS,
                    y - PluginOreRegen.SUPPORT_RADIUS,
                    y + PluginOreRegen.SUPPORT_RADIUS,
                    z - PluginOreRegen.SUPPORT_RADIUS,
                    z + PluginOreRegen.SUPPORT_RADIUS
            );
            statement = VerifyDecayDB.database.prepareStatement(sql);
            statement.execute();
            statement.close();

        }
    }
}
