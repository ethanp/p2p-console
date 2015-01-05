package p2p.console;

import p2p.Tracker;
import p2p.peer.Peer;

import java.util.Scanner;

/**
 * Ethan Petuchowski 1/4/15
 */
public class P2PConsole {
    static final String startString =
            "\nEnter 'peer' or 'tracker'\n" +
            "Note that there must be at least one tracker instance running" +
            "for this to work.\n";

    Scanner scanner;
    Tracker tracker;


    public static void main(String[] args) {
        new P2PConsole();
    }

    P2PConsole() {
        System.out.println(startString);
        scanner = new Scanner(System.in);
        boolean deployed = false;
        while (!deployed) {
            String type = scanner.nextLine();
            switch (type) {
                case "peer":
                    deployed = true;
                    startPeer();
                    break;
                case "tracker":
                    deployed = true;
                    startTracker();
                    break;
                default:
                    System.out.println("Command not recognized!\n"+startString);
                    break;
            }
        }
    }

    void startPeer() {
        System.out.println("starting peer...");
        new Peer(this);
    }

    void startTracker() {
        System.out.println("starting tracker...");
        tracker = new Tracker(this);
        tracker.start();
    }
}
