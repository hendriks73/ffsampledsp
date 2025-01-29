package test_stuffs;

import com.tagtraum.ffsampledsp.FFAudioFileReader;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class Main {
    public static void main(String[] args) throws MalformedURLException {
        FFAudioFileReader reader = new FFAudioFileReader();

        String tempStr_1 = "file:///C:/Users/Super/Downloads/temp/temp%20Again/audio.ogg";
        URL url_1 = URI.create(tempStr_1).toURL();

        String tempStr_2 = "file://CAMERON-NAS/home/Songs/space%20space/audio.mp3";
        URL url_2 = URI.create(tempStr_2).toURL();

        String file_1 = "\\\\CAMERON-NAS\\home\\Songs\\space space\\audio.mp3"; // creates a "file:////" url

        String file_2 = "C:\\Users\\Super\\Downloads\\temp\\temp Again\\audio.ogg";

        try {
            System.out.println("URL 1");
            System.out.println("Result: " + reader.getAudioFileFormat(url_1));

            System.out.println("URL 2");
            System.out.println("Result: " + reader.getAudioFileFormat(url_2));

            System.out.println("File 1");
            System.out.println("Result: " + reader.getAudioFileFormat(new File(file_1)));

            System.out.println("File 2");
            System.out.println("Result: " + reader.getAudioFileFormat(new File(file_2)));
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }
}
