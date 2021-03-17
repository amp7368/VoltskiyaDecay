package apple.voltskiya.plugin.ore_regen.sql;

import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static apple.voltskiya.plugin.DBNames.*;

public class DBUtils {
    private static final Map<UUID, Integer> realToMyWorld = new HashMap<>();
    private static final Map<Integer, UUID> myToRealWorld = new HashMap<>();
    private static final Map<Integer, Material> myToBlockName = new HashMap<>();
    private static final Map<Material, Integer> blockNameToMy = new HashMap<>();

    public static int getMyWorldUid(String worldUid) throws SQLException {
        synchronized (realToMyWorld) {
            Integer my = realToMyWorld.get(UUID.fromString(worldUid));
            if (my != null) return my;
        }
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            statement.execute(String.format(String.format("INSERT INTO %s (%s, %s)\n" +
                            "VALUES ('%%s',(\n" +
                            "            SELECT ifnull(max(%s), 0)\n" +
                            "            FROM %s) + 1)\n" +
                            "ON CONFLICT (%s) DO NOTHING\n",
                    WORLD_UID_TO_MY_UID_TABLE,
                    REAL_WORLD_UID,
                    MY_WORLD_UID,
                    MY_WORLD_UID,
                    WORLD_UID_TO_MY_UID_TABLE,
                    REAL_WORLD_UID
            ), worldUid));
            ResultSet response = statement.executeQuery(String.format(String.format("SELECT %s FROM %s WHERE %s = '%%s'",
                    MY_WORLD_UID, WORLD_UID_TO_MY_UID_TABLE, REAL_WORLD_UID), worldUid));
            final int my = response.getInt(MY_WORLD_UID);
            statement.close();
            synchronized (realToMyWorld) {
                realToMyWorld.put(UUID.fromString(worldUid), my);
            }
            return my;
        }
    }

    public static UUID getRealWorldUid(int myWorldUid) throws SQLException {
        synchronized (myToRealWorld) {
            UUID real = myToRealWorld.get(myWorldUid);
            if (real != null) return real;
        }
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(String.format("SELECT %s FROM %s WHERE %s = %%d",
                    REAL_WORLD_UID, WORLD_UID_TO_MY_UID_TABLE, MY_WORLD_UID), myWorldUid));

            UUID real = UUID.fromString(response.getString(REAL_WORLD_UID));
            statement.close();
            synchronized (myToRealWorld) {
                myToRealWorld.put(myWorldUid, real);
            }
            return real;
        }
    }

    public static Material getBlockName(int myBlockUid) throws SQLException {
        synchronized (myToBlockName) {
            Material real = myToBlockName.get(myBlockUid);
            if (real != null) return real;
        }
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            ResultSet response = statement.executeQuery(String.format(String.format(
                    "SELECT %s FROM %s WHERE %s = %%d",
                    BLOCK_NAME,
                    BLOCK_NAME_TO_MY_UID_TABLE,
                    MY_BLOCK_UID
            ), myBlockUid));
            final Material name = Material.valueOf(response.getString(BLOCK_NAME));
            statement.close();
            myToBlockName.put(myBlockUid, name);
            return name;
        }
    }

    public static int getMyBlockUid(@NotNull Material blockName) throws SQLException {
        synchronized (blockNameToMy) {
            Integer my = blockNameToMy.get(blockName);
            if (my != null) return my;
        }
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            statement.execute(String.format(String.format("INSERT INTO %s (%s, %s)\n" +
                            "VALUES ('%%s',(\n" +
                            "            SELECT ifnull(max(%s), 0)\n" +
                            "            FROM %s) + 1)\n" +
                            "ON CONFLICT (%s) DO NOTHING\n",
                    BLOCK_NAME_TO_MY_UID_TABLE,
                    BLOCK_NAME,
                    MY_BLOCK_UID,
                    MY_BLOCK_UID,
                    BLOCK_NAME_TO_MY_UID_TABLE,
                    BLOCK_NAME
            ), blockName));
            ResultSet response = statement.executeQuery(String.format(String.format(
                    "SELECT %s FROM %s WHERE %s = '%%s'",
                    MY_BLOCK_UID,
                    BLOCK_NAME_TO_MY_UID_TABLE,
                    BLOCK_NAME
            ), blockName));
            int my = response.getInt(MY_BLOCK_UID);
            statement.close();
            blockNameToMy.put(blockName, my);
            return my;
        }
    }

    public static int getMyAir() throws SQLException {
        return getMyBlockUid(Material.AIR);
    }
}
