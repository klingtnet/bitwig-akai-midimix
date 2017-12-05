package net.klingt.akai;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.ShortMidiDataReceivedCallback;
import com.bitwig.extension.controller.api.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static net.klingt.akai.MidiMix.*;

public class MidiHandler implements ShortMidiDataReceivedCallback {
    private final Transport transport;
    private final MasterTrack masterTrack;
    private final TrackBank trackBank;
    private final Map<Integer, Consumer<ShortMidiMessage>> noteHandlers;
    private final Map<Integer, Consumer<ShortMidiMessage>> ccHandlers;
    private final CursorRemoteControlsPage cursorRemoteControlsPage;

    MidiHandler(Transport transport, MasterTrack masterTrack, TrackBank trackBank, CursorRemoteControlsPage cursorRemoteControlsPage) {
        this.transport = transport;
        this.masterTrack = masterTrack;
        this.trackBank = trackBank;
        this.noteHandlers = registerNoteHandlers();
        this.ccHandlers = registerCCHandlers();
        this.cursorRemoteControlsPage = cursorRemoteControlsPage;
    }

    private Map<Integer, Consumer<ShortMidiMessage>> registerCCHandlers() {
        if (this.ccHandlers != null) {
            return Collections.emptyMap();
        }

        Map<Integer, Consumer<ShortMidiMessage>> ccHandlers = new HashMap<>();
        ccHandlers.put(MASTER_FADER, this::handleMasterFader);
        Arrays.stream(FADERS).forEach(key -> ccHandlers.put(key, this::handleFader));
        Arrays.stream(KNOBS).forEach(key -> ccHandlers.put(key, this::handleKnob));
        return ccHandlers;
    }

    private void handleKnob(ShortMidiMessage msg) {
        if (!msg.isControlChange()) {
            return;
        }

        indexOf(msg.getData1(), KNOBS_TOP_ROW)
                .filter(i -> i < cursorRemoteControlsPage.getParameterCount())
                .map(cursorRemoteControlsPage::getParameter)
                .ifPresent(param -> param.set(msg.getData2(), MIDI_RESOLUTION));
    }

    private void handleFader(ShortMidiMessage msg) {
        if (!msg.isControlChange()) {
            return;
        }

        indexOf(msg.getData1(), FADERS)
                .filter(i -> i < trackBank.getSizeOfBank())
                .map(trackBank::getChannel)
                .ifPresent(ch -> ch.getVolume().set(msg.getData2(), MIDI_RESOLUTION));
    }

    private void handleMasterFader(ShortMidiMessage msg) {
        if (!msg.isControlChange()) {
            return;
        }

        masterTrack.getVolume().set(msg.getData2(), MIDI_RESOLUTION);
    }

    private Map<Integer, Consumer<ShortMidiMessage>> registerNoteHandlers() {
        if (this.noteHandlers != null) {
            return Collections.emptyMap();
        }

        Map<Integer, Consumer<ShortMidiMessage>> noteHandlers = new HashMap<>();
        noteHandlers.put(BANK_LEFT, this::handleBankLeftRight);
        noteHandlers.put(BANK_RIGHT, this::handleBankLeftRight);
        Arrays.stream(REC_ARM).forEach(key -> noteHandlers.put(key, this::handleArm));
        Arrays.stream(SOLO).forEach(key -> noteHandlers.put(key, this::handleSolo));
        Arrays.stream(MUTE).forEach(key -> noteHandlers.put(key, this::handleMute));
        return noteHandlers;
    }

    @Override
    public void midiReceived(int statusByte, int data1, int data2) {
        ShortMidiMessage msg = new ShortMidiMessage(statusByte, data1, data2);
        handleCC(msg);
        handleNote(msg);
    }

    /**
     * handleCC calls registered handlers, if any, on an incoming MIDI control change.
     *
     * @param msg MIDI event
     */
    private void handleCC(ShortMidiMessage msg) {
        if (!msg.isControlChange()) {
            return;
        }
        if (!ccHandlers.containsKey(msg.getData1())) {
            return;
        }

        ccHandlers.get(msg.getData1()).accept(msg);
    }

    /**
     * handleNote calls registered handlers, if any, on an incoming MIDI note event.
     *
     * @param msg MIDI event
     */
    private void handleNote(ShortMidiMessage msg) {
        if (!msg.isNoteOn() && !msg.isNoteOff()) {
            return;
        }
        if (!noteHandlers.containsKey(msg.getData1())) {
            return;
        }

        noteHandlers.get(msg.getData1()).accept(msg);
    }

    private void handleBankLeftRight(ShortMidiMessage msg) {
        if (!msg.isNoteOn()) {
            return;
        }

        switch (msg.getData1()) {
            case BANK_LEFT:
                trackBank.scrollPageBackwards();
                return;
            case BANK_RIGHT:
                trackBank.scrollPageForwards();
        }
    }

    private void handleSolo(ShortMidiMessage msg) {
        if (!msg.isNoteOn() || isNotIn(msg.getData1(), SOLO)) {
            return;
        }

        indexOf(msg.getData1(), SOLO)
                .filter(i -> i < trackBank.getSizeOfBank())
                .map(trackBank::getChannel)
                .ifPresent(this::toggleSolo);
    }

    private void toggleSolo(Track track) {
        track.getSolo().toggle();
    }

    private void handleMute(ShortMidiMessage msg) {
        if (!msg.isNoteOn() || isNotIn(msg.getData1(), MUTE)) {
            return;
        }

        indexOf(msg.getData1(), MUTE)
                .filter(i -> i < trackBank.getSizeOfBank())
                .map(trackBank::getChannel)
                .ifPresent(this::toggleMute);
    }

    private void toggleMute(Track track) {
        track.getMute().toggle();
    }

    private void handleArm(ShortMidiMessage msg) {
        if (!msg.isNoteOn() || isNotIn(msg.getData1(), REC_ARM)) {
            return;
        }

        indexOf(msg.getData1(), REC_ARM)
                .filter(i -> i < trackBank.getSizeOfBank())
                .map(trackBank::getChannel)
                .ifPresent(this::toggleArm);
    }

    private void toggleArm(Track track) {
        track.getArm().toggle();
    }

    void sysexReceived(final String data) {
        // MMC Transport Controls:
        switch (data) {
            case "f07f7f0605f7":
                transport.rewind();
                break;
            case "f07f7f0604f7":
                transport.fastForward();
                break;
            case "f07f7f0601f7":
                transport.stop();
                break;
            case "f07f7f0602f7":
                transport.play();
                break;
            case "f07f7f0606f7":
                transport.record();
        }
    }
}
