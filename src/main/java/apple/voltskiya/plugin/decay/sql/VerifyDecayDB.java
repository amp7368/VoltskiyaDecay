package apple.voltskiya.plugin.decay.sql;

import apple.voltskiya.plugin.decay.PluginDecay;
import apple.voltskiya.plugin.VoltskiyaModule;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class VerifyDecayDB {
    protected static final Object syncDB = new Object();
    protected static Connection database;

    private static final String DATABASE_NAME = "decay.db";
    private static final String CREATE_TABLE_FORMAT = "CREATE TABLE IF NOT EXISTS %s ( %s );";
    private static final String PLAYER_BLOCK_CONTENT = String.format(
            "    %s     INTEGER     NOT NULL,\n" +
                    "    %s INTEGER     NOT NULL,\n" +
                    "    %s INTEGER     NOT NULL,\n" +
                    "    %s VARCHAR(40) NOT NULL,\n" +
                    "    %s INTEGER     NOT NULL,\n" +
                    "    %s INTEGER     NOT NULL,\n" +
                    "    %s NCHAR(36) NOT NULL,\n" +
                    "    %s NCHAR(36) NOT NULL,\n" +
                    "    PRIMARY KEY (%s,%s,%s)\n",
            DBNames.PlayerBlock.X,
            DBNames.PlayerBlock.Y,
            DBNames.PlayerBlock.Z,
            DBNames.PlayerBlock.BLOCK,
            DBNames.PlayerBlock.MY_STRENGTH,
            DBNames.PlayerBlock.EFFECTIVE_STRENGTH,
            DBNames.PlayerBlock.OWNER,
            DBNames.PlayerBlock.WORLD_UUID,
            DBNames.PlayerBlock.X,
            DBNames.PlayerBlock.Y,
            DBNames.PlayerBlock.Z
    );

    // set up the database file
    static {
        synchronized (syncDB) {
            VoltskiyaModule voltskiyaModule = PluginDecay.get();
            try {
                Class.forName("org.sqlite.JDBC");
                // never close this because we're always using it
                database = DriverManager.getConnection("jdbc:sqlite:" + voltskiyaModule.getDataFolder() + File.separator + DATABASE_NAME);
                verifyTables();
                voltskiyaModule.log(Level.INFO, "The sql database for decay is connected");
            } catch (ClassNotFoundException | SQLException e) {
                voltskiyaModule.log(Level.SEVERE, "The sql database for decay is not properly set up");
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
            statement.execute(String.format(CREATE_TABLE_FORMAT, DBNames.PlayerBlock.PLAYER_BLOCK, PLAYER_BLOCK_CONTENT));
            statement.close();
        }
    }
}
