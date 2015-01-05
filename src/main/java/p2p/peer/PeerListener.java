package p2p.peer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import p2p.Common;
import p2p.download.P2PDownload;
import p2p.file.Chunk;
import p2p.file.P2PFileMetadata;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.BitSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerListener extends Thread {

    static final Logger log = LogManager.getLogger(PeerListener.class.getName());
    int listeningPort;
    ServerSocket listener;
    Peer thisPeer;


    public PeerListener(Peer thisPeer) { this.thisPeer = thisPeer; }

    @Override
    public void run() {

        ExecutorService threadPool = Executors.newFixedThreadPool(50);

        // "0" finds a free port: stackoverflow.com/questions/2675362
        // and I was using that, but I just set my router to forward 3-3.5K to me
        try {
            listener = Common.socketPortInRange(Common.PORT_MIN, Common.PORT_MAX);
            listeningPort = listener.getLocalPort();

            thisPeer.startPeerConsole();
            while (true) {
                Socket conn = listener.accept();
                // TODO or maybe it should be submit(new P2PTransfer(conn))??
                threadPool.submit(new PeerServeTask(conn));
            }
        }
        catch (IOException e) {
            log.error("Peer not connected to Internet: can't contact tracker");
            e.printStackTrace();
            System.exit(Common.StatusCodes.NO_INTERNET.ordinal());
        }
    }

    class PeerServeTask extends Thread {

        P2PDownload xfer;
        Socket socket;
        ObjectOutputStream oos;
        ObjectInputStream ois;
        PeerServeTask(Socket socket) {
            this.socket = socket;
            try {
                oos = Common.objectOStream(socket);
                ois = Common.objectIStream(socket);
            }
            catch (IOException e) { e.printStackTrace(); }
        }

        @Override
        public void run() {
            try {
                String command = (String)ois.readObject();
                log.info("received command: "+command);
                switch (command) {
                    case Common.CHUNK_BITSET_CMD: sendAvlbtyToPeer(); break;
                    case Common.DL_CHUNK_CMD: sendChunkToPeer(); break;
                    default: throw new RuntimeException("UNKNOWN COMMAND: "+command);
                }
            }
            catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
            finally { try { socket.close(); } catch (IOException e) {} }
        }

        void sendAvlbtyToPeer() throws IOException, ClassNotFoundException {
            /* listen for which File Peer wants info about */
            P2PFileMetadata meta = (P2PFileMetadata) ois.readObject();

            /* create BitVector */
            // at this point this listener simply has ALL chunks, so we
            // don't even have to check, but that should change eventually
            try {
                thisPeer.getSeedingFile(meta);
                BitSet bitSet = new BitSet(meta.getNumChunks());
                for (int i = 0; i < meta.getNumChunks(); i++)
                    bitSet.set(i, true);
                oos.writeObject(bitSet);
            } catch (FileNotFoundException e) {
                oos.writeObject(Common.StatusCodes.FILE_NOT_FOUND);
            }
        }

        void sendChunkToPeer() throws IOException, ClassNotFoundException {

            /* receive which Chunk to send */
            int chunkIdx = ois.readInt();

            /* receive which File to send from */
            P2PFileMetadata meta = (P2PFileMetadata) ois.readObject();

            /* send the Chunk to the receiver */
            Chunk toSend = thisPeer.getSeedingFile(meta).getChunkNum(chunkIdx);
            oos.writeObject(toSend);
        }
    }
}
