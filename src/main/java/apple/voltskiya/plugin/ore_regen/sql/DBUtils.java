package apple.voltskiya.plugin.ore_regen.sql;

import apple.voltskiya.plugin.DBNames;
import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import apple.voltskiya.plugin.ore_regen.sql.VerifyRegenDB;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static apple.voltskiya.plugin.DBNames.*;

public class DBUtils {
    public static final Map<UUID, Integer> realToMy = new HashMap<>();
    public static final Map<Integer, UUID> myToReal = new HashMap<>();

    public static int getMyWorldUid(String worldUid) throws SQLException {
        synchronized (realToMy) {
            Integer my = realToMy.get(UUID.fromString(worldUid));
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
            synchronized (realToMy) {
                realToMy.put(UUID.fromString(worldUid), my);
            }
            return my;
        }
    }

    public static UUID getRealWorldUid(int myWorldUid) throws SQLException {
        synchronized (myToReal) {
            UUID real = myToReal.get(myWorldUid);
            if (real != null) return real;
        }
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = VerifyRegenDB.database.createStatement();
            final String SQL = String.format(String.format("SELECT %s FROM %s WHERE %s = %%d",
                    REAL_WORLD_UID, WORLD_UID_TO_MY_UID_TABLE, MY_WORLD_UID), myWorldUid);
            System.out.println(SQL);
            ResultSet response = statement.executeQuery(SQL);

            UUID real = UUID.fromString(response.getString(REAL_WORLD_UID));
            synchronized (myToReal) {
                myToReal.put(myWorldUid, real);
            }
            return real;
        }
    }
}
