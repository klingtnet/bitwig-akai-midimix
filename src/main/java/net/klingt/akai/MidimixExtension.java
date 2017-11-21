package net.klingt.akai;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

import java.util.stream.IntStream;

import static com.bitwig.extension.api.util.midi.ShortMidiMessage.NOTE_ON;
import static net.klingt.akai.MidiMix.*;

public class MidimixExtension extends ControllerExtension {

    private MidiOut midiOut;

    protected MidimixExtension(final MidimixExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        final ControllerHost host = getHost();
        this.midiOut = host.getMidiOutPort(0);
        TrackBank trackBank = host.getProject()
                .getShownTopLevelTrackGroup()
                .createTrackBank(8, 0, 0, false);

        MidiHandler midiHandler = new MidiHandler(host.createTransport(),
                host.createMasterTrack(0),
                trackBank,
                midiOut
        );
        host.getMidiInPort(0).setMidiCallback(midiHandler);
        host.getMidiInPort(0).setSysexCallback(midiHandler::sysexReceived);

        registerObservers(trackBank);

        host.showPopupNotification("Midimix Initialized");
    }

    private void registerObservers(TrackBank trackBank) {
        IntStream.range(0, trackBank.getSizeOfBank())
                .filter(i -> trackBank.getChannel(i) != null)
                .forEach(i -> registerChannelObservers(trackBank.getChannel(i), i));

        trackBank.canScrollBackwards().addValueObserver(value -> lightButton(value, BANK_LEFT));
        trackBank.canScrollForwards().addValueObserver(value -> lightButton(value, BANK_RIGHT));
    }

    private void registerChannelObservers(Track channel, int index) {
        indexOf(index, MUTE).ifPresent(buttonIndex -> channel.getMute().addValueObserver(value -> lightButton(value, buttonIndex)));
        indexOf(index, SOLO).ifPresent(buttonIndex -> channel.getSolo().addValueObserver(value -> lightButton(value, buttonIndex)));
        indexOf(index, REC_ARM).ifPresent(buttonIndex -> channel.getArm().addValueObserver(value -> lightButton(value, buttonIndex)));
    }

    @Override
    public void exit() {
        getHost().showPopupNotification("Midimix Exited");
    }

    @Override
    public void flush() {
    }

    private void lightButton(Boolean value, int buttonNote) {
        midiOut.sendMidi(NOTE_ON, buttonNote, value.compareTo(false));
    }
}
