import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Parser {
    public static boolean checkElf(final byte[] input) {
        return (input[0] == 0x7f && input[1] == 0x45 && input[2] == 0x4c && input[3] == 0x46);
    }

    public static boolean checkElf32(final byte[] input) {
        return (input[4] == 0x1);
    }

    public static boolean checkLittleEndian(final byte[] input) {
        return (input[5] == 0x1);
    }

    public static boolean checkRiscv(final byte[] input) {
        return ((input[18] + 256) == 0xf3);
    }

    public static void checkFormat(final byte[] input) {
        if (input.length < 4) {
            System.out.println("Incorrect input data");
            System.exit(0);
        }
        if (!checkElf(input)) {
            System.out.println("Input file not in elf format");
            System.exit(0);
        }
        if (!checkElf32(input)) {
            System.out.println("Input file not in elf32 format");
            System.exit(0);
        }
        if (!checkLittleEndian(input)) {
            System.out.println("Encoding is not Little Endian");
            System.exit(0);
        }
        if (!checkRiscv(input)) {
            System.out.println("Elf-file is not RISC-V");
            System.exit(0);
        }
    }

    public static int getInt4(final int x1, final int x2, final int x3, final int x4) {
        return (x4 << 24) + (x3 << 16) + (x2 << 8) + x1;
    }

    public static int getInt2(final int x1, final int x2) {
        return (x2 << 8) + x1;
    }

    public static int getE_shoff(final int[] bytes) { // 32 - 35
        return getInt4(bytes[32], bytes[33], bytes[34], bytes[35]);
    }

    public static int getE_shnum(final int[] bytes) { // 48 - 49
        return getInt2(bytes[48], bytes[49]);
    }

    public static int getE_shstrndx(final int[] bytes) { // 50 - 51
        return getInt2(bytes[50], bytes[51]);
    }

    public static int getShstrtab(final int e_shoff, final int e_shstrndx) {
        return (e_shoff + e_shstrndx * 40);
    }

    public static int getOffset(final int[] bytes, final int pos) {
        return getInt4(bytes[pos + 16], bytes[pos + 17], bytes[pos + 18], bytes[pos + 19]);
    }

    public static int getSz(final int[] bytes, final int pos) {
        return getInt4(bytes[pos + 20], bytes[pos + 21], bytes[pos + 22], bytes[pos + 23]);
    }

    public static int getIndex(final int[] bytes, final int offsetShstrtab, final String cur) {
        StringBuilder sb = new StringBuilder();
        int ans = -1;
        for (int i = offsetShstrtab; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                String now = sb.toString();
                sb.setLength(0);
                if (now.equals(cur)) {
                    ans = i - now.length();
                    break;
                }
            } else {
                sb.append((char) bytes[i]);
            }
        }
        return ans;
    }

    public static String getName(final int[] bytes, final int idName, final int offsetStrtab) {
        StringBuilder sb = new StringBuilder();
        int start = idName + offsetStrtab;
        while (start < bytes.length && bytes[start] != 0) {
            sb.append((char) bytes[start++]);
        }
        return sb.toString();
    }

    public static Symtab getSymtab(final int[] bytes, final int pos, final int offsetStrtab) {
        int idName = getInt4(bytes[pos], bytes[pos + 1], bytes[pos + 2], bytes[pos + 3]);
        String name = getName(bytes, idName, offsetStrtab);
        int value = getInt4(bytes[pos + 4], bytes[pos + 5], bytes[pos + 6], bytes[pos + 7]);
        int size = getInt4(bytes[pos + 8], bytes[pos + 9], bytes[pos + 10], bytes[pos + 11]);
        int info = bytes[pos + 12];
        int other = bytes[pos + 13];
        int shndx = getInt2(bytes[pos + 14], bytes[pos + 15]);
        return new Symtab(name, value, size, info, other, shndx);
    }

    public static int getaddress(final int[] bytes, final int id) {
        return getInt4(bytes[id + 12], bytes[id + 13], bytes[id + 14], bytes[id + 15]);
    }

    public static void printAns(final Symtab[] tab, final ArrayList<Command> commands, final String output) {
        try (FileWriter writer = new FileWriter(output)) {
            writer.write(".symtab" + System.lineSeparator());
            writer.write("Symbol Value              Size Type     Bind     Vis       Index Name" + System.lineSeparator());
            for (int i = 0; i < tab.length; i++) {
                writer.write(String.format("[%4d] 0x%-15X %5d %-8s %-8s %-8s %6s %s\n",
                        i, tab[i].getValue(), tab[i].getSize(), tab[i].getType(), tab[i].getBind(),
                        tab[i].getVis(), tab[i].getIndex(), tab[i].getName()));
                writer.write(System.lineSeparator());
            }
            writer.write(".text" + System.lineSeparator());
            for (int i = 0; i < commands.size(); i++) {
                writer.write(commands.get(i).toString() + System.lineSeparator());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Output file does not exist " + e.getMessage());
        } catch (IOException e) {
            System.out.println("File I/O error! " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try (InputStream reader = new FileInputStream(args[0])) {
            byte[] input = reader.readAllBytes();
            checkFormat(input);
            int length = input.length;
            int[] bytes = new int[length];
            for (int i = 0; i < length; i++) {
                bytes[i] = (int) (input[i] & 0xFF);
            }
            int e_shoff = getE_shoff(bytes);
            int e_shnum = getE_shnum(bytes);
            int e_shstrndx = getE_shstrndx(bytes);
            int shstrtab = getShstrtab(e_shoff, e_shstrndx);
            int offsetShstrtab = getOffset(bytes, shstrtab);
            int idText = getIndex(bytes, offsetShstrtab, ".text") - offsetShstrtab;
            int idSymtab = getIndex(bytes, offsetShstrtab, ".symtab") - offsetShstrtab;
            int idStrtab = getIndex(bytes, offsetShstrtab, ".strtab") - offsetShstrtab;
            int e_shoffLen = e_shoff + 40 * e_shnum;
            int posText = -1, posSymtab = -1, posStrtab = -1;
            for (int i = e_shoff; i < e_shoffLen; i += 40) {
                int id = getInt4(bytes[i], bytes[i + 1], bytes[i + 2], bytes[i + 3]);
                if (id == idText) {
                    posText = i;
                }
                if (id == idSymtab) {
                    posSymtab = i;
                }
                if (id == idStrtab) {
                    posStrtab = i;
                }
            }
            // parse symtab
            int offsetSymtab = getOffset(bytes, posSymtab);
            int szSymtab = getSz(bytes, posSymtab);
            Symtab[] tab = new Symtab[szSymtab >> 4];
            for (int i = offsetSymtab; i < offsetSymtab + szSymtab; i += 16) {
                tab[(i - offsetSymtab) / 16] = getSymtab(bytes, i, getOffset(bytes, posStrtab));
            }
            // parse Text
            int offsetText = getOffset(bytes, posText);
            int szText = getSz(bytes, posText);
            int addressText = getaddress(bytes, posText);
            ArrayList<Command> commands = new ArrayList<>();
            HashMap<Integer, String> labels = new HashMap<>();
            for (int i = offsetText; i < offsetText + szText; i += 4) {
                parseCommand4(labels, addressText, addressText + i - offsetText,
                        (long) bytes[i] + ((long) bytes[i + 1] << 8) + ((long) bytes[i + 2] << 16) + ((long) bytes[i + 3] << 24),
                        tab, commands);
            }
            printAns(tab, commands, args[1]);
        } catch (FileNotFoundException e) {
            System.out.println("Input file does not exist " + e.getMessage());
        } catch (IOException e) {
            System.out.println("File I/O error! " + e.getMessage());
        }
    }

    static int cntLoc = 0;

    public static String getRegister(final int id) {
        return switch (id) {
            case 0 -> "zero";
            case 1 -> "ra";
            case 2 -> "sp";
            case 3 -> "gp";
            case 4 -> "tp";
            case 5 -> "t0";
            case 6 -> "t1";
            case 7 -> "t2";
            case 8 -> "s0";
            case 9 -> "s1";
            case 10 -> "a0";
            case 11 -> "a1";
            case 12 -> "a2";
            case 13 -> "a3";
            case 14 -> "a4";
            case 15 -> "a5";
            case 16 -> "a6";
            case 17 -> "a7";
            case 18 -> "s2";
            case 19 -> "s3";
            case 20 -> "s4";
            case 21 -> "s5";
            case 22 -> "s6";
            case 23 -> "s7";
            case 24 -> "s8";
            case 25 -> "s9";
            case 26 -> "s10";
            case 27 -> "s11";
            case 28 -> "t3";
            case 29 -> "t4";
            case 30 -> "t5";
            case 31 -> "t6";
            default -> "unknown_register";
        };
    }

    public static int getFromItoJ(final long command, final int l, final int r) {
        int ans = 0;
        for (int i = l; i <= r; i++) {
            if ((command & (1L << i)) == (1L << i)) {
                ans += (1 << (i - l));
            }
        }
        return ans;
    }

    public static void parseU(final int address, final String commandName, final long command, ArrayList<Command> commands,
                              final String label, final boolean isLabel) {
        int rd = getFromItoJ(command, 7, 11);
        int imm = getFromItoJ(command, 12, 31);
        commands.add(new Command(String.format("%s %s, %d", commandName, getRegister(rd), imm), address, label, isLabel));
    }

    public static void parseI(final int address, final String commandName, final long command, ArrayList<Command> commands,
                              final String label, final boolean isLabel) {
        int rd = getFromItoJ(command, 7, 11);
        int rs1 = getFromItoJ(command, 15, 19);
        int imm = getFromItoJ(command, 20, 31);
        commands.add(new Command(
                String.format("%s %s, %d(%s)", commandName, getRegister(rd), imm, getRegister(rs1)),
                address, label, isLabel));
    }

    public static void parseI1(final int address, final String commandName, final long command, ArrayList<Command> commands,
                              final String label, final boolean isLabel) {
        int rd = getFromItoJ(command, 7, 11);
        int rs1 = getFromItoJ(command, 15, 19);
        int imm = getFromItoJ(command, 20, 31);
        commands.add(new Command(
                String.format("%s %s, %s, %s", commandName, getRegister(rd), getRegister(rs1), imm),
                address, label, isLabel));
    }

    public static void parseRV32MOrRV32IR(final int address, final String commandName, final long command, ArrayList<Command> commands,
                                          final String label, final boolean isLabel) {
        int rd = getFromItoJ(command, 7, 11);
        int rs1 = getFromItoJ(command, 15, 19);
        int rs2 = getFromItoJ(command, 20, 24);
        commands.add(new Command(
                String.format("%s %s, %s, %s", commandName, getRegister(rd), getRegister(rs1), getRegister(rs2)),
                address, label, isLabel));
    }

    public static void parseS(final int address, final String commandName, final long command, ArrayList<Command> commands,
                              final String label, final boolean isLabel) {
        int rs1 = getFromItoJ(command, 15, 19);
        int rs2 = getFromItoJ(command, 20, 24);
        int imm = getFromItoJ(command, 7, 11) + (getFromItoJ(command, 25, 31) << 5);
        if (getFromItoJ(command, 25, 25) == 1) {
            imm -= (1 << 13);
        }
        commands.add(new Command(
                String.format("%s %s, %d(%s)", commandName, getRegister(rs1), imm, getRegister(rs2)),
                address, label, isLabel));
    }

    public static void parseRWithShamt(final int address, final String commandName, final long command, ArrayList<Command> commands,
                                       final String label, final boolean isLabel) {
        int rd = getFromItoJ(command, 7, 11);
        int rs1 = getFromItoJ(command, 15, 19);
        int shamt = getFromItoJ(command, 20, 24);
        commands.add(new Command(
                String.format("%s %s, %s %d", commandName, getRegister(rd), getRegister(rs1), shamt),
                address, label, isLabel));
    }

    public static void parseB(final int firstAddress, final int address, final String commandName, final long command, ArrayList<Command> commands,
                              final String label, final boolean isLabel, HashMap<Integer, String> labels, final Symtab[] tab) {
        int rs1 = getFromItoJ(command, 15, 19);
        int rs2 = getFromItoJ(command, 20, 24);
        int imm = (getFromItoJ(command, 31, 31) << 12) + (getFromItoJ(command, 25, 30) << 5)
                + (getFromItoJ(command, 7, 7) << 11) + (getFromItoJ(command, 8, 11) << 1);
        commands.add(new Command(
                String.format("%s %s, %s, %s", commandName, getRegister(rs1), getRegister(rs2),
                        getLabel(firstAddress, address, imm + address, commands, labels, tab)), address, label, isLabel));
    }

    public static String checkLabel(final int address, final Symtab[] tab, HashMap<Integer, String> labels) {
        if (labels.containsKey(address)) {
            return labels.get(address);
        }
        for (Symtab cur : tab) {
            if (cur.getValue() == address && cur.getType().equals("FUNC")) {
                return cur.getName();
            }
        }
        return "";
    }

    public static String newLabel(final int cnt) {
        return (String.format("%s_%05x", "LOC", cnt));
    }

    public static String getLabel(final int firstAddress, final int address, final int addressLabel, final ArrayList<Command> commands,
                                  HashMap<Integer, String> labels, final Symtab[] tab) {
        String label = "";
        if (addressLabel < address) {
            int id = (addressLabel - firstAddress) / 4;
            Command command = commands.get(id);
            if (!command.isLabel) {
                command.isLabel = true;
                command.label = newLabel(cntLoc++);
            }
            label = command.label;
        }
        if (label.equals("")) {
            String cur = checkLabel(addressLabel, tab, labels);
            if (cur.equals("")) {
                cur = newLabel(cntLoc++);
            }
            labels.put(addressLabel, cur);
            label = cur;
        }
        return label;
    }

    public static void parseCommand4(HashMap<Integer, String> labels, final int firstAddress, final int address,
                                     final long command, final Symtab[] tab, ArrayList<Command> commands) {
        int opcode = getFromItoJ(command, 0, 6);
        String label = checkLabel(address, tab, labels);
        boolean isLabel = !Objects.equals(label, "");
        if (opcode == 0b0110111) {
            parseU(address, "lui", command, commands, label, isLabel); // LUI U-type
            return;
        }
        if (opcode == 0b0010111) {
            parseU(address, "auipc", command, commands, label, isLabel); // AUIPC U-type
            return;
        }
        if (opcode == 0b1101111) {    // JAL J-type
            int rd = getFromItoJ(command, 7, 11);
            int imm = (getFromItoJ(command, 31, 31) << 20) + (getFromItoJ(command, 21, 30) << 1)
                    + (getFromItoJ(command, 20, 20) << 11) + (getFromItoJ(command, 12, 19) << 12);
//                commands.add(new Command(
//                        String.format("%s  %s, %d", "jal", getLabel(imm + address), imm),
//                        address, label, isLabel));
            return;
        }
        if (opcode == 0b1100111) { // JALR
            //
            return;
        }
        if (opcode == 0b0000011) {    // I-type
            switch (getFromItoJ(command, 12, 14)) {
                case 0b0000000:
                    parseI(address, "lb", command, commands, label, isLabel); // LB
                    return;
                case 0b0010000:
                    parseI(address, "lh", command, commands, label, isLabel); // LH
                    return;
                case 0b0100000:
                    parseI(address, "lw", command, commands, label, isLabel); // LW
                    return;
                case 0b1000000:
                    parseI(address, "lbu", command, commands, label, isLabel); // LBU
                    return;
                case 0b1010000:
                    parseI(address, "lhu", command, commands, label, isLabel); // LHU
                    return;
            }
        }
        if (opcode == 0b0110011) { // R-type
            switch (getFromItoJ(command, 25, 31)) {
                case 0b0000001: // RV32M Standard Extension
                    switch (getFromItoJ(command, 12, 14)) {
                        case 0b0000000:
                            parseRV32MOrRV32IR(address, "mul", command, commands, label, isLabel); // MUL
                            return;
                        case 0b0010000:
                            parseRV32MOrRV32IR(address, "mulh", command, commands, label, isLabel); // MULH
                            return;
                        case 0b0100000:
                            parseRV32MOrRV32IR(address, "mulhsu", command, commands, label, isLabel); // MULHSU
                            return;
                        case 0b0110000:
                            parseRV32MOrRV32IR(address, "mulhu", command, commands, label, isLabel); // MULHU
                            return;
                        case 0b1000000:
                            parseRV32MOrRV32IR(address, "div", command, commands, label, isLabel); // DIV
                            return;
                        case 0b1010000:
                            parseRV32MOrRV32IR(address, "divu", command, commands, label, isLabel); // DIVU
                            return;
                        case 0b1100000:
                            parseRV32MOrRV32IR(address, "rem", command, commands, label, isLabel); // REM
                            return;
                        case 0b1110000:
                            parseRV32MOrRV32IR(address, "remu", command, commands, label, isLabel); // REMU
                            return;
                    }
                case 0b0000000:
                    switch (getFromItoJ(command, 12, 14)) {
                        case 0b0000000:
                            parseRV32MOrRV32IR(address, "add", command, commands, label, isLabel); // ADD
                            return;
                        case 0b0010000:
                            parseRV32MOrRV32IR(address, "sll", command, commands, label, isLabel); // SLL
                            return;
                        case 0b0100000:
                            parseRV32MOrRV32IR(address, "slt", command, commands, label, isLabel); // SLT
                            return;
                        case 0b0110000:
                            parseRV32MOrRV32IR(address, "sltu", command, commands, label, isLabel); // SLTU
                            return;
                        case 0b1000000:
                            parseRV32MOrRV32IR(address, "xor", command, commands, label, isLabel); // XOR
                            return;
                        case 0b1010000:
                            parseRV32MOrRV32IR(address, "srl", command, commands, label, isLabel); // SRL
                            return;
                        case 0b1100000:
                            parseRV32MOrRV32IR(address, "or", command, commands, label, isLabel); // OR
                            return;
                        case 0b1110000:
                            parseRV32MOrRV32IR(address, "and", command, commands, label, isLabel); // AND
                            return;
                    }
                case 0b0100000:
                    switch (getFromItoJ(command, 12, 14)) {
                        case 0b0000000:
                            parseRV32MOrRV32IR(address, "sub", command, commands, label, isLabel); // SUB
                            return;
                        case 0b1010000:
                            parseRV32MOrRV32IR(address, "sra", command, commands, label, isLabel); // SRA
                            return;
                    }
            }
        }
        if (opcode == 0b0100011) { // S -type
            switch (getFromItoJ(command, 12, 14)) {
                case 0b0000000:
                    parseS(address, "sb", command, commands, label, isLabel); // SB
                    return;
                case 0b0010000:
                    parseS(address, "sh", command, commands, label, isLabel); // SH
                    return;
                case 0b0100000:
                    parseS(address, "sw", command, commands, label, isLabel); // SW
                    return;
            }
        }
        if (opcode == 0b1100011) { // B-type
            switch (getFromItoJ(command, 12, 14)) {
                case 0b0000000:
                    parseB(firstAddress, address, "beq", command, commands, label, isLabel, labels, tab); // BEQ
                    return;
                case 0b0010000:
                    parseB(firstAddress, address, "bne", command, commands, label, isLabel, labels, tab); // BNE
                    return;
                case 0b1000000:
                    parseB(firstAddress, address, "blt", command, commands, label, isLabel, labels, tab); // BLT
                    return;
                case 0b1010000:
                    parseB(firstAddress, address, "bge", command, commands, label, isLabel, labels, tab); // BGE
                    return;
                case 0b1100000:
                    parseB(firstAddress, address, "bltu", command, commands, label, isLabel, labels, tab); // BLTU
                    return;
                case 0b1110000:
                    parseB(firstAddress, address, "bgeu", command, commands, label, isLabel, labels, tab); // BGEU
                    return;
            }
        }
        if (opcode == 0b0010011) {
            switch (getFromItoJ(command, 12, 14)) {
                case 0b0000000:
                    parseI1(address, "addi", command, commands, label, isLabel); // ADDI
                    return;
                case 0b0010000:
                    parseRWithShamt(address, "slli", command, commands, label, isLabel); // SLLI
                    return;
                case 0b0100000:
                    parseI1(address, "slti", command, commands, label, isLabel); // SLTI
                    return;
                case 0b0110000:
                    parseI1(address, "sltiu", command, commands, label, isLabel); // SLTIU
                    return;
                case 0b1000000: // XORI
                    parseI1(address, "xori", command, commands, label, isLabel); // SLTIU
                    return;
                case 0b1010000:
                    switch (getFromItoJ(command, 25, 31)) {
                        case 0b0000000:
                            parseRWithShamt(address, "srli", command, commands, label, isLabel);  // SRLI
                            return;
                        case 0b0100000:
                            parseRWithShamt(address, "srai", command, commands, label, isLabel);  // SRAI
                            return;
                    }
                case 0b1100000:
                    parseI1(address, "ori", command, commands, label, isLabel); // ORI
                    return;
                case 0b1110000:
                    parseI1(address, "andi", command, commands, label, isLabel); // ANDI
                    return;
            }
        }
        if (opcode == 0b111001) {
            switch (getFromItoJ(command, 20, 31)) {
                case 0:
                    commands.add(new Command(String.format("%s", "ecall"), address, label, isLabel)); // ECALL
                    return;
                case 1:
                    commands.add(new Command(String.format("%s", "ebreak"), address, label, isLabel)); // EBREAK
                    return;
            }
        }
        System.out.println("unknown_command");
    }
}

