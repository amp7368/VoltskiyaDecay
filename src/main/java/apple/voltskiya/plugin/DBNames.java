package apple.voltskiya.plugin;

public class DBNames {

    public static class Regen {
        public static final String DATABASE_NAME = "regen.db";
        public static final String TOOL_UID_TABLE = "tool_uid_info";
        public static final String TOOL_TO_HOST_BLOCK_TABLE = "tool_to_host_block";
        public static final String TOOL_TO_VEIN_BLOCK_TABLE = "tool_to_vein_block";
        public static final String TOOL_TO_DENSITY_TABLE = "tool_to_density_block";
        public static final String SECTION_INFO_TABLE = "section_info";
        public static final String SECTION_TO_BLOCK_TABLE = "section_to_block";

        public static final String TOOL_UID = "tool_uid";

        public static final String BRUSH_TYPE = "brush_type";
        public static final String BRUSH_RADIUS = "brush_radius";

        public static final String BLOCK_NAME = "block_name";
        public static final String BLOCK_COUNT = "block_count";
        public static final String X = "x";
        public static final String Y = "y";
        public static final String Z = "z";
        public static final String WORLD_UUID = "world_uid";
        public static final String IS_MARKED = "is_marked";
        public static final String VEIN_INDEX = "vein_index";
        public static final String IS_ORE = "is_ore";

        public static final String MINE = "mine";
    }

    public static class PlayerBlock {
        public static final String DATABASE_NAME = "decay.db";
        public static final String X = "x";
        public static final String Y = "y";
        public static final String Z = "z";
        public static final String BLOCK = "block";
        public static final String MY_STRENGTH = "my_strength";
        public static final String EFFECTIVE_STRENGTH = "effective_strength";
        public static final String OWNER = "owner";
        public static final String WORLD_UUID = "world_uid";
        public static final String PLAYER_BLOCK = "player_block";
    }
}
