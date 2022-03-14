public class Symtab {
    String name;
    int value, size, info, other, shndx;
    int type, bind, vis, index;

    public Symtab(String name, int value, int size, int info, int other, int shndx) {
        this.name = name;
        this.value = value;
        this.size = size;
        this.info = info;
        this.other = other;
        this.shndx = shndx;
        this.type = info % 16;
        this.bind = info / 16;
        this.vis = other;
        this.index = shndx;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public int getSize() {
        return size;
    }

    public String getIndex() {
        return switch (index) {
            case (0) -> "UNDEF";
            case (0xff00) -> "LORESERVE";
            case (0xff1f) -> "HIPROC";
            case (0xff20) -> "LOOS";
            case (0xff3f) -> "HIOS";
            case (0xfff1) -> "ABS";
            case (0xfff2) -> "COMMON";
            case (0xffff) -> "XINDEX";
            default -> Integer.valueOf(index).toString();
        };
    }

    public String getBind() {
        return switch (bind) {
            case (0) -> "LOCAL";
            case (1) -> "GLOBAL";
            case (2) -> "WEAK";
            case (10) -> "LOOS";
            case (12) -> "HIOS";
            case (13) -> "LOPROC";
            case (15) -> "HIPROC";
            default -> "ERROR";
        };
    }

    public String getType() {
        return switch (type) {
            case (0) -> "NOTYPE";
            case (1) -> "OBJECT";
            case (2) -> "FUNC";
            case (3) -> "SECTION";
            case (4) -> "FILE";
            case (5) -> "COMMON";
            case (6) -> "TLS";
            case (10) -> "LOOS";
            case (12) -> "HIOS";
            case (13) -> "LOPROC";
            case (15) -> "HIPROC";
            default -> "ERROR";
        };
    }

    public String getVis() {
        return switch (vis) {
            case (0) -> "DEFAULT";
            case (1) -> "INTERNAL";
            case (2) -> "HIDDEN";
            case (3) -> "PROTECTED";
            case (4) -> "EXPORTED";
            case (5) -> "SINGLETON";
            case (6) -> "ELIMINATE";
            default -> "ERROR";
        };
    }
}
