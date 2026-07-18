def get_clockwise(facing):
    return {"NORTH": "EAST", "EAST": "SOUTH", "SOUTH": "WEST", "WEST": "NORTH"}[facing]

def get_counter_clockwise(facing):
    return {"NORTH": "WEST", "EAST": "NORTH", "SOUTH": "EAST", "WEST": "SOUTH"}[facing]

def get_adjacent_dir(x1, z1, x2, z2):
    if x1 < x2: return "EAST"
    if x1 > x2: return "WEST"
    if z1 < z2: return "SOUTH"
    if z1 > z2: return "NORTH"
    return None

def process(facing, x1, z1, x2, z2):
    adj1 = get_adjacent_dir(x1, z1, x2, z2)
    adj2 = get_adjacent_dir(x2, z2, x1, z1)
    
    def get_type(f, adj):
        if adj == get_clockwise(f): return "LEFT"
        if adj == get_counter_clockwise(f): return "RIGHT"
        return "SINGLE"
        
    return get_type(facing, adj1), get_type(facing, adj2)

# Structure facing West (no rotation)
# Chest facing SOUTH, at (0,0) and (1,0)
print("Original South:", process("SOUTH", 0, 0, 1, 0))

# Structure facing East (rotated 180)
# Chest faces NORTH now.
# Original coordinates (0,0) and (1,0) rotated 180 degrees around origin become (0,0) and (-1,0).
print("Rotated North:", process("NORTH", 0, 0, -1, 0))
