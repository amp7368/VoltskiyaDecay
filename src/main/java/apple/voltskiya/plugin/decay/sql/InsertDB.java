package apple.voltskiya.plugin.decay.sql;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static apple.voltskiya.plugin.decay.sql.DBNames.PlayerBlock.*;

public class InsertDB {
    private static final int SUPPORT_RADIUS = 5;

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
            "INSERT INTO %s ( %s,%s,%s,%s,%s,%s,%s) VALUES ( %%d,%%d,%%d,%%s,%%d,( %%s ),%%s );",
            PLAYER_BLOCK,
            X,
            Y,
            Z,
            BLOCK,
            MY_STRENGTH,
            EFFECTIVE_STRENGTH,
            OWNER
    );
    private static final String sqlUpdateStrength = String.format(
            "UPDATE %s\n" +
                    "SET %s = %s + %%d\n" +
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

    public static void playerPlace(BlockPlaceEvent event) throws SQLException {
        @NotNull Block blockPlaced = event.getBlockPlaced();
        synchronized (VerifyDecayDB.syncDB) {
            String sql = String.format(sqlPlaceBlock,
                    blockPlaced.getX(),
                    blockPlaced.getY(),
                    blockPlaced.getZ(),
                    "?",
                    1, //TODO change the strength of blocks
                    String.format(
                            sqlDetermineStrength,
                            blockPlaced.getX() - SUPPORT_RADIUS,
                            blockPlaced.getX() + SUPPORT_RADIUS,
                            blockPlaced.getY() - SUPPORT_RADIUS,
                            blockPlaced.getY() + SUPPORT_RADIUS,
                            blockPlaced.getZ() - SUPPORT_RADIUS,
                            blockPlaced.getZ() + SUPPORT_RADIUS
                    ),
                    "?"
            );
            PreparedStatement statement = VerifyDecayDB.database.prepareStatement(sql);
            statement.setString(1, blockPlaced.getBlockData().getMaterial().toString());
            statement.setString(2, event.getPlayer().getUniqueId().toString());
            statement.execute();
            statement.close();

            sql = String.format(
                    sqlUpdateStrength,
                    1, //TODO change the strength of blocks
                    blockPlaced.getX() - SUPPORT_RADIUS,
                    blockPlaced.getX() + SUPPORT_RADIUS,
                    blockPlaced.getY() - SUPPORT_RADIUS,
                    blockPlaced.getY() + SUPPORT_RADIUS,
                    blockPlaced.getZ() - SUPPORT_RADIUS,
                    blockPlaced.getZ() + SUPPORT_RADIUS
            );
            statement = VerifyDecayDB.database.prepareStatement(sql);
            statement.execute();
            statement.close();
        }
    }
}
