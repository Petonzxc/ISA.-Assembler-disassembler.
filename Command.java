public class Command {
    final String command;
    final int address;
    String label;
    boolean isLabel = false;

    public Command(String command, int address, String label, boolean isLabel) {
        this.command = command;
        this.address = address;
        this.label = label;
        this.isLabel = isLabel;
    }

    @Override
    public String toString() {
        return String.format("%08x %10s: %s\n", address, (isLabel ? label : ""), command);
    }
}
