package net.klingt.akai;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

import java.util.stream.IntStream;

import static java.lang.String.format;
import static net.klingt.akai.MidiMix.BANK_LEFT;
import static net.klingt.akai.MidiMix.BANK_RIGHT;
import static net.klingt.akai.MidiMix.MUTE;
import static net.klingt.akai.MidiMix.REC_ARM;
import static net.klingt.akai.MidiMix.SOLO;
import static net.klingt.akai.MidiMix.valueOfIndex;
import static net.klingt.akai.MidimixExtensionDefinition.MODEL;
import static net.klingt.akai.MidimixExtensionDefinition.VENDOR;

public class MidimixExtension extends ControllerExtension {

    private static int NUM_TRACKS = 8;
    private MidiOut midiOut;
    private ControllerHost host;

    protected MidimixExtension(final MidimixExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    @Override
    public void init() {
        host = getHost();
        this.midiOut = host.getMidiOutPort(0);
        TrackBank trackBank = host.getProject()
                .getShownTopLevelTrackGroup()
                .createTrackBank(NUM_TRACKS, 0, 0, false);
        registerTrackBankPopup(trackBank);
        CursorRemoteControlsPage cursorRemoteControlsPage = host.createCursorTrack(0, 0).createCursorDevice().createCursorRemoteControlsPage(8);

        MidiHandler midiHandler = new MidiHandler(host.createTransport(),
                host.createMasterTrack(0),
                trackBank,
                cursorRemoteControlsPage
        );
        host.getMidiInPort(0).setMidiCallback(midiHandler);
        host.getMidiInPort(0).setSysexCallback(midiHandler::sysexReceived);

        registerObservers(trackBank);

        host.showPopupNotification("Midimix Initialized");
    }

    private void registerTrackBankPopup(TrackBank trackBank) {
        trackBank.scrollPosition().addValueObserver(this::showTrackBankPopup, 0);
    }

    private void showTrackBankPopup(int numberOfFirstTrack) {
        String msg = format("%s-%s: tracks %d-%d", VENDOR, MODEL, numberOfFirstTrack, numberOfFirstTrack + NUM_TRACKS);
        host.showPopupNotification(msg);
    }

    private void registerObservers(TrackBank trackBank) {
        IntStream.range(0, trackBank.getSizeOfBank())
                .filter(i -> trackBank.getChannel(i) != null)
                .forEach(i -> registerChannelObservers(trackBank.getChannel(i), i));

        trackBank.canScrollBackwards().addValueObserver(new ButtonObserver(BANK_LEFT, midiOut));
        trackBank.canScrollForwards().addValueObserver(new ButtonObserver(BANK_RIGHT, midiOut));
    }

    private void registerChannelObservers(Track channel, int index) {
        valueOfIndex(index, MUTE).ifPresent(buttonNote -> channel.getMute().addValueObserver(new ButtonObserver(buttonNote, midiOut)));
        valueOfIndex(index, SOLO).ifPresent(buttonNote -> channel.getSolo().addValueObserver(new ButtonObserver(buttonNote, midiOut)));
        valueOfIndex(index, REC_ARM).ifPresent(buttonNote -> channel.getArm().addValueObserver(new ButtonObserver(buttonNote, midiOut)));
    }

    @Override
    public void exit() {
        getHost().showPopupNotification("Midimix Exited");
    }

    @Override
    public void flush() {
    }
}
