package net.klingt.akai;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

import static java.lang.String.format;

public class MidimixExtensionDefinition extends ControllerExtensionDefinition {
    private static final UUID DRIVER_ID = UUID.fromString("9c561b6b-31bf-401c-aa86-a4a576a13f95");
    static final String MODEL = "Midimix";
    static final String VENDOR = "Akai";

    public MidimixExtensionDefinition() {
    }

    @Override
    public String getName() {
        return MODEL;
    }

    @Override
    public String getAuthor() {
        return "klingt.net";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public UUID getId() {
        return DRIVER_ID;
    }

    @Override
    public String getHardwareVendor() {
        return VENDOR;
    }

    @Override
    public String getHardwareModel() {
        return "Midimix";
    }

    @Override
    public int getRequiredAPIVersion() {
        return 4;
    }

    @Override
    public int getNumMidiInPorts() {
        return 1;
    }

    @Override
    public int getNumMidiOutPorts() {
        return 1;
    }

    @Override
    public void listAutoDetectionMidiPortNames(final AutoDetectionMidiPortNamesList portList, final PlatformType platformType) {
        switch (platformType) {
            case MAC:
            case WINDOWS:
                portList.add(new String[]{"MIDI Mix"}, new String[]{"MIDI Mix"});

                System.err.println(format("Support for platform '%s' is experimental.", platformType.name()));
                break;
            case LINUX:
                portList.add(new String[]{"MIDI Mix MIDI 1"}, new String[]{"MIDI Mix MIDI 1"});
        }
    }

    @Override
    public MidimixExtension createInstance(final ControllerHost host) {
        return new MidimixExtension(this, host);
    }
}
