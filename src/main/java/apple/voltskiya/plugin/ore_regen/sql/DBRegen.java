package apple.voltskiya.plugin.ore_regen.sql;

import apple.voltskiya.plugin.ore_regen.build.RegenConfigInstance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

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
}
