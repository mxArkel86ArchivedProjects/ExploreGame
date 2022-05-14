package main;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

public class SoundManager {
    Clip clip;

    public SoundManager() {
        try {
            clip = AudioSystem.getClip();
        } catch (LineUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void playAudio() {
        
    }
}
