package p2p.console;

import p2p.Common;
import p2p.exceptions.MetadataMismatchException;
import p2p.exceptions.SwarmNotFoundException;
import p2p.file.P2PFile;
import p2p.file.P2PFileMetadata;
import p2p.peer.Peer;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystemException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Ethan Petuchowski 1/5/15
 */
public class PeerConsole extends Thread {

    Peer peer;
    List<P2PFileMetadata> trkrLstg;
    Scanner scanner;

    public PeerConsole(Peer ownerPeer) {
        peer = ownerPeer;
        scanner = new Scanner(System.in);
    }

    static final String peerString =
            "\nCommands:\n" +
            "set tracker <trackerIP>\n" +
            "list\n" +
            "upload <filename>\n" +
            "download <listNo>\n" +
            "quit\n";

    @Override
    public void run() {
        while (true) {
            System.out.println(peerString);
            String cmd = scanner.nextLine();
            String[] parts = cmd.split(" ");
            if (quitCmd(cmd)) {
                System.out.println("exiting");
                System.exit(0);
            }
            else if (setTrackerCmd(cmd)) {
                if (parts.length != 3) {
                    System.out.println("wrong format, try again");
                    continue;
                }
                try {
                    InetSocketAddress trkrAddr = Common.addrFromString(parts[2]);
                    peer.setTracker(trkrAddr);
                }
                catch (UnknownHostException e) {
                    System.out.println("unknown host! please try again.");
                    continue;
                }
                System.out.println("tracker set successfully");
            }
            else if (peer.trkAddr == null) {
                System.out.println("actions require a tracker to be set");
            }
            else if (listCmd(cmd)) {
                if (parts.length != 1) {
                    System.out.println("wrong format, try again");
                    continue;
                }
                try {
                    trkrLstg = peer.listSavedTracker();
                }
                catch (ConnectException e) {
                    System.out.println("connection to tracker refused! try a different address.");
                    continue;
                }
                printListResult(trkrLstg);
            }
            else if (uploadCmd(cmd)) {
                if (parts.length != 2) {
                    System.out.println("wrong format, try again");
                    continue;
                }
                try {
                    Common.StatusCodes status = peer.shareFile(parts[1]);
                    switch (status) {
                        case ALREADY_LISTED:
                            System.out.println("You were already listed as a seeder");
                            break;
                        case ADDR_ADDED:
                            System.out.println("File already tracked, added to seeder list");
                            break;
                        case METADATA_MISMATCH:
                            System.out.println("Addition unsuccessful, filename already tracked " +
                                               "with differing associated metadata");
                            break;
                        case SWARM_CREATED:
                            System.out.println("Swarm created with you as seeder");
                            break;
                        case SERVER_EXCEPTION:
                            System.out.println("Failed due to exception on server");
                            break;
                        default:
                            throw new RuntimeException("Unknown status code response");
                    }
                }
                catch (FileNotFoundException | FileSystemException e) {
                    System.out.println(e.getMessage());
                }
            }
            else if (downloadCmd(cmd)) {
                if (parts.length != 2) {
                    System.out.println("wrong format, try again");
                    continue;
                }
                if (trkrLstg == null) {
                    System.out.println("can't download until after listing a tracker");
                    continue;
                }
                if (trkrLstg.isEmpty()) {
                    System.out.println("sorry, there are no listed files at this time");
                    continue;
                }
                int idx = Integer.parseInt(parts[1])-1;
                if (idx < 0 || idx >= trkrLstg.size()) {
                    System.out.println("index out of bounds," +
                                       "choose a value between 1 - "+trkrLstg.size());
                    continue;
                }
                P2PFileMetadata chosenMeta = trkrLstg.get(idx);
                System.out.printf("Downloading \"%s\"\n",
                                  chosenMeta.getFilename());

                /* download the file! */
                try {
                    P2PFile pFile = peer.downloadFromSavedTracker(chosenMeta);
                    pFile.writeToDiskInDir("downloads");
                }
                catch (InterruptedException e) {
                    System.out.println("Download failed: interrupted\n"+e.getMessage());
                    e.printStackTrace();
                }
                catch (ExecutionException e) {
                    System.out.print("Download failed: called get() on aborted Future"
                                     +e.getMessage()+e.getCause());
                    e.printStackTrace();
                }
                catch (TimeoutException e) {
                    System.out.println("Download failed: did not complete with 10x3 second spurts\n"
                                       +e.getMessage());
                    e.printStackTrace();
                }
                catch (SwarmNotFoundException e) {
                    System.out.println("Download failed: swarm not found on tracker\n"
                                       +e.getMessage());
                    e.printStackTrace();
                }
                catch (MetadataMismatchException e) {
                    System.out.println("Download failed: sent metadata did not match tracker's\n"
                                       +e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    boolean setTrackerCmd(String cmd) { return cmd.startsWith("set tracker"); }
    boolean listCmd(String cmd)       { return cmd.startsWith("list");        }
    boolean uploadCmd(String cmd)     { return cmd.startsWith("upload");      }
    boolean downloadCmd(String cmd)   { return cmd.startsWith("download");    }
    boolean quitCmd(String cmd)       { return cmd.startsWith("quit");        }
    boolean in(int v, int s, int l)   { return s <= v && v <= l;              }

    void printListResult(List<P2PFileMetadata> metaList) {
        if (metaList.isEmpty()) {
            System.out.println("It appears the tracker is empty");
        } else {
            System.out.println("The requested tracker lists the following files:");
            int i = 0;
            for (P2PFileMetadata meta : metaList) {
                System.out.printf("%d) \"%s\" %d B\n", ++i, meta.getFilename(), meta.getNumBytes());
            }
        }
    }
}
