package de.mikefox2k.taa;

import java.util.concurrent.TimeUnit;

public class Util {

    public static String formatTime(long seconds) {
        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (days * 24);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (days * 24 * 60) - (hours * 60);
        long secs = TimeUnit.SECONDS.toSeconds(seconds) - (days * 24 * 60 * 60) - (hours * 60 * 60) - (minutes * 60);

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(String.format("%02d:", days));
        }

        if (hours > 0 || days > 0) {
            sb.append(String.format("%02d:", hours));
        }

        sb.append(String.format("%02d:%02d", minutes, secs));

        return sb.toString();
    }
}
