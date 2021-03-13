package apple.voltskiya.plugin.ore_regen.sql;

import apple.voltskiya.plugin.DBNames;
import apple.voltskiya.plugin.VoltskiyaModule;
import apple.voltskiya.plugin.decay.PluginDecay;
import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import static apple.voltskiya.plugin.DBNames.*;
import static apple.voltskiya.plugin.DBNames.Regen.*;

public class VerifyRegenDB {
    private static final String CREATE_TABLE_FORMAT = "CREATE TABLE IF NOT EXISTS %s ( %s );";
    private static final String TOOL_UID_CONTENT = String.format(
            "%s INTEGER PRIMARY KEY NOT NULL,\n" +
                    "%s VARCHAR(20)        NOT NULL,\n" +
                    "%s INTEGER            NOT NULL",
            TOOL_UID,
            BRUSH_TYPE,
            BRUSH_RADIUS
    );
    private static final String TOOL_TO_BLOCK_CONTENT = String.format(
            "%s    INTEGER      NOT NULL,\n" +
                    "    %s  VARCHAR(50) NOT NULL,\n" +
                    "    %s  INTEGER     NOT NULL,\n" +
                    "    PRIMARY KEY (%s, %s)," +
                    "    FOREIGN KEY (%s) REFERENCES %s",
            TOOL_UID,
            BLOCK_NAME,
            BLOCK_COUNT,
            TOOL_UID,
            BLOCK_NAME,
            TOOL_UID,
            TOOL_UID_TABLE
    );
    private static final String TOOL_TO_VEIN_CONTENT = String.format(
            "%s    INTEGER      NOT NULL,\n" +
                    "    %s  VARCHAR(50) NOT NULL,\n" +
                    "    %s  INTEGER     NOT NULL,\n" +
                    "    %s  INTEGER     NOT NULL,\n" +
                    "    PRIMARY KEY (%s, %s, %s)," +
                    "    FOREIGN KEY (%s) REFERENCES %s",
            TOOL_UID,
            BLOCK_NAME,
            VEIN_INDEX,
            BLOCK_COUNT,
            TOOL_UID,
            BLOCK_NAME,
            VEIN_INDEX,
            TOOL_UID,
            TOOL_UID_TABLE
    );
    private static final String SECTION_TO_BLOCK_CONTENT = String.format(
            "%s   INTEGER      NOT NULL,\n" +
                    "    %s          INTEGER     NOT NULL,\n" +
                    "    %s          SMALLINT    NOT NULL,\n" +
                    "    %s          SMALLINT    NOT NULL,\n" +
                    "    %s          TINYINT     NOT NULL,\n" +
                    "    %s          BOOLEAN     NOT NULL,\n" +
                    "    %s          SMALLINT    NOT NULL,\n" +
                    "    %s          BOOLEAN     NOT NULL,\n" +
                    "    PRIMARY KEY (%s, %s, %s, %s, %s)," +
                    "    UNIQUE      (%s, %s, %s, %s)," +
                    " FOREIGN KEY (%s) REFERENCES %s",
            TOOL_UID,
            X,
            Y,
            Z,
            WORLD_UUID,
            IS_MARKED,
            IS_ORE,
            BLOCK_NAME,

            TOOL_UID,
            X,
            Y,
            Z,
            WORLD_UUID,

            X,
            Y,
            Z,
            WORLD_UUID,

            TOOL_UID,
            TOOL_UID_TABLE
    );
    private static final String WORLD_UID_TO_MY_UID_CONTENT = String.format(
            "   %s NCHAR(36) NOT NULL UNIQUE PRIMARY KEY,\n" +
                    "    %s   INTEGER   NOT NULL UNIQUE", REAL_WORLD_UID, MY_WORLD_UID);
    private static final String BLOCK_TO_MY_UID = String.format(
            "   %s NCHAR(50) NOT NULL UNIQUE PRIMARY KEY,\n" +
                    "    %s   INTEGER   NOT NULL UNIQUE", REAL_WORLD_UID, MY_WORLD_UID);
    protected static Connection database;

    // set up the database file
    static {
        synchronized (VerifyDecayDB.syncDB) {
            VoltskiyaModule voltskiyaModule = PluginDecay.get();
            try {
                Class.forName("org.sqlite.JDBC");
                // never close this because we're always using it
                database = DriverManager.getConnection("jdbc:sqlite:" + voltskiyaModule.getDataFolder() + File.separator + DBNames.Regen.DATABASE_NAME);
                verifyTables();
                voltskiyaModule.log(Level.INFO, "The sql database for regen is connected");
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
                voltskiyaModule.log(Level.SEVERE, "The sql database for regen is not properly set up");
                database = null;
            }
        }
    }

    /**
     * do any setup and make sure the static part of this class is completed
     */
    public static void initialize() {

    }

    private static void verifyTables() throws SQLException {
        synchronized (VerifyDecayDB.syncDB) {
            Statement statement = database.createStatement();
            statement.execute("pragma busy_timeout=7000; -- Busy timeout set to 7000 milliseconds");
            statement.execute(String.format(CREATE_TABLE_FORMAT, TOOL_UID_TABLE, TOOL_UID_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, TOOL_TO_HOST_BLOCK_TABLE, TOOL_TO_BLOCK_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, TOOL_TO_VEIN_BLOCK_TABLE, TOOL_TO_VEIN_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, TOOL_TO_DENSITY_TABLE, TOOL_TO_BLOCK_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, SECTION_INFO_TABLE, TOOL_TO_BLOCK_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, SECTION_TO_BLOCK_TABLE, SECTION_TO_BLOCK_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, WORLD_UID_TO_MY_UID_TABLE, WORLD_UID_TO_MY_UID_CONTENT));
            statement.close();
        }
    }
}
