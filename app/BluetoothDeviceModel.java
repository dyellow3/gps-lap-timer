public class BluetoothDeviceModel {
    private String name;
    private String hardwareAddress;

    public BluetoothDeviceModel(String name, String hardwareAddress) {
        this.name = name;
        this.hardwareAddress = hardwareAddress;
    }

    public String getName() {
        return name;
    }

    public String getHardwareAddress() {
        return hardwareAddress;
    }
}
