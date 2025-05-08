# import sys

# binfile = sys.argv[1]

# with open(binfile, "rb") as f:
#     while True:
#         first_two = f.read(2)
#         if len(first_two) < 2:
#             break

#         first_byte = first_two[0]

#         if first_byte & 0b11 != 0b11:
#             # compressed 16-bit instruction
#             val = int.from_bytes(first_two, "little")
#             print(f"{val:08x}")
#         else:
#             # standard 32-bit instruction
#             next_two = f.read(2)
#             if len(next_two) < 2:
#                 break
#             full_instr = first_two + next_two
#             val = int.from_bytes(full_instr, "little")
#             print(f"{val:08x}")


import sys

if len(sys.argv) != 2:
    print(f"Usage: {sys.argv[0]} <binary_file>")
    sys.exit(1)

bin_file = sys.argv[1]

with open(bin_file, "rb") as f:
    data = f.read()

# Each RISC-V instruction is 4 bytes (little-endian)
for i in range(0, len(data), 4):
    instr_bytes = data[i:i+4]
    if len(instr_bytes) < 4:
        instr_bytes = instr_bytes.ljust(4, b'\x00')  # pad last instruction
    # Convert little-endian bytes to integer
    instr = int.from_bytes(instr_bytes, byteorder='little')
    # Output as 32-bit hex string
    print(f"{instr:08x}")
