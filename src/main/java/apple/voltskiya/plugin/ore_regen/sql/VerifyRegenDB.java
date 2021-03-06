package apple.voltskiya.plugin.ore_regen.sql;

import apple.voltskiya.plugin.DBNames;
import apple.voltskiya.plugin.VoltskiyaModule;
import apple.voltskiya.plugin.decay.PluginDecay;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import static apple.voltskiya.plugin.DBNames.Regen.*;

public class VerifyRegenDB {
    public static final Object syncDB = new Object();
    private static final String CREATE_TABLE_FORMAT = "CREATE TABLE IF NOT EXISTS %s ( %s );";
    private static final String TOOL_UID_CONTENT = String.format(
            "%s BIGINT PRIMARY KEY NOT NULL,\n" +
                    "%s VARCHAR(20)        NOT NULL,\n" +
                    "%s INTEGER            NOT NULL",
            TOOL_UID,
            BRUSH_TYPE,
            BRUSH_RADIUS
    );
    private static final String TOOL_TO_BLOCK_CONTENT = String.format(
            "%s    BIGINT      NOT NULL PRIMARY KEY,\n" +
                    "    %s  VARCHAR(50) NOT NULL,\n" +
                    "    %s  INTEGER     NOT NULL,\n" +
                    "    FOREIGN KEY (%s) REFERENCES %s",
            TOOL_UID,
            BLOCK_NAME,
            BLOCK_COUNT,
            TOOL_UID,
            TOOL_UID_TABLE
    );
    private static final String SECTION_TO_BLOCK_CONTENT = String.format(
            "%s   BIGINT      NOT NULL,\n" +
                    "    %s          INTEGER     NOT NULL,\n" +
                    "    %s          INTEGER     NOT NULL,\n" +
                    "    %s          INTEGER     NOT NULL,\n" +
                    "    %s VARCHAR(50) NOT NULL,\n" +
                    "    PRIMARY KEY (%s, %s, %s, %s)," +
                    " FOREIGN KEY (%s) REFERENCES %s",
            TOOL_UID,
            X,
            Y,
            Z,
            BLOCK_NAME,
            TOOL_UID,
            X,
            Y,
            Z,
            TOOL_UID,
            TOOL_UID_TABLE
    );
    protected static Connection database;

    // set up the database file
    static {
        synchronized (syncDB) {
            VoltskiyaModule voltskiyaModule = PluginDecay.get();
            try {
                Class.forName("org.sqlite.JDBC");
                // never close this because we're always using it
                database = DriverManager.getConnection("jdbc:sqlite:" + voltskiyaModule.getDataFolder() + File.separator + DBNames.DATABASE_NAME);
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
        synchronized (syncDB) {
            Statement statement = database.createStatement();
            statement.execute(String.format(CREATE_TABLE_FORMAT, TOOL_UID_TABLE, TOOL_UID_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, TOOL_TO_HOST_BLOCK_TABLE, TOOL_TO_BLOCK_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, TOOL_TO_VEIN_BLOCK_TABLE, TOOL_TO_BLOCK_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, TOOL_TO_DENSITY_TABLE, TOOL_TO_BLOCK_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, SECTION_INFO_TABLE, TOOL_TO_BLOCK_CONTENT));
            statement.execute(String.format(CREATE_TABLE_FORMAT, SECTION_TO_BLOCK_TABLE, SECTION_TO_BLOCK_CONTENT));
            statement.close();
        }
    }
}
